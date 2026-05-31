package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ActiveBrew
import com.example.data.PotionRecipe
import com.example.data.CampaignCalendar
import com.example.data.DateTimeState
import com.example.data.ResidueItem
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

enum class AppScreen {
    Cauldron,
    StartBrewing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CauldronApp(viewModel: PotionViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.Cauldron) }
    var selectedBrewForResolution by remember { mutableStateOf<ActiveBrew?>(null) }
    
    // Track selected tab on Dashboard: 0 = Active Cauldrons, 1 = Residues, 2 = Recipe Book, 3 = Completed Logs
    var selectedDashboardTab by remember { mutableStateOf(0) }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activeBrews by viewModel.activeBrews.collectAsStateWithLifecycle()
    val archivedBrews by viewModel.archivedBrews.collectAsStateWithLifecycle()
    val availableResidues by viewModel.availableResidues.collectAsStateWithLifecycle()
    val allRecipes by viewModel.allRecipes.collectAsStateWithLifecycle()

    val currentYear = settings?.currentYear ?: 2023
    val currentMonth = settings?.currentMonth ?: 9
    val currentDay = settings?.currentDay ?: 17
    val currentPeriod = settings?.currentPeriod ?: "NOON"

    // Helper states for start-brewing popup notifications
    var showD100ToastMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🧪 Cauldron Alchemist",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = WizardPurple,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepObsidian,
                    titleContentColor = WizardPurple
                ),
                actions = {
                    IconButton(
                        onClick = { selectedDashboardTab = 3 },
                        modifier = Modifier.testTag("action_history_logs")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Historical Brew Logs",
                            tint = LegendaryGold
                        )
                    }
                }
            )
        },
        containerColor = DeepObsidian,
        floatingActionButton = {
            if (currentScreen == AppScreen.Cauldron) {
                ExtendedFloatingActionButton(
                    text = { Text("Start Brewing", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add cauldron") },
                    onClick = { currentScreen = AppScreen.StartBrewing },
                    containerColor = WizardPurple,
                    contentColor = DeepObsidian,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("start_brewing_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    AppScreen.Cauldron -> {
                        CauldronDashboard(
                            currentYear = currentYear,
                            currentMonth = currentMonth,
                            currentDay = currentDay,
                            currentPeriod = currentPeriod,
                            activeBrews = activeBrews,
                            archivedBrews = archivedBrews,
                            availableResidues = availableResidues,
                            allRecipes = allRecipes,
                            selectedTab = selectedDashboardTab,
                            onTabChanged = { selectedDashboardTab = it },
                            onAdvanceDay = { viewModel.advanceTimeBlock() },
                            onDecrementDay = { viewModel.decrementTimeBlock() },
                            onResolveBrew = { selectedBrewForResolution = it },
                            onCancelBrew = { viewModel.cancelBrew(it) }
                        )
                    }
                    AppScreen.StartBrewing -> {
                        BackHandler { currentScreen = AppScreen.Cauldron }
                        StartBrewingForm(
                            recipes = allRecipes,
                            availableResidues = availableResidues,
                            currentYear = currentYear,
                            currentMonth = currentMonth,
                            currentDay = currentDay,
                            currentPeriod = currentPeriod,
                            onBack = { currentScreen = AppScreen.Cauldron },
                            onStartBrew = { recipe, qty, residue, startYear, startMonth, startDay, startPeriod ->
                                // Compute d100 roll and notify if needed before adding
                                var popupMessage: String? = null
                                if (residue != null) {
                                    val peekRoll = Random.nextInt(1, 101)
                                    val effectText = when (peekRoll) {
                                        in 1..33 -> "Accelerated Batch! Rolled $peekRoll on % Dice. Brewing duration halved."
                                        in 34..66 -> "Potency Strain! Rolled $peekRoll on % Dice. Finished potions will be 50% more potent."
                                        in 67..99 -> "Secondary Separation! Rolled $peekRoll on % Dice. Harvest a free random same-rarity potion on completion."
                                        else -> "Supermutation! Rolled 100 on % Dice. Brew mutated completely into a random Rare potion!"
                                    }
                                    popupMessage = effectText
                                }
                                
                                viewModel.startBrewing(recipe, qty, residue, startYear, startMonth, startDay, startPeriod)
                                showD100ToastMessage = popupMessage
                                currentScreen = AppScreen.Cauldron
                            }
                        )
                    }
                }
            }

            // Custom Toast Overlay for d100 random rolls triggered immediately upon starting a brew
            showD100ToastMessage?.let { msg ->
                Dialog(onDismissRequest = { showD100ToastMessage = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SumpCardColor),
                        border = BorderStroke(1.dp, AlchemistGreen),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "🔮 Alchemical Catalyst Trigger",
                                style = MaterialTheme.typography.titleMedium,
                                color = AlchemistGreen,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = "Mixing sediment leftovers into your alembic has sparked an arcane mutation!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CommonWhite,
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .background(GlowPurple, RoundedCornerShape(8.dp))
                                    .border(1.dp, WizardPurple, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LegendaryGold,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Button(
                                onClick = { showD100ToastMessage = null },
                                colors = ButtonDefaults.buttonColors(containerColor = AlchemistGreen, contentColor = DeepObsidian),
                                modifier = Modifier.fillMaxWidth().testTag("close_d100_toast")
                            ) {
                                Text("Acknowledge Roll", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Show batch resolution dialog when selected
            selectedBrewForResolution?.let { brew ->
                BatchResolutionDialog(
                    brew = brew,
                    onDismiss = { selectedBrewForResolution = null },
                    onResolve = { roll ->
                        viewModel.resolveBrew(brew, roll)
                        selectedBrewForResolution = null
                    }
                )
            }
        }
    }
}

@Composable
fun CauldronDashboard(
    currentYear: Int,
    currentMonth: Int,
    currentDay: Int,
    currentPeriod: String,
    activeBrews: List<ActiveBrew>,
    archivedBrews: List<ActiveBrew>,
    availableResidues: List<ResidueItem>,
    allRecipes: List<PotionRecipe>,
    selectedTab: Int,
    onTabChanged: (Int) -> Unit,
    onAdvanceDay: () -> Unit,
    onDecrementDay: () -> Unit,
    onResolveBrew: (ActiveBrew) -> Unit,
    onCancelBrew: (ActiveBrew) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mystical Campaign Day Widget
        CampaignClockWidget(
            currentYear = currentYear,
            currentMonth = currentMonth,
            currentDay = currentDay,
            currentPeriod = currentPeriod,
            onAdvance = onAdvanceDay,
            onDecrement = onDecrementDay
        )

        // Custom Navigation Tab Bar
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = WizardPurple,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabChanged(0) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Cauldrons")
                        Badge(containerColor = if (activeBrews.isNotEmpty()) WizardPurple else Color.DarkGray) {
                            Text(activeBrews.size.toString(), color = DeepObsidian, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                modifier = Modifier.testTag("tab_active_cauldrons")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabChanged(1) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Residues")
                        Badge(containerColor = if (availableResidues.isNotEmpty()) AlchemistGreen else Color.DarkGray) {
                            Text(availableResidues.size.toString(), color = DeepObsidian, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                modifier = Modifier.testTag("tab_my_residues")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { onTabChanged(2) },
                text = { Text("Recipe Book") },
                modifier = Modifier.testTag("tab_recipe_book")
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { onTabChanged(3) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Logs")
                        if (archivedBrews.isNotEmpty()) {
                            Badge(containerColor = LegendaryGold) {
                                Text(archivedBrews.size.toString(), color = DeepObsidian, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("tab_brew_logs")
            )
        }

        // Tab Content Switching
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (selectedTab) {
                0 -> ActiveCauldronsList(
                    currentYear = currentYear,
                    currentMonth = currentMonth,
                    currentDay = currentDay,
                    currentPeriod = currentPeriod,
                    brews = activeBrews,
                    onResolveBrew = onResolveBrew,
                    onCancelBrew = onCancelBrew
                )
                1 -> ResidueInventoryView(residues = availableResidues)
                2 -> RecipeLibraryView(recipes = allRecipes)
                3 -> BrewHistoryLogs(logs = archivedBrews)
            }
        }
    }
}

@Composable
fun CampaignClockWidget(
    currentYear: Int,
    currentMonth: Int,
    currentDay: Int,
    currentPeriod: String,
    onAdvance: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SumpCardColor),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(WizardPurple, LegendaryGold))),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Decrement Day Button
            IconButton(
                onClick = onDecrement,
                enabled = true,
                modifier = Modifier
                    .size(44.dp)
                    .background(CauldronRim, CircleShape)
                    .border(1.dp, WizardPurple, CircleShape)
                    .testTag("btn_decrement_day")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Rewind Timeline Day",
                    tint = WizardPurple
                )
            }

            // Central Calendar Widget
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "GALENOR CAMPAIGN TIMELINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = LegendaryGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${CampaignCalendar.getMonthName(currentMonth)} $currentDay, $currentYear",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CommonWhite
                    )
                    Text(
                        text = CampaignCalendar.getPeriodLabel(currentPeriod),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = WizardPurple,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.testTag("campaign_day_text")
                    )
                }
                Text(
                    text = "Age mixtures by advancing time blocks",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Increment Day Button
            IconButton(
                onClick = onAdvance,
                modifier = Modifier
                    .size(44.dp)
                    .background(CauldronRim, CircleShape)
                    .border(1.dp, LegendaryGold, CircleShape)
                    .testTag("btn_advance_day")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Advance Campaign Day",
                    tint = LegendaryGold
                )
            }
        }
    }
}

@Composable
fun ActiveCauldronsList(
    currentYear: Int,
    currentMonth: Int,
    currentDay: Int,
    currentPeriod: String,
    brews: List<ActiveBrew>,
    onResolveBrew: (ActiveBrew) -> Unit,
    onCancelBrew: (ActiveBrew) -> Unit
) {
    if (brews.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔮",
                fontSize = 54.sp
            )
            Text(
                text = "Silent Cauldrons",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WizardPurple
            )
            Text(
                text = "Your cauldrons are cold. Tap 'Start Brewing' below to select a legendary recipe, add gold reagents, and light the mystical hearth fire.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(brews, key = { it.id }) { brew ->
                ActiveBrewCard(
                    currentYear = currentYear,
                    currentMonth = currentMonth,
                    currentDay = currentDay,
                    currentPeriod = currentPeriod,
                    brew = brew,
                    onResolveBrew = onResolveBrew,
                    onCancelBrew = onCancelBrew
                )
            }
        }
    }
}

