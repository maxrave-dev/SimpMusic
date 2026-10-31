package com.maxrave.simpmusic.recognition

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin

/**
 * Shazam-compatible audio fingerprint, ported from Metrolist (GPL-3.0), whose pure-Kotlin version
 * ports the vibra algorithm of SongRec (https://github.com/marin-m/SongRec). Metrolist's song
 * recognition is based on Aleksey Saenko's MusicRecognizer
 * (https://github.com/aleksey-saenko/MusicRecognizer).
 *
 * The numbers below are the algorithm's own, not tuning: change one and Shazam stops matching.
 */
internal object ShazamSignature {
    const val SAMPLE_RATE = 16_000

    private const val FFT_SIZE = 2048
    private const val FFT_OUTPUT_SIZE = FFT_SIZE / 2 + 1
    private const val MAX_PEAKS = 255
    private const val MAX_TIME_SECONDS = 12.0
    private const val RING_BUF_SIZE = 256

    // Bands 250-520, 520-1450, 1450-3500 and 3500-5500 Hz, in the order the signature lists them.
    private const val BAND_250_520 = 0
    private const val BAND_520_1450 = 1
    private const val BAND_1450_3500 = 2
    private const val BAND_3500_5500 = 3

    // vibra's precomputed Hanning window: 0.5 * (1 - cos(2π(i+1)/2049)).
    private val HANNING =
        DoubleArray(FFT_SIZE) { i ->
            0.5 * (1.0 - cos(2.0 * PI * (i + 1).toDouble() / 2049.0))
        }

    /** [pcm] is mono 16-bit audio at [sampleRate]; returns the `data:audio/vnd.shazam.sig` URI. */
    fun fromPcm(
        pcm: ShortArray,
        sampleRate: Int,
    ): String {
        val samples = resample(pcm, sampleRate)
        require(samples.isNotEmpty()) { "No audio to fingerprint" }
        return SignatureGeneratorState().process(samples)
    }

    // Linear interpolation, as Metrolist does: the fingerprint only looks at 250-5500 Hz.
    private fun resample(
        input: ShortArray,
        fromRate: Int,
    ): ShortArray {
        if (fromRate == SAMPLE_RATE) return input
        val ratio = SAMPLE_RATE.toDouble() / fromRate
        return ShortArray((input.size * ratio).toInt()) { i ->
            val srcPos = i / ratio
            val srcIndex = srcPos.toInt()
            val fraction = srcPos - srcIndex
            if (srcIndex + 1 < input.size) {
                (input[srcIndex] * (1.0 - fraction) + input[srcIndex + 1] * fraction).toInt().toShort()
            } else {
                input[srcIndex]
            }
        }
    }

    private class SignatureGeneratorState {
        private val samplesRing = IntArray(FFT_SIZE)
        private var samplesPos = 0

        private val fftOutputs = Array(RING_BUF_SIZE) { DoubleArray(FFT_OUTPUT_SIZE) }
        private var fftPos = 0

        private val spreadFfts = Array(RING_BUF_SIZE) { DoubleArray(FFT_OUTPUT_SIZE) }
        private var spreadPos = 0
        private var spreadNumWritten = 0

        private var numSamples = 0

        private val bandPeaks = Array(4) { mutableListOf<FrequencyPeak>() }
        private var totalPeaks = 0

        fun process(pcm: ShortArray): String {
            var offset = 0
            while (offset + 128 <= pcm.size) {
                // vibra stops only once BOTH the time and the peak budget are spent.
                val elapsedSec = numSamples.toDouble() / SAMPLE_RATE
                if (elapsedSec >= MAX_TIME_SECONDS && totalPeaks >= MAX_PEAKS) break

                numSamples += 128
                for (k in offset until offset + 128) {
                    samplesRing[samplesPos] = pcm[k].toInt()
                    samplesPos = (samplesPos + 1) % FFT_SIZE
                }
                doFFT()
                doPeakSpreading()
                if (spreadNumWritten >= 47) doPeakRecognition()
                offset += 128
            }
            return encodeSignature()
        }

        private fun doFFT() {
            // Oldest sample first.
            val windowed =
                DoubleArray(FFT_SIZE) { i ->
                    samplesRing[(samplesPos + i) % FFT_SIZE].toDouble() * HANNING[i]
                }
            computeRfft(windowed).copyInto(fftOutputs[fftPos])
            fftPos = (fftPos + 1) % RING_BUF_SIZE
        }

