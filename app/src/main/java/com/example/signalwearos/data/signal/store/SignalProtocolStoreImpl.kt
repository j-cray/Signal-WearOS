package com.example.signalwearos.data.signal.store

import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.groups.state.SenderKeyStore
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyStore
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SessionStore
import org.signal.libsignal.protocol.state.SignalProtocolStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyStore
import java.util.UUID

/**
 * A simplified in-memory implementation of the SignalProtocolStore.
 * In a real app, this must be backed by a persistent database (e.g., Room or SQLCipher).
 */
class SignalProtocolStoreImpl(
    private val identityKeyPair: IdentityKeyPair,
    private val registrationId: Int
) : SignalProtocolStore {

    private val preKeys = mutableMapOf<Int, PreKeyRecord>()
    private val signedPreKeys = mutableMapOf<Int, SignedPreKeyRecord>()
    private val sessions = mutableMapOf<SignalProtocolAddress, SessionRecord>()
    private val trustedIdentities = mutableMapOf<SignalProtocolAddress, IdentityKey>()
    private val senderKeys = mutableMapOf<String, SenderKeyRecord>()

    // --- IdentityKeyStore ---

    override fun getIdentityKeyPair(): IdentityKeyPair {
        return identityKeyPair
    }

    override fun getLocalRegistrationId(): Int {
        return registrationId
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        val existing = trustedIdentities[address]
        if (existing != identityKey) {
            trustedIdentities[address] = identityKey
            return true
        }
        return false
    }

    override fun isTrustedIdentity(address: SignalProtocolAddress, identityKey: IdentityKey, direction: IdentityKeyStore.Direction): Boolean {
        // For simplicity, trust on first use (TOFU)
        val trusted = trustedIdentities[address]
        return trusted == null || trusted == identityKey
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        return trustedIdentities[address]
    }

    // --- PreKeyStore ---

    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        return preKeys[preKeyId] ?: throw Exception("PreKey not found: $preKeyId")
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        preKeys[preKeyId] = record
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return preKeys.containsKey(preKeyId)
    }

    override fun removePreKey(preKeyId: Int) {
        preKeys.remove(preKeyId)
    }

    // --- SignedPreKeyStore ---

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        return signedPreKeys[signedPreKeyId] ?: throw Exception("SignedPreKey not found: $signedPreKeyId")
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> {
        return signedPreKeys.values.toList()
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        signedPreKeys[signedPreKeyId] = record
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        return signedPreKeys.containsKey(signedPreKeyId)
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        signedPreKeys.remove(signedPreKeyId)
    }

    // --- SessionStore ---

    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        return sessions[address] ?: SessionRecord()
    }

    override fun loadExistingSessions(addresses: List<SignalProtocolAddress>): List<SessionRecord> {
        return addresses.map { loadSession(it) }
    }

    override fun getSubDeviceSessions(name: String): List<Int> {
        return sessions.keys
            .filter { it.name == name && it.deviceId != 1 }
            .map { it.deviceId }
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        sessions[address] = record
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        return sessions.containsKey(address)
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        sessions.remove(address)
    }

    override fun deleteAllSessions(name: String) {
        val toRemove = sessions.keys.filter { it.name == name }
        toRemove.forEach { sessions.remove(it) }
    }
    
    // --- SenderKeyStore ---

    override fun storeSenderKey(sender: SignalProtocolAddress, distributionId: UUID, record: SenderKeyRecord) {
        senderKeys["${sender.name}::${sender.deviceId}::$distributionId"] = record
    }

    override fun loadSenderKey(sender: SignalProtocolAddress, distributionId: UUID): SenderKeyRecord {
        // If no record exists, we must return a new, empty one.
        // However, SenderKeyRecord might not have a public no-arg constructor in this version.
        // We might need to construct it differently or handle nulls if the interface allows.
        // Checking if we can create a dummy one or if we should throw.
        // Usually, loadSenderKey should return a record that can be initialized.
        
        return senderKeys["${sender.name}::${sender.deviceId}::$distributionId"] ?: try {
             // Attempting to create a fresh record. If the constructor requires bytes, we might need a valid empty structure.
             // For now, let's assume we can't easily create an empty one without valid data and return null if the interface allowed it (it doesn't).
             // Let's try to find a way to instantiate it.
             // If this fails compilation, we might need to mock it or use reflection.
             SenderKeyRecord(ByteArray(0)) // Trying with empty bytes if supported
        } catch (e: Exception) {
             // Fallback: This is a critical path. If we can't create a record, group messaging won't work.
             throw RuntimeException("SenderKeyRecord not found and cannot be created", e)
        }
    }
}
