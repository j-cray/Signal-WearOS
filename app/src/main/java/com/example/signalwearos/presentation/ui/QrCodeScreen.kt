package com.example.signalwearos.presentation.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.signalwearos.data.signal.SignalClient
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QrCodeScreen(
    onLinked: () -> Unit
) {
    val signalClient = remember { SignalClient() }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        val linkUri = signalClient.generateLinkUri()
        qrBitmap = generateQrCode(linkUri)
        
        // In a real implementation, we would start listening on the WebSocket here
        // signalClient.connectToWebSocket(uuid)
        
        // Simulate linking for now as we don't have a real Signal server to talk to
        // delay(10000)
        // onLinked()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap!!.asImageBitmap(),
                contentDescription = "Link Device QR Code",
                modifier = Modifier.size(150.dp)
            )
        } else {
            Text(text = "Generating Keys...", style = MaterialTheme.typography.body1)
        }
        
        // Instructions
        Text(
            text = "Scan with Signal",
            style = MaterialTheme.typography.caption1,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

suspend fun generateQrCode(content: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val width = 300
            val height = 300
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height
            )
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
