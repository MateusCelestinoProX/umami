package com.blackpiratex.umami

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : ComponentActivity() {

    companion object {
        const val UMAMI_BASE_URL = "https://umami-chi-topaz.vercel.app"
        const val PRIMARY_HOST = "umami-chi-topaz.vercel.app"
        const val INJECTED_USER = "mateuscelestino"
        const val INJECTED_AUTH_TOKEN =
            "AZ3LnzBh1mE9H1wO86LIGs8VJlSaY7rEPJPng9ikgODVBTh61e8gjMWigMtfFrKm3AGvbbi7bk1U7LT47G9OFrLS5EeCk6vJ6MtJQti2S/Kc87Q1RBJBKCigTVAl3ps6nYQy+b78cvWeFF3PT8W1SmzDobmRg0inMuonkcboe9k6TkakKdpAzE7P51fHBpQc9m3/ZL8Fz52hq/BoW8Xlm5OSNSQ4d5JsOZilbf3MvPXIvbOM3Nh21NBDlYc+YbFNUn1MLVOedthXjUMNj9AmpQKxeF0kFO4cg/OvYcENg6y/Q4M5iSa+fQRntgZwo2l2LiPIjPEi0NwPIz7QgXwBVUMFPJc6GdrSMogm1t20yHzP0rM+gU+BmTBHNLL9y+9oD+MDMCm3jEaLqnst5FMp97EuWpqUorgOeDE14rtkc8WZIlMpsWb23roijiuRHdQGrfIt9l9gSpF3P/E8jl7wiNcZRFAlS2b9goZDouWb1oGJSGJldEPTBnuFaXUwZgK8mLF88KKb+xt59gTqnjwtnZ19gvqdGo7gnw7ug1an1biH94t91oVSSQ6vfN8uzwnU3GmMgUzEBxf04a5U02dzqpQIojodTDX4kfXljg5KmACtAUwO"
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

        // Configure edge-to-edge window with immersive AMOLED dark theme
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        // False = white/light status bar icons (for dark background)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        // Root layout
        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        // SwipeRefreshLayout
        swipeRefreshLayout = SwipeRefreshLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            setProgressBackgroundColorSchemeColor(Color.parseColor("#18181b")) // Zinc 900
            setColorSchemeColors(Color.parseColor("#8b5cf6")) // Purple accent
        }

        // WebView with hardware acceleration for WebGL liquid crystal shaders
        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
        }

        // Progress bar for sleek loading indication
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                6
            )
            max = 100
            progressDrawable = getDrawable(android.R.drawable.progress_horizontal)
            visibility = View.GONE
        }

        // Apply Window Insets for proper status and navigation bar padding
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

        // Configure WebSettings for maximum performance and full modern web support
        setupWebSettings()

        // Configure Cookies for permanent 10-year session tokens
        setupCookies()

        // Configure Clients
        setupClients()

        // Configure Pull to Refresh
        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        // Handle Back Navigation: navigate inside Umami history if available
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })

        // Pre-populate native session if needed
        val app = application as? UmamiApplication
        if (app != null && !app.sessionManager.isLoggedIn()) {
            app.sessionManager.saveSession(
                serverUrl = UMAMI_BASE_URL,
                token = INJECTED_AUTH_TOKEN,
                username = INJECTED_USER
            )
            app.sessionManager.saveSelectedWebsite("f4cb9526-35ff-4975-9e7b-1691b79800c2")
        }

        // Restore state or load URL with pre-injected authentication
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            val authHeaders = mapOf("Authorization" to "Bearer $INJECTED_AUTH_TOKEN")
            webView.loadUrl(UMAMI_BASE_URL, authHeaders)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebSettings() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(true)
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
            userAgentString = "$userAgentString UmamiMobile/1.1 (Android; NativeContainer)"
        }
    }

    private fun setupCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        val encodedToken = Uri.encode(INJECTED_AUTH_TOKEN)
        val expires = "Fri, 31 Dec 2038 23:59:59 GMT"
        val cookieDomain = "umami.auth=$encodedToken; Domain=$PRIMARY_HOST; Path=/; Max-Age=315360000; Expires=$expires; SameSite=Lax; Secure"
        val cookieRoot = "umami.auth=$encodedToken; Path=/; Max-Age=315360000; Expires=$expires; SameSite=Lax; Secure"

        cookieManager.setCookie(UMAMI_BASE_URL, cookieDomain)
        cookieManager.setCookie(UMAMI_BASE_URL, cookieRoot)
        cookieManager.setCookie("https://$PRIMARY_HOST", cookieDomain)
        cookieManager.setCookie("https://$PRIMARY_HOST", cookieRoot)
        cookieManager.flush()
    }

    private fun injectAuth(view: WebView?) {
        val js = """
            (function() {
                try {
                    var token = "$INJECTED_AUTH_TOKEN";
                    localStorage.setItem('umami.auth', JSON.stringify(token));
                    sessionStorage.setItem('umami.auth', JSON.stringify(token));
                    document.cookie = "umami.auth=" + encodeURIComponent(token) + "; path=/; max-age=315360000; expires=Fri, 31 Dec 2038 23:59:59 GMT; SameSite=Lax; Secure";
                } catch(e) {}
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
    }

    private fun setupClients() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return false
                val host = url.host ?: ""

                // Keep Umami and authentication links inside the native container
                if (host.contains(PRIMARY_HOST) || host.contains("vercel.app") || host.contains("umami.is")) {
                    return false
                }

                // Handle external links in the default browser or corresponding app
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
                injectAuth(view)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectAuth(view)
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
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

    override fun onDestroy() {
        CookieManager.getInstance().flush()
        webView.destroy()
        super.onDestroy()
    }
}
