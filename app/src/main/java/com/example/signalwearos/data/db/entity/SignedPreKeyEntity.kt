package com.example.signalwearos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signed_pre_keys")
data class SignedPreKeyEntity(
    @PrimaryKey val signedPreKeyId: Int,
    val record: ByteArray // serialized SignedPreKeyRecord
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignedPreKeyEntity

        if (signedPreKeyId != other.signedPreKeyId) return false
        if (!record.contentEquals(other.record)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = signedPreKeyId
        result = 31 * result + record.contentHashCode()
        return result
    }
}
