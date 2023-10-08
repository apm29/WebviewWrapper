package com.apm29.webviewwrapper

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import cn.com.cybertech.pdk.utils.GsonUtils
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.util.UUID
import kotlin.jvm.Throws

/**
 *  author : apm29
 *  date : 2023/9/26 5:02 PM
 *  description : io.apm29.talkie.utils
 */

object UnifiedAuthorizationUtils {

    data class ResourceItem(
        @SerializedName("resourceAddress")
        val resourceAddress: String,
        @SerializedName("resourceId")
        val resourceId: String,
        @SerializedName("resourceRegionalismCode")
        val resourceRegionalismCode: String,
        @SerializedName("resourceServiceType")
        val resourceServiceType: String? = null
    )

    data class HttpInfo(
        val messageId: String,
        val appCredential: String,
        val userCredential: String,
        val resId: String,
        val resOrgId: String,
        val resAddress: String
    )

    // 获取票据凭证URI
    private val URI_UA_GET_CREDENTIAL = Uri.parse("content://com.ydjw.ua.getCredential")

    // 资源服务寻址URI
    private val URI_RSB_GET_RESOURCE_ADDRESS =
        Uri.parse("content://com.ydjw.rsb.getResourceAddress")

    // 作为输⼊参数，当前为2，所有新对 接或升级的APP都需使⽤该版本； 作为返回参数，可⽤逗号分隔，表⽰接⼝当前⽀持的所有版本；
    private const val CONST_UA_VERSION = "2"

    // 应⽤注册时，由发布系统提供
    private const val CONST_ORG_ID = "330000000000"

    // 应⽤注册时，由发布系统提供
    private const val CONST_NETWORK_AREA_CODE = "3"


    private var rawCredentialResult: Bundle? = null
    private var rawResourceAddressResult: Bundle? = null
    private var rawStaticResourceAddressResult: Bundle? = null

    val userCredential
        get() = rawCredentialResult?.getString("userCredential")

    val appCredential
        get() = rawCredentialResult?.getString("appCredential")

    private var resourceList: List<ResourceItem> = listOf()
    private var staticResourceBase: String? = null

    private var credentialAuthorized = false
    private var resourcesAuthorized = false
    private var staticResourcesAuthorized = false

    fun getHttpRequestInfo(resourceId: String, context: Context): HttpInfo {
        val resourceItem = resourceList.find {
            it.resourceId == resourceId
        }

        return if (resourceItem != null && appCredential != null && userCredential != null) {
            HttpInfo(
                UUID.randomUUID().toString(),
                appCredential!!,
                userCredential!!,
                resourceItem.resourceId,
                resourceItem.resourceRegionalismCode,
                resourceItem.resourceAddress
            )
        } else {
            getCredentials(context)
            getResourceAddress(context)
            getStaticResourceAddress(context)
            getHttpRequestInfo(resourceId, context)
        }

    }

    fun getStaticResourceBase(context: Context): String {
        return if (staticResourceBase != null) {
            staticResourceBase!!
        } else {
            getCredentials(context)
            getResourceAddress(context)
            getStaticResourceAddress(context)
            getStaticResourceBase(context)
        }
    }


    @Throws(IllegalArgumentException::class, NullPointerException::class)
    fun getCredentials(context: Context, authorizeAgain: Boolean = false) {
        if (credentialAuthorized && !authorizeAgain) {
            return
        }
        credentialAuthorized = false
        val messageId = UUID.randomUUID().toString()
        val appId = BuildConfig.CLIENT_ID
        val version = CONST_UA_VERSION
        val orgId = CONST_ORG_ID
        val networkAreaCode = CONST_NETWORK_AREA_CODE
        val packageName = context.packageName
        val params = bundleOf(
            "messageId" to messageId,
            "appId" to appId,
            "version" to version,
            "orgId" to orgId,
            "networkAreaCode" to networkAreaCode,
            "packageName" to packageName,
        )
        rawCredentialResult =
            context.contentResolver.call(URI_UA_GET_CREDENTIAL, "", null, params)
        if (rawCredentialResult == null) {
            throw NullPointerException("获取UA凭证失败")
        }
        // 0: 成功； -3: 参数错误； -5: 凭证不存在(三 ⽅应⽤应等待统⼀认证去后台获取凭证；
        val resultCode = rawCredentialResult?.getString("resultCode")
        // 错误码对应的⽂字信息描述
        val message = rawCredentialResult?.getString("message")
        if (resultCode == "-3") {
            throw IllegalArgumentException("获取UA凭证失败：参数错误-$message")
        }
        if (resultCode == "-5") {
            throw IllegalArgumentException("获取UA凭证失败：凭证不存在-$message")
        }
        credentialAuthorized = true
    }

