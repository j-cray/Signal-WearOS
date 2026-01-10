package com.example.signalwearos.data.signal.proto

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

/**
 * A manual implementation of the Provisioning Message structure.
 * 
 * message ProvisioningMessage {
 *   optional string uuid = 1;
 *   optional bytes publicKey = 2; // The phone's ephemeral public key
 *   optional bytes body = 3;      // The encrypted payload
 * }
 */
data class ProvisioningMessage(
    val uuid: String? = null,
    val publicKey: ByteArray? = null,
    val body: ByteArray? = null
) {
    companion object {
        fun parseFrom(data: ByteArray): ProvisioningMessage {
            val input = ByteArrayInputStream(data)
            var uuid: String? = null
            var publicKey: ByteArray? = null
            var body: ByteArray? = null

            while (true) {
                val tag = readVarint32(input)
                if (tag == 0) break // End of stream

                val fieldNumber = tag ushr 3
                val wireType = tag and 0x07

                when (fieldNumber) {
                    1 -> { // uuid (string)
                        if (wireType != 2) skipField(input, wireType)
                        else uuid = readString(input)
                    }
                    2 -> { // publicKey (bytes)
                        if (wireType != 2) skipField(input, wireType)
                        else publicKey = readBytes(input)
                    }
                    3 -> { // body (bytes)
                        if (wireType != 2) skipField(input, wireType)
                        else body = readBytes(input)
                    }
                    else -> skipField(input, wireType)
                }
            }

            return ProvisioningMessage(uuid, publicKey, body)
        }

        // --- Helper functions for reading Protobuf primitives ---

        private fun readVarint32(input: InputStream): Int {
            var result = 0
            var shift = 0
            while (true) {
                val b = input.read()
                if (b == -1) return 0 // EOF treated as 0 for tag reading loop
                
                result = result or ((b and 0x7F) shl shift)
                if ((b and 0x80) == 0) return result
                shift += 7
                if (shift > 32) throw IOException("Malformed varint")
            }
        }

        private fun readString(input: InputStream): String {
            val length = readVarint32(input)
            if (length == 0) return ""
            val bytes = ByteArray(length)
            var pos = 0
            while (pos < length) {
                val read = input.read(bytes, pos, length - pos)
                if (read == -1) throw IOException("Unexpected EOF")
                pos += read
            }
            return String(bytes, Charsets.UTF_8)
        }

        private fun readBytes(input: InputStream): ByteArray {
            val length = readVarint32(input)
            if (length == 0) return ByteArray(0)
            val bytes = ByteArray(length)
            var pos = 0
            while (pos < length) {
                val read = input.read(bytes, pos, length - pos)
                if (read == -1) throw IOException("Unexpected EOF")
                pos += read
            }
            return bytes
        }

        private fun skipField(input: InputStream, wireType: Int) {
            when (wireType) {
                0 -> readVarint32(input) // Varint
                1 -> readFixed64(input)  // 64-bit
                2 -> { // Length delimited
                    val length = readVarint32(input)
                    input.skip(length.toLong())
                }
                5 -> readFixed32(input)  // 32-bit
                else -> throw IOException("Unsupported wire type: $wireType")
            }
        }

        private fun readFixed32(input: InputStream) {
            input.read(); input.read(); input.read(); input.read()
        }

        private fun readFixed64(input: InputStream) {
            readFixed32(input)
            readFixed32(input)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProvisioningMessage

        if (uuid != other.uuid) return false
        if (publicKey != null) {
            if (other.publicKey == null) return false
            if (!publicKey.contentEquals(other.publicKey)) return false
        } else if (other.publicKey != null) return false
        if (body != null) {
            if (other.body == null) return false
            if (!body.contentEquals(other.body)) return false
        } else if (other.body != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid?.hashCode() ?: 0
        result = 31 * result + (publicKey?.contentHashCode() ?: 0)
        result = 31 * result + (body?.contentHashCode() ?: 0)
        return result
    }
}