        private fun doPeakSpreading() {
            val lastFftIdx = (fftPos - 1 + RING_BUF_SIZE) % RING_BUF_SIZE
            val spread = fftOutputs[lastFftIdx].copyOf()

            // Frequency spreading: 3-point running max, forward pass.
            for (pos in 0 until FFT_OUTPUT_SIZE - 2) {
                spread[pos] = maxOf(spread[pos], spread[pos + 1], spread[pos + 2])
            }

            // Time spreading reaches only the older entries at -1, -3 and -6; the new entry keeps
            // frequency spreading alone, as in vibra.
            for (pos in 0 until FFT_OUTPUT_SIZE) {
                var maxVal = spread[pos]
                for (offset in intArrayOf(-1, -3, -6)) {
                    val idx = ((spreadPos + offset) % RING_BUF_SIZE + RING_BUF_SIZE) % RING_BUF_SIZE
                    val oldVal = spreadFfts[idx][pos]
                    if (oldVal > maxVal) maxVal = oldVal
                    spreadFfts[idx][pos] = maxVal
                }
            }

            spread.copyInto(spreadFfts[spreadPos])
            spreadPos = (spreadPos + 1) % RING_BUF_SIZE
            spreadNumWritten++
        }

        private fun doPeakRecognition() {
            val fftMinus46 = fftOutputs[(fftPos - 46 + RING_BUF_SIZE * 2) % RING_BUF_SIZE]
            val spreadMinus49 = spreadFfts[(spreadPos - 49 + RING_BUF_SIZE * 2) % RING_BUF_SIZE]

            val otherOffsets = intArrayOf(-53, -45, 165, 172, 179, 186, 193, 200, 214, 221, 228, 235, 242, 249)

            for (binPos in 10 until FFT_OUTPUT_SIZE - 8) {
                val fftVal = fftMinus46[binPos]
                if (fftVal < 1.0 / 64.0 || fftVal < spreadMinus49[binPos]) continue

                var maxNeighborSpread49 = 0.0
                for (neighborOffset in intArrayOf(-10, -7, -4, -3, 1, 2, 5, 8)) {
                    val v = spreadMinus49[binPos + neighborOffset]
                    if (v > maxNeighborSpread49) maxNeighborSpread49 = v
                }
                if (fftVal <= maxNeighborSpread49) continue

                var maxNeighborOther = maxNeighborSpread49
                for (otherOffset in otherOffsets) {
                    val spreadIdx = ((spreadPos + otherOffset) % RING_BUF_SIZE + RING_BUF_SIZE) % RING_BUF_SIZE
                    val v = spreadFfts[spreadIdx][binPos - 1]
                    if (v > maxNeighborOther) maxNeighborOther = v
                }
                if (fftVal <= maxNeighborOther) continue

                val fftNumber = spreadNumWritten - 46

                val peakMag = ln(max(1.0 / 64.0, fftVal)) * 1477.3 + 6144
                val peakMagBefore = ln(max(1.0 / 64.0, fftMinus46[binPos - 1])) * 1477.3 + 6144
                val peakMagAfter = ln(max(1.0 / 64.0, fftMinus46[binPos + 1])) * 1477.3 + 6144

                val peakVariation1 = peakMag * 2 - peakMagBefore - peakMagAfter
                val peakVariation2 = (peakMagAfter - peakMagBefore) * 32 / peakVariation1

                val correctedBin = binPos * 64.0 + peakVariation2
                val frequencyHz = correctedBin * (16000.0 / 2.0 / 1024.0 / 64.0)

                val band =
                    when {
                        frequencyHz < 250.0 -> continue
                        frequencyHz < 520.0 -> BAND_250_520
                        frequencyHz < 1450.0 -> BAND_520_1450
                        frequencyHz < 3500.0 -> BAND_1450_3500
                        frequencyHz <= 5500.0 -> BAND_3500_5500
                        else -> continue
                    }

                bandPeaks[band].add(
                    FrequencyPeak(
                        fftPassNumber = fftNumber,
                        peakMagnitude = peakMag.toInt(),
                        correctedPeakFrequencyBin = correctedBin.toInt(),
                    ),
                )
                totalPeaks++
            }
        }

