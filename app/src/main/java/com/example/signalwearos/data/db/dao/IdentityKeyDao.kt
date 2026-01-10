package com.example.signalwearos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.signalwearos.data.db.entity.IdentityKeyEntity

@Dao
interface IdentityKeyDao {
    @Query("SELECT * FROM identity_keys WHERE address = :address")
    fun getIdentityKey(address: String): IdentityKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveIdentityKey(identityKey: IdentityKeyEntity)
}
