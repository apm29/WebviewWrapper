package com.apm29.webviewwrapper

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import com.just.agentweb.AgentWeb
import com.just.agentweb.WebChromeClient

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val mAgentWeb = AgentWeb.with(this)
                .setAgentWebParent(
                        findViewById(R.id.layoutWebView),
                        LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT
                        )
                )
                .useDefaultIndicator()
                .setWebChromeClient(object : WebChromeClient(){
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        supportActionBar?.title = title
                    }
                })
                .createAgentWeb()
                .ready()
                .go("http://jwttest.ciih.net/#/cuttingEdgeNews")

        onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true){
                    override fun handleOnBackPressed() {
                        if(mAgentWeb.webCreator.webView.canGoBack()){
                            mAgentWeb.webCreator.webView.goBack()
                        }else{
                            finish()
                        }
                    }
                }
        )
    }
}