package com.apm29.webviewwrapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import cn.com.cybertech.pdk.OperationLog
import com.fri.libfriapkrecord.read.SignRecordTools
import com.just.agentweb.*

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

    private val homeUrl: String = "${BuildConfig.SERVER_URL}/#/"
    private val searchUrl: String = "${BuildConfig.SERVER_URL}/#/search"
    private val logUrl: String = "${BuildConfig.SERVER_URL}/#/logSubmit"

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
            .setWebViewClient(object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url?.toString()
                    if(!url.isNullOrEmpty() && url.contains("java") && !BuildConfig.DEBUG){
                        println(url)
                        OperationLog.logging(
                            this@MainActivity, BuildConfig.CLIENT_ID, "User",
                            OperationLog.OperationType.CODE_OTHER,
                            OperationLog.OperationResult.CODE_SUCCESS,
                            OperationLog.LogType.CODE_USER_OPERATION,
                            "url='${url}'"
                        )
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            })
            .setAgentWebWebSettings(AgentWebSettingsImpl())
            .setOpenOtherPageWays(DefaultWebClient.OpenOtherPageWays.ASK)//打开其他应用时，弹窗咨询用户是否前往其他应用
            .interceptUnkownUrl() //拦截找不到相关页面的Scheme
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


        if (!BuildConfig.DEBUG) {
            OperationLog.logging(
                this, BuildConfig.CLIENT_ID, "User",
                OperationLog.OperationType.CODE_LAUNCH,
                OperationLog.OperationResult.CODE_SUCCESS,
                OperationLog.LogType.CODE_USER_OPERATION,
                "condition='open-app'"
            )
        }

        //系统环境下APK路径
        val apkPath: String? = getNativeApkPath(this@MainActivity.applicationContext)
        //读取备案号
        val recordNum = SignRecordTools.readNumbers(apkPath)
        findViewById<TextView>(R.id.tvSerialNo).text = "全国注册备案号：$recordNum"
    }

    //获取系统内APK文件路径
    private fun getNativeApkPath(context: Context): String? {
        var apkPath: String? = null
        try {
            val applicationInfo = context.applicationInfo ?: return null
            apkPath = applicationInfo.sourceDir
        } catch (e: Throwable) {
        }
        return apkPath
    }

    override fun onDestroy() {
        super.onDestroy()
        unregister()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val search = menu.findItem(R.id.menu_search)
        val home = menu.findItem(R.id.menu_home)
        val log = menu.findItem(R.id.menu_log)
        search.isVisible = pageType == 0
        home.isVisible = pageType == 1
        log.isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_search -> {
                mAgentWeb.urlLoader.loadUrl(searchUrl)
                pageType = 1
                if (!BuildConfig.DEBUG) {
                    OperationLog.logging(
                        this, BuildConfig.CLIENT_ID, "User",
                        OperationLog.OperationType.CODE_OTHER,
                        OperationLog.OperationResult.CODE_SUCCESS,
                        OperationLog.LogType.CODE_USER_OPERATION,
                        "page='${searchUrl}'"
                    )
                }
            }
            R.id.menu_home -> {
                mAgentWeb.urlLoader.loadUrl(homeUrl)
                pageType = 0
                if (!BuildConfig.DEBUG) {
                    OperationLog.logging(
                        this, BuildConfig.CLIENT_ID, "User",
                        OperationLog.OperationType.CODE_OTHER,
                        OperationLog.OperationResult.CODE_SUCCESS,
                        OperationLog.LogType.CODE_USER_OPERATION,
                        "page='${homeUrl}'"
                    )
                }
            }
            R.id.menu_sign_up -> {
                if (!BuildConfig.DEBUG) {
                    OperationLog.logging(
                        this, BuildConfig.CLIENT_ID, "User",
                        OperationLog.OperationType.CODE_OTHER,
                        OperationLog.OperationResult.CODE_SUCCESS,
                        OperationLog.LogType.CODE_USER_OPERATION,
                        "signup='success'"
                    )
                }
                Toast.makeText(this,R.string.sign_up_success,Toast.LENGTH_SHORT).show()
            }
            else -> {
                mAgentWeb.urlLoader.loadUrl(logUrl)
                if (!BuildConfig.DEBUG) {
                    OperationLog.logging(
                        this, BuildConfig.CLIENT_ID, "User",
                        OperationLog.OperationType.CODE_OTHER,
                        OperationLog.OperationResult.CODE_SUCCESS,
                        OperationLog.LogType.CODE_USER_OPERATION,
                        "page='${logUrl}'"
                    )
                }
            }
        }
        invalidateOptionsMenu()
        return true
    }
}