const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const cors = require('cors');
const { v4: uuidv4 } = require('uuid');

const app = express();
app.use(cors());
app.use(express.json());

const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

/**
 * Room structure:
 * {
 *   hostId: string,
 *   users: Map<userId, { ws, name, imageUrl, online, lastSeen }>,
 *   permissions: JamPermissions,
 *   state: JamPlaybackState,
 *   queue: JamQueueItem[],
 *   votes: Map<queueId, Set<userId>>,
 *   contributorCursor: Map<userId, number>,  // next-index pointer per contributor
 *   recommendationsEnabled: boolean,
 *   recommendations: JamQueueItem[],
 *   tastes: Map<userId, string[]>,
 *   disconnectTimers: Map<userId, TimerId>,
 * }
 */
const sessions = new Map();

// ─── Helpers ────────────────────────────────────────────────────────────────

function generateRoomCode() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let code;
    do {
        code = '';
        for (let i = 0; i < 6; i++) code += chars[Math.floor(Math.random() * chars.length)];
    } while (sessions.has(code));
    return code;
}

function broadcast(roomId, message, excludeWs = null) {
    const session = sessions.get(roomId);
    if (!session) return;
    const data = JSON.stringify(message);
    for (const [, u] of session.users) {
        if (u.ws !== excludeWs && u.ws?.readyState === WebSocket.OPEN) {
            u.ws.send(data);
        }
    }
}

function sendTo(ws, message) {
    if (ws?.readyState === WebSocket.OPEN) ws.send(JSON.stringify(message));
}

// ─── Fair Queue Insertion (Contributor Rotation) ────────────────────────────
/**
 * Inserts a new song at the fairest position in the queue.
 *
 * Algorithm: Round-robin contributor rotation.
 * Given queue positions assigned to contributors, find the slot where the new
 * contributor "should" have their next song so that no contributor dominates.
 *
 * Example with A, A, B → inserting C → A, A, B, C
 * Next insert by A → A, A, B, C, A  (A gets position after all others had ≥1)
 */
function fairInsertPosition(queue, addedBy) {
    if (queue.length === 0) return 0;

    // Count how many songs each contributor already has in the queue (excluding recommendations)
    const counts = {};
    const manualQueue = queue.filter(item => !item.isRecommendation);
    for (const item of manualQueue) {
        counts[item.addedBy] = (counts[item.addedBy] || 0) + 1;
    }

    const myCount = counts[addedBy] || 0;

    // Find contributors who have MORE songs than us — our song should go before theirs
    // but after all contributors with the same or fewer songs.
    // Walk backward to find the insertion point.
    let insertAt = manualQueue.length; // default: end of manual queue

    // Count seen contributors from the end until we find a slot where the running
    // contribution count of all others >= our count.
    const seenCounts = {};
    for (let i = manualQueue.length - 1; i >= 0; i--) {
        const contributor = manualQueue[i].addedBy;
        if (contributor === addedBy) break; // our last song boundary
        seenCounts[contributor] = (seenCounts[contributor] || 0) + 1;
        const allOthersHaveMore = Object.values(seenCounts).every(c => c >= 1);
        if (allOthersHaveMore && myCount <= Math.min(...Object.values(counts).filter((_, idx) => Object.keys(counts)[idx] !== addedBy))) {
            insertAt = i + 1;
        }
    }

    // Simple fallback: if the new contributor hasn't added any songs yet, interleave them
    // at position = (their slot in round-robin order).
    if (myCount === 0) {
        const uniqueContributors = [...new Set(manualQueue.map(i => i.addedBy))];
        const numContributors = uniqueContributors.length;
        // The new song goes at the end of the first "round" 
        insertAt = Math.min(numContributors, manualQueue.length);
    }

    // Map manual-queue index back to full-queue index (skip recommendations at start)
    const firstRecIdx = queue.findIndex(i => i.isRecommendation);
    if (firstRecIdx !== -1) {
        return Math.min(insertAt, firstRecIdx);
    }
    return insertAt;
}

