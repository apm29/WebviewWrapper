package com.apm29.webviewwrapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import cn.com.cybertech.pdk.OperationLog
import com.fri.libfriapkrecord.read.SignRecordTools
import com.just.agentweb.AgentWeb
import com.just.agentweb.AgentWebSettingsImpl
import com.just.agentweb.DefaultWebClient
import com.just.agentweb.WebChromeClient
import com.just.agentweb.WebViewClient
import java.net.URL

class MainActivity : AppCompatActivity() {


    object PStoreIntent {
        const val ACTION_LOGIN_SUCCEED = "cybertech.pstore.intent.action.LOGIN_SUCCEED"
        const val ACTION_PSTORE_EXIT = "cybertech.pstore.intent.action.EXIT"
    }

    object UAIntent {
        // 公开广播：认证成功
        const val ACTION_UA_LOGIN = "com.ydjw.ua.ACTION_LOGIN"

        // 定向广播：认证成功，可以不监听
        const val ACTION_UA_LOGIN_SPECIFY_TARGET_PACKAGE = "%s.ACTION_LOGIN"

        // 公开广播：认证注销
        const val ACTION_UA_LOGOUT = "com.ydjw.ua.ACTION_LOGOUT"
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PStoreIntent.ACTION_PSTORE_EXIT -> {
                    finish()
                }

                UAIntent.ACTION_UA_LOGIN -> {
                    UnifiedAuthorizationUtils.getCredentials(context, true)
                    UnifiedAuthorizationUtils.getResourceAddress(context, true)
                    UnifiedAuthorizationUtils.getStaticResourceAddress(context, true)
                }

                String.format(
                    UAIntent.ACTION_UA_LOGIN_SPECIFY_TARGET_PACKAGE,
                    packageName
                ) -> {
                    // 定向广播，可以不监听
                    //应用认证成功，再次调用获取凭证
                }

                UAIntent.ACTION_UA_LOGOUT -> {
                    // 统一认证已经注销认证，应当执行自身的注销操作
                    finish()
                }
            }

        }
    }

    private fun register() {
        val filter = IntentFilter()
        filter.addAction(PStoreIntent.ACTION_PSTORE_EXIT)

        filter.addAction(UAIntent.ACTION_UA_LOGIN)
        filter.addAction(
            String.format(
                UAIntent.ACTION_UA_LOGIN_SPECIFY_TARGET_PACKAGE,
                packageName
            )
        )
        filter.addAction(UAIntent.ACTION_UA_LOGOUT)


        registerReceiver(mReceiver, filter)
    }

    private fun unregister() {
        unregisterReceiver(mReceiver)
    }

    private val homeUrl: String = "${BuildConfig.SERVER_URL}/index.html/#/"
    private val searchUrl: String = "${BuildConfig.SERVER_URL}/index.html/#/search"
    private val logUrl: String = "${BuildConfig.SERVER_URL}/index.html/#/logSubmit"

    lateinit var mAgentWeb: AgentWeb
    private var pageType: Int = 0 // 0首页 1搜索

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        register()

        class UnifiedAuthorization {
            @JavascriptInterface
            fun getHttpRequestInfo(resId: String): UnifiedAuthorizationUtils.HttpInfo {
                return try {
                    UnifiedAuthorizationUtils.getHttpRequestInfo(resId, this@MainActivity)
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                    UnifiedAuthorizationUtils.HttpInfo("", "", "", "", "", "")
                }
            }
        }

        val newUrl: String = getProxyUrl(homeUrl)

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
            .addJavascriptInterface("UnifiedAuthorization", UnifiedAuthorization())
            .setWebViewClient(object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    val url = request.url.toString()
                    println(url)
                    if (url.contains("/java")) {
                        if (!BuildConfig.DEBUG) {
                            OperationLog.logging(
                                this@MainActivity, BuildConfig.CLIENT_ID, "User",
                                OperationLog.OperationType.CODE_OTHER,
                                OperationLog.OperationResult.CODE_SUCCESS,
                                OperationLog.LogType.CODE_USER_OPERATION,
                                "url='${url}'"
                            )
                        }
                    }

                    return super.shouldInterceptRequest(view, request)
                }
            })
            .setAgentWebWebSettings(AgentWebSettingsImpl())
            .setOpenOtherPageWays(DefaultWebClient.OpenOtherPageWays.ASK)//打开其他应用时，弹窗咨询用户是否前往其他应用
            .interceptUnkownUrl() //拦截找不到相关页面的Scheme
            .createAgentWeb()
            .ready()
            .go(newUrl)

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
        findViewById<TextView>(R.id.tvSerialNo).text = getString(R.string.national_code, recordNum)
    }

    private fun getProxyUrl(originalUrl: String): String {
        var newUrl = originalUrl
        try {
            val oldUrl = URL(newUrl)
            val base = UnifiedAuthorizationUtils.getStaticResourceBase(this)
            //"http://192.168.0.15:8080/"
            newUrl =
                "$base${oldUrl.path.substring(1)}${if (oldUrl.ref != null) "#" else ""}${oldUrl.ref ?: ""}"
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
        }
        return newUrl
    }

    //获取系统内APK文件路径
    private fun getNativeApkPath(context: Context): String? {
        var apkPath: String? = null
        try {
            val applicationInfo = context.applicationInfo ?: return null
            apkPath = applicationInfo.sourceDir
        } catch (e: Throwable) {
            e.printStackTrace()
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
                mAgentWeb.urlLoader.loadUrl(getProxyUrl(searchUrl))
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
                mAgentWeb.urlLoader.loadUrl(getProxyUrl(homeUrl))
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
                Toast.makeText(this, R.string.sign_up_success, Toast.LENGTH_SHORT).show()
            }

            else -> {
                mAgentWeb.urlLoader.loadUrl(getProxyUrl(logUrl))
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