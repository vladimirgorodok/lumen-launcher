package ru.lumen.launcher.web

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.time.LocalTime

/**
 * Контейнер для веб-приложений из opencode/OpenChamber. Отдельная Activity,
 * чтобы такие приложения не смешивались с PackageManager и системным списком.
 */
class WebAppActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var titleView: TextView

    private val night: Boolean by lazy {
        val hour = LocalTime.now().hour
        hour >= 22 || hour < 6
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Веб-приложение" }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(if (night) Color.parseColor("#061117") else Color.parseColor("#F8FDFF"))
        }

        val textColor = if (night) Color.parseColor("#EAF6FA") else Color.parseColor("#0F172A")
        val secondary = if (night) Color.parseColor("#8FAAB6") else Color.parseColor("#64748B")

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

        val back = TextView(this).apply {
            text = "←"
            textSize = 22f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { finish() }
        }
        toolbar.addView(back, LinearLayout.LayoutParams(dp(48), dp(44)))

        titleView = TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(textColor)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        toolbar.addView(titleView, LinearLayout.LayoutParams(0, dp(44), 1f).apply {
            marginStart = dp(4)
        })

        val refresh = TextView(this).apply {
            text = "⟳"
            textSize = 20f
            setTextColor(secondary)
            gravity = Gravity.CENTER
            setOnClickListener { webView.reload() }
        }
        toolbar.addView(refresh, LinearLayout.LayoutParams(dp(44), dp(44)))

        val external = TextView(this).apply {
            text = "↗"
            textSize = 20f
            setTextColor(secondary)
            gravity = Gravity.CENTER
            setOnClickListener {
                val current = webView.url ?: url
                if (current.isNotBlank()) {
                    runCatching {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(current)))
                    }
                }
            }
        }
        toolbar.addView(external, LinearLayout.LayoutParams(dp(44), dp(44)))

        root.addView(toolbar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#26CBD5"))
        }
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)))

        val container = FrameLayout(this)
        root.addView(container, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(if (night) Color.parseColor("#061117") else Color.parseColor("#F8FDFF"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
        }
        container.addView(webView)

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                super.onPageFinished(view, loadedUrl)
                progress.visibility = View.GONE
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress >= 100) {
                    progress.visibility = View.GONE
                } else {
                    progress.visibility = View.VISIBLE
                    progress.progress = newProgress
                }
            }

            override fun onReceivedTitle(view: WebView?, pageTitle: String?) {
                super.onReceivedTitle(view, pageTitle)
                val resolved = pageTitle?.trim().orEmpty()
                if (resolved.isNotEmpty()) titleView.text = resolved
            }
        }

        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(top = bars.top, bottom = bars.bottom, left = bars.left, right = bars.right)
            insets
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        if (url.isNotBlank()) webView.loadUrl(url)
    }

    override fun onDestroy() {
        runCatching {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}