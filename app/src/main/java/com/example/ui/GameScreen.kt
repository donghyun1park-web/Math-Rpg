package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class OptionColorSet(
    val bg: Color,
    val borderBottom: Color,
    val text: Color
)

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentProblem by viewModel.currentProblem.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2E9)) // Natural Tones Fawn Background
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (!uiState.isGameStarted) {
            StartScreen(
                highScore = uiState.highScore,
                onStartGame = { viewModel.startGame() }
            )
        } else {
            if (uiState.gameMode == "MAP") {
                ForestExploreScreen(
                    uiState = uiState,
                    onMove = { dx, dy -> viewModel.movePlayer(dx, dy) },
                    onBuyHeal = { viewModel.buyHealPotion() },
                    onBuyShield = { viewModel.buyShield() },
                    onBuySword = { viewModel.buySword() },
                    onExitGame = { viewModel.exitGame() }
                )
            } else {
                BattlePlayScreen(
                    uiState = uiState,
                    problem = currentProblem,
                    onAnswerSelected = { viewModel.submitAnswer(it) },
                    onBuyHeal = { viewModel.buyHealPotion() },
                    onBuyShield = { viewModel.buyShield() },
                    onBuySword = { viewModel.buySword() },
                    onRemoveEffect = { viewModel.removeFloatingEffect(it) },
                    onExitGame = { viewModel.exitGame() }
                )
            }
        }

        // Level Up Banner Overlay
        if (uiState.isLevelUpEffect) {
            LevelUpOverlay(
                level = uiState.level,
                onDismiss = { viewModel.dismissLevelUpEffect() }
            )
        }

        // Game Over Overlay
        if (uiState.isGameOver) {
            GameOverOverlay(
                score = uiState.score,
                highScore = uiState.highScore,
                defeatedMonsters = uiState.defeatedMonsters,
                levelReached = uiState.level,
                onRestart = { viewModel.startGame() },
                onExit = { viewModel.exitGame() }
            )
        }

        // Floating Sound Synth Toggler Control Button
        IconButton(
            onClick = { viewModel.toggleSoundMuted() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape)
                .testTag("sound_toggle_button")
        ) {
            Text(
                text = if (uiState.isSoundMuted) "🔇" else "🔊",
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun StartScreen(
    highScore: Int,
    onStartGame: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High Score Banner
        Box(
            modifier = Modifier
                .background(Color(0xFF8DAA78).copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp))
                .border(1.5.dp, Color(0xFF8DAA78), shape = RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star Icon",
                    tint = Color(0xFF8DAA78),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "최고 점수: ${highScore}점 🏆",
                    color = Color(0xFF5D5749),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Large Game Title
        Text(
            text = "수학 몬스터 배틀 ⚔️",
            color = Color(0xFF5D5749),
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 44.sp
        )

        Text(
            text = "두 자릿수 더하기 빼기 대모험!",
            color = Color(0xFF8DAA78),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Cute central dragon illustration
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(Color.White, shape = CircleShape)
                .border(1.5.dp, Color(0xFFE8E1D1), shape = CircleShape)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            MonsterDrawingCanvas(
                monsterType = MonsterType.DRAGON,
                isBoss = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Pulsing Start Button
        Button(
            onClick = onStartGame,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .testTag("start_game_button")
                .fillMaxWidth()
                .height(60.dp)
                .shadow(4.dp, shape = RoundedCornerShape(24.dp)),
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Icon",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "모험 시작하기 ⚔️",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    ),
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Tutorial Instructions Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE8E1D1), shape = RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "💡 게임 규칙 안내",
                    color = Color(0xFF5D5749),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))
                InstructionRow(icon = "🎯", text = "두 자릿수 수식에 알맞은 정답을 클릭하세요.")
                InstructionRow(icon = "👾", text = "문제를 맞히면 몬스터가 대미지를 받습니다.")
                InstructionRow(icon = "💔", text = "틀리면 하트를 잃게 되니 조심하세요!")
                InstructionRow(icon = "💰", text = "몬스터를 물리치고 번 골드로 물약과 방패를 사 전술을 펼치세요!")
            }
        }
    }
}

