package com.example.signalwearos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pre_keys")
data class PreKeyEntity(
    @PrimaryKey val preKeyId: Int,
    val record: ByteArray // serialized PreKeyRecord
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreKeyEntity

        if (preKeyId != other.preKeyId) return false
        if (!record.contentEquals(other.record)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = preKeyId
        result = 31 * result + record.contentHashCode()
        return result
    }
}
