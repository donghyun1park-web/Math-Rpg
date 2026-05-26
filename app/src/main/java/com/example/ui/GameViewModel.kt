package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.PlayerProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.random.Random

// Represents a math problem for children: 2-digit adding/subtracting 1-digit number
data class MathProblem(
    val num1: Int,
    val operator: String,
    val num2: Int,
    val correctAnswer: Int,
    val options: List<Int>,
    val tens1: Int = num1 / 10,
    val ones1: Int = num1 % 10,
    val isCarry: Boolean = (operator == "+" && (num1 % 10 + num2) > 9),
    val isBorrow: Boolean = (operator == "-" && (num1 % 10) < num2),
    val carryValue: Int = if (operator == "+" && (num1 % 10 + num2) > 9) 1 else 0,
    val borrowValue: Int = if (operator == "-" && (num1 % 10) < num2) 10 else 0,
    val tensResult: Int = correctAnswer / 10,
    val onesResult: Int = correctAnswer % 10
)

enum class MonsterType(
    val displayName: String,
    val baseHp: Int,
    val baseGold: Int,
    val baseExp: Int,
    val colorHex: String,
    val isBoss: Boolean = false
) {
    SLIME("말랑 슬라임 (Slime)", 15, 10, 20, "#81C784"),
    GOBLIN("개구쟁이 도깨비 (Goblin)", 25, 15, 30, "#DCE775"),
    GOLEM("든든 바위 골렘 (Golem)", 40, 25, 45, "#B0BEC5"),
    ORC("용감한 오크 대장 (Orc)", 60, 40, 60, "#FF8A65"),
    DRAGON("전설의 레드 드래곤 (Red Dragon)", 100, 70, 100, "#E57373", isBoss = true)
}

data class ActiveEffect(
    val text: String,
    val colorHex: String,
    val isMonsterSide: Boolean,
    val id: Long = System.currentTimeMillis() + Random.nextLong(1, 1000)
)

data class MapCell(
    val x: Int,
    val y: Int,
    val type: String, // "GRASS", "TREE", "MONSTER", "CHEST", "PORTAL"
    val monsterType: MonsterType? = null,
    val isCleared: Boolean = false
)