@Composable
fun InstructionRow(icon: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color(0xFF4A453E),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun BattlePlayScreen(
    uiState: GameUiState,
    problem: MathProblem?,
    onAnswerSelected: (Int) -> Unit,
    onBuyHeal: () -> Unit,
    onBuyShield: () -> Unit,
    onBuySword: () -> Unit,
    onRemoveEffect: (Long) -> Unit,
    onExitGame: () -> Unit
) {
    var showHelpPrinciple by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP: Dashboard Bar
        DashboardHeader(
            level = uiState.level,
            exp = uiState.exp,
            currentHp = uiState.currentHp,
            maxHp = uiState.maxHp,
            gold = uiState.gold,
            score = uiState.score,
            shields = uiState.activeShields,
            onExit = onExitGame
        )

        // MIDDLE 1: Battle Arena containing Monster and Effects
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (uiState.isBossFight) {
                            listOf(Color(0xFFF0E5E3), Color(0xFFE5D1D1))
                        } else {
                            listOf(Color(0xFFE8F0E3), Color(0xFFD4E4BC))
                        }
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .border(4.dp, Color.White, shape = RoundedCornerShape(32.dp))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Part: Hero status
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val heroShakeOffset = if (uiState.heroHitAnim == "HIT") {
                        val infiniteTransition = rememberInfiniteTransition()
                        infiniteTransition.animateFloat(
                            initialValue = -8f,
                            targetValue = 8f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(60, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        ).value.dp
                    } else 0.dp

                    val heroAttackSlide = if (uiState.heroAttackAnim == "SWORD") {
                        val infiniteTransition = rememberInfiniteTransition()
                        infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 45f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(180, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        ).value.dp
                    } else 0.dp

                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .offset(x = heroAttackSlide, y = heroShakeOffset)
                            .background(Color.White.copy(alpha = 0.5f), shape = CircleShape)
                            .border(1.5.dp, Color(0xFFD6CDBA), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        HeroDrawingCanvas(
                            hitState = uiState.heroHitAnim,
                            modifier = Modifier.size(76.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "수학 용사 🛡️",
                        color = Color(0xFF5D5749),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    
                    if (uiState.activeSwordBoost) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFF7043), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "검 버프 x2",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Center Battle Indicator / Versus
                Column(
                    modifier = Modifier.width(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (uiState.isBossFight) Color(0xFFFF7043) else Color(0xFF8C8471)
                    )
                }

                // Right Part: Monster display
                Column(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Monster Name & Level
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Lv.${uiState.monsterLevel}",
                            color = Color(0xFF5D5749),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = uiState.currentMonsterType.displayName,
                            color = Color(0xFF4A453E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bouncing & shaking Animation for Creature
                    // 1. Defeated collapsing transition
                    val monsterScale = if (uiState.monsterHitAnim == "DEAD") {
                        animateFloatAsState(targetValue = 0f, animationSpec = tween(1100)).value
                    } else 1f

                    val monsterRotation = if (uiState.monsterHitAnim == "DEAD") {
                        animateFloatAsState(targetValue = -90f, animationSpec = tween(1000)).value
                    } else 0f

                    val monsterAlpha = if (uiState.monsterHitAnim == "DEAD") {
                        animateFloatAsState(targetValue = 0f, animationSpec = tween(1100)).value
                    } else 1f

                    // 2. Hit shaking offset
                    val monsterShakeOffset = if (uiState.monsterHitAnim == "HIT") {
                        val infiniteTransition = rememberInfiniteTransition()
                        infiniteTransition.animateFloat(
                            initialValue = -12f,
                            targetValue = 12f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(50, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        ).value.dp
                    } else 0.dp

                    // 3. Normal float bouncer
                    val normalBounce = if (uiState.monsterHitAnim != "DEAD" && uiState.monsterHitAnim != "HIT") {
                        val infiniteTransition = rememberInfiniteTransition()
                        infiniteTransition.animateFloat(
                            initialValue = -5f,
                            targetValue = 5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            )
                        ).value.dp
                    } else 0.dp

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .graphicsLayer(
                                scaleX = monsterScale,
                                scaleY = monsterScale,
                                rotationZ = monsterRotation,
                                alpha = monsterAlpha
                            )
                            .offset(x = monsterShakeOffset, y = normalBounce),
                        contentAlignment = Alignment.Center
                    ) {
                        MonsterDrawingCanvas(
                            monsterType = uiState.currentMonsterType,
                            isBoss = uiState.isBossFight,
                            modifier = Modifier.size(96.dp)
                        )
                        
                        // Hit Red Overlay Glow
                        if (uiState.monsterHitAnim == "HIT") {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .background(Color(0xFFFF1744).copy(alpha = 0.3f), shape = CircleShape)
                            )
                        }
                    }

                    // Monster HP bar
                    Spacer(modifier = Modifier.height(8.dp))
                    val progressFraction = uiState.monsterCurrentHp.toFloat() / uiState.monsterMaxHp.toFloat()
                    Row(
                        modifier = Modifier.fillMaxWidth(0.95f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when {
                                progressFraction > 0.5f -> Color(0xFF8DAA78)   // Green
                                progressFraction > 0.25f -> Color(0xFFF2D7AC)  // Amber
                                else -> Color(0xFFFF8A65)                      // Red
                            },
                            trackColor = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${uiState.monsterCurrentHp}/${uiState.monsterMaxHp}",
                            color = Color(0xFF4A453E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Weapon/Spell Attack Animations Overlay Layers!
            if (uiState.heroAttackAnim == "FIRE") {
                val fireballMove = rememberInfiniteTransition().animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(animation = tween(400), repeatMode = RepeatMode.Restart)
                ).value

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val startX = size.width * 0.22f
                    val endX = size.width * 0.78f
                    val y = size.height * 0.52f
                    val curX = startX + (endX - startX) * fireballMove

                    // Draw Magic Fireball Flare
                    drawCircle(Color(0xFFE53935), radius = 15f, center = Offset(curX, y))
                    drawCircle(Color(0xFFFFB300), radius = 9f, center = Offset(curX, y))
                    drawCircle(Color.White, radius = 4f, center = Offset(curX, y))
                }
            } else if (uiState.heroAttackAnim == "SWORD") {
                val slashMove = rememberInfiniteTransition().animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(animation = tween(280), repeatMode = RepeatMode.Restart)
                ).value

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val startX = size.width * 0.65f
                    val startY = size.height * 0.25f
                    val sweepRadius = size.width * 0.15f

                    // Draw a glowing golden sword slash sweep curve
                    val path = Path().apply {
                        moveTo(startX, startY)
                        quadraticTo(
                            startX + sweepRadius * 0.5f, startY + sweepRadius * 0.8f,
                            startX + sweepRadius * slashMove, startY + sweepRadius * slashMove
                        )
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFFFD54F),
                        style = Stroke(width = 10f * (1f - slashMove), cap = StrokeCap.Round)
                    )
                }
            }

            // Layered floating effects overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                uiState.activeEffects.forEach { effect ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = if (effect.isMonsterSide) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        FloatingEffectLabel(
                            effect = effect,
                            onFinished = { onRemoveEffect(effect.id) }
                        )
                    }
                }
            }
        }

        // MIDDLE 2: Battle Action Bubble / Chat
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, Color(0xFFE8E1D1), shape = RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📢", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uiState.actionMessage,
                    color = Color(0xFF4A453E),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
            }
        }

        // MIDDLE 3: Active Math Question card
        MathProblemCard(problem = problem, isIncorrectFeedback = uiState.isIncorrectAnswerFeedback, feedbackMsg = uiState.feedbackMessage)

        // Toggle principle help card button
        if (problem != null) {
            Button(
                onClick = { showHelpPrinciple = !showHelpPrinciple },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showHelpPrinciple) Color(0xFFC2DEDC) else Color(0xFFD4E4BC)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(
                        width = 1.dp,
                        color = if (showHelpPrinciple) Color(0xFF93BDBA) else Color(0xFFB1C990),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (showHelpPrinciple) "닫기 ❌ (원리 도우미 숨기기)" else "💡 덧셈/뺄셈 원리 학습 네모칸 보기 🔍",
                    color = if (showHelpPrinciple) Color(0xFF3D5654) else Color(0xFF4E5C3D),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(
                visible = showHelpPrinciple,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                MathPrincipleCard(problem = problem)
            }
        }

        // BOTTOM: Multiple answers & Shop
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Options grid (re-implemented using standard rows/cols for 100% scroll compatibility)
            if (problem != null) {
                val optionColors = listOf(
                    OptionColorSet(Color(0xFFF2D7AC), Color(0xFFD9B67C), Color(0xFF6B502C)), // Yellowish-Tan
                    OptionColorSet(Color(0xFFD4E4BC), Color(0xFFB1C990), Color(0xFF4E5C3D)), // Muted Green
                    OptionColorSet(Color(0xFFC2DEDC), Color(0xFF93BDBA), Color(0xFF3D5654)), // Cool Seafoam
                    OptionColorSet(Color(0xFFE8D1D1), Color(0xFFD19E9E), Color(0xFF5C3D3D))  // Muted Rose
                )

                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val chunkedOptions = problem.options.chunked(2)
                    chunkedOptions.forEachIndexed { rowIndex, rowList ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowList.forEachIndexed { colIndex, option ->
                                val origIndex = rowIndex * 2 + colIndex
                                val colorSet = optionColors[origIndex % optionColors.size]
                                Box(
                                    modifier = Modifier
                                        .testTag("answer_option_$option")
                                        .weight(1f)
                                        .height(68.dp)
                                        .shadow(2.dp, shape = RoundedCornerShape(16.dp))
                                        .background(colorSet.borderBottom, shape = RoundedCornerShape(16.dp))
                                        .clickable { onAnswerSelected(option) }
                                        .padding(bottom = 4.dp) // Bevel bottom margin
                                        .background(colorSet.bg, shape = RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option.toString(),
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 28.sp
                                        ),
                                        color = colorSet.text
                                    )
                                }
                            }
                            if (rowList.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Quick Shop Footer
            ShopFooterSection(
                gold = uiState.gold,
                shields = uiState.activeShields,
                swordBoost = uiState.activeSwordBoost,
                onBuyHeal = onBuyHeal,
                onBuyShield = onBuyShield,
                onBuySword = onBuySword
            )
        }
    }
}

