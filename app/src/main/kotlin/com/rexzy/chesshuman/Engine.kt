package com.rexzy.chesshuman

import fr.axl_lvy.stockfish.getStockfish
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Candidate(val uci: String, val cp: Int)

object HumanEngine {

    suspend fun topMoves(fen: String, depth: Int = 15, multipv: Int = 3): List<Candidate> {
        return try {
            val engine = getStockfish()
            engine.setPosition(fen = fen)
            val result = engine.search(depth = depth, multipv = multipv)
            // Asumsi: result.lines berisi daftar move + eval.
            // Kalau nama propertinya beda, compile akan gagal di sini — sesuaikan.
            result.lines.map { line ->
                Candidate(
                    uci = line.move,
                    cp = line.scoreCp ?: 0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun weightFor(diffCp: Int): Double = when {
        diffCp <= 10 -> 1.0
        diffCp <= 30 -> 0.55
        diffCp <= 60 -> 0.18
        diffCp <= 100 -> 0.05
        else -> 0.01
    }

    fun pickHuman(candidates: List<Candidate>): String? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first().uci

        val best = candidates.maxOf { it.cp }
        val weights = candidates.map { weightFor(best - it.cp) }
        val total = weights.sum()
        if (total <= 0.0) return candidates.first().uci

        var roll = Random.nextDouble() * total
        for (i in candidates.indices) {
            roll -= weights[i]
            if (roll <= 0.0) return candidates[i].uci
        }
        return candidates.first().uci
    }

    fun maybeControlledBlunder(candidates: List<Candidate>, evalCp: Int): String? {
        if (candidates.isEmpty()) return null
        if (evalCp < 300) return pickHuman(candidates)
        if (Random.nextDouble() > 0.05) return pickHuman(candidates)

        val best = candidates.maxOf { it.cp }
        val pool = candidates.filter { best - it.cp <= 80 }
        return pool.minByOrNull { it.cp }?.uci ?: candidates.first().uci
    }

    suspend fun humanDelay(baseMs: Long = 1200) {
        val jitter = Random.nextLong(-400, 1300)
        delay((baseMs + jitter).coerceIn(800, 2500))
    }
}