data class GameUiState(
    val id: Int = 1,
    val level: Int = 1,
    val exp: Int = 0,
    val currentHp: Int = 5,
    val maxHp: Int = 5,
    val gold: Int = 0,
    val score: Int = 0,
    val highScore: Int = 0,
    val defeatedMonsters: Int = 0,
    
    // Battle States
    val currentMonsterType: MonsterType = MonsterType.SLIME,
    val monsterMaxHp: Int = 15,
    val monsterCurrentHp: Int = 15,
    val monsterLevel: Int = 1,
    
    // Inventory Items
    val activeShields: Int = 0,
    val activeSwordBoost: Boolean = false, // Double damage active
    
    // Message / Explanations / Interactions
    val actionMessage: String = "야생의 슬라임이 나타났다! 수학 문제를 풀어 물리치자!",
    val isIncorrectAnswerFeedback: Boolean = false,
    val feedbackMessage: String = "",
    val isLevelUpEffect: Boolean = false,
    val isGameOver: Boolean = false,
    val isGameStarted: Boolean = false,
    val isBossFight: Boolean = false,
    
    // Active Floating Text Effects
    val activeEffects: List<ActiveEffect> = emptyList(),

    // Forest RPG Map Exploration States
    val gameMode: String = "MAP", // "MAP" or "BATTLE"
    val playerX: Int = 0,
    val playerY: Int = 5,
    val forestLevel: Int = 1,
    val forestMap: List<MapCell> = emptyList(),
    val textDialogue: String = "덧셈 뺄셈의 마법 숲에 들어왔습니다! 🌲 보물상자와 몬스터를 찾아 탐험하세요!",

    // Visual Battle Animations
    val heroAttackAnim: String = "NONE", // "NONE", "SWORD", "FIRE"
    val monsterHitAnim: String = "NONE",  // "NONE", "HIT", "DEAD"
    val heroHitAnim: String = "NONE",     // "NONE", "HIT"

    // Sound toggle state
    val isSoundMuted: Boolean = false
)

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _currentProblem = MutableStateFlow<MathProblem?>(null)
    val currentProblem: StateFlow<MathProblem?> = _currentProblem.asStateFlow()

    init {
        loadSavedProgress()
    }

    private fun loadSavedProgress() {
        viewModelScope.launch {
            repository.getPlayerProgress().collect { progress ->
                progress?.let {
                    _uiState.value = _uiState.value.copy(
                        level = it.level,
                        exp = it.exp,
                        currentHp = it.currentHp,
                        maxHp = it.maxHp,
                        gold = it.gold,
                        score = it.score,
                        highScore = it.highScore,
                        defeatedMonsters = it.defeatedMonsters
                    )
                }
            }
        }
    }

    fun generateForestMap(fLevel: Int): List<MapCell> {
        val cells = mutableListOf<MapCell>()
        // Fixed seed or time-based random
        val random = Random(System.currentTimeMillis())
        
        // Define monsters pool based on forest floor
        val possibleMonsters = when {
            fLevel == 1 -> listOf(MonsterType.SLIME, MonsterType.GOBLIN)
            fLevel == 2 -> listOf(MonsterType.SLIME, MonsterType.GOBLIN, MonsterType.GOLEM)
            fLevel == 3 -> listOf(MonsterType.GOBLIN, MonsterType.GOLEM, MonsterType.ORC)
            else -> listOf(MonsterType.GOLEM, MonsterType.ORC, MonsterType.DRAGON)
        }

        // We fill a 6x6 grid
        for (y in 0 until 6) {
            for (x in 0 until 6) {
                // Player starts at (0, 5) & Portal is at (5, 0)
                var type = "GRASS"
                var mType: MonsterType? = null
                
                if (x == 0 && y == 5) {
                    type = "GRASS"
                } else if (x == 5 && y == 0) {
                    type = "PORTAL"
                } else {
                    // Safe checkers to ensure paths aren't fully blocked
                    val isTreeSlot = (x == 1 && y == 1) || (x == 3 && y == 1) || (x == 1 && y == 3) || (x == 4 && y == 2) || (x == 2 && y == 4) || (x == 4 && y == 4)
                    
                    if (isTreeSlot) {
                        type = "TREE"
                    } else {
                        val r = random.nextInt(100)
                        when {
                            r < 18 -> {
                                type = "CHEST"
                            }
                            r < 38 -> {
                                type = "MONSTER"
                                mType = possibleMonsters.random()
                            }
                            else -> {
                                type = "GRASS"
                            }
                        }
                    }
                }
                
                cells.add(MapCell(x = x, y = y, type = type, monsterType = mType, isCleared = false))
            }
        }
        
        // Ensure there is at least 1 monster so they fight
        if (cells.none { it.type == "MONSTER" }) {
            val idx = cells.indexOfFirst { it.x == 3 && it.y == 3 }
            if (idx != -1) {
                cells[idx] = cells[idx].copy(type = "MONSTER", monsterType = possibleMonsters.random())
            }
        }
        
        // Ensure there is at least 1 chest for rewards
        if (cells.none { it.type == "CHEST" }) {
            val idx = cells.indexOfFirst { it.x == 2 && it.y == 2 }
            if (idx != -1) {
                cells[idx] = cells[idx].copy(type = "CHEST")
            }
        }
        
        return cells
    }

    fun movePlayer(dx: Int, dy: Int) {
        val state = _uiState.value
        val newX = (state.playerX + dx).coerceIn(0, 5)
        val newY = (state.playerY + dy).coerceIn(0, 5)
        
        val targetCell = state.forestMap.find { it.x == newX && it.y == newY }
        if (targetCell != null && targetCell.type == "TREE") {
            _uiState.value = state.copy(
                textDialogue = "앗! 빽빽한 나무 덩굴🌲이 막고 있어서 갈 수 없어요! 우회해 볼까요?"
            )
            SoundSynth.playIncorrect() // blunt buzz for roadblock
            return
        }
        
        var updatedMap = state.forestMap
        var mode = "MAP"
        var encounterMonster = state.currentMonsterType
        var encounterMonsterMaxHp = state.monsterMaxHp
        var encounterMonsterCurrentHp = state.monsterCurrentHp
        var encounterMonsterLevel = state.monsterLevel
        var dialogue = "바스락바스락... 마법 숲을 걸으며 주위를 살펴봅니다. 🌲🍁"
        
        if (targetCell != null) {
            when (targetCell.type) {
                "MONSTER" -> {
                    if (!targetCell.isCleared) {
                        val monster = targetCell.monsterType ?: MonsterType.SLIME
                        mode = "BATTLE"
                        encounterMonster = monster
                        val scaleMultiplier = 1.0 + (state.defeatedMonsters * 0.12)
                        encounterMonsterMaxHp = (monster.baseHp * scaleMultiplier).toInt()
                        encounterMonsterCurrentHp = encounterMonsterMaxHp
                        encounterMonsterLevel = state.defeatedMonsters + 1
                        dialogue = "⚔️ 위기! 야생의 ${monster.displayName}이(가) 나타났습니다! 수학 마법으로 해치웁시다!"
                        _uiState.value = state.copy(
                            playerX = newX,
                            playerY = newY,
                            gameMode = mode,
                            currentMonsterType = encounterMonster,
                            monsterMaxHp = encounterMonsterMaxHp,
                            monsterCurrentHp = encounterMonsterCurrentHp,
                            monsterLevel = encounterMonsterLevel,
                            actionMessage = dialogue,
                            isBossFight = monster.isBoss
                        )
                        generateNewProblem()
                        SoundSynth.startBgm("BATTLE") // High energy BGM
                        return
                    } else {
                        dialogue = "이미 물리친 ${targetCell.monsterType?.displayName ?: "몬스터"}의 자리에 평화가 감돕니다. 🍂"
                        SoundSynth.playStep()
                    }
                }
                "CHEST" -> {
                    if (!targetCell.isCleared) {
                        val goldReward = Random.nextInt(15, 30)
                        val extraHearts = if (Random.nextBoolean() && state.currentHp < state.maxHp) 1 else 0
                        val newHp = state.currentHp + extraHearts
                        
                        dialogue = "🎁 보물상자를 발견했습니다! 무려 +${goldReward}골드 획득!" + 
                                  if (extraHearts > 0) " 주화 마법 기운으로 체력도 1칸 채워졌어요! 💖" else ""
                        
                        updatedMap = state.forestMap.map {
                            if (it.x == newX && it.y == newY) it.copy(isCleared = true) else it
                        }
                        
                        _uiState.value = state.copy(
                            playerX = newX,
                            playerY = newY,
                            forestMap = updatedMap,
                            gold = state.gold + goldReward,
                            currentHp = newHp,
                            textDialogue = dialogue
                        )
                        addFloatingEffect("+${goldReward}💰", "#FFD54F", isMonsterSide = false)
                        if (extraHearts > 0) {
                            addFloatingEffect("+1💖", "#4CAF50", isMonsterSide = false)
                        }
                        saveProgressToDatabase()
                        SoundSynth.playChestOpen() // Treasure chime!
                        return
                    } else {
                        dialogue = "아쉽게도 빈 보물상자만 뎅그러니 열려 있습니다. 📦"
                        SoundSynth.playStep()
                    }
                }
                "PORTAL" -> {
                    val unclearedMonsters = state.forestMap.count { it.type == "MONSTER" && !it.isCleared }
                    if (unclearedMonsters > 0) {
                        dialogue = "포탈 에너지가 작동하지 않아요! 숲속의 모든 몬스터(${unclearedMonsters}마리)를 해치우고 와주세요! 🌀"
                        SoundSynth.playIncorrect() // Deny vibe
                    } else {
                        val nextFloor = state.forestLevel + 1
                        val newForest = generateForestMap(nextFloor)
                        _uiState.value = state.copy(
                            playerX = 0,
                            playerY = 5,
                            forestLevel = nextFloor,
                            forestMap = newForest,
                            textDialogue = "포탈의 소용돌이 마법! 🌀 더 깊은 숲 ${nextFloor}층으로 차원이동했습니다! 🌲✨"
                        )
                        saveProgressToDatabase()
                        SoundSynth.playLevelUp() // Portal fanfare!
                        return
                    }
                }
                else -> {
                    dialogue = "풀숲을 헤치며 모험을 속개합니다. 🌿"
                    SoundSynth.playStep()
                }
            }
        } else {
            SoundSynth.playStep()
        }
        
        _uiState.value = state.copy(
            playerX = newX,
            playerY = newY,
            forestMap = updatedMap,
            textDialogue = dialogue
        )
    }

    fun toggleSoundMuted() {
        val nextMuted = !_uiState.value.isSoundMuted
        _uiState.value = _uiState.value.copy(isSoundMuted = nextMuted)
        SoundSynth.setMuted(nextMuted)
        if (!nextMuted) {
            val mode = if (_uiState.value.gameMode == "BATTLE") "BATTLE" else "MAP"
            SoundSynth.startBgm(mode)
        }
    }

    fun startGame() {
        val startMap = generateForestMap(1)
        val currentMuteState = _uiState.value.isSoundMuted
        
        _uiState.value = GameUiState(
            id = 1,
            level = 1,
            exp = 0,
            currentHp = 5,
            maxHp = 5,
            gold = 15, // giving 15 start gold
            score = 0,
            highScore = _uiState.value.highScore,
            defeatedMonsters = 0,
            isGameStarted = true,
            gameMode = "MAP",
            forestLevel = 1,
            forestMap = startMap,
            playerX = 0,
            playerY = 5,
            textDialogue = "마법 수학 숲에 들어왔어요! 🌲 숲속을 걸으며 칼 아이콘(몬스터)과 상자(보물)를 탐험해보세요! 포탈로 넘어가려면 모든 몬스터를 물리쳐야 해요!",
            isSoundMuted = currentMuteState
        )
        generateNewProblem()
        SoundSynth.startBgm("MAP")
    }

    private fun selectMonsterType(defeatedCount: Int): MonsterType {
        val index = (defeatedCount + 1)
        return when {
            index % 5 == 0 -> MonsterType.DRAGON // Boss fight every 5th monster
            defeatedCount <= 2 -> MonsterType.SLIME
            defeatedCount <= 4 -> MonsterType.GOBLIN
            defeatedCount <= 6 -> MonsterType.GOLEM
            else -> {
                // Randomly choose among standard or boss variants
                val randomVal = Random.nextInt(4)
                when (randomVal) {
                    0 -> MonsterType.SLIME
                    1 -> MonsterType.GOBLIN
                    2 -> MonsterType.GOLEM
                    else -> MonsterType.ORC
                }
            }
        }
    }

    fun generateNewProblem() {
        val isAddition = Random.nextBoolean()
        val num1 = Random.nextInt(10, 91) // range 10-90
        val num2 = Random.nextInt(1, 10)  // range 1-9
        
        val op = if (isAddition) "+" else "-"
        val ans = if (isAddition) num1 + num2 else num1 - num2
        
        // Generate child friendly multiple-choice distractors
        val optionsSet = mutableSetOf<Int>()
        optionsSet.add(ans)
        
        val offsetPool = listOf(1, -1, 10, -10, 2, -2, 5, -5)
        for (offset in offsetPool.shuffled()) {
            val cand = ans + offset
            if (cand > 0 && cand != ans && cand !in optionsSet) {
                optionsSet.add(cand)
            }
            if (optionsSet.size == 4) break
        }
        
        // Safety check if we're lacking options
        while (optionsSet.size < 4) {
            val randOffset = Random.nextInt(-9, 10)
            val cand = ans + randOffset
            if (cand > 0 && cand != ans && cand !in optionsSet) {
                optionsSet.add(cand)
            }
        }
        
        _currentProblem.value = MathProblem(
            num1 = num1,
            operator = op,
            num2 = num2,
            correctAnswer = ans,
            options = optionsSet.toList().shuffled()
        )
        
        _uiState.value = _uiState.value.copy(
            isIncorrectAnswerFeedback = false,
            feedbackMessage = ""
        )
    }

    fun submitAnswer(selectedOption: Int) {
        val problem = _currentProblem.value ?: return
        val isCorrect = selectedOption == problem.correctAnswer
        
        if (isCorrect) {
            handleCorrectAnswer()
        } else {
            handleIncorrectAnswer(problem)
        }
    }

    private fun handleCorrectAnswer() {
        viewModelScope.launch {
            val baseDamage = Random.nextInt(10, 15)
            val finalDamage = if (_uiState.value.activeSwordBoost) baseDamage * 2 else baseDamage
            val newMonsterHp = (_uiState.value.monsterCurrentHp - finalDamage).coerceAtLeast(0)
            
            // Choose a random visual attack shape: SWORD (Slash) or FIRE (Fireball)
            val attackType = if (Random.nextBoolean()) "SWORD" else "FIRE"
            
            _uiState.value = _uiState.value.copy(
                heroAttackAnim = attackType,
                actionMessage = "수학 용사 정답! 마법 공격을 충전합니다! ✨🔋"
            )
            
            SoundSynth.playCorrect() // Immediate happy chime
            
            // 1. Casting Travel effect 
            kotlinx.coroutines.delay(700)
            
            // Play physical attack connection sounds precisely!
            if (attackType == "SWORD") {
                SoundSynth.playSwordSlash()
            } else {
                SoundSynth.playFireball()
            }
            
            // 2. Spark hit reaction
            _uiState.value = _uiState.value.copy(
                monsterHitAnim = "HIT",
                monsterCurrentHp = newMonsterHp,
                activeSwordBoost = false, // consume sword boost
                actionMessage = if (attackType == "SWORD") {
                    "용사가 돌진하여 전설의 일격검(⚔️)을 휘둘렀습니다! 대미지 -$finalDamage!"
                } else {
                    "용사가 뜨거운 파이어볼(🔥) 주문을 완벽히 영창했습니다! 대미지 -$finalDamage!"
                }
            )
            
            addFloatingEffect("-$finalDamage⚔️", "#FF1744", isMonsterSide = true)
            
            kotlinx.coroutines.delay(600)
            
            // Clear regular attack and hit anims
            _uiState.value = _uiState.value.copy(
                heroAttackAnim = "NONE",
                monsterHitAnim = "NONE"
            )
            
            if (newMonsterHp <= 0) {
                // Play monster collapsing/death slide animation for 1200ms
                _uiState.value = _uiState.value.copy(
                    monsterHitAnim = "DEAD",
                    actionMessage = "${_uiState.value.currentMonsterType.displayName}이(가) 치명상을 입고 비틀거리며 쓰러집니다! 💥🦖"
                )
                kotlinx.coroutines.delay(1200)
                _uiState.value = _uiState.value.copy(monsterHitAnim = "NONE")
                handleMonsterDefeated()
            } else {
                generateNewProblem()
            }
        }
    }

    private fun handleIncorrectAnswer(problem: MathProblem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                heroHitAnim = "HIT",
                actionMessage = "어이쿠 틀렸습니다! 몬스터가 무기력한 용사를 향해 반격 기술을 시전합니다! ⚡"
            )
            
            SoundSynth.playIncorrect() // Discordant error buzz
            
            kotlinx.coroutines.delay(700)
            
            if (_uiState.value.activeShields > 0) {
                // Shield absorbed damage
                _uiState.value = _uiState.value.copy(
                    activeShields = _uiState.value.activeShields - 1,
                    actionMessage = "튼튼한 마법 방패(🛡️)가 전방 대미지를 수호해주었습니다!",
                    isIncorrectAnswerFeedback = true,
                    feedbackMessage = "집중력이 흐려졌나요? ${problem.num1} ${problem.operator} ${problem.num2} = ${problem.correctAnswer} 입니다!",
                    heroHitAnim = "NONE"
                )
                addFloatingEffect("방어🛡️", "#29B6F6", isMonsterSide = false)
                SoundSynth.playClick() // small wood shield knock sound
            } else {
                // Player takes 1 hp damage
                val newHp = _uiState.value.currentHp - 1
                _uiState.value = _uiState.value.copy(
                    currentHp = newHp,
                    actionMessage = "악! 몬스터 공격에 정면 타격을 받았습니다. 하트 한 칸을 잃었습니다!",
                    isIncorrectAnswerFeedback = true,
                    feedbackMessage = "수학 공식 연습: ${problem.num1} ${problem.operator} ${problem.num2} = ${problem.correctAnswer} 이에요!",
                    heroHitAnim = "NONE"
                )
                addFloatingEffect("-1💖", "#FF1744", isMonsterSide = false)
                SoundSynth.playIncorrect() // Double fail buzzer for true damage
                
                if (newHp <= 0) {
                    _uiState.value = _uiState.value.copy(
                        isGameOver = true,
                        actionMessage = "정신이 아득해집니다... 체력이 모두 다해 마법 수련으로 되돌아갑니다. 🧙"
                    )
                    saveHighscoreOnly()
                    SoundSynth.stopBgm() // Stop music on game over
                }
            }
        }
    }

    private fun handleMonsterDefeated() {
        val mType = _uiState.value.currentMonsterType
        val goldEarned = mType.baseGold + Random.nextInt(1, 5)
        val expEarned = mType.baseExp
        
        var newExp = _uiState.value.exp + expEarned
        var newLevel = _uiState.value.level
        var newMaxHp = _uiState.value.maxHp
        var newHp = _uiState.value.currentHp
        var levelUpOccurred = false
        
        if (newExp >= 100) {
            newLevel++
            newExp -= 100
            newMaxHp++
            newHp = newMaxHp // Full heal on level-up
            levelUpOccurred = true
            addFloatingEffect("레벨 업!🎉", "#FFD54F", isMonsterSide = false)
        }
        
        val newDefeatedCount = _uiState.value.defeatedMonsters + 1
        val scoreEarned = if (mType.isBoss) 50 else 10
        val newScore = _uiState.value.score + scoreEarned
        val newHighScore = if (newScore > _uiState.value.highScore) newScore else _uiState.value.highScore
        
        // Mark the current player coordinate on the forestMap as cleared
        val updatedMap = _uiState.value.forestMap.map {
            if (it.x == _uiState.value.playerX && it.y == _uiState.value.playerY) {
                it.copy(isCleared = true)
            } else {
                it
            }
        }
        
        // Prepare next monster specifications precalculated
        val nextMonsterType = selectMonsterType(newDefeatedCount)
        val scaleMultiplier = 1.0 + (newDefeatedCount * 0.15)
        val nextMonsterMaxHp = (nextMonsterType.baseHp * scaleMultiplier).toInt()
        val nextMonsterLevel = newDefeatedCount + 1
        
        _uiState.value = _uiState.value.copy(
            level = newLevel,
            exp = newExp,
            currentHp = newHp,
            maxHp = newMaxHp,
            gold = _uiState.value.gold + goldEarned,
            score = newScore,
            highScore = newHighScore,
            defeatedMonsters = newDefeatedCount,
            
            // Map modifications: exit to exploration screen
            gameMode = "MAP",
            forestMap = updatedMap,
            textDialogue = "🎉 축하합니다! ${mType.displayName}을(를) 완벽하게 소탕했습니다! (+${goldEarned}골드, +${expEarned} 경험치 획득!) 마법 숲 조사를 계속하세요!",
            
            currentMonsterType = nextMonsterType,
            monsterMaxHp = nextMonsterMaxHp,
            monsterCurrentHp = nextMonsterMaxHp,
            monsterLevel = nextMonsterLevel,
            isBossFight = nextMonsterType.isBoss,
            isLevelUpEffect = levelUpOccurred
        )
        
        addFloatingEffect("+${goldEarned}💰", "#FFD54F", isMonsterSide = false)
        addFloatingEffect("+${expEarned}XP", "#BA68C8", isMonsterSide = false)
        
        saveProgressToDatabase()
        
        if (levelUpOccurred) {
            SoundSynth.playLevelUp()
        } else {
            SoundSynth.playChestOpen()
        }
        SoundSynth.startBgm("MAP")
    }

    private fun addFloatingEffect(text: String, colorHex: String, isMonsterSide: Boolean) {
        val effect = ActiveEffect(text, colorHex, isMonsterSide)
        _uiState.value = _uiState.value.copy(
            activeEffects = _uiState.value.activeEffects + effect
        )
    }

    fun removeFloatingEffect(effectId: Long) {
        _uiState.value = _uiState.value.copy(
            activeEffects = _uiState.value.activeEffects.filter { it.id != effectId }
        )
    }

    fun buyHealPotion() {
        val state = _uiState.value
        val cost = 15
        if (state.gold >= cost) {
            val restoredHp = (state.currentHp + 2).coerceAtMost(state.maxHp)
            _uiState.value = state.copy(
                gold = state.gold - cost,
                currentHp = restoredHp,
                actionMessage = "물약 맛있다! 하트가 2개 충전되었어요! 💖"
            )
            addFloatingEffect("+2💖", "#4CAF50", isMonsterSide = false)
            saveProgressToDatabase()
            SoundSynth.playChestOpen()
        } else {
            _uiState.value = state.copy(
                actionMessage = "골드가 부족해요! 수학 문제를 맞혀서 골드를 벌어보자!"
            )
            SoundSynth.playIncorrect()
        }
    }

    fun buyShield() {
         val state = _uiState.value
         val cost = 20
         if (state.gold >= cost) {
              SoundSynth.playChestOpen()
             _uiState.value = state.copy(
                 gold = state.gold - cost,
                 activeShields = state.activeShields + 1,
                 actionMessage = "방패를 샀어요! 다음 실수 대미지를 한 번 막아줍니다 🛡️"
             )
             addFloatingEffect("+1🛡️", "#29B6F6", isMonsterSide = false)
             saveProgressToDatabase()
         } else {
             _uiState.value = state.copy(
                 actionMessage = "골드가 부족해요! 열심히 공부해서 골드를 더 벌어오자!"
              )
              SoundSynth.playIncorrect()
         }
    }

    fun buySword() {
         val state = _uiState.value
         val cost = 25
         if (state.gold >= cost) {
              SoundSynth.playChestOpen()
              _uiState.value = state.copy(
                  gold = state.gold - cost,
                  activeSwordBoost = true,
                  actionMessage = "전설의 검을 구매했어요! 다음 공격 대미지가 2배로 늘어납니다! ⚔️"
              )
              addFloatingEffect("Power⚔️", "#FF1744", isMonsterSide = false)
              saveProgressToDatabase()
         } else {
              SoundSynth.playIncorrect()
              _uiState.value = state.copy(
                  actionMessage = "골드가 부족해요! 골드를 모아 전설의 검을 구입하세요!"
              )
         }
    }

    private fun saveProgressToDatabase() {
        viewModelScope.launch {
            val progress = PlayerProgress(
                id = 1,
                level = _uiState.value.level,
                exp = _uiState.value.exp,
                currentHp = _uiState.value.currentHp,
                maxHp = _uiState.value.maxHp,
                gold = _uiState.value.gold,
                score = _uiState.value.score,
                highScore = _uiState.value.highScore,
                defeatedMonsters = _uiState.value.defeatedMonsters
            )
            repository.saveProgress(progress)
        }
    }

    private fun saveHighscoreOnly() {
        viewModelScope.launch {
            val progress = PlayerProgress(
                id = 1,
                level = 1, // Reset general stats on dead, but keep high score!
                exp = 0,
                currentHp = 5,
                maxHp = 5,
                gold = 10,
                score = 0,
                highScore = _uiState.value.highScore,
                defeatedMonsters = 0
            )
            repository.saveProgress(progress)
        }
    }

    fun dismissLevelUpEffect() {
        _uiState.value = _uiState.value.copy(isLevelUpEffect = false)
    }

    fun exitGame() {
        _uiState.value = _uiState.value.copy(
            isGameStarted = false,
            isGameOver = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        SoundSynth.release()
    }
}

class GameViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
