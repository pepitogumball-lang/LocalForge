package com.localforge.app.server

import com.localforge.app.models.*
import com.localforge.app.tools.WorkspaceManager
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import java.util.*

class LocalServer(
    private val port: Int,
    private val workspaceManager: WorkspaceManager,
    private val onLog: (String) -> Unit
) {
    private var server: ApplicationEngine? = null

    fun start() {
        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            routing {
                get("/health") {
                    onLog("GET /health")
                    call.respond(mapOf("status" to "ok", "workspace" to "active"))
                }
                
                get("/v1/models") {
                    onLog("GET /v1/models")
                    call.respond(ModelList(data = listOf(ModelItem(id = "localforge-v1"))))
                }

                post("/v1/chat/completions") {
                    val request = call.receive<ChatRequest>()
                    onLog("POST /v1/chat/completions - Model: ${request.model}")
                    
                    // Simple logic to handle file operations via chat
                    val lastMessage = request.messages.last().content
                    val responseText = handleAgentLogic(lastMessage)
                    
                    val response = ChatResponse(
                        id = "chatcmpl-" + UUID.randomUUID().toString(),
                        created = System.currentTimeMillis() / 1000,
                        model = request.model,
                        choices = listOf(
                            ChatChoice(
                                index = 0,
                                message = ChatMessage(role = "assistant", content = responseText)
                            )
                        )
                    )
                    call.respond(response)
                }
            }
        }.start(wait = false)
        onLog("Servidor iniciado en puerto $port")
    }

    fun stop() {
        server?.stop(1000, 2000)
        onLog("Servidor detenido")
    }

    private fun handleAgentLogic(input: String): String {
        // Here we could implement a tool loop or JSON command parser
        // For now, let's provide a basic response that acknowledges the workspace
        return "LocalForge Agent: He recibido tu mensaje. El workspace está listo para operar sobre archivos. \nInput: $input"
    }
}
