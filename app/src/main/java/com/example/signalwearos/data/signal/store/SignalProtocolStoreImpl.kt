package com.example.signalwearos.data.signal.store

import android.content.Context
import com.example.signalwearos.data.db.SignalDatabase
import com.example.signalwearos.data.db.entity.IdentityKeyEntity
import com.example.signalwearos.data.db.entity.PreKeyEntity
import com.example.signalwearos.data.db.entity.SessionEntity
import com.example.signalwearos.data.db.entity.SignedPreKeyEntity
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
 * A Room-backed implementation of the SignalProtocolStore.
 */
class SignalProtocolStoreImpl(
    context: Context,
    private val identityKeyPair: IdentityKeyPair,
    private val registrationId: Int
) : SignalProtocolStore {

    private val database = SignalDatabase.getDatabase(context)
    private val senderKeys = mutableMapOf<String, SenderKeyRecord>() // SenderKeys still in memory for now

    // --- IdentityKeyStore ---

    override fun getIdentityKeyPair(): IdentityKeyPair {
        return identityKeyPair
    }

    override fun getLocalRegistrationId(): Int {
        return registrationId
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        val addressString = "${address.name}::${address.deviceId}"
        val existingEntity = database.identityKeyDao().getIdentityKey(addressString)
        val existingKey = existingEntity?.let { IdentityKey(it.identityKey, 0) }

        if (existingKey != identityKey) {
            database.identityKeyDao().saveIdentityKey(
                IdentityKeyEntity(addressString, identityKey.serialize())
            )
            return true
        }
        return false
    }

    override fun isTrustedIdentity(address: SignalProtocolAddress, identityKey: IdentityKey, direction: IdentityKeyStore.Direction): Boolean {
        val addressString = "${address.name}::${address.deviceId}"
        val existingEntity = database.identityKeyDao().getIdentityKey(addressString)
        val trusted = existingEntity?.let { IdentityKey(it.identityKey, 0) }
        return trusted == null || trusted == identityKey
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        val addressString = "${address.name}::${address.deviceId}"
        val entity = database.identityKeyDao().getIdentityKey(addressString)
        return entity?.let { IdentityKey(it.identityKey, 0) }
    }

    // --- PreKeyStore ---

    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        val entity = database.preKeyDao().getPreKey(preKeyId)
            ?: throw Exception("PreKey not found: $preKeyId")
        return PreKeyRecord(entity.record)
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        database.preKeyDao().savePreKey(PreKeyEntity(preKeyId, record.serialize()))
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return database.preKeyDao().getPreKey(preKeyId) != null
    }

    override fun removePreKey(preKeyId: Int) {
        database.preKeyDao().deletePreKey(preKeyId)
    }

    // --- SignedPreKeyStore ---

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        val entity = database.signedPreKeyDao().getSignedPreKey(signedPreKeyId)
            ?: throw Exception("SignedPreKey not found: $signedPreKeyId")
        return SignedPreKeyRecord(entity.record)
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> {
        return database.signedPreKeyDao().getAllSignedPreKeys().map {
            SignedPreKeyRecord(it.record)
        }
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        database.signedPreKeyDao().saveSignedPreKey(SignedPreKeyEntity(signedPreKeyId, record.serialize()))
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        return database.signedPreKeyDao().getSignedPreKey(signedPreKeyId) != null
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        database.signedPreKeyDao().deleteSignedPreKey(signedPreKeyId)
    }

    // --- SessionStore ---

    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        val addressString = "${address.name}::${address.deviceId}"
        val entity = database.sessionDao().getSession(addressString)
        return if (entity != null) {
            SessionRecord(entity.record)
        } else {
            SessionRecord()
        }
    }

    override fun loadExistingSessions(addresses: List<SignalProtocolAddress>): List<SessionRecord> {
        val addressStrings = addresses.map { "${it.name}::${it.deviceId}" }
        val entities = database.sessionDao().getSessions(addressStrings)
        // Map back to the order of requested addresses, returning empty records for missing ones
        val entityMap = entities.associateBy { it.address }
        return addressStrings.map { addr ->
            entityMap[addr]?.let { SessionRecord(it.record) } ?: SessionRecord()
        }
    }

    override fun getSubDeviceSessions(name: String): List<Int> {
        // This is tricky with Room without a specific query. 
        // For now, we'll return empty list or implement a specific DAO method if needed.
        // A proper implementation would query "SELECT address FROM sessions WHERE address LIKE :name || '::%'"
        // and parse the device IDs.
        return emptyList() 
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        val addressString = "${address.name}::${address.deviceId}"
        database.sessionDao().saveSession(SessionEntity(addressString, record.serialize()))
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        val addressString = "${address.name}::${address.deviceId}"
        return database.sessionDao().getSession(addressString) != null
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        val addressString = "${address.name}::${address.deviceId}"
        database.sessionDao().deleteSession(addressString)
    }

    override fun deleteAllSessions(name: String) {
        database.sessionDao().deleteAllSessions(name)
    }
    
    // --- SenderKeyStore ---

    override fun storeSenderKey(sender: SignalProtocolAddress, distributionId: UUID, record: SenderKeyRecord) {
        senderKeys["${sender.name}::${sender.deviceId}::$distributionId"] = record
    }

    override fun loadSenderKey(sender: SignalProtocolAddress, distributionId: UUID): SenderKeyRecord {
        return senderKeys["${sender.name}::${sender.deviceId}::$distributionId"] ?: try {
             SenderKeyRecord(ByteArray(0)) 
        } catch (e: Exception) {
             throw RuntimeException("SenderKeyRecord not found and cannot be created", e)
        }
    }
}
