package com.example.pixlwallo.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SelectionRepository(private val context: Context) {
    private val store: DataStore<Preferences> = PreferencesDataStore.create(context)
    private val resolver: ContentResolver = context.contentResolver

    private object Keys {
        val selectedUris = stringPreferencesKey("selected_uris")
    }

    val selectedFlow: Flow<List<Uri>> = store.data.map { p ->
        val raw = p[Keys.selectedUris].orEmpty()
        raw.split('\n').filter { it.isNotBlank() }.map { Uri.parse(it) }
    }

    suspend fun setSelection(uris: List<Uri>) {
        store.edit { p ->
            p[Keys.selectedUris] = uris.joinToString("\n") { it.toString() }
        }
    }

    fun takePersistable(uri: Uri) {
        try {
            // 对于文件 URI，只需要读取权限
            val flags = IntentFlagRead
            resolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            // 忽略权限错误，某些 URI 可能不需要持久化权限
        }
    }

    fun takePersistableTree(uri: Uri) {
        try {
            // 对于文件夹树 URI，需要读取权限
            val flags = IntentFlagRead
            resolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            // 忽略权限错误
        }
    }

    /**
     * 递归读取文件夹中的所有图片文件
     * @param treeUri 文件夹的 URI
     * @return 图片文件的 URI 列表
     */
    fun scanImagesFromFolder(treeUri: Uri): List<Uri> {
        val imageUris = mutableListOf<Uri>()
        val imageMimeTypes = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp",
            "image/heic",
            "image/heif"
        )

        fun scanRecursive(documentFile: DocumentFile?) {
            documentFile?.let { file ->
                if (file.isDirectory) {
                    // 递归扫描子文件夹
                    file.listFiles()?.forEach { child ->
                        scanRecursive(child)
                    }
                } else if (file.isFile) {
                    // 检查是否是图片文件
                    val mimeType = file.type
                    val uri = file.uri
                    
                    val isImage = when {
                        mimeType != null -> imageMimeTypes.contains(mimeType.lowercase())
                        else -> {
                            // 也检查文件扩展名（某些情况下 mimeType 可能为空）
                            val name = file.name?.lowercase() ?: ""
                            name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                            name.endsWith(".png") || name.endsWith(".gif") ||
                            name.endsWith(".webp") || name.endsWith(".bmp") ||
                            name.endsWith(".heic") || name.endsWith(".heif")
                        }
                    }
                    
                    if (isImage) {
                        imageUris.add(uri)
                    }
                }
            }
        }

        val documentFile = DocumentFile.fromTreeUri(context, treeUri)
        scanRecursive(documentFile)
        return imageUris
    }

    private val IntentFlags: Int
        get() = (IntentFlagRead or IntentFlagWrite)

    private val IntentFlagRead: Int
        get() = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION

    private val IntentFlagWrite: Int
        get() = android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
}