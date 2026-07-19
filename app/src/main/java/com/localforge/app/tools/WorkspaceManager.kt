package com.localforge.app.tools

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.OutputStreamWriter

class WorkspaceManager(private val context: Context, private val rootUri: Uri) {

    private val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
        ?: throw IllegalArgumentException("Cannot open tree URI: $rootUri")

    val rootPath: String
        get() = rootDoc.name ?: "workspace"

    fun listFiles(path: String = ""): List<String> {
        val dir = getDocument(path)
        return dir?.listFiles()?.map { it.name ?: "" } ?: emptyList()
    }

    fun readFile(path: String): String? {
        val file = getDocument(path)
        return if (file != null && file.isFile) {
            context.contentResolver.openInputStream(file.uri)?.use {
                it.bufferedReader().readText()
            }
        } else null
    }

    fun writeFile(path: String, content: String): Boolean {
        val segments = path.split("/").filter { it.isNotEmpty() }
        val fileName = segments.last()
        val dirPath = segments.dropLast(1).joinToString("/")

        val dir = getDocument(dirPath, createIfMissing = true)
        val file = dir?.findFile(fileName) ?: dir?.createFile("text/plain", fileName)

        return file?.let {
            context.contentResolver.openOutputStream(it.uri, "wt")?.use { out ->
                OutputStreamWriter(out).use { writer ->
                    writer.write(content)
                }
            }
            true
        } ?: false
    }

    fun createDirectory(path: String): Boolean {
        val segments = path.split("/").filter { it.isNotEmpty() }
        if (segments.isEmpty()) return false

        var current = rootDoc
        for (segment in segments) {
            val existing = current.findFile(segment)
            if (existing != null) {
                if (!existing.isDirectory) return false
                current = existing
            } else {
                val created = current.createDirectory(segment) ?: return false
                current = created
            }
        }
        return true
    }

    fun isWorkspaceValid(): Boolean {
        return rootDoc.exists() && rootDoc.isDirectory
    }

    private fun getDocument(path: String, createIfMissing: Boolean = false): DocumentFile? {
        if (path.isEmpty() || path == "/") return rootDoc

        var current = rootDoc
        val segments = path.split("/").filter { it.isNotEmpty() }

        for (segment in segments) {
            var next = current?.findFile(segment)
            if (next == null && createIfMissing) {
                next = current?.createDirectory(segment)
            }
            current = next ?: return null
        }
        return current
    }

    private fun DocumentFile.findFile(name: String): DocumentFile? {
        return listFiles().find { it.name == name }
    }
}
