package com.natancarff.launcher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box // Importação adicionada
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults // Importação adicionada para cores da TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect // Importação adicionada
import androidx.compose.runtime.LaunchedEffect // Importação adicionada
import androidx.compose.runtime.mutableIntStateOf // Importação adicionada
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue // Importação adicionada
import androidx.compose.runtime.setValue // Importação adicionada
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale // Importação adicionada
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage // Importação adicionada
import com.natancarff.launcher.ui.theme.LauncherTheme

const val PREFS_NAME = "LauncherPrefs"
const val PREF_BACKGROUND_COLOR_KEY = "backgroundColor"
const val PREF_BACKGROUND_IMAGE_URI_KEY = "backgroundImageUri"
const val PREF_TEXT_ICON_COLOR_KEY = "textIconColor"

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauncherTheme {
                val context = LocalContext.current
                var backgroundColor by remember { mutableIntStateOf(getDefaultBackgroundColor(context)) }
                var backgroundImageUri by remember { mutableStateOf<String?>(null) }
                var textIconColor by remember { mutableIntStateOf(getDefaultTextIconColor(context)) }

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                LaunchedEffect(Unit) {
                    backgroundColor = prefs.getInt(PREF_BACKGROUND_COLOR_KEY, Color.White.toArgb())
                    backgroundImageUri = prefs.getString(PREF_BACKGROUND_IMAGE_URI_KEY, null)
                    textIconColor = prefs.getInt(PREF_TEXT_ICON_COLOR_KEY, Color.Black.toArgb())
                }

                DisposableEffect(Unit) {
                    val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                        when (key) {
                            PREF_BACKGROUND_COLOR_KEY -> {
                                backgroundColor = sharedPreferences.getInt(PREF_BACKGROUND_COLOR_KEY, Color.White.toArgb())
                            }
                            PREF_BACKGROUND_IMAGE_URI_KEY -> {
                                backgroundImageUri = sharedPreferences.getString(PREF_BACKGROUND_IMAGE_URI_KEY, null)
                            }
                            PREF_TEXT_ICON_COLOR_KEY -> {
                                textIconColor = sharedPreferences.getInt(PREF_TEXT_ICON_COLOR_KEY, Color.Black.toArgb())
                            }
                        }
                    }
                    prefs.registerOnSharedPreferenceChangeListener(listener)
                    onDispose {
                        prefs.unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }

                Box( // Box para aplicar o background
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (backgroundImageUri != null) {
                        AsyncImage(
                            model = backgroundImageUri,
                            contentDescription = "Background Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(backgroundColor))
                        )
                    }

                    SettingsScreen(
                        onBackPressed = { finish() },
                        context = context,
                        textIconColor = Color(textIconColor) // Passa a cor do texto/ícone
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackPressed: () -> Unit, context: Context, textIconColor: Color) { // textIconColor como parâmetro
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            saveBackgroundImageUri(context, it.toString())
            clearBackgroundColor(context)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent, // Fundo transparente para o Box de background ser visível
        topBar = {
            TopAppBar(
                title = { Text("Configurações", color = textIconColor) }, // Aplica cor ao título
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = textIconColor) // Aplica cor ao ícone de voltar
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent // Deixa a TopAppBar transparente
                )
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Cor de Fundo Padrão",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textIconColor, // Aplica cor ao cabeçalho
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                ColorOption(color = Color.White) { selectedColor ->
                    saveBackgroundColor(context, selectedColor)
                }
            }
            item {
                ColorOption(color = Color.LightGray) { selectedColor ->
                    saveBackgroundColor(context, selectedColor)
                }
            }
            item {
                ColorOption(color = Color.Cyan) { selectedColor ->
                    saveBackgroundColor(context, selectedColor)
                }
            }
            item {
                ColorOption(color = Color.Yellow) { selectedColor ->
                    saveBackgroundColor(context, selectedColor)
                }
            }
            item {
                ColorOption(color = Color.Magenta) { selectedColor ->
                    saveBackgroundColor(context, selectedColor)
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Plano de Fundo de Imagem",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textIconColor, // Aplica cor ao cabeçalho
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                ActionOption(text = "Selecionar Imagem da Galeria", textIconColor = textIconColor) { // Passa textIconColor
                    imagePickerLauncher.launch("image/*")
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                ActionOption(text = "Remover Imagem de Fundo", textIconColor = textIconColor) { // Passa textIconColor
                    clearBackgroundImageUri(context)
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Cor dos Ícones e Texto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textIconColor, // Aplica cor ao cabeçalho
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }
            item {
                ColorOption(color = Color.Black) { selectedColor ->
                    saveTextIconColor(context, selectedColor)
                }
            }
            item {
                ColorOption(color = Color.White) { selectedColor ->
                    saveTextIconColor(context, selectedColor)
                }
            }
            item {
                ColorOption(color = Color.Gray) { selectedColor ->
                    saveTextIconColor(context, selectedColor)
                }
            }
            item {
                ColorOption(color = Color.Red) { selectedColor ->
                    saveTextIconColor(context, selectedColor)
                }
            }
            item {
                ColorOption(color = Color.Blue) { selectedColor ->
                    saveTextIconColor(context, selectedColor)
                }
            }
        }
    }
}

// === Funções auxiliares para SharedPreferences (mantidas do estado anterior) ===

fun saveBackgroundImageUri(context: Context, uri: String) {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(PREF_BACKGROUND_IMAGE_URI_KEY, uri).apply()
}

fun clearBackgroundImageUri(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().remove(PREF_BACKGROUND_IMAGE_URI_KEY).apply()
}

fun saveBackgroundColor(context: Context, color: Color) {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(PREF_BACKGROUND_COLOR_KEY, color.toArgb()).apply()
    clearBackgroundImageUri(context)
}

fun clearBackgroundColor(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().remove(PREF_BACKGROUND_COLOR_KEY).apply()
}

fun saveTextIconColor(context: Context, color: Color) {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(PREF_TEXT_ICON_COLOR_KEY, color.toArgb()).apply()
}

fun getDefaultBackgroundColor(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(PREF_BACKGROUND_COLOR_KEY, Color.White.toArgb())
}

fun getDefaultTextIconColor(context: Context): Int {
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(PREF_TEXT_ICON_COLOR_KEY, Color.Black.toArgb())
}

// === Composable para as opções de cores (ajustado para visual clean) ===

// Função auxiliar para determinar a cor do texto de contraste
fun getContrastingTextColor(backgroundColor: Color): Color {
    val luminance = backgroundColor.red * 0.299 + backgroundColor.green * 0.587 + backgroundColor.blue * 0.114
    return if (luminance > 0.5) Color.Black else Color.White // Preto para fundo claro, branco para fundo escuro
}

@Composable
fun ColorOption(color: Color, onColorSelected: (Color) -> Unit) {
    val colorName = remember { mutableStateOf(getColorName(color)) }
    val textColor = getContrastingTextColor(color) // Cor do texto baseada na cor de fundo da opção
    Text(
        text = colorName.value,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color = color)
            .clickable { onColorSelected(color) }
            .padding(16.dp),
        color = textColor // Aplica a cor do texto de contraste
    )
}

// Novo Composable para opções de ação (Selecionar/Remover Imagem)
@Composable
fun ActionOption(text: String, textIconColor: Color, onClick: () -> Unit) { // textIconColor como parâmetro
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer) // Mantém a cor de fundo do tema
            .clickable { onClick() }
            .padding(16.dp),
        color = textIconColor // Aplica a cor definida pelo usuário
    )
}

fun getColorName(color: Color): String {
    return when (color) {
        Color.White -> "Branco"
        Color.Black -> "Preto"
        Color.LightGray -> "Cinza Claro"
        Color.Cyan -> "Ciano"
        Color.Yellow -> "Amarelo"
        Color.Magenta -> "Magenta"
        Color.Gray -> "Cinza"
        Color.Red -> "Vermelho"
        Color.Blue -> "Azul"
        else -> "Cor Personalizada"
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    LauncherTheme {
        SettingsScreen(onBackPressed = {}, context = LocalContext.current, textIconColor = Color.Black)
    }
}