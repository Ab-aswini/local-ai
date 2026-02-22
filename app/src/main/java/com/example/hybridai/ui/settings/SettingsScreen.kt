package com.example.hybridai.ui.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hybridai.data.ModelCatalog
import com.example.hybridai.data.ModelInfo
import com.example.hybridai.data.PersonaCatalog
import com.example.hybridai.ui.theme.*

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val tabs = listOf("🔑 Cloud AI", "🧠 Local Models", "🎨 Theme", "🤖 Persona")
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkGray)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryAccent)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Settings", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkGray,
            contentColor = PrimaryAccent,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = OnlineIndicator
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) PrimaryAccent else SecondaryAccent,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> CloudAITab(settingsViewModel)
            1 -> LocalModelsTab(settingsViewModel)
            2 -> ThemeTab(settingsViewModel)
            3 -> PersonaTab(settingsViewModel)
        }
    }
}

// ─── Tab 1: Cloud AI ──────────────────────────────────────────────────────────

@Composable
fun CloudAITab(vm: SettingsViewModel) {
    val apiKey by vm.geminiApiKey.collectAsState()
    var inputKey by remember(apiKey) { mutableStateOf(apiKey) }
    var showKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Gemini API Key", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Text(
            "Get a free key at aistudio.google.com/app/apikey\nEnables real AI responses for complex questions.",
            color = SecondaryAccent,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )

        OutlinedTextField(
            value = inputKey,
            onValueChange = {
                inputKey = it
                saved = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Paste API key here", color = SecondaryAccent) },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(
                        if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = SecondaryAccent
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OnlineIndicator,
                unfocusedBorderColor = SurfaceGray,
                focusedTextColor = PrimaryAccent,
                unfocusedTextColor = PrimaryAccent,
                cursorColor = OnlineIndicator
            )
        )

        Button(
            onClick = {
                vm.saveApiKey(inputKey)
                saved = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = OnlineIndicator),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = TrueBlack)
            Spacer(Modifier.width(8.dp))
            Text("Save API Key", color = TrueBlack, fontWeight = FontWeight.Bold)
        }

        if (saved) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LocalIndicator, modifier = Modifier.size(16.dp))
                Text("API key saved! Cloud AI is now active.", color = LocalIndicator, fontSize = 13.sp)
            }
        }

        if (apiKey.isNotEmpty() && !saved) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Cloud, contentDescription = null, tint = OnlineIndicator, modifier = Modifier.size(16.dp))
                Text("Cloud AI is active.", color = OnlineIndicator, fontSize = 13.sp)
            }
        }
    }
}

// ─── Tab 2: Local Models ─────────────────────────────────────────────────────

@Composable
fun LocalModelsTab(vm: SettingsViewModel) {
    val downloadProgress by vm.downloadProgress.collectAsState()
    val activeModelName by vm.selectedModelName.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Download a local model to run AI entirely on your device — no internet needed.",
                color = SecondaryAccent,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(ModelCatalog.models) { model ->
            ModelCard(
                model = model,
                isActive = activeModelName == model.name,
                isDownloaded = vm.isDownloaded(model),
                progress = downloadProgress[model.id],
                onDownload = { vm.downloadModel(model) },
                onActivate = { vm.activateModel(model) },
                onDelete = { vm.deleteModel(model) }
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun ModelCard(
    model: ModelInfo,
    isActive: Boolean,
    isDownloaded: Boolean,
    progress: Float?,
    onDownload: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    val isDownloading = progress != null && progress > 0f && progress < 1f
    val borderColor = if (isActive) LocalIndicator else SurfaceGray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(DarkGray)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(model.name, color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (isActive) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LocalIndicator)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("ACTIVE", color = TrueBlack, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(model.description, color = SecondaryAccent, fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Chip("💾 ${model.sizeLabel}")
                    Chip("🧠 ${model.ramRequired}")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Progress bar
        if (isDownloading) {
            LinearProgressIndicator(
                progress = progress ?: 0f,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color = LocalIndicator,
                trackColor = SurfaceGray
            )
            Spacer(Modifier.height(8.dp))
            Text("Downloading... ${((progress ?: 0f) * 100).toInt()}%", color = SecondaryAccent, fontSize = 12.sp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    isDownloaded && isActive -> {
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", fontSize = 13.sp)
                        }
                    }
                    isDownloaded -> {
                        Button(
                            onClick = onActivate,
                            colors = ButtonDefaults.buttonColors(containerColor = LocalIndicator),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TrueBlack, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Use this model", color = TrueBlack, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                    else -> {
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceGray),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Download", color = PrimaryAccent, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceGray)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = SecondaryAccent, fontSize = 11.sp)
    }
}

// ─── Tab 3: Theme ────────────────────────────────────────────────────────────

@Composable
fun ThemeTab(vm: SettingsViewModel) {
    val currentTheme by vm.appTheme.collectAsState()

    val themes = listOf(
        Triple("amoled", "⚫ AMOLED Black", "True black — saves battery on OLED screens"),
        Triple("dark", "🌙 Dark", "Deep dark grey — easier on eyes for long use"),
        Triple("light", "☀️ Light", "Light mode — great for outdoor use")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("App Theme", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)

        themes.forEach { (id, title, description) ->
            val isSelected = currentTheme == id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isSelected) LocalIndicator else SurfaceGray,
                        RoundedCornerShape(12.dp)
                    )
                    .background(if (isSelected) SurfaceGray else DarkGray)
                    .clickable { vm.setTheme(id) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = PrimaryAccent, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text(description, color = SecondaryAccent, fontSize = 12.sp)
                }
                if (isSelected) {
                    Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = LocalIndicator)
                } else {
                    Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = SecondaryAccent)
                }
            }
        }
    }
}

// ─── Tab 4: Persona ──────────────────────────────────────────────────────────

@Composable
fun PersonaTab(vm: SettingsViewModel) {
    val selectedId by vm.selectedPersonaId.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                "Choose AI Persona",
                color = PrimaryAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "The persona sets the AI's personality and expertise for every response.",
                color = SecondaryAccent,
                fontSize = 12.sp
            )
        }

        items(PersonaCatalog.personas) { persona ->
            val isActive = persona.id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isActive) SurfaceGray
                        else DarkGray
                    )
                    .border(
                        width = if (isActive) 1.dp else 0.dp,
                        color = if (isActive) LocalIndicator else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { vm.savePersona(persona.id) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(persona.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            persona.name,
                            color = if (isActive) PrimaryAccent else SecondaryAccent,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                        Text(
                            persona.description,
                            color = SecondaryAccent,
                            fontSize = 12.sp
                        )
                    }
                }
                if (isActive) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = LocalIndicator,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