// ─── Recommendations: interleave all participants' taste lists ───────────────
function buildRecommendations(tastes, existingQueue) {
    if (!tastes || tastes.size === 0) return [];
    const existingIds = new Set(existingQueue.map(i => i.videoId));
    const lists = [...tastes.entries()];
    const maxLen = Math.max(...lists.map(([, l]) => l.length));
    const merged = [];
    const seen = new Set(existingIds);
    for (let i = 0; i < maxLen; i++) {
        for (const [userId, tracks] of lists) {
            if (i < tracks.length && !seen.has(tracks[i])) {
                seen.add(tracks[i]);
                merged.push({
                    queueId: uuidv4(),
                    videoId: tracks[i],
                    addedBy: userId,
                    isRecommendation: true,
                    addedTimestamp: Date.now(),
                    voteCount: 0,
                    orderWeight: 0,
                });
            }
        }
    }
    return merged;
}

function fullQueuePayload(session) {
    return [...session.queue, ...session.recommendations];
}

function getParticipantsList(session) {
    const list = [];
    for (const [userId, u] of session.users) {
        list.push({
            userId,
            name: u.name,
            imageUrl: u.imageUrl,
            online: u.online
        });
    }
    return list;
}

// ─── WebSocket Handler ───────────────────────────────────────────────────────

