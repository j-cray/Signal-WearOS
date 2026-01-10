package com.example.signalwearos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.signalwearos.data.db.entity.SessionEntity

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE address = :address")
    fun getSession(address: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE address IN (:addresses)")
    fun getSessions(addresses: List<String>): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE address = :address")
    fun deleteSession(address: String)

    @Query("DELETE FROM sessions WHERE address LIKE :name || '::%'")
    fun deleteAllSessions(name: String)
}