@Composable
fun DashboardHeader(
    level: Int,
    exp: Int,
    currentHp: Int,
    maxHp: Int,
    gold: Int,
    score: Int,
    shields: Int,
    onExit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player Profile with Exp
        Column(modifier = Modifier.weight(1.2f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "LV.$level",
                    color = Color(0xFF5D5749),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Exp small bar
                Column(modifier = Modifier.width(80.dp)) {
                    LinearProgressIndicator(
                        progress = { exp / 100f },
                        modifier = Modifier
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF8DAA78), // Organic Sage Exp
                        trackColor = Color(0xFFD6CDBA).copy(alpha = 0.5f)
                    )
                    Text(
                        text = "XP $exp/100",
                        color = Color(0xFF8C8471),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Hearts Bar
        Row(
            modifier = Modifier.weight(1.4f),
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 1..maxHp) {
                Icon(
                    imageVector = if (i <= currentHp) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Hearts HP",
                    tint = if (i <= currentHp) Color(0xFFFF7043) else Color(0xFFD6CDBA).copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
            if (shields > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF8DAA78), shape = CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🛡️$shields",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Score 
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${score}점 🏆",
                color = Color(0xFF5D5749),
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Text(
                text = "💰 $gold",
                color = Color(0xFF8C8471),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        // Exit button
        IconButton(
            onClick = onExit,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit to Menu",
                tint = Color(0xFF5D5749)
            )
        }
    }
}

@Composable
fun MathProblemCard(
    problem: MathProblem?,
    isIncorrectFeedback: Boolean,
    feedbackMsg: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isIncorrectFeedback) Color(0xFFE8D1D1) else Color.White
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                2.dp,
                if (isIncorrectFeedback) Color(0xFFD19E9E) else Color(0xFFE8E1D1),
                shape = RoundedCornerShape(28.dp)
            )
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (problem != null) {
                Text(
                    text = "정답을 맞춰 공격하세요!",
                    color = Color(0xFF8C8471),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = problem.num1.toString(),
                        color = Color(0xFF5D5749),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = problem.operator,
                        color = Color(0xFFA7C091),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = problem.num2.toString(),
                        color = Color(0xFF5D5749),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "=",
                        color = Color(0xFFA7C091),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFFF7F2E9), shape = RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color(0xFFE8E1D1)), shape = RoundedCornerShape(8.dp))
                            .padding(bottom = 3.dp)
                            .background(Color(0xFFA7C091), shape = RoundedCornerShape(8.dp))
                            .padding(bottom = 3.dp)
                            .background(Color(0xFFF7F2E9), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "?",
                            color = Color(0xFFA7C091),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            } else {
                Text(
                    text = "다음 수식을 소환 중...",
                    color = Color(0xFF8C8471),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isIncorrectFeedback && feedbackMsg.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = feedbackMsg,
                    color = Color(0xFF5C3D3D),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MathPrincipleCard(
    problem: MathProblem?
) {
    if (problem == null) return

    var revealedCarry by remember(problem) { mutableStateOf(false) }
    var revealedBorrow by remember(problem) { mutableStateOf(false) }
    var revealedOnes by remember(problem) { mutableStateOf(false) }
    var revealedTens by remember(problem) { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, Color(0xFFE8E1D1), shape = RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🔎 수학 원리 학습 도우미",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D5749)
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF8DAA78).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (problem.isCarry) "올림 있음 📈" else if (problem.isBorrow) "내림(빌려오기) 있음 📉" else "기본 연산 ✏️",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4E5C3D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val explanationText = when {
                problem.isCarry -> {
                    "일의 자리의 수 (${problem.ones1} + ${problem.num2} = ${problem.ones1 + problem.num2})가 10을 넘어서 십의 자리로 1십(올림 1)을 올려줍니다."
                }
                problem.isBorrow -> {
                    "일의 자리의 수 (${problem.ones1})가 빼는 수 (${problem.num2})보다 작아서, 십의 자리에서 10을 빌려와야 해요!"
                }
                else -> {
                    "일의 자리는 일의 자리끼리, 십의 자리는 십의 자리끼리 바로 계산해 보아요!"
                }
            }

            Text(
                text = explanationText,
                fontSize = 12.sp,
                color = Color(0xFF8C8471),
                lineHeight = 16.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .background(Color(0xFFF7F2E9), shape = RoundedCornerShape(16.dp))
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(text = "십", fontSize = 11.sp, color = Color(0xFF8C8471), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = problem.operator, fontSize = 18.sp, color = Color(0xFFA7C091), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "=", fontSize = 18.sp, color = Color(0xFFA7C091), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp)
                ) {
                    if (problem.isCarry) {
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 28.dp)
                                .background(
                                    color = if (revealedCarry) Color(0xFFD4E4BC) else Color.White,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (revealedCarry) Color(0xFFB1C990) else Color(0xFFD6CDBA),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { revealedCarry = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (revealedCarry) "올림 1" else "?",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (revealedCarry) Color(0xFF4E5C3D) else Color(0xFF8C8471)
                            )
                        }
                    } else if (problem.isBorrow) {
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 28.dp)
                                .background(
                                    color = if (revealedBorrow) Color(0xFFE8D1D1) else Color.White,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (revealedBorrow) Color(0xFFD19E9E) else Color(0xFFD6CDBA),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { revealedBorrow = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (revealedBorrow) "빌려줌" else "빌림 ?",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (revealedBorrow) Color(0xFF5C3D3D) else Color(0xFF8C8471)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = problem.tens1.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (problem.isBorrow && revealedBorrow) Color(0xFF8C8471) else Color(0xFF5D5749),
                            style = if (problem.isBorrow && revealedBorrow) {
                                androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                            } else {
                                androidx.compose.ui.text.TextStyle()
                            }
                        )
                        if (problem.isBorrow && revealedBorrow) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "➔ ${(problem.tens1 - 1)}",
                                fontSize = 14.sp,
                                color = Color(0xFFFF7043),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "0",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFD6CDBA)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFE8E1D1), thickness = 2.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 36.dp)
                            .background(
                                color = if (revealedTens) Color(0xFFF2D7AC) else Color.White,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.5.dp,
                                if (revealedTens) Color(0xFFD9B67C) else Color(0xFFE8E1D1),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { revealedTens = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (revealedTens) problem.tensResult.toString() else "?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (revealedTens) Color(0xFF6B502C) else Color(0xFFA7C091)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp)
                ) {
                    if (problem.isBorrow) {
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 28.dp)
                                .background(
                                    color = if (revealedBorrow) Color(0xFFC2DEDC) else Color.White,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (revealedBorrow) Color(0xFF93BDBA) else Color(0xFFD6CDBA),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { revealedBorrow = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (revealedBorrow) "+10" else "?",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (revealedBorrow) Color(0xFF3D5654) else Color(0xFF8C8471)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = problem.ones1.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D5749)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = problem.num2.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D5749)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFE8E1D1), thickness = 2.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 36.dp)
                            .background(
                                color = if (revealedOnes) Color(0xFFF2D7AC) else Color.White,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.5.dp,
                                if (revealedOnes) Color(0xFFD9B67C) else Color(0xFFE8E1D1),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { revealedOnes = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (revealedOnes) problem.onesResult.toString() else "?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (revealedOnes) Color(0xFF6B502C) else Color(0xFFA7C091)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "💡 흰색 네모칸(?)들을 터치하면 숨겨진 받아올림(올림)과 받아내림(빌림), 각 자리 합계를 확인할 수 있어요!",
                fontSize = 10.sp,
                color = Color(0xFF8C8471),
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ShopFooterSection(
    gold: Int,
    shields: Int,
    swordBoost: Boolean,
    onBuyHeal: () -> Unit,
    onBuyShield: () -> Unit,
    onBuySword: () -> Unit
) {
    Text(
        text = "🎒 상점 (아이템 골라서 구매하기)",
        color = Color(0xFF8C8471),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Heal item
        Button(
            onClick = onBuyHeal,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4E4BC)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .testTag("buy_heal_button")
                .weight(1f)
                .height(48.dp)
                .border(1.dp, Color(0xFFB1C990), shape = RoundedCornerShape(12.dp)),
            contentPadding = PaddingValues(2.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🧪 HP물약 💖+2",
                    color = Color(0xFF4E5C3D),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "💰 15 골드",
                    color = Color(0xFF6B502C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // Shield item
        Button(
            onClick = onBuyShield,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2DEDC)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .testTag("buy_shield_button")
                .weight(1f)
                .height(48.dp)
                .border(1.dp, Color(0xFF93BDBA), shape = RoundedCornerShape(12.dp)),
            contentPadding = PaddingValues(2.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🛡️ 든든방패 [${shields}개]",
                    color = Color(0xFF3D5654),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "💰 20 골드",
                    color = Color(0xFF6B502C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // Sword item
        Button(
            onClick = onBuySword,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (swordBoost) Color(0xFFFFCC80) else Color(0xFFF2D7AC)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .testTag("buy_sword_button")
                .weight(1f)
                .height(48.dp)
                .border(1.5.dp, if (swordBoost) Color(0xFFFF7043) else Color(0xFFD9B67C), shape = RoundedCornerShape(12.dp)),
            contentPadding = PaddingValues(2.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (swordBoost) "⚡ 소검 장착중!" else "⚔️ 전설의 검 x2",
                    color = Color(0xFF6B502C),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "💰 25 골드",
                    color = Color(0xFF6B502C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun FloatingEffectLabel(
    effect: ActiveEffect,
    onFinished: () -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        transitionState.targetState = true
        delay(1000)
        transitionState.targetState = false
        delay(200)
        onFinished()
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (transitionState.currentState && transitionState.targetState) 1f else 0f,
        animationSpec = tween(durationMillis = 200)
    )

    val animatedYOffset by animateDpAsState(
        targetValue = if (transitionState.targetState) (-50).dp else 0.dp,
        animationSpec = tween(durationMillis = 1000)
    )

    Text(
        text = effect.text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
        color = Color(android.graphics.Color.parseColor(effect.colorHex)),
        modifier = Modifier
            .offset(y = animatedYOffset)
            .alpha(animatedAlpha)
            .background(Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun LevelUpOverlay(
    level: Int,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2E9)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(2.dp, Color(0xFFA7C091), shape = RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 레벨 업! 🎉",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF7043)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "현재 레벨: $level",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D5749)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "용사의 체력이 가득 회복되고,\n최대 하트 개수가 한 개 증가했습니다! ❤️",
                    textAlign = TextAlign.Center,
                    color = Color(0xFF4A453E),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8DAA78)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "계속 모험하기 ⚔️",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    score: Int,
    highScore: Int,
    defeatedMonsters: Int,
    levelReached: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2E9)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(2.dp, Color(0xFFFF7043), shape = RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💀 모험 종료 💀",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF7043)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "용사가 지쳤어요! 다시 복습하고 도전해봐요.",
                    fontSize = 13.sp,
                    color = Color(0xFF8C8471),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.weight(1f).border(1.dp, Color(0xFFE8E1D1), shape = RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "달성 레벨", color = Color(0xFF8C8471), fontSize = 11.sp)
                            Text(text = "Lv.$levelReached", color = Color(0xFF5D5749), fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.weight(1f).border(1.dp, Color(0xFFE8E1D1), shape = RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "처치한 수", color = Color(0xFF8C8471), fontSize = 11.sp)
                            Text(text = "${defeatedMonsters}마리", color = Color(0xFF5D5749), fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE8E1D1), shape = RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "최종 점수: $score 점", color = Color(0xFFFF7043), fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "역대 최고 점수: $highScore 점", color = Color(0xFF8C8471), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8DAA78)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .testTag("restart_game_button")
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "다시 모험 떠나기 ⚔️",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onExit,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5D5749)),
                    border = BorderStroke(1.dp, Color(0xFF8C8471)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "메인 화면으로", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun MonsterDrawingCanvas(
    monsterType: MonsterType,
    isBoss: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f

        when (monsterType) {
            MonsterType.SLIME -> {
                // Soft Green Slime
                val slimeColor = Color(0xFF4CAF50)
                val blushColor = Color(0xFFFF8A80)
                
                // Draw Slime main dome shape
                val p = Path()
                p.moveTo(cx - width * 0.4f, cy + height * 0.3f)
                p.quadraticTo(cx - width * 0.45f, cy + height * 0.1f, cx - width * 0.35f, cy - height * 0.1f)
                p.quadraticTo(cx, cy - height * 0.4f, cx + width * 0.35f, cy - height * 0.1f)
                p.quadraticTo(cx + width * 0.45f, cy + height * 0.1f, cx + width * 0.4f, cy + height * 0.3f)
                p.quadraticTo(cx, cy + height * 0.38f, cx - width * 0.4f, cy + height * 0.3f)
                p.close()
                drawPath(p, slimeColor)

                // Blushing cheeks
                drawCircle(blushColor, radius = width * 0.06f, center = Offset(cx - width * 0.22f, cy + height * 0.12f))
                drawCircle(blushColor, radius = width * 0.06f, center = Offset(cx + width * 0.22f, cy + height * 0.12f))

                // Big white spherical eyes
                drawCircle(Color.White, radius = width * 0.07f, center = Offset(cx - width * 0.15f, cy + height * 0.02f))
                drawCircle(Color.White, radius = width * 0.07f, center = Offset(cx + width * 0.15f, cy + height * 0.02f))

                // Small black pupils
                drawCircle(Color.Black, radius = width * 0.03f, center = Offset(cx - width * 0.13f, cy + height * 0.02f))
                drawCircle(Color.Black, radius = width * 0.03f, center = Offset(cx + width * 0.17f, cy + height * 0.02f))

                // Cute mouth
                val mouthPath = Path()
                mouthPath.moveTo(cx - width * 0.06f, cy + height * 0.14f)
                mouthPath.quadraticTo(cx, cy + height * 0.23f, cx + width * 0.06f, cy + height * 0.14f)
                drawPath(mouthPath, Color.Black, style = Stroke(width = 4f))
            }
            MonsterType.GOBLIN -> {
                // Goblin colors
                val goblinColor = Color(0xFFC0CA33)
                val earColor = Color(0xFFAFB42B)

                // Triangular Pointy Left Ear
                val leftEar = Path().apply {
                    moveTo(cx - width * 0.2f, cy - height * 0.1f)
                    lineTo(cx - width * 0.48f, cy - height * 0.25f)
                    lineTo(cx - width * 0.28f, cy + height * 0.05f)
                    close()
                }
                drawPath(leftEar, earColor)

                // Triangular Pointy Right Ear
                val rightEar = Path().apply {
                    moveTo(cx + width * 0.2f, cy - height * 0.1f)
                    lineTo(cx + width * 0.48f, cy - height * 0.25f)
                    lineTo(cx + width * 0.28f, cy + height * 0.05f)
                    close()
                }
                drawPath(rightEar, earColor)

                // Round face structure
                drawCircle(goblinColor, radius = width * 0.3f, center = Offset(cx, cy))

                // Mischievous glowing yellow cat eyes
                drawCircle(Color(0xFFFFF176), radius = width * 0.05f, center = Offset(cx - width * 0.12f, cy - height * 0.02f))
                drawCircle(Color(0xFFFFF176), radius = width * 0.05f, center = Offset(cx + width * 0.12f, cy - height * 0.02f))
                // Slanted pupils
                drawRect(
                    Color.Black,
                    topLeft = Offset(cx - width * 0.13f, cy - height * 0.05f),
                    size = Size(width * 0.02f, height * 0.06f)
                )
                drawRect(
                    Color.Black,
                    topLeft = Offset(cx + width * 0.11f, cy - height * 0.05f),
                    size = Size(width * 0.02f, height * 0.06f)
                )

                // Broad grin mouth showing single funny tooth
                val grin = Path()
                grin.moveTo(cx - width * 0.15f, cy + height * 0.1f)
                grin.quadraticTo(cx, cy + height * 0.24f, cx + width * 0.15f, cy + height * 0.1f)
                drawPath(grin, Color.Black, style = Stroke(width = 4f))

                // Tooth
                val tooth = Path().apply {
                    moveTo(cx - width * 0.03f, cy + height * 0.13f)
                    lineTo(cx, cy + height * 0.18f)
                    lineTo(cx + width * 0.03f, cy + height * 0.13f)
                    close()
                }
                drawPath(tooth, Color.White)
            }
            MonsterType.GOLEM -> {
                // Rock Golem
                val stoneColor = Color(0xFF78909C)
                val crackColor = Color(0xFF37474F)

                // Base block frame
                drawRoundRect(
                    color = stoneColor,
                    topLeft = Offset(cx - width * 0.35f, cy - height * 0.35f),
                    size = Size(width * 0.7f, height * 0.7f),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // Cracked patterns (Vector drawings)
                drawLine(crackColor, Offset(cx - width * 0.2f, cy - height * 0.2f), Offset(cx - width * 0.05f, cy - height * 0.05f), strokeWidth = 5f)
                drawLine(crackColor, Offset(cx - width * 0.05f, cy - height * 0.05f), Offset(cx - width * 0.3f, cy + height * 0.1f), strokeWidth = 5f)
                drawLine(crackColor, Offset(cx + width * 0.1f, cy + height * 0.05f), Offset(cx + width * 0.28f, cy + height * 0.25f), strokeWidth = 4f)

                // Glowing Cyan visor eyes
                drawRoundRect(
                    color = Color(0xFF00E5FF),
                    topLeft = Offset(cx - width * 0.25f, cy - height * 0.12f),
                    size = Size(width * 0.5f, height * 0.08f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
            MonsterType.ORC -> {
                // Orc warrior orange face
                val redOrcColor = Color(0xFFE64A19)
                val helmetColor = Color(0xFF455A64)

                // Face structure
                drawCircle(redOrcColor, radius = width * 0.32f, center = Offset(cx, cy + height * 0.08f))

                // Barbarian Spiked Helmet
                val helmet = Path().apply {
                    moveTo(cx - width * 0.32f, cy - height * 0.05f)
                    lineTo(cx - width * 0.32f, cy - height * 0.25f)
                    lineTo(cx - width * 0.1f, cy - height * 0.35f)
                    lineTo(cx, cy - height * 0.45f) // Spike center top
                    lineTo(cx + width * 0.1f, cy - height * 0.35f)
                    lineTo(cx + width * 0.32f, cy - height * 0.25f)
                    lineTo(cx + width * 0.32f, cy - height * 0.05f)
                    quadraticTo(cx, cy - height * 0.15f, cx - width * 0.32f, cy - height * 0.05f)
                    close()
                }
                drawPath(helmet, helmetColor)

                // Angry eyebrows angle
                drawLine(Color.Black, Offset(cx - width * 0.18f, cy - height * 0.02f), Offset(cx - width * 0.05f, cy + height * 0.04f), strokeWidth = 5f)
                drawLine(Color.Black, Offset(cx + width * 0.18f, cy - height * 0.02f), Offset(cx + width * 0.05f, cy + height * 0.04f), strokeWidth = 5f)

                // Glowing tiny red eyes
                drawCircle(Color.White, radius = width * 0.04f, center = Offset(cx - width * 0.11f, cy + height * 0.05f))
                drawCircle(Color.White, radius = width * 0.04f, center = Offset(cx + width * 0.11f, cy + height * 0.05f))
                drawCircle(Color(0xFFFF1744), radius = width * 0.02f, center = Offset(cx - width * 0.1f, cy + height * 0.05f))
                drawCircle(Color(0xFFFF1744), radius = width * 0.02f, center = Offset(cx + width * 0.1f, cy + height * 0.05f))

                // White sharp fangs pointing up
                val leftFang = Path().apply {
                    moveTo(cx - width * 0.11f, cy + height * 0.28f)
                    lineTo(cx - width * 0.16f, cy + height * 0.18f)
                    lineTo(cx - width * 0.06f, cy + height * 0.25f)
                    close()
                }
                drawPath(leftFang, Color.White)

                val rightFang = Path().apply {
                    moveTo(cx + width * 0.11f, cy + height * 0.28f)
                    lineTo(cx + width * 0.16f, cy + height * 0.18f)
                    lineTo(cx + width * 0.06f, cy + height * 0.25f)
                    close()
                }
                drawPath(rightFang, Color.White)
            }
            MonsterType.DRAGON -> {
                // Boss Red Dragon
                val rubyRed = Color(0xFFD32F2F)
                val wingColor = Color(0xFF8B0000)
                val hornColor = Color(0xFFFFEB3B)

                // Tiny Dragon Left wing
                val leftWing = Path().apply {
                    moveTo(cx - width * 0.2f, cy - height * 0.05f)
                    quadraticTo(cx - width * 0.5f, cy - height * 0.28f, cx - width * 0.45f, cy - height * 0.05f)
                    quadraticTo(cx - width * 0.35f, cy + height * 0.1f, cx - width * 0.2f, cy + height * 0.05f)
                }
                drawPath(leftWing, wingColor)

                // Tiny Dragon Right wing
                val rightWing = Path().apply {
                    moveTo(cx + width * 0.2f, cy - height * 0.05f)
                    quadraticTo(cx + width * 0.5f, cy - height * 0.28f, cx + width * 0.45f, cy - height * 0.05f)
                    quadraticTo(cx + width * 0.35f, cy + height * 0.1f, cx + width * 0.2f, cy + height * 0.05f)
                }
                drawPath(rightWing, wingColor)

                // Head Base
                drawRoundRect(
                    color = rubyRed,
                    topLeft = Offset(cx - width * 0.3f, cy - height * 0.28f),
                    size = Size(width * 0.6f, height * 0.56f),
                    cornerRadius = CornerRadius(24f, 24f)
                )

                // Yellow little horns
                val leftHorn = Path().apply {
                    moveTo(cx - width * 0.2f, cy - height * 0.28f)
                    lineTo(cx - width * 0.3f, cy - height * 0.46f)
                    lineTo(cx - width * 0.1f, cy - height * 0.28f)
                    close()
                }
                drawPath(leftHorn, hornColor)

                val rightHorn = Path().apply {
                    moveTo(cx + width * 0.2f, cy - height * 0.28f)
                    lineTo(cx + width * 0.3f, cy - height * 0.46f)
                    lineTo(cx + width * 0.1f, cy - height * 0.28f)
                    close()
                }
                drawPath(rightHorn, hornColor)

                // Snout details
                drawRoundRect(
                    color = Color(0xFFFF5252),
                    topLeft = Offset(cx - width * 0.2f, cy + height * 0.05f),
                    size = Size(width * 0.4f, height * 0.18f),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                // Flashing yellow dragon eyes
                drawCircle(Color.Black, radius = width * 0.06f, center = Offset(cx - width * 0.11f, cy - height * 0.05f))
                drawCircle(Color.Black, radius = width * 0.06f, center = Offset(cx + width * 0.11f, cy - height * 0.05f))
                drawCircle(Color(0xFFFFD54F), radius = width * 0.03f, center = Offset(cx - width * 0.1f, cy - height * 0.05f))
                drawCircle(Color(0xFFFFD54F), radius = width * 0.03f, center = Offset(cx + width * 0.12f, cy - height * 0.05f))

                // Nose holes
                drawCircle(Color.Black.copy(alpha = 0.6f), radius = width * 0.02f, center = Offset(cx - width * 0.06f, cy + height * 0.11f))
                drawCircle(Color.Black.copy(alpha = 0.6f), radius = width * 0.02f, center = Offset(cx + width * 0.06f, cy + height * 0.11f))
            }
        }
    }
}

@Composable
fun ForestExploreScreen(
    uiState: GameUiState,
    onMove: (Int, Int) -> Unit,
    onBuyHeal: () -> Unit,
    onBuyShield: () -> Unit,
    onBuySword: () -> Unit,
    onExitGame: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP: Dashboard Bar
        DashboardHeader(
            level = uiState.level,
            exp = uiState.exp,
            currentHp = uiState.currentHp,
            maxHp = uiState.maxHp,
            gold = uiState.gold,
            score = uiState.score,
            shields = uiState.activeShields,
            onExit = onExitGame
        )

        // Forest Level Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🌲 마법의 숫자 숲 - 제 ${uiState.forestLevel} 구역 🌲",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E5C3D)
            )
        }

        // Dialogue Box / Narrative
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFFE8E1D1), shape = RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🧚", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = uiState.textDialogue,
                    color = Color(0xFF5D5749),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp
                )
            }
        }

        // --- MAP SECTION ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0E3)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color(0xFF8DAA78), shape = RoundedCornerShape(24.dp))
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Draw cells as 6 rows of 6 cells
                for (y in 0 until 6) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        for (x in 0 until 6) {
                            val isPlayer = (uiState.playerX == x && uiState.playerY == y)
                            val cell = uiState.forestMap.find { it.x == x && it.y == y }
                            
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = when {
                                            isPlayer -> Color(0xFFFFD54F) // Gold for current player
                                            cell?.type == "TREE" -> Color(0xFFC8E6C9) // Green trees
                                            cell?.type == "PORTAL" -> Color(0xFFD1C4E9) // Purple Portal
                                            cell?.type == "MONSTER" && !cell.isCleared -> Color(0xFFFFCCBC).copy(alpha = 0.5f) // Danger redish
                                            else -> Color.White.copy(alpha = 0.8f) // Clean safe grass
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = if (isPlayer) 2.dp else 1.dp,
                                        color = if (isPlayer) Color(0xFFFF7043) else Color(0xFFD6CDBA),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        // Allow direct movement to adjacent tiles
                                        val dx = x - uiState.playerX
                                        val dy = y - uiState.playerY
                                        if ((kotlin.math.abs(dx) == 1 && dy == 0) || (kotlin.math.abs(dy) == 1 && dx == 0)) {
                                            onMove(dx, dy)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPlayer) {
                                    // Hero
                                    Text("🧙‍♂️", fontSize = 22.sp)
                                } else if (cell != null) {
                                    when (cell.type) {
                                        "TREE" -> Text("🌲", fontSize = 18.sp)
                                        "PORTAL" -> Text("🌀", fontSize = 20.sp)
                                        "CHEST" -> {
                                            if (cell.isCleared) {
                                                Text("📦", fontSize = 16.sp, modifier = Modifier.alpha(0.5f))
                                            } else {
                                                Text("🎁", fontSize = 20.sp)
                                            }
                                        }
                                        "MONSTER" -> {
                                            if (cell.isCleared) {
                                                Text("☠️", fontSize = 16.sp, modifier = Modifier.alpha(0.5f))
                                            } else {
                                                Text("⚔️", fontSize = 18.sp)
                                            }
                                        }
                                        else -> {
                                            // Empty safe grass cell, draw light flower or pebble occasionally
                                            if ((x + y) % 5 == 0) {
                                                Text("🌿", fontSize = 11.sp, modifier = Modifier.alpha(0.3f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CONTROL D-PAD & FOOTER SHOPPING
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🧭 가고 싶은 지도 칸을 터치하거나 아래 버튼으로 움직이세요!",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8C8471),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Dynamic D-Pad Cross Controller
            Box(
                modifier = Modifier
                    .size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                // UP
                IconButton(
                    onClick = { onMove(0, -1) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(46.dp)
                        .background(Color(0xFF8DAA78), shape = RoundedCornerShape(12.dp))
                        .shadow(2.dp, shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = Color.White)
                }

                // LEFT
                IconButton(
                    onClick = { onMove(-1, 0) },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(46.dp)
                        .background(Color(0xFF8DAA78), shape = RoundedCornerShape(12.dp))
                        .shadow(2.dp, shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Move Left", tint = Color.White)
                }

                // CENTER: Decorative compass rose
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFF7043).copy(alpha = 0.15f), shape = CircleShape)
                        .border(1.5.dp, Color(0xFFFF7043), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF7043), shape = CircleShape))
                }

                // RIGHT
                IconButton(
                    onClick = { onMove(1, 0) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(46.dp)
                        .background(Color(0xFF8DAA78), shape = RoundedCornerShape(12.dp))
                        .shadow(2.dp, shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Move Right", tint = Color.White)
                }

                // DOWN
                IconButton(
                    onClick = { onMove(0, 1) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(46.dp)
                        .background(Color(0xFF8DAA78), shape = RoundedCornerShape(12.dp))
                        .shadow(2.dp, shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // SHOP FOOTER
        ShopFooterSection(
            gold = uiState.gold,
            shields = uiState.activeShields,
            swordBoost = uiState.activeSwordBoost,
            onBuyHeal = onBuyHeal,
            onBuyShield = onBuyShield,
            onBuySword = onBuySword
        )
    }
}

@Composable
fun HeroDrawingCanvas(
    hitState: String,
    modifier: Modifier = Modifier
) {
    val isHardHit = hitState == "HIT"
    val tint = if (isHardHit) Color(0xFFFF1744) else Color.Transparent

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cx = width / 2f
            val cy = height / 2f

            // Red wizard cape / warrior cloak
            val cloak = Path().apply {
                moveTo(cx - width * 0.25f, cy + height * 0.1f)
                lineTo(cx - width * 0.45f, cy + height * 0.45f)
                quadraticTo(cx, cy + height * 0.5f, cx + width * 0.45f, cy + height * 0.45f)
                lineTo(cx + width * 0.25f, cy + height * 0.1f)
                close()
            }
            drawPath(cloak, Color(0xFFE53935))

            // Silver Knight Armor Body
            drawRoundRect(
                color = Color(0xFFB0BEC5),
                topLeft = Offset(cx - width * 0.22f, cy),
                size = Size(width * 0.44f, height * 0.42f),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Dynamic golden chestplate crest
            val star = Path().apply {
                moveTo(cx, cy + height * 0.1f)
                lineTo(cx + width * 0.05f, cy + height * 0.18f)
                lineTo(cx + width * 0.15f, cy + height * 0.2f)
                lineTo(cx + width * 0.07f, cy + height * 0.26f)
                lineTo(cx + width * 0.1f, cy + height * 0.35f)
                lineTo(cx, cy + height * 0.29f)
                lineTo(cx - width * 0.1f, cy + height * 0.35f)
                lineTo(cx - width * 0.07f, cy + height * 0.26f)
                lineTo(cx - width * 0.15f, cy + height * 0.2f)
                lineTo(cx - width * 0.05f, cy + height * 0.18f)
                close()
            }
            drawPath(star, Color(0xFFFFD54F))

            // Round head with cute eye colors
            drawCircle(Color(0xFFFFCC80), radius = width * 0.16f, center = Offset(cx, cy - height * 0.12f))

            // Shining warrior visor helmet (M3 Steel Color)
            val visor = Path().apply {
                moveTo(cx - width * 0.18f, cy - height * 0.24f)
                lineTo(cx + width * 0.18f, cy - height * 0.24f)
                quadraticTo(cx + width * 0.2f, cy - height * 0.04f, cx, cy - height * 0.04f)
                quadraticTo(cx - width * 0.2f, cy - height * 0.04f, cx - width * 0.18f, cy - height * 0.24f)
                close()
            }
            drawPath(visor, Color(0xFF78909C))
            
            // Visor opening (glowing light blue slit eyes)
            drawRoundRect(
                color = Color(0xFF80DEEA),
                topLeft = Offset(cx - width * 0.13f, cy - height * 0.17f),
                size = Size(width * 0.26f, height * 0.05f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Red hair feathers on helm
            val feather = Path().apply {
                moveTo(cx, cy - height * 0.24f)
                quadraticTo(cx - width * 0.08f, cy - height * 0.43f, cx, cy - height * 0.46f)
                quadraticTo(cx + width * 0.1f, cy - height * 0.36f, cx, cy - height * 0.24f)
                close()
            }
            drawPath(feather, Color(0xFFFF1744))

            // Left Hand: Knightly Round Shield (Steel Blue)
            drawCircle(Color(0xFF455A64), radius = width * 0.13f, center = Offset(cx - width * 0.28f, cy + height * 0.22f))
            drawCircle(Color(0xFF90A4AE), radius = width * 0.08f, center = Offset(cx - width * 0.28f, cy + height * 0.22f))

            // Right Hand: Golden Broadsword
            val swordMain = Path().apply {
                moveTo(cx + width * 0.25f, cy + height * 0.15f)
                lineTo(cx + width * 0.3f, cy - height * 0.35f) // Sword point
                lineTo(cx + width * 0.35f, cy + height * 0.15f)
                close()
            }
            drawPath(swordMain, Color(0xFFFFD54F))
            
            // Sword hilt
            drawRoundRect(
                color = Color(0xFF8D6E63),
                topLeft = Offset(cx + width * 0.22f, cy + height * 0.15f),
                size = Size(width * 0.16f, height * 0.04f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawRect(
                color = Color(0xFF4E342E),
                topLeft = Offset(cx + width * 0.28f, cy + height * 0.19f),
                size = Size(width * 0.04f, height * 0.1f)
            )

            // Damage red overlay tint if we are taking a hit
            if (isHardHit) {
                drawCircle(tint.copy(alpha = 0.5f), radius = width * 0.5f, center = Offset(cx, cy))
            }
        }
    }
}
