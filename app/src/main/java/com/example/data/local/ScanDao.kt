package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<ScanRecord>>

    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsList(): List<ScanRecord>

    @Query("SELECT * FROM scan_records WHERE isDuplicate = 1 ORDER BY timestamp DESC")
    fun getDuplicateRecords(): Flow<List<ScanRecord>>

    @Query("SELECT * FROM scan_records WHERE code = :code ORDER BY timestamp DESC")
    suspend fun getRecordsByCode(code: String): List<ScanRecord>

    @Query("SELECT * FROM scan_records WHERE code = :code LIMIT 1")
    suspend fun findFirstByCode(code: String): ScanRecord?

    @Query("SELECT COUNT(*) FROM scan_records")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT code) FROM scan_records")
    fun getUniqueCodeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_records WHERE isDuplicate = 1")
    fun getDuplicateCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ScanRecord): Long

    @Update
    suspend fun update(record: ScanRecord)

    @Delete
    suspend fun delete(record: ScanRecord)

    @Query("DELETE FROM scan_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM scan_records WHERE code LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR courierName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchRecords(query: String): Flow<List<ScanRecord>>
}
