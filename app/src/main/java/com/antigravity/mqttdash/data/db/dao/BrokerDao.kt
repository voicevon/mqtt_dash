package com.antigravity.mqttdash.data.db.dao

import androidx.room.*
import com.antigravity.mqttdash.data.db.entity.BrokerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrokerDao {

    @Query("SELECT * FROM brokers ORDER BY lastConnectedAt DESC")
    fun getAllBrokers(): Flow<List<BrokerEntity>>

    @Query("SELECT * FROM brokers WHERE id = :id")
    suspend fun getBrokerById(id: Long): BrokerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroker(broker: BrokerEntity): Long

    @Update
    suspend fun updateBroker(broker: BrokerEntity)

    @Delete
    suspend fun deleteBroker(broker: BrokerEntity)

    @Query("UPDATE brokers SET lastConnectedAt = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Long, timestamp: Long)

    @Query("SELECT * FROM brokers WHERE host = :host AND port = :port AND useTls = :useTls AND clientId = :clientId")
    suspend fun getBrokersByAddress(
        host: String,
        port: Int,
        useTls: Boolean,
        clientId: String
    ): List<BrokerEntity>
}