wss.on('connection', (ws) => {
    let currentRoomId = null;
    let currentUserId = null;

    ws.on('message', (raw) => {
        try {
            const msg = JSON.parse(raw.toString());

            switch (msg.type) {
                // ── CREATE_SESSION ──────────────────────────────────────────
                case 'CREATE_SESSION': {
                    currentUserId = msg.userId;
                    currentRoomId = generateRoomCode();
                    console.log(`[Jam] Room ${currentRoomId} created by ${currentUserId}`);

                    const usersMap = new Map();
                    usersMap.set(currentUserId, {
                        ws,
                        name: msg.name || 'Host',
                        imageUrl: msg.imageUrl || '',
                        online: true,
                        lastSeen: Date.now()
                    });

                    sessions.set(currentRoomId, {
                        hostId: currentUserId,
                        users: usersMap,
                        permissions: {
                            allowAddSongs: true,
                            allowRemoveSongs: true,
                            allowReorder: false,
                            allowPause: true,
                            allowSkip: true,
                            allowSeek: false,
                            allowVoting: true,
                        },
                        state: {
                            currentSongId: null,
                            playbackPositionMs: 0,
                            isPlaying: false,
                            shuffle: false,
                            repeatMode: 'OFF',
                            serverTimestampMs: Date.now(),
                        },
                        queue: [],
                        votes: new Map(),
                        recommendationsEnabled: true,
                        recommendations: [],
                        tastes: new Map(),
                        disconnectTimers: new Map(),
                    });

                    sendTo(ws, { type: 'SESSION_CREATED', roomId: currentRoomId });
                    break;
                }

                // ── JOIN_SESSION ────────────────────────────────────────────
                case 'JOIN_SESSION': {
                    const { roomId, userId, name, imageUrl } = msg;
                    if (!sessions.has(roomId)) {
                        sendTo(ws, { type: 'ERROR', message: 'Room not found' });
                        return;
                    }

                    currentRoomId = roomId;
                    currentUserId = userId;
                    const session = sessions.get(roomId);

                    // Cancel pending disconnect timer if reconnecting
                    if (session.disconnectTimers.has(userId)) {
                        clearTimeout(session.disconnectTimers.get(userId));
                        session.disconnectTimers.delete(userId);
                        console.log(`[Jam] ${userId} reconnected to ${roomId}`);
                    }

                    session.users.set(userId, { 
                        ws, 
                        name: name || 'Guest', 
                        imageUrl: imageUrl || '', 
                        online: true, 
                        lastSeen: Date.now() 
                    });

                    sendTo(ws, {
                        type: 'SESSION_JOINED',
                        roomId,
                        hostId: session.hostId,
                        state: session.state,
                        permissions: session.permissions,
                        queue: fullQueuePayload(session),
                        recommendationsEnabled: session.recommendationsEnabled,
                        participants: getParticipantsList(session),
                    });

                    broadcast(roomId, { 
                        type: 'PARTICIPANTS_UPDATED', 
                        participants: getParticipantsList(session) 
                    });
                    console.log(`[Jam] ${userId} joined room ${roomId}`);
                    break;
                }

                // ── UPDATE_PERMISSIONS ──────────────────────────────────────
                case 'UPDATE_PERMISSIONS': {
                    if (!currentRoomId) return;
                    const session = sessions.get(currentRoomId);
                    if (!session || session.hostId !== currentUserId) return;

                    session.permissions = { ...session.permissions, ...msg.permissions };
                    broadcast(currentRoomId, { type: 'PERMISSIONS_UPDATED', permissions: session.permissions });
                    break;
                }

                // ── SYNC_STATE ──────────────────────────────────────────────
                case 'SYNC_STATE': {
                    if (!currentRoomId) return;
                    const session = sessions.get(currentRoomId);
                    if (!session || session.hostId !== currentUserId) return;

                    session.state = {
                        ...session.state,
                        ...msg.state,
                        serverTimestampMs: Date.now(),
                    };
                    broadcast(currentRoomId, {
                        type: 'STATE_SYNC',
                        state: session.state,
                    }, ws);
                    break;
                }

                // ── COMMAND ─────────────────────────────────────────────────
                case 'COMMAND': {
                    if (!currentRoomId) return;
                    const session = sessions.get(currentRoomId);
                    if (!session) return;
                    const isHost = session.hostId === currentUserId;
                    const perms = session.permissions;

                    // ── Permission guards ────────────────────────────────────
                    if (!isHost) {
                        const cmd = msg.command;
                        if ((cmd === 'PLAY' || cmd === 'PAUSE') && !perms.allowPause) return;
                        if (cmd === 'SEEK' && !perms.allowSeek) return;
                        if (cmd === 'SKIP' || cmd === 'SKIP_TO') {
                            if (!perms.allowSkip) return;
                        }
                        if (cmd === 'ADD_TO_QUEUE' && !perms.allowAddSongs) return;
                        if (cmd === 'REMOVE_QUEUE_ITEM') {
                            if (!perms.allowRemoveSongs) return;
                            // Participants may only remove their own items
                            const item = session.queue.find(i => i.queueId === msg.payload?.queueId);
                            if (item && item.addedBy !== currentUserId) return;
                        }
                        if (cmd === 'MOVE_QUEUE_ITEM' && !perms.allowReorder) return;
                        if (cmd === 'VOTE' && !perms.allowVoting) return;
                    }

                    console.log(`[Jam] ${currentRoomId} | ${currentUserId}: ${msg.command}`);

                    // ── Stateful mutations on server ─────────────────────────
                    switch (msg.command) {
                        case 'ADD_TO_QUEUE': {
                            const { videoId, title, artist, thumbnailUrl, durationMs } = msg.payload || {};
                            if (!videoId) break;

                            const newItem = {
                                queueId: uuidv4(),
                                videoId,
                                title: title || '',
                                artist: artist || '',
                                thumbnailUrl: thumbnailUrl || null,
                                durationMs: durationMs || 0,
                                addedBy: currentUserId,
                                addedTimestamp: Date.now(),
                                voteCount: 0,
                                orderWeight: 0,
                                isRecommendation: false,
                            };

                            const pos = fairInsertPosition(session.queue, currentUserId);
                            session.queue.splice(pos, 0, newItem);

                            // Rebuild recommendations to exclude the newly queued song
                            if (session.recommendationsEnabled) {
                                session.recommendations = buildRecommendations(
                                    session.tastes,
                                    session.queue
                                );
                            }

                            broadcast(currentRoomId, {
                                type: 'QUEUE_UPDATED',
                                queue: fullQueuePayload(session),
                                reason: 'SONG_ADDED',
                                queueId: newItem.queueId,
                                addedBy: currentUserId,
                            });
                            break;
                        }

                        case 'REMOVE_QUEUE_ITEM': {
                            const { queueId } = msg.payload || {};
                            if (!queueId) break;

                            // Check in manual queue
                            let idx = session.queue.findIndex(i => i.queueId === queueId);
                            if (idx !== -1) {
                                session.queue.splice(idx, 1);
                                session.votes.delete(queueId);
                            } else {
                                // Check in recommendations
                                idx = session.recommendations.findIndex(i => i.queueId === queueId);
                                if (idx !== -1) session.recommendations.splice(idx, 1);
                            }

                            broadcast(currentRoomId, {
                                type: 'QUEUE_UPDATED',
                                queue: fullQueuePayload(session),
                                reason: 'SONG_REMOVED',
                                queueId,
                            });
                            break;
                        }

                        case 'MOVE_QUEUE_ITEM': {
                            const { queueId, toIndex } = msg.payload || {};
                            if (queueId === undefined || toIndex === undefined) break;

                            const fromIdx = session.queue.findIndex(i => i.queueId === queueId);
                            if (fromIdx === -1) break;

                            // Participants can only move their own items
                            if (!isHost && session.queue[fromIdx].addedBy !== currentUserId) break;

                            const [item] = session.queue.splice(fromIdx, 1);
                            const clampedTo = Math.max(0, Math.min(toIndex, session.queue.length));
                            session.queue.splice(clampedTo, 0, item);

                            broadcast(currentRoomId, {
                                type: 'QUEUE_UPDATED',
                                queue: fullQueuePayload(session),
                                reason: 'SONG_MOVED',
                                queueId,
                                toIndex: clampedTo,
                            });
                            break;
                        }

                        case 'VOTE': {
                            const { queueId } = msg.payload || {};
                            if (!queueId) break;

                            if (!session.votes.has(queueId)) session.votes.set(queueId, new Set());
                            const voters = session.votes.get(queueId);
                            if (voters.has(currentUserId)) break; // already voted

                            voters.add(currentUserId);
                            const item = session.queue.find(i => i.queueId === queueId)
                                || session.recommendations.find(i => i.queueId === queueId);
                            if (item) item.voteCount = voters.size;

                            broadcast(currentRoomId, {
                                type: 'VOTE_UPDATED',
                                queueId,
                                voteCount: voters.size,
                                voterIds: [...voters],
                            });
                            break;
                        }

                        case 'ENABLE_RECOMMENDATIONS': {
                            const enabled = msg.payload?.enabled ?? true;
                            session.recommendationsEnabled = enabled;
                            if (enabled) {
                                session.recommendations = buildRecommendations(session.tastes, session.queue);
                            } else {
                                session.recommendations = [];
                            }
                            broadcast(currentRoomId, {
                                type: 'RECOMMENDATIONS_UPDATED',
                                enabled,
                                recommendations: session.recommendations,
                            });
                            break;
                        }

                        case 'REFRESH_RECOMMENDATIONS': {
                            if (!isHost) break;
                            session.recommendations = buildRecommendations(session.tastes, session.queue);
                            broadcast(currentRoomId, {
                                type: 'RECOMMENDATIONS_UPDATED',
                                enabled: session.recommendationsEnabled,
                                recommendations: session.recommendations,
                            });
                            break;
                        }

                        case 'SHARE_TASTE': {
                            const tracks = msg.payload?.tracks || [];
                            session.tastes.set(currentUserId, tracks);
                            if (session.recommendationsEnabled) {
                                session.recommendations = buildRecommendations(session.tastes, session.queue);
                                broadcast(currentRoomId, {
                                    type: 'RECOMMENDATIONS_UPDATED',
                                    enabled: true,
                                    recommendations: session.recommendations,
                                });
                            }
                            // Relay taste to others for display
                            broadcast(currentRoomId, {
                                type: 'COMMAND',
                                command: 'SHARE_TASTE',
                                userId: currentUserId,
                                payload: msg.payload,
                            }, ws);
                            break;
                        }

                        // Pass-through commands: server relays to all clients
                        // so the host can apply them to the media player.
                        default: {
                            broadcast(currentRoomId, {
                                type: 'COMMAND',
                                command: msg.command,
                                payload: msg.payload,
                                userId: currentUserId,
                            }, ws);
                        }
                    }
                    break;
                }
            }
        } catch (e) {
            console.error('[Jam] Message error:', e);
        }
    });

    // ── Disconnect handler ────────────────────────────────────────────────────
    ws.on('close', () => {
        if (!currentRoomId) return;
        const session = sessions.get(currentRoomId);
        if (!session) return;

        const isHost = session.hostId === currentUserId;
        const GRACE_PERIOD_MS = 30_000; // 30-second reconnect window

        if (isHost) {
            // Find next host among guests (online ones first)
            const otherUsers = [...session.users.entries()].filter(([id]) => id !== currentUserId);
            if (otherUsers.length > 0) {
                // Grace period: wait 30 s before migrating host
                const timer = setTimeout(() => {
                    const s = sessions.get(currentRoomId);
                    if (!s || s.hostId !== currentUserId) return; // already reconnected or migrated
                    
                    const availableUsers = [...s.users.entries()].filter(([id]) => id !== currentUserId);
                    if (availableUsers.length === 0) return;
                    
                    const [newHostId] = availableUsers[0];
                    s.hostId = newHostId;
                    console.log(`[Jam] Host ${currentUserId} left. Promoted ${newHostId} in ${currentRoomId}`);
                    broadcast(currentRoomId, { type: 'HOST_TRANSFERRED', newHostId, oldHostId: currentUserId });
                }, GRACE_PERIOD_MS);
                session.disconnectTimers.set(currentUserId, timer);
                
                if (session.users.has(currentUserId)) {
                    session.users.get(currentUserId).online = false;
                }
                broadcast(currentRoomId, { 
                    type: 'PARTICIPANTS_UPDATED', 
                    participants: getParticipantsList(session) 
                });
            } else {
                console.log(`[Jam] Host ${currentUserId} left. Room ${currentRoomId} destroyed.`);
                broadcast(currentRoomId, { type: 'SESSION_ENDED' });
                sessions.delete(currentRoomId);
            }
        } else {
            // Guest: mark offline, give them 30 s to reconnect
            if (session.users.has(currentUserId)) {
                session.users.get(currentUserId).online = false;
                session.users.get(currentUserId).lastSeen = Date.now();
            }
            broadcast(currentRoomId, { 
                type: 'PARTICIPANTS_UPDATED', 
                participants: getParticipantsList(session) 
            });

            const timer = setTimeout(() => {
                const s = sessions.get(currentRoomId);
                if (!s) return;
                s.users.delete(currentUserId);
                s.disconnectTimers.delete(currentUserId);
                broadcast(currentRoomId, { 
                    type: 'PARTICIPANTS_UPDATED', 
                    participants: getParticipantsList(s) 
                });
                console.log(`[Jam] ${currentUserId} fully left room ${currentRoomId}`);
            }, GRACE_PERIOD_MS);
            session.disconnectTimers.set(currentUserId, timer);
        }
    });
});

// ─── REST ────────────────────────────────────────────────────────────────────

app.get('/health', (req, res) => res.json({
    status: 'ok',
    sessions: sessions.size,
    rooms: [...sessions.keys()],
}));

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => console.log(`JAM WebSocket Server running on port ${PORT}`));
