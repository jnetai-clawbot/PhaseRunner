package com.jnetai.phaserunner

import android.graphics.*
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Bundle
import android.widget.ImageView
import android.widget.ScrollView
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*
import java.util.*
import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "PhaseRunner"
        const val CURRENT_VERSION = "1.0.0"
        const val GITHUB_REPO = "jnetai-clawbot/PhaseRunner"
    }

    private lateinit var gameView: GameView
    private lateinit var aboutButton: Button
    private lateinit var scoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF0A0A1A.toInt()
        window.navigationBarColor = 0xFF0A0A1A.toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0A1A.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        scoreText = TextView(this).apply {
            text = "Level 1"
            setTextColor(0xFF44FFCC.toInt())
            textSize = 18f
            setPadding(32, 32, 32, 8)
            typeface = Typeface.MONOSPACE
        }

        gameView = GameView(this, ::updateScore)

        val buttonBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(16, 8, 16, 48)
        }

        val restartBtn = Button(this).apply {
            text = "Restart"
            setBackgroundColor(0xFF1A2A3A.toInt())
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 14f
            minHeight = 0
            minimumHeight = 80
            setPadding(24, 12, 24, 12)
            setOnClickListener { gameView.restart() }
        }

        aboutButton = Button(this).apply {
            text = "About"
            setBackgroundColor(0xFF1A2A3A.toInt())
            setTextColor(0xFF44FFCC.toInt())
            textSize = 14f
            minHeight = 0
            minimumHeight = 80
            setPadding(24, 12, 24, 12)
            setOnClickListener { showAbout() }
        }

        buttonBar.addView(restartBtn)
        val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(32, 0) }
        buttonBar.addView(spacer)
        buttonBar.addView(aboutButton)

        root.addView(scoreText)
        root.addView(gameView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(buttonBar)
        setContentView(root)
    }

    private fun updateScore(level: Int) {
        runOnUiThread {
            scoreText.text = "Level $level"
        }
    }

    private fun showAbout() {
        val builder = AlertDialog.Builder(this, R.style.AboutDialogTheme)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 32)
            setBackgroundColor(0xFF151528.toInt())
        }

        layout.addView(TextView(this).apply {
            text = "Phase Runner"
            setTextColor(0xFF44FFCC.toInt())
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 8)
        })

        layout.addView(TextView(this).apply {
            text = "Made by jnetai.com"
            setTextColor(0xFF888899.toInt())
            textSize = 14f
            setPadding(0, 0, 0, 16)
        })

        layout.addView(TextView(this).apply {
            text = "Version $CURRENT_VERSION"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 16f
            setPadding(0, 0, 0, 24)
        })

        val checkBtn = Button(this).apply {
            text = "Check for Update"
            setBackgroundColor(0xFF006644.toInt())
            setTextColor(0xFF44FFCC.toInt())
            textSize = 15f
            minimumHeight = 96
            setPadding(32, 16, 32, 16)
            val btn = this
            setOnClickListener {
                btn.isEnabled = false
                btn.text = "Checking..."
                checkForUpdate { result ->
                    runOnUiThread {
                        btn.text = result
                        btn.isEnabled = true
                    }
                }
            }
        }
        layout.addView(checkBtn)

        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 24)
        })

        val shareBtn = Button(this).apply {
            text = "Share App"
            setBackgroundColor(0xFF234A6A.toInt())
            setTextColor(0xFF00CCFF.toInt())
            textSize = 15f
            minimumHeight = 96
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Phase Runner")
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message))
                }
                startActivity(Intent.createChooser(intent, "Share via"))
            }
        }
        layout.addView(shareBtn)

        val scrollView = ScrollView(this).apply {
            addView(layout)
        }

        builder.setView(scrollView)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun checkForUpdate(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val latestTag = json.getString("tag_name").removePrefix("v")

                if (latestTag != CURRENT_VERSION) {
                    callback("New version $latestTag available!")
                } else {
                    callback("You're up to date!")
                }
            } catch (e: Exception) {
                callback("Could not check updates: ${e.message}")
            }
        }
    }
}

class GameView(context: Context, private val scoreCallback: (Int) -> Unit) : View(context) {
    companion object {
        const val GRID_W = 12
        const val GRID_H = 12
        const val CELL_EMPTY = 0
        const val CELL_WALL = 1
        const val CELL_BUTTON = 2
        const val CELL_PLATFORM = 3
        const val END_TILE = 8
    }

