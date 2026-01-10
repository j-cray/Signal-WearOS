package com.example.signalwearos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.signalwearos.data.db.entity.PreKeyEntity

@Dao
interface PreKeyDao {
    @Query("SELECT * FROM pre_keys WHERE preKeyId = :preKeyId")
    fun getPreKey(preKeyId: Int): PreKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun savePreKey(preKey: PreKeyEntity)

    @Query("DELETE FROM pre_keys WHERE preKeyId = :preKeyId")
    fun deletePreKey(preKeyId: Int)
}
