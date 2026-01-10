package com.example.signalwearos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val address: String, // format: name::deviceId
    val record: ByteArray // serialized SessionRecord
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SessionEntity

        if (address != other.address) return false
        if (!record.contentEquals(other.record)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + record.contentHashCode()
        return result
    }
}
