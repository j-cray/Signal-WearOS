package com.example.signalwearos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.signalwearos.data.db.entity.SignedPreKeyEntity

@Dao
interface SignedPreKeyDao {
    @Query("SELECT * FROM signed_pre_keys WHERE signedPreKeyId = :signedPreKeyId")
    fun getSignedPreKey(signedPreKeyId: Int): SignedPreKeyEntity?

    @Query("SELECT * FROM signed_pre_keys")
    fun getAllSignedPreKeys(): List<SignedPreKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSignedPreKey(signedPreKey: SignedPreKeyEntity)

    @Query("DELETE FROM signed_pre_keys WHERE signedPreKeyId = :signedPreKeyId")
    fun deleteSignedPreKey(signedPreKeyId: Int)
}
