package com.localforge.app.tools

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.OutputStreamWriter

class WorkspaceManager(private val context: Context, private val rootUri: Uri) {

    private val rootDoc = DocumentFile.fromTreeUri(context, rootUri)

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
            context.contentResolver.openOutputStream(it.uri)?.use { out ->
                OutputStreamWriter(out).use { writer ->
                    writer.write(content)
                }
            }
            true
        } ?: false
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
}
