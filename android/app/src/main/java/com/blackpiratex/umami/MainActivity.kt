package com.blackpiratex.umami

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    companion object {
        const val UMAMI_BASE_URL = UmamiApplication.UMAMI_BASE_URL
        const val PRIMARY_HOST = UmamiApplication.PRIMARY_HOST
        const val AUTH_TOKEN = UmamiApplication.AUTH_TOKEN
    }

    private lateinit var webView: WebView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (fileUploadCallback != null) {
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            fileUploadCallback?.onReceiveValue(uris)
            fileUploadCallback = null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Immersive edge-to-edge window styling with AMOLED black baseline
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Enable Chrome DevTools inspection
        WebView.setWebContentsDebuggingEnabled(true)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        // Root FrameLayout
        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#040407"))
        }

        // SwipeRefreshLayout (desabilitado para gestos acidentais; a recarga ocorre de forma específica e deliberada)
        swipeRefreshLayout = SwipeRefreshLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#040407"))
            setProgressBackgroundColorSchemeColor(Color.parseColor("#121524"))
            setColorSchemeColors(Color.parseColor("#8b5cf6"), Color.parseColor("#06b6d4"))
            isEnabled = false
        }

        // Hardware-accelerated WebView enabling WebGL shaders and liquid crystal glass
        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isNestedScrollingEnabled = false
        }

        // High priority GPU rasterization
        webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)

        // Sleek progress bar
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                8
            )
            max = 100
            progressDrawable = getDrawable(android.R.drawable.progress_horizontal)
            visibility = View.GONE
        }

        // Apply Window Insets for status and nav bar spacing
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            swipeRefreshLayout.setPadding(0, statusBars.top, 0, navBars.bottom)
            windowInsets
        }

        swipeRefreshLayout.addView(webView)
        rootLayout.addView(swipeRefreshLayout)
        rootLayout.addView(progressBar)
        setContentView(rootLayout)

        setupWebSettings()
        setupCookies()
        setupClients()

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            // Load direct websites dashboard for instant fast start
            webView.loadUrl(UmamiApplication.TARGET_URL)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebSettings() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            defaultTextEncodingName = "utf-8"
            loadsImagesAutomatically = true
            blockNetworkImage = false

            // Turbo boost: offscreen pre-rendering eliminates stutter
            offscreenPreRaster = true

            @Suppress("DEPRECATION")
            setRenderPriority(WebSettings.RenderPriority.HIGH)

            userAgentString = "$userAgentString UmamiMobile/1.3 (Android; MaxFastPreloader)"
        }
    }

    private fun setupCookies() {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)

            val encodedToken = URLEncoder.encode(AUTH_TOKEN, "UTF-8")
            val cookieValue = "umami.auth=$encodedToken; Path=/; Max-Age=315360000; SameSite=Lax; Secure"

            cookieManager.setCookie(UMAMI_BASE_URL, cookieValue)
            cookieManager.setCookie("https://$PRIMARY_HOST", cookieValue)
            cookieManager.setCookie("https://vercel.app", cookieValue)
            cookieManager.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun injectSessionStorage(view: WebView?) {
        try {
            val encodedToken = URLEncoder.encode(AUTH_TOKEN, "UTF-8")
            val script = """
                (function() {
                    try {
                        var rawToken = decodeURIComponent('$encodedToken');
                        var serialized = JSON.stringify(rawToken);
                        if (localStorage.getItem('umami.auth') !== serialized) {
                            localStorage.setItem('umami.auth', serialized);
                        }
                    } catch (e) {
                        console.error('Umami session injection error:', e);
                    }
                })();
            """.trimIndent()
            view?.evaluateJavascript(script, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupClients() {
        val app = application as UmamiApplication

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val cached = app.cacheManager.interceptRequest(request)
                if (cached != null) {
                    return cached
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return false
                val host = url.host ?: ""
                val path = url.path ?: ""

                if (path == "/login" || path.startsWith("/login")) {
                    injectSessionStorage(view)
                    view?.loadUrl(UmamiApplication.TARGET_URL)
                    return true
                }

                if (host.contains(PRIMARY_HOST) || host.contains("vercel.app") || host.contains("umami.is")) {
                    return false
                }

                return try {
                    val intent = Intent(Intent.ACTION_VIEW, url)
                    startActivity(intent)
                    true
                } catch (e: ActivityNotFoundException) {
                    false
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                injectSessionStorage(view)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                injectSessionStorage(view)
                CookieManager.getInstance().flush()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    swipeRefreshLayout.isRefreshing = false
                    progressBar.visibility = View.GONE
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback

                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }

                return try {
                    filePickerLauncher.launch(intent)
                    true
                } catch (e: ActivityNotFoundException) {
                    fileUploadCallback = null
                    Toast.makeText(this@MainActivity, "No file manager found", Toast.LENGTH_SHORT).show()
                    false
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                return super.onConsoleMessage(consoleMessage)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        CookieManager.getInstance().flush()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        CookieManager.getInstance().flush()
    }

    fun triggerExplicitReload() {
        (application as? UmamiApplication)?.cacheManager?.invalidateShellCache()
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 15
        webView.clearCache(false)
        webView.reload()
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun reloadPage() {
            runOnUiThread {
                triggerExplicitReload()
            }
        }

        @JavascriptInterface
        fun vibrate(milliseconds: Long) {
            runOnUiThread {
                try {
                    val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(
                            VibrationEffect.createOneShot(
                                milliseconds.coerceIn(5, 100),
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(milliseconds.coerceIn(5, 100))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onDestroy() {
        CookieManager.getInstance().flush()
        webView.destroy()
        super.onDestroy()
    }
}
