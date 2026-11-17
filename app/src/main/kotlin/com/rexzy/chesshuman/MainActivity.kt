package com.rexzy.chesshuman

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = Color(0xFF1A1A1A)) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(24.dp))
                        ChessBoardStatic(fen = START_FEN)
                    }
                }
            }
        }
    }
}

const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"

val PIECE_TO_CHAR = mapOf(
    'r' to "\u265C", 'n' to "\u265E", 'b' to "\u265D",
    'q' to "\u265B", 'k' to "\u265A", 'p' to "\u265F",
    'R' to "\u2656", 'N' to "\u2658", 'B' to "\u2657",
    'Q' to "\u2655", 'K' to "\u2654", 'P' to "\u2659"
)

fun fenToBoard(fen: String): Array<CharArray> {
    val board = Array(8) { CharArray(8) { ' ' } }
    val rows = fen.split("/")
    for (r in 0..7) {
        var c = 0
        for (ch in rows[r]) {
            if (ch.isDigit()) c += ch.digitToInt()
            else { if (c < 8) board[r][c] = ch; c++ }
        }
    }
    return board
}

@Composable
fun ChessBoardStatic(fen: String) {
    val board = fenToBoard(fen)
    val lightColor = Color(0xFFF0D9B5)
    val darkColor = Color(0xFFB58863)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color.Transparent)
    ) {
        val cell = size.width / 8f
        val paint = Paint().apply {
            textSize = cell * 0.78f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        for (r in 0..7) {
            for (c in 0..7) {
                val isLight = (r + c) % 2 == 0
                drawRect(
                    color = if (isLight) lightColor else darkColor,
                    topLeft = Offset(c * cell, r * cell),
                    size = Size(cell, cell)
                )
            }
        }

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != ' ') {
                    val glyph = PIECE_TO_CHAR[piece] ?: continue
                    val isWhite = piece.isUpperCase()

                    // Outline (stroke) supaya bentuk selalu terbaca, baik di kotak terang maupun gelap
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = cell * 0.04f
                    paint.color = if (isWhite) 0xFF000000.toInt() else 0xFF888888.toInt()

                    drawIntoCanvas { canvas ->
                        val native = canvas.nativeCanvas
                        val x = c * cell + cell / 2f
                        val y = r * cell + cell / 2f - (paint.ascent() + paint.descent()) / 2f
                        native.drawText(glyph, x, y, paint)
                    }

                    // Fill solid di atas outline
                    paint.style = Paint.Style.FILL
                    paint.color = if (isWhite) 0xFFFFFFFF.toInt() else 0xFF111111.toInt()

                    drawIntoCanvas { canvas ->
                        val native = canvas.nativeCanvas
                        val x = c * cell + cell / 2f
                        val y = r * cell + cell / 2f - (paint.ascent() + paint.descent()) / 2f
                        native.drawText(glyph, x, y, paint)
                    }
                }
            }
        }
    }
}
