package com.rexzy.chesshuman

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
                    drawPiece(
                        piece = piece,
                        center = Offset(c * cell + cell / 2f, r * cell + cell / 2f),
                        radius = cell * 0.38f
                    )
                }
            }
        }
    }
}

fun DrawScope.drawPiece(piece: Char, center: Offset, radius: Float) {
    val isWhite = piece.isUpperCase()
    val fill = if (isWhite) Color(0xFFFAFAFA) else Color(0xFF222222)
    val outline = if (isWhite) Color(0xFF222222) else Color(0xFF000000)

    when (piece.lowercaseChar()) {
        'p' -> {
            drawCircle(outline, radius * 1.02f, center)
            drawCircle(fill, radius * 0.86f, center)
        }
        'r' -> {
            drawRect(
                color = fill,
                topLeft = Offset(center.x - radius * 0.6f, center.y - radius * 0.8f),
                size = Size(radius * 1.2f, radius * 1.6f)
            )
            drawRect(
                color = outline,
                topLeft = Offset(center.x - radius * 0.6f, center.y - radius * 0.8f),
                size = Size(radius * 1.2f, radius * 0.2f)
            )
        }
        'n' -> {
            val path = Path().apply {
                moveTo(center.x - radius * 0.5f, center.y + radius * 0.8f)
                lineTo(center.x - radius * 0.5f, center.y - radius * 0.2f)
                lineTo(center.x, center.y - radius * 0.9f)
                lineTo(center.x + radius * 0.6f, center.y - radius * 0.4f)
                lineTo(center.x + radius * 0.6f, center.y + radius * 0.8f)
                close()
            }
            drawPath(path, fill)
            drawPath(path, outline, style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.15f))
        }
        'b' -> {
            val path = Path().apply {
                moveTo(center.x, center.y - radius * 0.9f)
                lineTo(center.x + radius * 0.55f, center.y + radius * 0.3f)
                lineTo(center.x + radius * 0.3f, center.y + radius * 0.8f)
                lineTo(center.x - radius * 0.3f, center.y + radius * 0.8f)
                lineTo(center.x - radius * 0.55f, center.y + radius * 0.3f)
                close()
            }
            drawPath(path, fill)
            drawPath(path, outline, style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.15f))
        }
        'q' -> {
            drawCircle(fill, radius * 0.85f, center)
            drawCircle(outline, radius * 0.85f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.15f))
        }
        'k' -> {
            drawRect(
                color = fill,
                topLeft = Offset(center.x - radius * 0.15f, center.y - radius * 0.9f),
                size = Size(radius * 0.3f, radius * 0.9f)
            )
            drawRect(
                color = fill,
                topLeft = Offset(center.x - radius * 0.45f, center.y - radius * 0.6f),
                size = Size(radius * 0.9f, radius * 0.3f)
            )
            drawCircle(fill, radius * 0.6f, Offset(center.x, center.y + radius * 0.3f))
            drawCircle(outline, radius * 0.6f, Offset(center.x, center.y + radius * 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.15f))
        }
    }
}
