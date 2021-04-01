package com.apm29.webviewwrapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceRequest
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import com.just.agentweb.AgentWeb
import com.just.agentweb.WebChromeClient
import com.just.agentweb.WebViewClient

class MainActivity : AppCompatActivity() {


    object PStoreIntent {
        const val ACTION_LOGIN_SUCCEED = "cybertech.pstore.intent.action.LOGIN_SUCCEED"
        const val ACTION_PSTORE_EXIT = "cybertech.pstore.intent.action.EXIT"
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (PStoreIntent.ACTION_PSTORE_EXIT == action) {
                finish()
            }
        }
    }

    private fun register() {
        val filter = IntentFilter()
        filter.addAction(PStoreIntent.ACTION_PSTORE_EXIT)
        registerReceiver(mReceiver, filter)
    }

    private fun unregister() {
        unregisterReceiver(mReceiver)
    }

    private val homeUrl: String = "http://sjsytest.ciih.net/#/"
    private val searchUrl: String = "http://sjsytest.ciih.net/#/search"

    lateinit var mAgentWeb: AgentWeb
    private var pageType: Int = 0 // 0首页 1搜索

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        register()

        mAgentWeb = AgentWeb.with(this)
                .setAgentWebParent(
                        findViewById(R.id.layoutWebView),
                        LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT
                        )
                )
                .useDefaultIndicator()
                .setWebChromeClient(object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        supportActionBar?.title = title
                    }
                })
                .createAgentWeb()
                .ready()
                .go(homeUrl)

        onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (mAgentWeb.webCreator.webView.canGoBack()) {
                            mAgentWeb.webCreator.webView.goBack()
                        } else {
                            finish()
                        }
                    }
                }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregister()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val search = menu.findItem(R.id.menu_search)
        val home = menu.findItem(R.id.menu_home)
        search.isVisible = pageType != 1
        home.isVisible = pageType == 1
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        pageType = if (item.itemId == R.id.menu_search) {
            mAgentWeb.urlLoader.loadUrl(searchUrl)
            1
        } else {
            mAgentWeb.urlLoader.loadUrl(homeUrl)
            0
        }
        invalidateOptionsMenu()
        return true
    }
}