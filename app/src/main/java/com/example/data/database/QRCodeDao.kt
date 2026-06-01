package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QRCodeDao {
    @Query("SELECT * FROM qr_codes ORDER BY isPinned DESC, timestamp DESC")
    fun getAllQRCodes(): Flow<List<QRCodeEntity>>

    @Query("SELECT * FROM qr_codes WHERE isScanned = :isScanned ORDER BY isPinned DESC, timestamp DESC")
    fun getQRCodesFiltered(isScanned: Boolean): Flow<List<QRCodeEntity>>

    @Query("SELECT * FROM qr_codes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY isPinned DESC, timestamp DESC")
    fun searchQRCodes(query: String): Flow<List<QRCodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQRCode(qrCode: QRCodeEntity): Long

    @Update
    suspend fun updateQRCode(qrCode: QRCodeEntity)

    @Delete
    suspend fun deleteQRCode(qrCode: QRCodeEntity)

    @Query("DELETE FROM qr_codes")
    suspend fun clearAll()

    @Query("SELECT * FROM qr_codes WHERE id = :id LIMIT 1")
    suspend fun getQRCodeById(id: Int): QRCodeEntity?
}
