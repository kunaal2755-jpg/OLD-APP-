package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ClientLicense
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientLicenseDao {

    @Query("SELECT * FROM client_licenses ORDER BY createdAt DESC")
    fun getAllLicenses(): Flow<List<ClientLicense>>

    @Query("SELECT * FROM client_licenses WHERE id = :id")
    suspend fun getLicenseById(id: Long): ClientLicense?

    @Query("SELECT * FROM client_licenses WHERE activationKey = :key LIMIT 1")
    suspend fun getLicenseByKey(key: String): ClientLicense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(license: ClientLicense): Long

    @Update
    suspend fun update(license: ClientLicense)

    @Delete
    suspend fun delete(license: ClientLicense)

    @Query("DELETE FROM client_licenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}
