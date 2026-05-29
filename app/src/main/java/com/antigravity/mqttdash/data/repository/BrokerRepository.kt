package com.antigravity.mqttdash.data.repository

import com.antigravity.mqttdash.data.db.dao.BrokerDao
import com.antigravity.mqttdash.data.db.entity.BrokerEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrokerRepository @Inject constructor(
    private val brokerDao: BrokerDao
) {
    /** All brokers ordered by most recently connected. */
    val allBrokers: Flow<List<BrokerEntity>> = brokerDao.getAllBrokers()

    suspend fun getBrokerById(id: Long): BrokerEntity? = brokerDao.getBrokerById(id)

    /** Insert or update a broker. Returns the row id. */
    suspend fun saveBroker(broker: BrokerEntity): Long = brokerDao.insertBroker(broker)

    suspend fun deleteBroker(broker: BrokerEntity) = brokerDao.deleteBroker(broker)

    /** Call after a successful connection to bubble this broker to the top of the history list. */
    suspend fun markConnected(id: Long) =
        brokerDao.updateLastConnected(id, System.currentTimeMillis())
}