        private fun encodeSignature(): String {
            val contentsStream = ByteArrayOutputStream()

            for (bandId in 0..3) {
                val peaks = bandPeaks[bandId]
                if (peaks.isEmpty()) continue

                val peakBuf = ByteArrayOutputStream()
                var prevFftPassNumber = 0

                for (peak in peaks) {
                    if (peak.fftPassNumber - prevFftPassNumber >= 255) {
                        // A jump too long for one byte is written as 0xFF plus the absolute position.
                        peakBuf.write(0xFF)
                        writeLittleEndian32(peakBuf, peak.fftPassNumber)
                        prevFftPassNumber = peak.fftPassNumber
                    }
                    peakBuf.write(peak.fftPassNumber - prevFftPassNumber)
                    writeLittleEndian16(peakBuf, peak.peakMagnitude)
                    writeLittleEndian16(peakBuf, peak.correctedPeakFrequencyBin)
                    prevFftPassNumber = peak.fftPassNumber
                }

                val peakBytes = peakBuf.toByteArray()
                writeLittleEndian32(contentsStream, 0x60030040 + bandId)
                writeLittleEndian32(contentsStream, peakBytes.size)
                contentsStream.write(peakBytes)
                repeat((4 - peakBytes.size % 4) % 4) { contentsStream.write(0) }
            }

            val contents = contentsStream.toByteArray()
            val headerBytes =
                ByteBuffer
                    .allocate(48)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .apply {
                        putInt(0xcafe2580.toInt()) // magic1
                        putInt(0) // crc32, filled in below
                        putInt(contents.size + 8) // size_minus_header
                        putInt(0x94119c00.toInt()) // magic2
                        putInt(0)
                        putInt(0)
                        putInt(0)
                        putInt(3 shl 27) // shifted_sample_rate_id: 16 kHz
                        putInt(0)
                        putInt(0)
                        putInt((numSamples + SAMPLE_RATE * 0.24).toInt())
                        putInt((15 shl 19) + 0x40000) // fixed_value
                    }.array()

            val full = ByteArrayOutputStream(56 + contents.size)
            full.write(headerBytes)
            writeLittleEndian32(full, 0x40000000)
            writeLittleEndian32(full, contents.size + 8)
            full.write(contents)
            val fullBytes = full.toByteArray()

            // CRC32 over everything after magic1 and the CRC field itself.
            val crc = CRC32()
            crc.update(fullBytes, 8, fullBytes.size - 8)
            val crc32Value = crc.value.toInt()
            fullBytes[4] = (crc32Value and 0xFF).toByte()
            fullBytes[5] = ((crc32Value shr 8) and 0xFF).toByte()
            fullBytes[6] = ((crc32Value shr 16) and 0xFF).toByte()
            fullBytes[7] = ((crc32Value shr 24) and 0xFF).toByte()

            return "data:audio/vnd.shazam.sig;base64," + Base64.encodeToString(fullBytes, Base64.NO_WRAP)
        }
    }

    private class FrequencyPeak(
        val fftPassNumber: Int,
        val peakMagnitude: Int,
        val correctedPeakFrequencyBin: Int,
    )

    private fun writeLittleEndian32(
        out: ByteArrayOutputStream,
        value: Int,
    ) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
        out.write((value ushr 16) and 0xFF)
        out.write((value ushr 24) and 0xFF)
    }

    private fun writeLittleEndian16(
        out: ByteArrayOutputStream,
        value: Int,
    ) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
    }

    /**
     * Real-input FFT of the 2048 windowed samples, iterative radix-2 Cooley-Tukey. Returns the 1025
     * magnitudes (re² + im²) / 2^17, floored at 1e-10, matching FFTW's r2c output in vibra.
     */
    private fun computeRfft(windowed: DoubleArray): DoubleArray {
        val n = windowed.size
        val re = windowed.copyOf()
        val im = DoubleArray(n)

        var j = 0
        for (i in 1 until n) {
            var bit = n ushr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit ushr 1
            }
            j = j xor bit
            if (i < j) {
                var tmp = re[i]
                re[i] = re[j]
                re[j] = tmp
                tmp = im[i]
                im[i] = im[j]
                im[j] = tmp
            }
        }

        var len = 2
        while (len <= n) {
            val halfLen = len ushr 1
            val ang = -PI / halfLen
            val wBaseRe = cos(ang)
            val wBaseIm = sin(ang)
            var i = 0
            while (i < n) {
                var wRe = 1.0
                var wIm = 0.0
                for (k in 0 until halfLen) {
                    val u = i + k
                    val v = u + halfLen
                    val evenRe = re[u]
                    val evenIm = im[u]
                    val oddRe = re[v] * wRe - im[v] * wIm
                    val oddIm = re[v] * wIm + im[v] * wRe
                    re[u] = evenRe + oddRe
                    im[u] = evenIm + oddIm
                    re[v] = evenRe - oddRe
                    im[v] = evenIm - oddIm
                    val newWRe = wRe * wBaseRe - wIm * wBaseIm
                    wIm = wRe * wBaseIm + wIm * wBaseRe
                    wRe = newWRe
                }
                i += len
            }
            len = len shl 1
        }

        val scaleFactor = 1.0 / (1 shl 17)
        return DoubleArray(FFT_OUTPUT_SIZE) { idx ->
            val mag = (re[idx] * re[idx] + im[idx] * im[idx]) * scaleFactor
            if (mag < 1e-10) 1e-10 else mag
        }
    }
}
