---
name: simpmusic-release-changelog
description: Use when preparing a SimpMusic release, writing release notes or changelogs (changelogs/<version>, fastlane changelogs, GitHub release text), or checking which shipped fixes and features are missing from the GitHub Project board before a release.
---

# SimpMusic Release Changelog

A release changelog lists only work that has shipped, and every line links the issue or PR that
tracks it. Reconcile the board first: the changelog's links point at what the board holds.

## Files

| What | Where | Notes |
|---|---|---|
| Full changelog | `changelogs/<version>/en-US.md`, `changelogs/<version>/vi-VN.md` | No length limit |
| Store text (F-Droid / Play) | `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt` | ≤ 500 characters, written by the owner. Never cut the owner's items to make room; anything that does not fit goes into `changelogs/` |
| GitHub release | Release body | New / Improved / Fixed, plus a link to the full changelog |

## Step 1: reconcile Project #2

1. `git fetch` both repos and make sure neither branch is behind origin.
2. Inventory everything since the last release tag:
   - app: `git log <lastTag>..origin/dev --no-merges`
   - core: `git -C core log $(git ls-tree <lastTag> core | awk '{print $3}')..HEAD`
   - merged PRs in `maxrave-dev/SimpMusic` and `maxrave-dev/core`
   - board: `gh project item-list 2 --owner maxrave-dev --limit 500 --format json`
3. Report only **done** work the board is missing:
   - issues fixed by a shipped commit but still open
   - shipped changes that have no issue
   - merged PRs that are not on the board
   - board items whose shipped part is not reflected

   Leave new and untriaged issues out of this report.
4. Before calling an issue fixed, read its timeline (commit refs, linked PRs, owner comments) and the code. A fix that names the issue only in a code comment leaves no timeline link, so match the stack trace against the commit.
5. After the owner approves:
   - **Fixed issue:**
     1. `gh issue comment N --body "Fixed in <version> (<sha>)"`
     2. `gh issue edit N --milestone v<version>`
     3. `gh project item-add` to Project #2
     4. Set Status to Done

     Done closes the issue by itself. A `gh issue close --comment` run after that prints "already closed" and drops the comment, which is why the comment comes first.
   - **Shipped changes with no issue:** create ONE task per release, titled `Polish for <version>: …`. Give it the labels `Improvement` plus the topical ones, the milestone and Status Done, then close it. List each change with its commit.

Project IDs:
- project: `PVT_kwHOBsekuM4AoHIU`
- Status field: `PVTSSF_lAHOBsekuM4AoHIUzgfvIvs`
- Done option: `98236657`

Always pass `--repo maxrave-dev/SimpMusic`, because `origin` may point at a fork.

## Step 2: write `changelogs/<version>/`

Each file has this shape:

```markdown
# SimpMusic <version>

## New
- **<Feature>**: <what the user can do now>. ([#N](https://github.com/maxrave-dev/SimpMusic/issues/N))

## Improved
- **<Area>**: <what got better>. ([#N](https://github.com/maxrave-dev/SimpMusic/issues/N))

## Fixed
- <The symptom the user saw>. ([#N](https://github.com/maxrave-dev/SimpMusic/issues/N))
```

In `vi-VN.md` the headings are `## Mới`, `## Cải thiện` and `## Sửa lỗi`.

- **Every line ends with at least one link.**
  - issue: `https://github.com/maxrave-dev/SimpMusic/issues/N`
  - PR: `https://github.com/maxrave-dev/SimpMusic/pull/N`
  - core PR: `[core#N](https://github.com/maxrave-dev/core/pull/N)`
  - a change with no issue of its own: the release's Polish task
  - a new app language: `[Crowdin](https://crowdin.com/project/simpmusic)`

  Use full URLs. GitHub does not autolink `#N` inside repository files.
- **Order:** follow the owner's fastlane `<versionCode>.txt` first, then add the remaining items grouped by area.
- **Wording:** describe what changed for the user, not the code. Use one line per item and no hard wraps.
- **Language:** `vi-VN.md` addresses the reader as "bạn". Reserve "anh" for talking to the owner.
- **Parity:** both files carry the same items and the same links. Check with `grep -c '^- '` and a sorted diff of the link targets.

## Common mistakes

| Mistake | Fix |
|---|---|
| Listing new, untriaged issues in the "missing from the board" report | List done work only |
| One task per small shipped change | One Polish task per release |
| Changelog lines without a link | Link the issue or PR, or the Polish task when there is none |
| `gh issue close --comment` after setting Done | Comment first, then set Done |
| `#N` shorthand in the `.md` files | Full URLs |
| "anh" in `vi-VN.md` | "bạn" |
| Trimming the owner's fastlane items to fit new ones under 500 characters | Leave the fastlane text as written; the full list is `changelogs/` |