    data class Position(val x: Int, val y: Int)
    data class Block(var pos: Position, var onTarget: Boolean = false)

    private var levelNum = 1
    private var worldGrid = Array(GRID_H) { IntArray(GRID_W) }
    private var realPos = Position(1, GRID_H - 2)
    private var ghostPos = Position(GRID_W - 2, GRID_H - 2)
    private var endPos = Position(GRID_W - 2, 1)
    private var blocks = mutableListOf<Block>()
    private var levelComplete = false
    private var swapCooldown = 0
    private var particles = mutableListOf<Particle>()

    private val bgPaint = Paint().apply { color = 0xFF0A0A1A.toInt(); style = Paint.Style.FILL }
    private val wallPaint = Paint().apply { color = 0xFF1A2A3A.toInt(); style = Paint.Style.FILL }
    private val platformPaint = Paint().apply { color = 0xFF1E3060.toInt(); style = Paint.Style.FILL }
    private val buttonPaint = Paint().apply { color = 0xFF664400.toInt(); style = Paint.Style.FILL }
    private val buttonActivePaint = Paint().apply { color = 0xFF44FFCC.toInt(); style = Paint.Style.FILL }
    private val endPaint = Paint().apply { color = 0xFF44FFCC.toInt(); style = Paint.Style.FILL; alpha = 160 }
    private val realPaint = Paint().apply { color = 0xFFFF8833.toInt(); style = Paint.Style.FILL }
    private val realGlowPaint = Paint().apply { color = 0x33FF8833.toInt(); style = Paint.Style.FILL }
    private val ghostPaint = Paint().apply { color = 0xFF4488FF.toInt(); style = Paint.Style.FILL }
    private val ghostGlowPaint = Paint().apply { color = 0x334488FF.toInt(); style = Paint.Style.FILL }
    private val blockPaint = Paint().apply { color = 0xFF886633.toInt(); style = Paint.Style.FILL }
    private val textPaint = Paint().apply {
        color = 0xFFFFFFFF.toInt(); textSize = 36f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val smallTextPaint = Paint().apply {
        color = 0xFF888888.toInt(); textSize = 24f; textAlign = Paint.Align.CENTER; typeface = Typeface.MONOSPACE
    }
    private val particlePaint = Paint().apply { style = Paint.Style.FILL }
    private val dividerPaint = Paint().apply { color = 0x3344FFCC.toInt(); style = Paint.Style.FILL; strokeWidth = 2f }
    private val swapBtnPaint = Paint().apply { color = 0xFF44FFCC.toInt(); style = Paint.Style.FILL; alpha = 200 }
    private val swapBtnTextPaint = Paint().apply {
        color = 0xFF0A0A1A.toInt(); textSize = 16f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }

    init {
        loadLevel(levelNum)
        post(gameLoop)
    }

    private fun loadLevel(num: Int) {
        blocks.clear()
        levelComplete = false
        swapCooldown = 0
        particles.clear()

        val levels = listOf(
            LevelData(
                walls = listOf(
                    0 to 3, 1 to 3, 2 to 3, 3 to 3, 5 to 6, 6 to 6, 7 to 6, 8 to 6,
                    0 to 8, 1 to 8, 2 to 8, 3 to 8, 4 to 8, 5 to 8, 6 to 8, 7 to 8, 8 to 8,
                    8 to 7, 8 to 5, 4 to 5, 0 to 4, 0 to 5, 0 to 6, 0 to 7,
                    4 to 0, 4 to 1, 4 to 2, 4 to 3, 4 to 4,
                    5 to 4, 6 to 4, 7 to 4, 8 to 4
                ),
                platforms = listOf(6 to 2),
                buttons = listOf(Position(8, 7)),
                blocks = listOf(Position(7, 3)),
                targets = listOf(Position(3, 7)),
                realStart = Position(1, 8),
                ghostStart = Position(9, 8),
                end = Position(10, 1)
            ),
            LevelData(
                walls = listOf(
                    0 to 3, 1 to 3, 2 to 3, 4 to 2, 5 to 2, 6 to 2,
                    0 to 5, 1 to 5, 4 to 5, 5 to 5, 8 to 5, 9 to 5,
                    3 to 6, 3 to 7, 7 to 6, 7 to 7, 10 to 6, 10 to 7,
                    0 to 9, 1 to 9, 2 to 9, 3 to 9, 4 to 9, 5 to 9,
                    6 to 9, 7 to 9, 8 to 9, 9 to 9, 10 to 9, 11 to 9,
                    11 to 2, 11 to 3, 11 to 4, 11 to 7, 11 to 8,
                    9 to 3, 10 to 3, 10 to 4
                ),
                platforms = listOf(2 to 1, 7 to 1),
                buttons = listOf(Position(3, 8), Position(8, 8)),
                blocks = listOf(Position(1, 7), Position(5, 3)),
                targets = listOf(Position(3, 8), Position(8, 8)),
                realStart = Position(1, 9),
                ghostStart = Position(10, 9),
                end = Position(5, 1)
            ),
            LevelData(
                walls = listOf(
                    2 to 1, 2 to 2, 2 to 3, 2 to 4,
                    6 to 1, 6 to 2, 6 to 3, 6 to 4,
                    4 to 6, 5 to 6, 6 to 6, 7 to 6, 8 to 6,
                    0 to 7, 1 to 7, 2 to 7, 8 to 7, 9 to 7, 10 to 7,
                    0 to 8, 1 to 8, 10 to 8, 11 to 8, 5 to 8, 6 to 8,
                    11 to 0, 11 to 1, 11 to 2, 11 to 4, 11 to 5, 11 to 6,
                    3 to 10, 4 to 10, 5 to 10, 6 to 10,
                    0 to 11, 1 to 11, 2 to 11, 3 to 11, 4 to 11, 5 to 11,
                    6 to 11, 7 to 11, 8 to 11, 9 to 11, 10 to 11, 11 to 11
                ),
                platforms = listOf(5 to 3, 8 to 2),
                buttons = listOf(Position(10, 6), Position(0, 6)),
                blocks = listOf(Position(3, 5), Position(9, 5)),
                targets = listOf(Position(10, 6), Position(0, 6)),
                realStart = Position(1, 11),
                ghostStart = Position(10, 11),
                end = Position(5, 1)
            ),
            LevelData(
                walls = listOf(
                    0 to 0, 1 to 0, 2 to 0, 3 to 0, 8 to 0, 9 to 0, 10 to 0, 11 to 0,
                    0 to 1, 11 to 1, 0 to 2, 11 to 2,
                    3 to 3, 4 to 3, 5 to 3, 6 to 3, 7 to 3, 8 to 3,
                    0 to 4, 0 to 5, 11 to 4, 11 to 5,
                    2 to 6, 3 to 6, 4 to 6, 7 to 6, 8 to 6, 9 to 6,
                    5 to 8, 5 to 9, 6 to 8, 6 to 9,
                    0 to 10, 0 to 11, 11 to 10, 11 to 11,
                    0 to 11, 1 to 11, 2 to 11, 3 to 11, 4 to 11,
                    7 to 11, 8 to 11, 9 to 11, 10 to 11, 11 to 11
                ),
                platforms = listOf(3 to 1, 8 to 1, 5 to 7),
                buttons = listOf(Position(2, 10), Position(9, 10)),
                blocks = listOf(Position(1, 9), Position(6, 6)),
                targets = listOf(Position(2, 10), Position(9, 10)),
                realStart = Position(1, 11),
                ghostStart = Position(10, 11),
                end = Position(6, 1)
            ),
            LevelData(
                walls = listOf(
                    1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0, 6 to 0, 7 to 0, 8 to 0, 9 to 0, 10 to 0,
                    0 to 1, 11 to 1, 0 to 2, 11 to 2, 0 to 3, 11 to 3, 0 to 4, 11 to 4,
                    4 to 2, 5 to 2, 6 to 2, 7 to 2,
                    2 to 4, 9 to 4,
                    0 to 6, 11 to 6,
                    3 to 7, 4 to 7, 5 to 7, 6 to 7, 7 to 7, 8 to 7,
                    1 to 9, 10 to 9,
                    0 to 11, 1 to 11, 2 to 11, 3 to 11, 4 to 11, 5 to 11,
                    6 to 11, 7 to 11, 8 to 11, 9 to 11, 10 to 11, 11 to 11,
                    5 to 8, 5 to 9, 5 to 10, 6 to 8, 6 to 9, 6 to 10
                ),
                platforms = listOf(3 to 3, 8 to 3),
                buttons = listOf(Position(4, 10), Position(7, 10)),
                blocks = listOf(Position(3, 6), Position(8, 6)),
                targets = listOf(Position(4, 10), Position(7, 10)),
                realStart = Position(1, 11),
                ghostStart = Position(10, 11),
                end = Position(5, 1)
            )
        )

        val index = (num - 1) % levels.size
        val lvl = levels[index]
        levelNum = num

        for (y in 0 until GRID_H) {
            for (x in 0 until GRID_W) {
                worldGrid[y][x] = CELL_EMPTY
            }
        }

        for ((wx, wy) in lvl.walls) {
            if (wy in 0 until GRID_H && wx in 0 until GRID_W)
                worldGrid[wy][wx] = CELL_WALL
        }
        for ((px, py) in lvl.platforms) {
            if (py in 0 until GRID_H && px in 0 until GRID_W)
                worldGrid[py][px] = CELL_PLATFORM
        }
        for (btn in lvl.buttons) {
            worldGrid[btn.y][btn.x] = CELL_BUTTON
        }

        realPos = lvl.realStart
        ghostPos = lvl.ghostStart
        endPos = lvl.end

        blocks.clear()
        for (i in lvl.blocks.indices) {
            val b = lvl.blocks[i]
            val t = lvl.targets.getOrNull(i)
            val onTarget = t != null && b.x == t.x && b.y == t.y
            blocks.add(Block(b, onTarget))
        }

        scoreCallback(levelNum)
    }

    private fun canMove(worldX: Int, worldY: Int): Boolean {
        if (worldX < 0 || worldX >= GRID_W || worldY < 0 || worldY >= GRID_H) return false
        val cell = worldGrid[worldY][worldX]
        if (cell == CELL_WALL) return false
        if (cell == CELL_BUTTON) return true
        for (blk in blocks) {
            if (blk.pos.x == worldX && blk.pos.y == worldY) return false
        }
        return true
    }

    private fun canGhostMove(worldX: Int, worldY: Int): Boolean {
        if (worldX < 0 || worldX >= GRID_W || worldY < 0 || worldY >= GRID_H) return false
        for (blk in blocks) {
            if (blk.pos.x == worldX && blk.pos.y == worldY) return false
        }
        return true
    }

    private fun pushBlock(block: Block, dx: Int, dy: Int): Boolean {
        val nx = block.pos.x + dx
        val ny = block.pos.y + dy
        if (nx < 0 || nx >= GRID_W || ny < 0 || ny >= GRID_H) return false
        if (worldGrid[ny][nx] == CELL_WALL) return false
        if (worldGrid[ny][nx] == CELL_BUTTON) {
            block.pos = Position(nx, ny)
            block.onTarget = true
            checkWin()
            return true
        }
        for (other in blocks) {
            if (other != block && other.pos.x == nx && other.pos.y == ny) return false
        }
        if (worldGrid[ny][nx] == CELL_PLATFORM) {
            block.pos = Position(nx, ny)
            block.onTarget = false
            return true
        }
        block.pos = Position(nx, ny)
        block.onTarget = false
        return true
    }

    private fun isBlockAt(x: Int, y: Int): Block? {
        return blocks.find { it.pos.x == x && it.pos.y == y }
    }

    private fun moveReal(dx: Int, dy: Int) {
        if (levelComplete) return
        val nx = realPos.x + dx
        val ny = realPos.y + dy

        val blockHere = isBlockAt(nx, ny)
        if (blockHere != null) {
            val pushed = pushBlock(blockHere, dx, dy)
            if (!pushed) return
        }

        if (!canMove(nx, ny)) return

        realPos = Position(nx, ny)

        if (realPos.x == endPos.x && realPos.y == endPos.y) {
            levelComplete = true
            spawnParticles(realPos)
            postDelayed({
                loadLevel(levelNum + 1)
                invalidate()
            }, 1500)
        }
    }

    private fun moveGhost(dx: Int, dy: Int) {
        if (levelComplete) return
        val nx = ghostPos.x + dx
        val ny = ghostPos.y + dy

        if (!canGhostMove(nx, ny)) return

        ghostPos = Position(nx, ny)

        if (worldGrid[ny][nx] == CELL_BUTTON) {
            activateButton(nx, ny)
        }

        if (ghostPos.x == endPos.x && ghostPos.y == endPos.y) {
            levelComplete = true
            spawnParticles(ghostPos)
            postDelayed({
                loadLevel(levelNum + 1)
                invalidate()
            }, 1500)
        }
    }

    private fun activateButton(bx: Int, by: Int) {
        for (y in 0 until GRID_H) {
            for (x in 0 until GRID_W) {
                if (worldGrid[y][x] == CELL_WALL && (x == bx - 1 || x == bx + 1 || y == by - 1 || y == by + 1)) {
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            val wx = bx + dx * 2
                            val wy = by + dy * 2
                            if (wx in 0 until GRID_W && wy in 0 until GRID_H) {
                                if (worldGrid[wy][wx] == CELL_WALL) {
                                    val hasBlock = blocks.any { it.pos.x == wx && it.pos.y == wy }
                                    val hasReal = realPos.x == wx && realPos.y == wy
                                    val hasGhost = ghostPos.x == wx && ghostPos.y == wy
                                    if (!hasBlock && !hasReal && !hasGhost) {
                                        worldGrid[wy][wx] = CELL_EMPTY
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        spawnButtonParticles(bx, by)
    }

    private fun checkWin() {
        var allOnTarget = true
        for (blk in blocks) {
            if (!blk.onTarget) {
                allOnTarget = false
                break
            }
        }
        if (allOnTarget) {
            levelComplete = true
            spawnParticles(realPos)
            postDelayed({
                loadLevel(levelNum + 1)
                invalidate()
            }, 1500)
        }
    }

    private fun doSwap() {
        val temp = realPos
        realPos = ghostPos
        ghostPos = temp
        swapCooldown = 15
        spawnSwapParticles()
    }

    private fun spawnParticles(pos: Position) {
        val rng = Random()
        for (i in 0 until 20) {
            val angle = rng.nextFloat() * PI.toFloat() * 2
            val speed = 2f + rng.nextFloat() * 4f
            particles.add(Particle(
                pos.x.toFloat(), pos.y.toFloat(),
                cos(angle) * speed, sin(angle) * speed,
                0xFF44FFCC.toInt() or (rng.nextInt(200) + 55 shl 24),
                30 + rng.nextInt(20)
            ))
        }
    }

    private fun spawnButtonParticles(x: Int, y: Int) {
        val rng = Random()
        for (i in 0 until 8) {
            val angle = rng.nextFloat() * PI.toFloat() * 2
            val speed = 1f + rng.nextFloat() * 2f
            particles.add(Particle(
                x.toFloat() + 0.5f, y.toFloat() + 0.5f,
                cos(angle) * speed, sin(angle) * speed,
                0xFF88FFCC.toInt() or (rng.nextInt(180) + 75 shl 24),
                20 + rng.nextInt(15)
            ))
        }
    }

    private fun spawnSwapParticles() {
        val rng = Random()
        for (i in 0 until 12) {
            val x = GRID_W / 2f
            val y = GRID_H / 2f
            val angle = rng.nextFloat() * PI.toFloat() * 2
            val speed = 3f + rng.nextFloat() * 4f
            particles.add(Particle(
                x, y, cos(angle) * speed, sin(angle) * speed,
                0xFF44FFCC.toInt() or (rng.nextInt(150) + 105 shl 24),
                25 + rng.nextInt(20)
            ))
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (levelComplete || event.action != MotionEvent.ACTION_DOWN) return true

        val halfW = width / 2f
        val gridSize = (height * 0.85f) / GRID_H
        val totalGridW = gridSize * GRID_W
        val offsetX = (halfW - totalGridW) / 2f

        val touchX = event.x
        val touchY = event.y

        val swapBtnCX = halfW
        val swapBtnCY = height * 0.93f
        val swapBtnR = gridSize * 0.6f
        val sdx = touchX - swapBtnCX
        val sdy = touchY - swapBtnCY
        if (sqrt(sdx * sdx + sdy * sdy) < swapBtnR && swapCooldown <= 0) {
            doSwap()
            invalidate()
            return true
        }

        if (touchX < halfW) {
            val lOffsetX = offsetX
            val gx = ((touchX - lOffsetX) / gridSize).toInt()
            val gy = (touchY / gridSize).toInt()
            val dx = gx - realPos.x
            val dy = gy - realPos.y
            if (abs(dx) + abs(dy) == 1) {
                moveReal(dx, dy)
                invalidate()
            }
            return true
        }

        val rOffsetX = halfW + offsetX
        val gx = ((touchX - rOffsetX) / gridSize).toInt()
        val gy = (touchY / gridSize).toInt()
        val dx = gx - ghostPos.x
        val dy = gy - ghostPos.y
        if (abs(dx) + abs(dy) == 1) {
            moveGhost(dx, dy)
            invalidate()
        }
        return true
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (levelComplete) {
                val iter = particles.iterator()
                while (iter.hasNext()) {
                    val p = iter.next()
                    p.life--
                    if (p.life <= 0) {
                        iter.remove()
                    }
                }
                invalidate()
                postDelayed(this, 33)
                return
            }
            if (swapCooldown > 0) swapCooldown--
            val iter = particles.iterator()
            while (iter.hasNext()) {
                val p = iter.next()
                p.x += p.vx
                p.y += p.vy
                p.life--
                if (p.life <= 0) iter.remove()
            }
            invalidate()
            postDelayed(this, 50)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        val halfW = viewW / 2f

        canvas.drawRect(0f, 0f, viewW, viewH, bgPaint)

        val gridSize = (viewH * 0.85f) / GRID_H
        val totalGridW = gridSize * GRID_W
        val offsetX = (halfW - totalGridW) / 2f

        val labelRealPaint = Paint().apply {
            color = 0xAAFF8833.toInt(); textSize = 14f; textAlign = Paint.Align.CENTER; typeface = Typeface.MONOSPACE
        }
        canvas.drawText("REAL", offsetX + totalGridW / 2f, gridSize - 4f, labelRealPaint)

        val labelGhostPaint = Paint().apply {
            color = 0xAA4488FF.toInt(); textSize = 14f; textAlign = Paint.Align.CENTER; typeface = Typeface.MONOSPACE
        }
        canvas.drawText("GHOST", halfW + offsetX + totalGridW / 2f, gridSize - 4f, labelGhostPaint)

        drawGrid(canvas, offsetX, 0f, gridSize)
        drawGrid(canvas, halfW + offsetX, 0f, gridSize)

        drawCharacter(canvas, offsetX + realPos.x * gridSize + gridSize / 2f,
            realPos.y * gridSize + gridSize / 2f, gridSize, realPaint, realGlowPaint)

        drawCharacter(canvas, halfW + offsetX + ghostPos.x * gridSize + gridSize / 2f,
            ghostPos.y * gridSize + gridSize / 2f, gridSize, ghostPaint, ghostGlowPaint)

        for (p in particles) {
            particlePaint.color = p.color
            val px = if (p.x < GRID_W) offsetX + p.x * gridSize + gridSize / 2f else halfW + offsetX + (p.x - GRID_W) * gridSize + gridSize / 2f
            val py = p.y * gridSize + gridSize / 2f
            val alphaRatio = p.life.toFloat() / 40f
            particlePaint.alpha = (alphaRatio * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(px, py, gridSize * 0.06f * alphaRatio, particlePaint)
        }

        canvas.drawLine(halfW, 0f, halfW, viewH, dividerPaint)

        val swapBtnCX = halfW
        val swapBtnCY = viewH * 0.93f
        val swapBtnR = gridSize * 0.55f
        canvas.drawCircle(swapBtnCX, swapBtnCY, swapBtnR, swapBtnPaint)
        canvas.drawText("SWAP", swapBtnCX, swapBtnCY + 6f, swapBtnTextPaint)

        if (levelComplete) {
            val overlayPaint = Paint().apply { color = 0x99000000.toInt(); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, viewW, viewH, overlayPaint)
            canvas.drawText("Level Complete!", viewW / 2f, viewH / 2f - 12, textPaint)
            canvas.drawText("Loading next...", viewW / 2f, viewH / 2f + 36, smallTextPaint)
        }
    }

    private fun drawGrid(canvas: Canvas, offX: Float, offY: Float, gs: Float) {
        for (y in 0 until GRID_H) {
            for (x in 0 until GRID_W) {
                val rx = offX + x * gs
                val ry = offY + y * gs
                val cell = worldGrid[y][x]

                when (cell) {
                    CELL_WALL -> {
                        val wp = Paint().apply { color = 0xFF1A2A3A.toInt(); style = Paint.Style.FILL }
                        canvas.drawRect(rx + 1, ry + 1, rx + gs - 1, ry + gs - 1, wp)
                        val borderPaint = Paint().apply {
                            color = 0x66222244.toInt(); style = Paint.Style.STROKE; strokeWidth = 1f
                        }
                        canvas.drawRect(rx + 1, ry + 1, rx + gs - 1, ry + gs - 1, borderPaint)
                    }
                    CELL_BUTTON -> {
                        val bp = Paint().apply {
                            color = if (blocks.any { it.pos.x == x && it.pos.y == y && it.onTarget }) 0xFF44FFCC.toInt() else 0xFF664400.toInt()
                            style = Paint.Style.FILL
                        }
                        canvas.drawRect(rx + 2, ry + 2, rx + gs - 2, ry + gs - 2, bp)
                        val innerPaint = Paint().apply {
                            color = if (blocks.any { it.pos.x == x && it.pos.y == y && it.onTarget }) 0xFF88FFDD.toInt() else 0xFF886600.toInt()
                            style = Paint.Style.FILL
                        }
                        canvas.drawRect(rx + gs * 0.25f, ry + gs * 0.25f, rx + gs * 0.75f, ry + gs * 0.75f, innerPaint)
                    }
                    CELL_PLATFORM -> {
                        val pp = Paint().apply { color = 0xFF1E3060.toInt(); style = Paint.Style.FILL }
                        canvas.drawRect(rx + 1, ry + 1, rx + gs - 1, ry + gs - 1, pp)
                    }
                }

                if (x == endPos.x && y == endPos.y) {
                    val ep = Paint().apply { color = 0xFF44FFCC.toInt(); style = Paint.Style.FILL; alpha = 160 }
                    canvas.drawRect(rx + 2, ry + 2, rx + gs - 2, ry + gs - 2, ep)
                    val eTextPaint = Paint().apply {
                        color = 0xFF0A0A1A.toInt(); textSize = gs * 0.35f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
                    }
                    canvas.drawText("E", rx + gs / 2f, ry + gs * 0.68f, eTextPaint)
                }

                for (blk in blocks) {
                    if (blk.pos.x == x && blk.pos.y == y) {
                        val bp = Paint().apply {
                            color = if (blk.onTarget) 0xFF44FFCC.toInt() else 0xFF886633.toInt()
                            style = Paint.Style.FILL
                        }
                        canvas.drawRoundRect(rx + gs * 0.1f, ry + gs * 0.1f, rx + gs * 0.9f, ry + gs * 0.9f,
                            gs * 0.15f, gs * 0.15f, bp)
                    }
                }
            }
        }
    }

    private fun drawCharacter(canvas: Canvas, cx: Float, cy: Float, gs: Float,
                              fillPaint: Paint, glowPaint: Paint) {
        val r = gs * 0.3f
        canvas.drawCircle(cx, cy, r + 4f, glowPaint)
        canvas.drawCircle(cx, cy, r, fillPaint)

        val eyePaint = Paint().apply { color = 0xFF0A0A1A.toInt(); style = Paint.Style.FILL }
        val eyeR = r * 0.3f
        canvas.drawCircle(cx - r * 0.3f, cy - r * 0.2f, eyeR, eyePaint)
        canvas.drawCircle(cx + r * 0.3f, cy - r * 0.2f, eyeR, eyePaint)
    }

    fun restart() {
        loadLevel(levelNum)
        invalidate()
    }
}

data class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var color: Int, var life: Int)

data class LevelData(
    val walls: List<Pair<Int, Int>>,
    val platforms: List<Pair<Int, Int>>,
    val buttons: List<GameView.Position>,
    val blocks: List<GameView.Position>,
    val targets: List<GameView.Position>,
    val realStart: GameView.Position,
    val ghostStart: GameView.Position,
    val end: GameView.Position
)
