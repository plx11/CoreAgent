package com.coreagent.core.tool

import android.content.Context
import com.coreagent.data.tool.ToolDao
import com.coreagent.data.tool.InstalledToolEntity
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

// --------------------- 定義專門的異常類別 ---------------------
class ToolSchemaException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ToolDownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ToolIntegrityException(message: String) : Exception(message)

// --------------------- ToolSchema 結構 (與先前定義一致) ---------------------
@Serializable
data class ToolSchema(
    val name: String,
    val version: String,
    val entryPoint: String,
    val sha256: String,
    val downloadUrl: String
)

/**
 * 生產級動態外掛管理器
 * 整合 Ktor、Room DB，提供完整的插件下載、安裝、驗證與管理功能
 */
class ToolManager(
    private val context: Context, // 注入 Context 以訪問 filesDir
    private val toolDao: ToolDao,
    private val httpClient: HttpClient // 注入 Ktor HttpClient
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val pluginsDir = File(context.filesDir, "plugins") // 插件儲存目錄

    init {
        if (!pluginsDir.exists()) {
            if (!pluginsDir.mkdirs()) {
                throw IOException("無法創建插件目錄: ${pluginsDir.absolutePath}")
            }
        }
    }

    /**
     * 下載、解析、驗證並安裝工具插件
     */
    @Throws(
        ToolSchemaException::class,
        ToolDownloadException::class,
        ToolIntegrityException::class,
        IOException::class,
        IllegalStateException::class // for already installed
    )
    suspend fun installTool(schemaUrl: String): File {
        // 1. 下載並解析 Schema
        val schemaJson = try {
            downloadSchema(schemaUrl)
        } catch (e: Exception) {
            throw ToolSchemaException("獲取 Tool Schema 失敗", e)
        }

        val schema = try {
            parseToolSchema(schemaJson)
        } catch (e: Exception) {
            throw ToolSchemaException("解析 Tool Schema 失敗", e)
        }

        // 檢查是否已安裝，避免重複安裝
        if (toolDao.getToolByName(schema.name) != null) {
            throw IllegalStateException("Tool ${schema.name} (v${schema.version}) is already installed.")
        }

        val targetFile = File(pluginsDir, "${schema.name}-${schema.version}.js") // 假定是 JS 插件
        
        // 2. 下載插件主體
        val downloadedFile = try {
            downloadPlugin(schema.downloadUrl, targetFile)
        } catch (e: Exception) {
            throw ToolDownloadException("下載插件 ${schema.name} 失敗", e)
        }

        // 3. 驗證插件完整性
        if (!verifyToolIntegrity(downloadedFile, schema.sha256)) {
            downloadedFile.delete() // 清理不合格的檔案
            throw ToolIntegrityException("Tool ${schema.name} integrity check failed. Expected SHA256: ${schema.sha256}")
        }

        // 4. 持久化工具資訊
        val installedTool = InstalledToolEntity(
            name = schema.name,
            version = schema.version,
            entryPoint = schema.entryPoint, // 插件內的入口點
            sha256 = schema.sha256,
            localPath = downloadedFile.absolutePath,
            downloadUrl = schema.downloadUrl
        )
        toolDao.insertTool(installedTool)

        return downloadedFile
    }

    /**
     * 從 URL 下載 Schema JSON
     */
    private suspend fun downloadSchema(schemaUrl: String): String {
        return withContext(Dispatchers.IO) {
            val response: HttpResponse = httpClient.get(schemaUrl)

            if (!response.status.isSuccess()) {
                throw IOException("Failed to download schema from $schemaUrl: ${response.status}")
            }
            response.bodyAsText()
        }
    }

    /**
     * 從 URL 下載插件檔案
     */
    private suspend fun downloadPlugin(url: String, targetFile: File): File {
        return withContext(Dispatchers.IO) {
            val response: HttpResponse = httpClient.get(url)

            if (!response.status.isSuccess()) {
                throw IOException("Download failed for $url: ${response.status}")
            }

            response.body<ByteArray>().let { bytes ->
                FileOutputStream(targetFile).use { output ->
                    output.write(bytes)
                }
            }

            if (!targetFile.exists() || targetFile.length() == 0L) {
                throw IOException("Downloaded file is empty or does not exist: ${targetFile.absolutePath}")
            }
            targetFile
        }
    }

    /**
     * 解析 Tool Schema JSON
     */
    private fun parseToolSchema(jsonString: String): ToolSchema {
        return try {
            json.decodeFromString<ToolSchema>(jsonString)
        } catch (e: Exception) {
            throw ToolSchemaException("JSON 解析失敗", e)
        }
    }

    /**
     * 驗證檔案的 SHA-256 雜湊值
     */
    private fun verifyToolIntegrity(file: File, expectedHash: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        return actualHash.equals(expectedHash, ignoreCase = true)
    }
}

