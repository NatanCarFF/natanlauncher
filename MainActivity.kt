package com.natancarff.launcher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import android.provider.Settings // Importação necessária para acessar configurações do aplicativo
import android.net.Uri // Importação necessária para criar a URI do pacote
import com.natancarff.launcher.ui.theme.LauncherTheme

data class AppInfo(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LauncherTheme {
                val context = LocalContext.current
                var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
                var backgroundColor by remember { mutableIntStateOf(getDefaultBackgroundColor(context)) }
                var backgroundImageUri by remember { mutableStateOf<String?>(null) }
                var textIconColor by remember { mutableIntStateOf(getDefaultTextIconColor(context)) }

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                LaunchedEffect(Unit) {
                    allApps = getInstalledAppsList()
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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    // Abre a SettingsActivity do próprio Launcher quando um toque longo é detectado no fundo
                                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                                }
                            )
                        }
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

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        AppListScreen(
                            modifier = Modifier.padding(innerPadding),
                            apps = allApps,
                            onAppClick = { packageName ->
                                // Abre o aplicativo normalmente
                                val intent = packageManager.getLaunchIntentForPackage(packageName)
                                intent?.let {
                                    startActivity(it)
                                }
                            },
                            // NOVO: Adiciona onAppLongClick para lidar com o toque longo no ícone
                            onAppLongClick = { packageName ->
                                // Abre as configurações do aplicativo no sistema
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                    addCategory(Intent.CATEGORY_DEFAULT)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Importante para iniciar como nova tarefa
                                }
                                startActivity(intent)
                            },
                            onSettingsClick = {
                                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                            },
                            textIconColor = Color(textIconColor)
                        )
                    }
                }
            }
        }
    }

    private fun getDefaultBackgroundColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_BACKGROUND_COLOR_KEY, Color.White.toArgb())
    }

    fun getInstalledAppsList(): List<AppInfo> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = packageManager.queryIntentActivities(mainIntent, 0)
        val appList = mutableListOf<AppInfo>()

        for (resolveInfo in apps) {
            val packageName = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(packageManager).toString()
            val icon = resolveInfo.loadIcon(packageManager)
            appList.add(AppInfo(label, packageName, icon))
        }
        return appList.sortedBy { it.label }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onAppLongClick: (String) -> Unit, // NOVO: Parâmetro para o toque longo no app
    onSettingsClick: () -> Unit,
    textIconColor: Color
) {
    var searchText by remember { mutableStateOf("") }

    val filteredApps = apps.filter {
        it.label.contains(searchText, ignoreCase = true)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Buscar aplicativos", color = textIconColor) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = textIconColor,
                    focusedTextColor = textIconColor,
                    unfocusedTextColor = textIconColor,
                    disabledTextColor = textIconColor,
                    errorTextColor = textIconColor,
                    focusedLeadingIconColor = textIconColor,
                    unfocusedLeadingIconColor = textIconColor,
                    disabledLeadingIconColor = textIconColor,
                    errorLeadingIconColor = textIconColor
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Ícone de Busca"
                    )
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Configurações", tint = textIconColor)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(filteredApps) { app ->
                // Passa o onAppLongClick para o AppItem
                AppItem(app = app, onAppClick = onAppClick, onAppLongClick = onAppLongClick, textIconColor = textIconColor)
            }
        }

        if (filteredApps.isEmpty() && searchText.isNotEmpty()) {
            Text(
                text = "Nenhum aplicativo encontrado para \"$searchText\".",
                modifier = Modifier.padding(16.dp),
                color = textIconColor
            )
        } else if (apps.isEmpty()) {
            Text(
                text = "Nenhum aplicativo encontrado. Verifique as permissões ou se há aplicativos instalados.",
                modifier = Modifier.padding(16.dp),
                color = textIconColor
            )
        }
    }
}

@Composable
fun AppItem(app: AppInfo, onAppClick: (String) -> Unit, onAppLongClick: (String) -> Unit, textIconColor: Color) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            // MODIFICADO: Usa pointerInput para gerenciar toques curtos e longos
            .pointerInput(app.packageName) { // Use app.packageName como key para que o modifier seja recriado se o pacote mudar
                detectTapGestures(
                    onTap = { onAppClick(app.packageName) }, // Toque curto: abre o app
                    onLongPress = { onAppLongClick(app.packageName) } // Toque longo: abre as configurações do app
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = app.icon),
            contentDescription = "Ícone do ${app.label}",
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(text = app.label, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textIconColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun AppListScreenPreview() {
    LauncherTheme {
        val sampleApps = listOf(
            AppInfo("WhatsApp", "com.whatsapp", android.graphics.drawable.ColorDrawable(0xFF25D366.toInt())),
            AppInfo("Instagram", "com.instagram", android.graphics.drawable.ColorDrawable(0xFFC13584.toInt())),
            AppInfo("Facebook", "com.facebook", android.graphics.drawable.ColorDrawable(0xFF4267B2.toInt())),
            AppInfo("YouTube", "com.youtube", android.graphics.drawable.ColorDrawable(0xFFFF0000.toInt())),
            AppInfo("Google Chrome", "com.android.chrome", android.graphics.drawable.ColorDrawable(0xFF4285F4.toInt()))
        )
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent
            ) { innerPadding ->
                AppListScreen(
                    apps = sampleApps,
                    onAppClick = {},
                    onAppLongClick = {}, // Fornece um lambda vazio para o preview
                    onSettingsClick = {},
                    modifier = Modifier.padding(innerPadding),
                    textIconColor = Color.Black
                )
            }
        }
    }
}