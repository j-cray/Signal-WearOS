package com.example.signalwearos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identity_keys")
data class IdentityKeyEntity(
    @PrimaryKey val address: String, // format: name::deviceId
    val identityKey: ByteArray // serialized IdentityKey
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IdentityKeyEntity

        if (address != other.address) return false
        if (!identityKey.contentEquals(other.identityKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + identityKey.contentHashCode()
        return result
    }
}
