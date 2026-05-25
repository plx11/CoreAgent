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
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

// --------------------- 定義專門的異常類別 ---------------------
class ToolSchemaException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ToolDownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ToolIntegrityException(message: String) : Exception(message)

// --------------------- ToolSchema 結構 ---------------------
@Serializable
data class ToolSchema(
    val name: String,
    val version: String,
    val entryPoint: String,
    val sha256: String,
    val downloadUrl: String,
    val signature: String
)

/**
 * 生產級動態外掛管理器
 */
class ToolManager(
    private val context: Context,
    private val toolDao: ToolDao,
    private val httpClient: HttpClient,
    private val publicKey: PublicKey // 注入驗證用公鑰
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val pluginsDir = File(context.filesDir, "plugins")

    init {
        if (!pluginsDir.exists()) pluginsDir.mkdirs()
    }

    @Throws(ToolSchemaException::class, ToolDownloadException::class, ToolIntegrityException::class, IOException::class)
    suspend fun installTool(schemaUrl: String): File {
        val schemaJson = downloadSchema(schemaUrl)
        val schema = parseToolSchema(schemaJson)

        if (toolDao.getToolByName(schema.name) != null) {
            throw IllegalStateException("Tool ${schema.name} is already installed.")
        }

        val targetFile = File(pluginsDir, "${schema.name}-${schema.version}.js")
        val downloadedFile = downloadPlugin(schema.downloadUrl, targetFile)

        // 必須真實調用雙重驗證
        val signatureBytes = Base64.getDecoder().decode(schema.signature)
        if (!verifyToolIntegrity(downloadedFile, schema.sha256, signatureBytes, publicKey)) {
            downloadedFile.delete()
            throw ToolIntegrityException("Tool ${schema.name} integrity check failed.")
        }

        val installedTool = InstalledToolEntity(
            name = schema.name,
            version = schema.version,
            entryPoint = schema.entryPoint,
            sha256 = schema.sha256,
            localPath = downloadedFile.absolutePath,
            downloadUrl = schema.downloadUrl
        )
        toolDao.insertTool(installedTool)

        return downloadedFile
    }

    private suspend fun downloadSchema(schemaUrl: String): String = withContext(Dispatchers.IO) {
        val response: HttpResponse = httpClient.get(schemaUrl)
        if (!response.status.isSuccess()) throw IOException("Failed to download schema")
        response.bodyAsText()
    }

    private suspend fun downloadPlugin(url: String, targetFile: File): File = withContext(Dispatchers.IO) {
        val response: HttpResponse = httpClient.get(url)
        if (!response.status.isSuccess()) throw IOException("Download failed")
        response.body<ByteArray>().let { bytes ->
            FileOutputStream(targetFile).use { it.write(bytes) }
        }
        targetFile
    }

    private fun parseToolSchema(jsonString: String): ToolSchema = json.decodeFromString(jsonString)

    private fun verifyToolIntegrity(file: File, expectedHash: String, signature: ByteArray, publicKey: PublicKey): Boolean {
        // 1. SHA-256 Hash 比對
        val digest = MessageDigest.getInstance("SHA-256")
        val fileBytes = file.readBytes()
        val actualHash = digest.digest(fileBytes).joinToString("") { "%02x".format(it) }
        if (!actualHash.equals(expectedHash, ignoreCase = true)) return false

        // 2. 數位簽章驗證
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initVerify(publicKey)
        sig.update(fileBytes)
        return sig.verify(signature)
    }
}