    fun getResourceAddress(context: Context, authorizeAgain: Boolean = false) {
        if (resourcesAuthorized && !authorizeAgain) {
            return
        }
        resourcesAuthorized = false
        val messageId = UUID.randomUUID().toString()
        val version = CONST_UA_VERSION
        val packageName = context.packageName
        val params = bundleOf(
            "appCredential" to appCredential,
            "messageId" to messageId,
            "version" to version,
            "packageName" to packageName,
        )
        rawResourceAddressResult =
            context.contentResolver.call(URI_RSB_GET_RESOURCE_ADDRESS, "", null, params)

        if (rawResourceAddressResult == null) {
            throw NullPointerException("获取UA凭证失败")
        }
        // 0 - 成功； 40000 - 服务内部错误；
        //40001 - ⼊参不完整； 40002 - 应⽤凭证认证失败； 40003 - ⽤户凭证认证失败； 41000 - 其他错误；
        val resultCode = rawResourceAddressResult?.getString("resultCode")
        // 详细信息
        val message = rawResourceAddressResult?.getString("message")
        if (resultCode == "40000") {
            throw IllegalArgumentException("获取UA凭证失败：服务内部错误-$message")
        }
        if (resultCode == "40001") {
            throw IllegalArgumentException("获取UA凭证失败：入参不完整-$message")
        }
        if (resultCode == "40002") {
            throw IllegalArgumentException("获取UA凭证失败：应⽤凭证认证失败-$message")
        }
        if (resultCode == "40003") {
            throw IllegalArgumentException("获取UA凭证失败：户凭证认证失败-$message")
        }
        if (resultCode == "41000") {
            throw IllegalArgumentException("获取UA凭证失败：其他错误-$message")
        }
        val json = rawResourceAddressResult?.getString("resourceList") ?: "[]"
        resourceList = GsonUtils.fromJson(
            json, TypeToken.getParameterized(
                List::class.java,
                ResourceItem::class.java
            ).type
        )
        resourcesAuthorized = true
    }

    fun getStaticResourceAddress(context: Context, authorizeAgain: Boolean = false) {
        if (staticResourcesAuthorized && !authorizeAgain) {
            return
        }
        staticResourcesAuthorized = false
        val messageId = UUID.randomUUID().toString()
        val version = CONST_UA_VERSION
        val packageName = context.packageName
        val params = bundleOf(
            "appCredential" to appCredential,
            "messageId" to messageId,
            "version" to version,
            "packageName" to packageName,
            "staticResource" to true,
        )
        rawStaticResourceAddressResult =
            context.contentResolver.call(URI_RSB_GET_RESOURCE_ADDRESS, "", null, params)

        if (rawStaticResourceAddressResult == null) {
            throw NullPointerException("获取UA凭证失败")
        }
        // 0 - 成功； 40000 - 服务内部错误；
        //40001 - ⼊参不完整； 40002 - 应⽤凭证认证失败； 40003 - ⽤户凭证认证失败； 41000 - 其他错误；
        val resultCode = rawStaticResourceAddressResult?.getString("resultCode")
        // 详细信息
        val message = rawStaticResourceAddressResult?.getString("message")
        if (resultCode == "40000") {
            throw IllegalArgumentException("获取UA凭证失败：服务内部错误-$message")
        }
        if (resultCode == "40001") {
            throw IllegalArgumentException("获取UA凭证失败：入参不完整-$message")
        }
        if (resultCode == "40002") {
            throw IllegalArgumentException("获取UA凭证失败：应⽤凭证认证失败-$message")
        }
        if (resultCode == "40003") {
            throw IllegalArgumentException("获取UA凭证失败：户凭证认证失败-$message")
        }
        if (resultCode == "41000") {
            throw IllegalArgumentException("获取UA凭证失败：其他错误-$message")
        }
        staticResourceBase = rawStaticResourceAddressResult?.getString("staticResource")
        if (staticResourceBase == null) {
            throw NullPointerException("获取的静态资源路径为null")
        }
        staticResourcesAuthorized = true
    }
}