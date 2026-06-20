package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.theme.MyApplicationTheme
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "campking_prefs"
    private val KEY_BASE_URL = "base_url"
    private val DEFAULT_URL = "https://campking.in/present/app"

    // States for interaction with dialogs
    private val showSettings = mutableStateOf(false)
    private val showExitConfirmation = mutableStateOf(false)
    private val isOfflineState = mutableStateOf(false)
    private val tapCount = mutableIntStateOf(0)
    private var lastTapTime: Long = 0

    // Reference to the created webView so we can reload it
    private var mainWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fully enable edge-to-edge drawing
        enableEdgeToEdge()
        hideSystemUI()

        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val initialUrl = sharedPref.getString(KEY_BASE_URL, DEFAULT_URL) ?: DEFAULT_URL

        // Verify initial network connectivity on start
        isOfflineState.value = !isNetworkAvailable(this)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0A) // Ultra-luxurious velvet charcoal black background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        
                        // 1. Core Presenter WebView wrapper or Offline screen
                        if (isOfflineState.value) {
                            OfflineUI(
                                onRetry = {
                                    if (isNetworkAvailable(this@MainActivity)) {
                                        isOfflineState.value = false
                                        mainWebView?.reload()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Network still unavailable", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        } else {
                            WebViewContainer(
                                initialUrl = initialUrl,
                                onWebViewCreated = { webView ->
                                    mainWebView = webView
                                },
                                onReceivedError = {
                                    isOfflineState.value = true
                                },
                                onTapHeader = {
                                    handleInteractiveTaps()
                                }
                            )
                        }

                        // 2. Hidden Settings Dialog Overlay
                        if (showSettings.value) {
                            SettingsDialog(
                                currentUrl = sharedPref.getString(KEY_BASE_URL, DEFAULT_URL) ?: DEFAULT_URL,
                                onSave = { newUrl ->
                                    sharedPref.edit().putString(KEY_BASE_URL, newUrl).apply()
                                    showSettings.value = false
                                    isOfflineState.value = !isNetworkAvailable(this@MainActivity)
                                    mainWebView?.loadUrl(newUrl)
                                    Toast.makeText(this@MainActivity, "URL Saved permanently", Toast.LENGTH_SHORT).show()
                                },
                                onDefault = {
                                    sharedPref.edit().putString(KEY_BASE_URL, DEFAULT_URL).apply()
                                    showSettings.value = false
                                    isOfflineState.value = !isNetworkAvailable(this@MainActivity)
                                    mainWebView?.loadUrl(DEFAULT_URL)
                                    Toast.makeText(this@MainActivity, "Restored default presenter URL", Toast.LENGTH_SHORT).show()
                                },
                                onExit = {
                                    showSettings.value = false
                                    showExitConfirmation.value = true
                                },
                                onDismiss = {
                                    showSettings.value = false
                                }
                            )
                        }

                        // 3. Exit Dialog Confirmation
                        if (showExitConfirmation.value) {
                            ExitDialog(
                                onConfirm = {
                                    finishAffinity()
                                    exitProcess(0)
                                },
                                onDismiss = {
                                    showExitConfirmation.value = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    /**
     * Completely hides system status/navigation bars in high-res 4K for absolute fullscreen kiosk experience
     */
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    /**
     * Touch/Click sequence tracker (Tapping 5 times on top right area triggers hidden settings)
     */
    private fun handleInteractiveTaps() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTapTime < 1200) {
            tapCount.value += 1
            if (tapCount.value >= 5) {
                showSettings.value = true
                tapCount.value = 0
            }
        } else {
            tapCount.value = 1
        }
        lastTapTime = currentTime
    }

    /**
     * Handle physical Android TV remote buttons
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Initiate backtracking or start tracking for back long-press (3s) to display exit dialog
            event.startTracking()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU) {
            // Track central selection click long-press (3s) to pop active settings
            event.startTracking()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Mandatory requirement 8: Long-press Back button for 3 seconds opens exit dialog
            showExitConfirmation.value = true
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU) {
            // Secondary remote shortcut: Long-press select/OK button for 3 seconds triggers standard hidden settings
            showSettings.value = true
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Standard back key behavior: if WebView can go back, do so; otherwise ask to exit to protect user flow
            if (mainWebView?.canGoBack() == true) {
                mainWebView?.goBack()
            } else {
                showExitConfirmation.value = true
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    /**
     * Safe internet checker utility
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}

/**
 * Highly optimized, full viewport WebView Container with Hardware Acceleration and cookie retention
 */
@Composable
fun WebViewContainer(
    initialUrl: String,
    onWebViewCreated: (WebView) -> Unit,
    onReceivedError: () -> Unit,
    onTapHeader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Maintain single live WebView instance to prevent multi-recomposition recreations
    val webView = remember {
        WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            // Highly robust, ultra-scaled high-performance settings for Android TV
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Keep video autoplay working perfectly on TV without requiring clicks
                mediaPlaybackRequiresUserGesture = false
                
                // viewport fit scaling properties
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                setBuiltInZoomControls(false)
                setDisplayZoomControls(false)
                
                // Allow safe file configurations and mixed contents
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                // Set custom high-performance user agent representing clean high fidelity TV rendering
                userAgentString = "Mozilla/5.0 (Linux; Android 10; BRAVIA 4K UR3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.127 Safari/537.36 CampkingTV/1.0"
            }

            // Explicitly enable deep hardware layers
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            // Persist standard user cookies permanently across TV shutdown
            val currentWebView = this
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(currentWebView, true)
            }

            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    // Ensure the error is related to key content delivery (Main Frame load failures)
                    if (request?.isForMainFrame == true) {
                        onReceivedError()
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Flush session cookies to physical storage to guarantee lifetime retention across power loss
                    CookieManager.getInstance().flush()
                }
            }
        }
    }

    LaunchedEffect(initialUrl) {
        webView.loadUrl(initialUrl)
        onWebViewCreated(webView)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )

        // Ultra-discreet invisible top-right click trigger area for mouse/pointer screen taps to trigger hidden settings
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(120.dp)
                .height(80.dp)
                .background(Color.Transparent)
                .clickable { onTapHeader() }
        )
    }
}