@Composable
fun ActiveBrewCard(
    currentYear: Int,
    currentMonth: Int,
    currentDay: Int,
    currentPeriod: String,
    brew: ActiveBrew,
    onResolveBrew: (ActiveBrew) -> Unit,
    onCancelBrew: (ActiveBrew) -> Unit
) {
    val startAbs = CampaignCalendar.getAbsolutePeriods(brew.startYear, brew.startMonth, brew.startDay, brew.startPeriod)
    val durationPeriods = Math.round(brew.actualDurationDays * 5.0).toLong().coerceAtLeast(1L)
    val finishAbs = startAbs + durationPeriods
    val currentAbs = CampaignCalendar.getAbsolutePeriods(currentYear, currentMonth, currentDay, currentPeriod)
    
    val elapsedPeriods = currentAbs - startAbs
    val progress = (elapsedPeriods.toDouble() / durationPeriods.toDouble()).coerceIn(0.0, 1.0).toFloat()
    val progressPercent = (progress * 100).toInt()
    
    val isReady = currentAbs >= finishAbs
    val daysRemaining = ((finishAbs - currentAbs).toDouble() / 5.0).coerceAtLeast(0.0)
    
    val finishState = CampaignCalendar.getCalendarFromAbsolutePeriods(finishAbs)
    val finishDateLabel = "${CampaignCalendar.getMonthName(finishState.month)} ${finishState.day}, ${finishState.year} at ${CampaignCalendar.getPeriodLabel(finishState.period)}"
    
    val progressColor = if (isReady) AlchemistGreen else RareBlue
    
    // Format duration nicely
    val formattedRemaining = String.format(Locale.US, "%.1f", daysRemaining)
    val formattedDuration = String.format(Locale.US, "%.1f", brew.actualDurationDays)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("brew_card_${brew.id}"),
        colors = CardDefaults.cardColors(containerColor = SumpCardColor),
        border = BorderStroke(
            width = 1.dp,
            color = if (isReady) AlchemistGreen else CauldronRim
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Potion Name and Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = brew.potionName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CommonWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "(${brew.batchQuantity}x)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LegendaryGold
                        )
                    }
                    Text(
                        text = "Started: ${CampaignCalendar.getMonthName(brew.startMonth)} ${brew.startDay}, ${brew.startYear} (${CampaignCalendar.getPeriodLabel(brew.startPeriod)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "Completion: $finishDateLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isReady) AlchemistGreen else WizardPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Delete/Cancel potion mix
                IconButton(
                    onClick = { onCancelBrew(brew) },
                    modifier = Modifier.testTag("btn_cancel_brew_${brew.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel and Empty Cauldron",
                        tint = ErrorCrimson
                    )
                }
            }

            // Custom Catalyst modifier tags
            if (brew.appliedResidueName != null) {
                Box(
                    modifier = Modifier
                        .background(GlowPurple, RoundedCornerShape(6.dp))
                        .border(BorderStroke(1.dp, WizardPurple.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🧬", fontSize = 12.sp)
                        Text(
                            text = "Catalyzed by ${brew.appliedResidueName} (${brew.appliedResidueType})",
                            style = MaterialTheme.typography.labelSmall,
                            color = WizardPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Show active mutation notes
            if (brew.d100Effect != null) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0C0A0E), RoundedCornerShape(6.dp))
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "🔮 ROLL RESULT: ${brew.d100Effect}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LegendaryGold,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Time analytics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Progress: $progressPercent%",
                        style = MaterialTheme.typography.bodySmall,
                        color = progressColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total brew time: $formattedDuration days",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }

                if (isReady) {
                    Text(
                        text = "✨ Finished!",
                        style = MaterialTheme.typography.bodySmall,
                        color = AlchemistGreen,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "$formattedRemaining days remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = LegendaryGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Premium progress indicator bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = CauldronRim
            )

            // Resolve Action Button (Unlocks at Day Remaining == 0)
            if (isReady) {
                Button(
                    onClick = { onResolveBrew(brew) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlchemistGreen,
                        contentColor = DeepObsidian
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_roll_d20_${brew.id}")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎲 ROLL PHYSICAL 1D20 TO RESOLVE",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResidueInventoryView(residues: List<ResidueItem>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "ALCHEMICAL SEDIMENT DEPOSITS",
            style = MaterialTheme.typography.labelMedium,
            color = LegendaryGold,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (residues.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📜", fontSize = 48.sp)
                    Text(
                        text = "Inventory Empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WizardPurple
                    )
                    Text(
                        text = "Whenever you score a Great Success (d20 score 16-19) or standard Masterwork Dilutions (Nat 20), leftover magic sediment is collected in your inventory. Apply these residues to new batches for extreme accelerated mutations!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(residues, key = { it.id }) { residue ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SumpCardColor),
                        border = BorderStroke(1.dp, CauldronRim)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = residue.potionSource,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CommonWhite
                                )
                                Text(
                                    text = "Deposit Type: ${residue.residueType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (residue.residueType.contains("Catalyst")) LegendaryGold else WizardPurple,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(GlowPurple, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Ready to Mix",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AlchemistGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeLibraryView(recipes: List<PotionRecipe>) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredRecipes = remember(searchQuery, recipes) {
        if (searchQuery.isBlank()) recipes else {
            recipes.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search potions library...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = WizardPurple) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WizardPurple,
                unfocusedBorderColor = CauldronRim,
                focusedContainerColor = SumpCardColor,
                unfocusedContainerColor = SumpCardColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recipe_search_input"),
            singleLine = true
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredRecipes, key = { it.id }) { recipe ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SumpCardColor),
                    border = BorderStroke(1.dp, CauldronRim)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = recipe.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CommonWhite,
                                modifier = Modifier.weight(1f)
                            )
                            RarityTag(rarity = recipe.rarity)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "🪙 Reagent Cost: ${recipe.craftingPriceGp} GP",
                                style = MaterialTheme.typography.bodySmall,
                                color = LegendaryGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "⚔️ Min Level: ${recipe.minLevel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = WizardPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = CauldronRim, thickness = 1.dp)

                        Text(
                            text = recipe.effects,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrewHistoryLogs(logs: List<ActiveBrew>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "ARCHIVED BREWING MANUSCRIPT",
            style = MaterialTheme.typography.labelMedium,
            color = LegendaryGold,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📜", fontSize = 48.sp)
                    Text(
                        text = "History Idle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WizardPurple
                    )
                    Text(
                        text = "No completed cauldrons yet. Complete. Brew to 100%, roll your d20 and claim results to add logs to the records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SumpCardColor),
                        border = BorderStroke(1.dp, CauldronRim.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log.potionName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CommonWhite
                                )
                                Text(
                                    text = "Roll: D20 [${log.d20Roll ?: 0}]",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LegendaryGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Started: ${CampaignCalendar.getMonthName(log.startMonth)} ${log.startDay}, ${log.startYear} (${CampaignCalendar.getPeriodLabel(log.startPeriod)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Quantity: ${log.batchQuantity} ➔ ${log.finalQuantity ?: log.batchQuantity} Potions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AlchemistGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DeepObsidian, RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = log.notes ?: "Batch claimed successfully.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StartBrewingForm(
    recipes: List<PotionRecipe>,
    availableResidues: List<ResidueItem>,
    currentYear: Int,
    currentMonth: Int,
    currentDay: Int,
    currentPeriod: String,
    onBack: () -> Unit,
    onStartBrew: (PotionRecipe, Int, ResidueItem?, Int, Int, Int, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }
    var selectedRecipe by remember { mutableStateOf<PotionRecipe?>(null) }
    var selectedResidue by remember { mutableStateOf<ResidueItem?>(null) }
    var mixResidueEnabled by remember { mutableStateOf(false) }

    // Start Date selection state
    var startDay by remember { mutableStateOf(currentDay) }
    var startMonth by remember { mutableStateOf(currentMonth) }
    var startYear by remember { mutableStateOf(currentYear) }
    var startPeriod by remember { mutableStateOf(currentPeriod) }

    // Dropdown filters
    val filteredRecipes = remember(searchQuery, recipes) {
        if (searchQuery.isBlank()) recipes else {
            recipes.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onBack() }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WizardPurple)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Cauldron", style = MaterialTheme.typography.titleMedium, color = WizardPurple, fontWeight = FontWeight.Bold)
        }

        Text(
            text = "PREPARE BREWING CRUCIBLE",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = CommonWhite
        )

        // Dropdown Search & Picker for Recipe Library
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("SELECT RECIPE", style = MaterialTheme.typography.labelSmall, color = LegendaryGold, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter potions library...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WizardPurple) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WizardPurple,
                    unfocusedBorderColor = CauldronRim,
                    focusedContainerColor = SumpCardColor,
                    unfocusedContainerColor = SumpCardColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_recipe_search"),
                singleLine = true
            )

            // Results List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp)
                    .border(BorderStroke(1.dp, CauldronRim), RoundedCornerShape(8.dp))
                    .background(DeepObsidian),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredRecipes) { recipe ->
                    val isSelected = selectedRecipe == recipe
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) GlowPurple else Color.Transparent)
                            .clickable {
                                selectedRecipe = recipe
                                searchQuery = recipe.name // set search query to selected name
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = recipe.name,
                            color = if (isSelected) WizardPurple else CommonWhite,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        RarityTag(rarity = recipe.rarity)
                    }
                }
            }
        }

        // Display Selected Recipe Details
        selectedRecipe?.let { recipe ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SumpCardColor),
                border = BorderStroke(1.dp, CauldronRim)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = recipe.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CommonWhite
                        )
                        RarityTag(rarity = recipe.rarity)
                    }
                    Text(
                        text = recipe.effects,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }
        }

        // Quantity Selector slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "BATCH CONTAINER SIZE",
                    style = MaterialTheme.typography.labelSmall,
                    color = LegendaryGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$quantity Potions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WizardPurple,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = quantity.toFloat(),
                onValueChange = { quantity = it.toInt() },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(
                    activeTrackColor = WizardPurple,
                    inactiveTrackColor = CauldronRim,
                    thumbColor = LegendaryGold
                ),
                modifier = Modifier.testTag("potion_quantity_slider")
            )
        }

        // Residue Applicator Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = mixResidueEnabled,
                        onCheckedChange = {
                            mixResidueEnabled = it
                            if (!it) selectedResidue = null
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = WizardPurple,
                            uncheckedColor = Color.Gray,
                            checkmarkColor = DeepObsidian
                        ),
                        modifier = Modifier.testTag("checkbox_use_residue")
                    )
                    Text(
                        text = "MIX LEFTOVER ALCHEMY RESIDUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = LegendaryGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (availableResidues.isEmpty() && mixResidueEnabled) {
                    Text("(No available residues)", color = ErrorCrimson, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (mixResidueEnabled && availableResidues.isNotEmpty()) {
                Text(
                    text = "Choose Magic Residue catalyst from inventory:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableResidues) { res ->
                        val isSel = selectedResidue == res
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) GlowPurple else SumpCardColor)
                                .border(BorderStroke(1.dp, if (isSel) AlchemistGreen else CauldronRim), RoundedCornerShape(8.dp))
                                .clickable { selectedResidue = res }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(res.potionSource, color = CommonWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(res.residueType, color = LegendaryGold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Standard start date & time periods picker card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SumpCardColor),
            border = BorderStroke(1.dp, CauldronRim)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "START TIMELINE TIMESTAMP",
                    style = MaterialTheme.typography.labelSmall,
                    color = LegendaryGold,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Month dropdown
                    var monthExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1.3f)) {
                        OutlinedButton(
                            onClick = { monthExpanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CommonWhite),
                            border = BorderStroke(1.dp, CauldronRim),
                            modifier = Modifier.fillMaxWidth().testTag("picker_start_month")
                        ) {
                            Text(CampaignCalendar.getMonthName(startMonth), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                        }
                        DropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false },
                            modifier = Modifier.background(SumpCardColor)
                        ) {
                            for (m in 1..12) {
                                DropdownMenuItem(
                                    text = { Text(CampaignCalendar.getMonthName(m), color = CommonWhite, fontSize = 13.sp) },
                                    onClick = {
                                        startMonth = m
                                        monthExpanded = false
                                        val maxDays = CampaignCalendar.getLocalDate(startYear, startMonth, 1).lengthOfMonth()
                                        if (startDay > maxDays) startDay = maxDays
                                    }
                                )
                            }
                        }
                    }

                    // Day dropdown
                    var dayExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(0.8f)) {
                        OutlinedButton(
                            onClick = { dayExpanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CommonWhite),
                            border = BorderStroke(1.dp, CauldronRim),
                            modifier = Modifier.fillMaxWidth().testTag("picker_start_day")
                        ) {
                            Text(startDay.toString(), fontSize = 12.sp)
                        }
                        DropdownMenu(
                            expanded = dayExpanded,
                            onDismissRequest = { dayExpanded = false },
                            modifier = Modifier.background(SumpCardColor).heightIn(max = 200.dp)
                        ) {
                            val maxDays = CampaignCalendar.getLocalDate(startYear, startMonth, 1).lengthOfMonth()
                            for (d in 1..maxDays) {
                                DropdownMenuItem(
                                    text = { Text(d.toString(), color = CommonWhite, fontSize = 13.sp) },
                                    onClick = {
                                        startDay = d
                                        dayExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Year dropdown
                    var yearExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { yearExpanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CommonWhite),
                            border = BorderStroke(1.dp, CauldronRim),
                            modifier = Modifier.fillMaxWidth().testTag("picker_start_year")
                        ) {
                            Text(startYear.toString(), fontSize = 12.sp)
                        }
                        DropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false },
                            modifier = Modifier.background(SumpCardColor)
                        ) {
                            for (yr in listOf(2023, 2024, 2025, 2026)) {
                                DropdownMenuItem(
                                    text = { Text(yr.toString(), color = CommonWhite, fontSize = 13.sp) },
                                    onClick = {
                                        startYear = yr
                                        yearExpanded = false
                                        val maxDays = CampaignCalendar.getLocalDate(startYear, startMonth, 1).lengthOfMonth()
                                        if (startDay > maxDays) startDay = maxDays
                                    }
                                )
                            }
                        }
                    }
                }

                // Period Dropdown
                var periodExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { periodExpanded = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CommonWhite),
                        border = BorderStroke(1.dp, CauldronRim),
                        modifier = Modifier.fillMaxWidth().testTag("picker_start_period")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(CampaignCalendar.getPeriodLabel(startPeriod), fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = WizardPurple)
                        }
                    }
                    DropdownMenu(
                        expanded = periodExpanded,
                        onDismissRequest = { periodExpanded = false },
                        modifier = Modifier.fillMaxWidth().background(SumpCardColor)
                    ) {
                        CampaignCalendar.periods.forEachIndexed { idx, pId ->
                            DropdownMenuItem(
                                text = { Text(CampaignCalendar.periodLabels[idx], color = CommonWhite, fontSize = 13.sp) },
                                onClick = {
                                    startPeriod = pId
                                    periodExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Alchemical Calculations with estimated completion date
        selectedRecipe?.let { recipe ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CauldronRim),
                border = BorderStroke(1.dp, WizardPurple.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "CRUCIBLE PREDICTION FORECAST",
                        style = MaterialTheme.typography.labelSmall,
                        color = AlchemistGreen,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val gpCost = recipe.craftingPriceGp * quantity
                    val baseBrews = sqrt(recipe.craftingPriceGp.toDouble()) / 5.0
                    val speedReduced = if (selectedResidue != null) " (Active Catalyst halve roll possible!)" else ""

                    // Dynamic calculated completion date based on standard calendar
                    val startAbs = CampaignCalendar.getAbsolutePeriods(startYear, startMonth, startDay, startPeriod)
                    val estimateDuration = if (selectedResidue != null) baseBrews / 2.0 else baseBrews
                    val durationPeriods = Math.round(estimateDuration * 5.0).toLong().coerceAtLeast(1L)
                    val finishState = CampaignCalendar.getCalendarFromAbsolutePeriods(startAbs + durationPeriods)
                    val finishDateLabel = "${CampaignCalendar.getMonthName(finishState.month)} ${finishState.day}, ${finishState.year} at ${CampaignCalendar.getPeriodLabel(finishState.period)}"

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gold Cost Reagents:", fontSize = 12.sp, color = Color.LightGray)
                        Text("$gpCost GP", fontSize = 12.sp, color = LegendaryGold, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Brewing Duration:", fontSize = 12.sp, color = Color.LightGray)
                        Text(String.format(Locale.US, "%.2f Days", baseBrews) + speedReduced, fontSize = 12.sp, color = WizardPurple, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated Completion:", fontSize = 12.sp, color = Color.LightGray)
                        Text(finishDateLabel, fontSize = 11.sp, color = AlchemistGreen, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Minimum Alchemist Level:", fontSize = 12.sp, color = Color.LightGray)
                        Text("Level ${recipe.minLevel}", fontSize = 12.sp, color = CommonWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Fire Hearth / Action Button
        Button(
            onClick = {
                selectedRecipe?.let {
                    onStartBrew(it, quantity, selectedResidue, startYear, startMonth, startDay, startPeriod)
                }
            },
            enabled = selectedRecipe != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = WizardPurple,
                contentColor = DeepObsidian
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_light_furnace_brew"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "🔥 CAST BREWING SPELL / IGNITE",
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun BatchResolutionDialog(
    brew: ActiveBrew,
    onDismiss: () -> Unit,
    onResolve: (Int) -> Unit
) {
    var rollInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Parsed roll
    val d20Score = rollInput.toIntOrNull()?.coerceIn(1, 20)
    
    // Outcome prediction breakdown
    val outcomeExplanation = remember(d20Score, brew.batchQuantity) {
        val qty = brew.batchQuantity
        when (d20Score) {
            null -> "Awaiting standard mechanical physical roll enter..."
            1 -> {
                val ruined = floor(qty * 0.5).toInt()
                "NATURAL 1: Disaster! The brew combusts. You lose half the batch, yielding only $ruined finished bottle(s)."
            }
            in 2..5 -> {
                "LOW QUALITY (2-5): Complete normal yield of $qty potion(s), but 25% dilution index leaves potions half-strength when drunk."
            }
            in 6..15 -> {
                "STANDARD SUCCESS (6-15): Normal completion! Brewed $qty potion(s) successfully with zero modifiers."
            }
            in 16..19 -> {
                "GREAT SUCCESS (16-19): Masterwork standard completion! Gained $qty standard potions AND 1x free leftover magic sediment residue."
            }
            20 -> {
                val bonus = ceil(qty * 1.5).toInt()
                "NATURAL 20: Perfect Masterwork Dilution! Extra batch volume harvested! Yield boosted to $bonus potions + 1x Masterwork Catalyst residue!"
            }
            else -> "Invalid D20 format index."
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = SumpCardColor),
            border = BorderStroke(1.dp, WizardPurple),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎲 Brew Completion Roll",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = WizardPurple
                )

                Text(
                    text = "Enter physical 1D20 die score for ${brew.potionName} (${brew.batchQuantity}x):",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                // Large Roll input Box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = rollInput,
                        onValueChange = { newValue ->
                            // limit to numbers 1..20
                            if (newValue.isEmpty()) {
                                rollInput = ""
                            } else {
                                val clean = newValue.filter { it.isDigit() }
                                val numeric = clean.toIntOrNull()
                                if (numeric != null && numeric in 1..20) {
                                    rollInput = numeric.toString()
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = LegendaryGold,
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LegendaryGold,
                            unfocusedBorderColor = CauldronRim,
                            focusedContainerColor = DeepObsidian,
                            unfocusedContainerColor = DeepObsidian
                        ),
                        modifier = Modifier
                            .width(100.dp)
                            .testTag("resolution_d20_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Roll for me button
                    Button(
                        onClick = {
                            val rVal = Random.nextInt(1, 21)
                            rollInput = rVal.toString()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CauldronRim, contentColor = WizardPurple),
                        border = BorderStroke(1.dp, WizardPurple.copy(alpha = 0.5f))
                    ) {
                        Text("🎲 Roll For Me", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Interactive predictor notes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepObsidian, RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, CauldronRim), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = outcomeExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (d20Score == 20) LegendaryGold else if (d20Score == 1) ErrorCrimson else CommonWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Finish and archive interactions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Color.Gray),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Discharge")
                    }

                    Button(
                        onClick = {
                            d20Score?.let {
                                onResolve(it)
                            }
                        },
                        enabled = d20Score != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AlchemistGreen,
                            contentColor = DeepObsidian
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_archive_resolve_claim")
                    ) {
                        Text("Archive & Claim", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RarityTag(rarity: String) {
    val charValue = rarity.lowercase(Locale.US).trim()
    val (backColor, borderCol) = when (charValue) {
        "common" -> Color(0xFF1E1E1E) to CommonWhite.copy(alpha = 0.5f)
        "uncommon" -> Color(0x3326D07C) to UncommonGreen
        "rare" -> Color(0x333B82F6) to RareBlue
        "very rare" -> Color(0x33A855F7) to VeryRarePurple
        "legendary" -> Color(0x33EAB308) to LegendaryGoldAcc
        else -> Color(0xFF1E1E1E) to CommonWhite.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backColor)
            .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = rarity.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = borderCol
        )
    }
}
