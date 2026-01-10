package com.example.signalwearos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.signalwearos.data.db.dao.IdentityKeyDao
import com.example.signalwearos.data.db.dao.PreKeyDao
import com.example.signalwearos.data.db.dao.SessionDao
import com.example.signalwearos.data.db.dao.SignedPreKeyDao
import com.example.signalwearos.data.db.entity.IdentityKeyEntity
import com.example.signalwearos.data.db.entity.PreKeyEntity
import com.example.signalwearos.data.db.entity.SessionEntity
import com.example.signalwearos.data.db.entity.SignedPreKeyEntity

@Database(
    entities = [
        IdentityKeyEntity::class,
        PreKeyEntity::class,
        SignedPreKeyEntity::class,
        SessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SignalDatabase : RoomDatabase() {
    abstract fun identityKeyDao(): IdentityKeyDao
    abstract fun preKeyDao(): PreKeyDao
    abstract fun signedPreKeyDao(): SignedPreKeyDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: SignalDatabase? = null

        fun getDatabase(context: Context): SignalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SignalDatabase::class.java,
                    "signal_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