/**
 * Premium luxury offline placeholder screen themed carefully in Velvet Gold
 */
@Composable
fun OfflineUI(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val luxuryGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E1E1D), Color(0xFF0F0F0E))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(luxuryGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Elegant Gold warning crown badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color(0x11FFC83B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "No Internet Connection Logo",
                    tint = Color(0xFFFFC83B),
                    modifier = Modifier.size(45.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Campking Presenter",
                color = Color(0xFFFFC83B),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "No Internet Connection",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please check your network connection of TV settings.",
                color = Color(0xFFB5B5B5),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC83B),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFFF1C6)),
                modifier = Modifier
                    .width(220.dp)
                    .height(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry Icon",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RETRY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * Premium stylized TV dialog showing hidden presentation Base URL setting Configuration details
 * This accurately replicates the design tokens and layout structure of the "Elegant Dark" design theme:
 * - Top Status block (Emerald dot, System Ready, 4K UHD status)
 * - Brand badge showing gold gradient base + Capital serif "C" character
 * - Beautiful `#161618` card containers with `#232325` sub-toggles
 * - MONO-spaced target input fields styled strictly on `#2C2C2E` backgrounds with custom Gold `#D4AF37` outline glows
 * - Clean high-contrast action list buttons and custom controller icon footer guides
 */
@Composable
fun SettingsDialog(
    currentUrl: String,
    onSave: (String) -> Unit,
    onDefault: () -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit
) {
    var urlInput by remember { mutableStateOf(currentUrl) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .width(580.dp)
                .wrapContentSize()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0A0A0A), // Black Velvet elegant backdrop
                contentColor = Color(0xFFA0AEC0)
            ),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF232325))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A0A))
            ) {
                // 1. TV Status Bar Accent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF10B981)) // Emerald Green System Ready
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "System Ready",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "4K UHD • NATIVE",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp
                    )
                }

                // Divider line
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF1C1C1E))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // 2. Branding Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // C Gold Badge
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFD4AF37), Color(0xFF8B6B23))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "C",
                                color = Color.Black,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Campking Presenter",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Enterprise Web Shell v2.4.0",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // 3. Configuration Card Block
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF161618)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF2C2C2E))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "TARGET BASE URL",
                                color = Color(0xFFD4AF37),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Custom outlined look aligned style with inner URL
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color(0xFFCBD5E1),
                                    focusedBorderColor = Color(0xFFD4AF37),
                                    unfocusedBorderColor = Color(0xFF444444),
                                    focusedContainerColor = Color(0xFF2C2C2E),
                                    unfocusedContainerColor = Color(0xFF202022)
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Primary production endpoint for presentation assets.",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Grid-style toggles description
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Resolution Pill
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF232325))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "RESOLUTION",
                                            color = Color(0xFF64748B),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Native 4K UHD",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Persistence Pill
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF232325))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "PERSISTENCE",
                                            color = Color(0xFF64748B),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Login Retained",
                                            color = Color(0xFF10B981), // Emerald green persistence indicator
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Command Action Panel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onSave(urlInput) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(2f)
                                .height(54.dp)
                        ) {
                            Text(
                                text = "Launch Presentation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Exit app trigger styled exactly like the Web Shell SVG action
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1C1C1E))
                                .clickable { onExit() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Exit App Icon",
                                tint = Color(0xFFEF4444), // red alert
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onDefault,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x15FFFFFF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Reset Default", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Dismiss Menu", fontSize = 12.sp)
                        }
                    }
                }

                // 2. Interaction Hint Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0F11))
                        .padding(vertical = 14.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // BACK pill
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E1E20))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "BACK",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Long press (3s) to exit shell",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }

                        // Divider line
                        Spacer(modifier = Modifier.width(20.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(12.dp)
                                .background(Color(0xFF2E2E32))
                        )
                        Spacer(modifier = Modifier.width(20.dp))

                        // OK pill
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E1E20))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "OK",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit Target URL Route",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Elegant exit app popup prompt
 */
@Composable
fun ExitDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(420.dp)
                .wrapContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF141414),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0x33FFC83B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Exit Alert Icon",
                    tint = Color(0xFF8B1212),
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Exit Campking Presenter?",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Are you sure you want to exit the presentation player?",
                    color = Color(0xFF888888),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E2E2E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B1212),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("EXIT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
