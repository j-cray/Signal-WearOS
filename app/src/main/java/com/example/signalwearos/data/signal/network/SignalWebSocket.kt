package com.example.signalwearos.data.signal.network

import android.util.Log
import com.google.protobuf.ByteString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.whispersystems.signalservice.internal.push.SignalServiceProtos
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class SignalWebSocket(
    private val client: OkHttpClient
) {
    private var webSocket: WebSocket? = null
    private val requestCounter = AtomicLong(0)
    
    private val _incomingMessages = Channel<SignalServiceProtos.WebSocketMessage>(Channel.BUFFERED)
    val incomingMessages: Flow<SignalServiceProtos.WebSocketMessage> = _incomingMessages.receiveAsFlow()

    fun connect(url: String, headers: Map<String, String> = emptyMap()) {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        
        webSocket = client.newWebSocket(requestBuilder.build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Connected to $url")
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                try {
                    val message = SignalServiceProtos.WebSocketMessage.parseFrom(bytes.toByteArray())
                    Log.d(TAG, "Received WebSocket Message: Type=${message.type}")
                    _incomingMessages.trySend(message)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse WebSocket message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closing: $code / $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failure", t)
            }
        })
    }

    fun sendRequest(verb: String, path: String, body: ByteArray? = null): Long {
        val id = requestCounter.incrementAndGet()
        
        val requestMessage = SignalServiceProtos.WebSocketRequestMessage.newBuilder()
            .setVerb(verb)
            .setPath(path)
            .setId(id)
        
        if (body != null) {
            requestMessage.setBody(ByteString.copyFrom(body))
        }

        val envelope = SignalServiceProtos.WebSocketMessage.newBuilder()
            .setType(SignalServiceProtos.WebSocketMessage.Type.REQUEST)
            .setRequest(requestMessage)
            .build()

        val sent = webSocket?.send(okio.ByteString.of(*envelope.toByteArray())) ?: false
        if (!sent) {
            Log.e(TAG, "Failed to send request $id")
        }
        return id
    }

    fun close() {
        webSocket?.close(1000, "Goodbye")
        webSocket = null
    }

    companion object {
        private const val TAG = "SignalWebSocket"
    }
}
