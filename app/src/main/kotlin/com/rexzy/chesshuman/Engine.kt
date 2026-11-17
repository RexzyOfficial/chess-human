package com.rexzy.chesshuman

import fr.axl_lvy.stockfish_multiplatform.getStockfish

data class EngineHint(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val rank: Int // 0 = terbaik, 1 = kedua, 2 = ketiga
)

object HumanEngine {

    suspend fun findHints(fen: String): List<EngineHint> {
        val engine = try { getStockfish() } catch (e: Exception) { return emptyList() }
        val seen = mutableSetOf<String>()
        val hints = mutableListOf<EngineHint>()
        val depths = listOf(18, 14, 10)

        for ((rank, d) in depths.withIndex()) {
            try {
                engine.setPosition(fen)
                val r = engine.search(depth = d)
                val move = r.bestMove
                if (move.length >= 4 && seen.add(move)) {
                    val fromFile = move[0] - 'a'
                    val fromRank = move[1].digitToInt()
                    val toFile = move[2] - 'a'
                    val toRank = move[3].digitToInt()
                    hints += EngineHint(
                        fromRow = 8 - fromRank,
                        fromCol = fromFile,
                        toRow = 8 - toRank,
                        toCol = toFile,
                        rank = rank
                    )
                }
            } catch (e: Exception) {
            }
        }
        return hints.sortedBy { it.rank }
    }
}
