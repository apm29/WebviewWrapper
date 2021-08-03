package com.apm29.webviewwrapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceRequest
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
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


        if(!BuildConfig.DEBUG) {
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
                OperationLog.logging(
                    this,BuildConfig.CLIENT_ID,"User",
                    OperationLog.OperationType.CODE_OTHER,
                    OperationLog.OperationResult.CODE_SUCCESS,
                    OperationLog.LogType.CODE_USER_OPERATION,
                    "page='${searchUrl}'"
                )
            }
            R.id.menu_home -> {
                mAgentWeb.urlLoader.loadUrl(homeUrl)
                pageType = 0
                OperationLog.logging(
                    this,BuildConfig.CLIENT_ID,"User",
                    OperationLog.OperationType.CODE_OTHER,
                    OperationLog.OperationResult.CODE_SUCCESS,
                    OperationLog.LogType.CODE_USER_OPERATION,
                    "page='${homeUrl}'"
                )
            }
            else -> {
                mAgentWeb.urlLoader.loadUrl(logUrl)
                OperationLog.logging(
                    this,BuildConfig.CLIENT_ID,"User",
                    OperationLog.OperationType.CODE_OTHER,
                    OperationLog.OperationResult.CODE_SUCCESS,
                    OperationLog.LogType.CODE_USER_OPERATION,
                    "page='${logUrl}'"
                )
            }
        }
        invalidateOptionsMenu()
        return true
    }
}