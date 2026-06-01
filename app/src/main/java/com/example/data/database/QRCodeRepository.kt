package com.example.data.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class QRCodeRepository(private val qrCodeDao: QRCodeDao) {

    val allQRCodes: Flow<List<QRCodeEntity>> = qrCodeDao.getAllQRCodes()

    fun getFilteredCodes(isScanned: Boolean): Flow<List<QRCodeEntity>> {
        return qrCodeDao.getQRCodesFiltered(isScanned)
    }

    fun searchCodes(query: String): Flow<List<QRCodeEntity>> {
        return qrCodeDao.searchQRCodes(query)
    }

    suspend fun insert(qrCode: QRCodeEntity): Long = withContext(Dispatchers.IO) {
        qrCodeDao.insertQRCode(qrCode)
    }

    suspend fun update(qrCode: QRCodeEntity) = withContext(Dispatchers.IO) {
        qrCodeDao.updateQRCode(qrCode)
    }

    suspend fun delete(qrCode: QRCodeEntity) = withContext(Dispatchers.IO) {
        qrCodeDao.deleteQRCode(qrCode)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        qrCodeDao.clearAll()
    }

    suspend fun getCodeById(id: Int): QRCodeEntity? = withContext(Dispatchers.IO) {
        qrCodeDao.getQRCodeById(id)
    }
}
