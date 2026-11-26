package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppUser
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUserDao {

    @Query("SELECT * FROM app_users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<AppUser>>

    @Query("SELECT * FROM app_users ORDER BY createdAt DESC")
    suspend fun getAllUsersSync(): List<AppUser>

    @Query("SELECT * FROM app_users WHERE id = :id")
    suspend fun getUserById(id: Long): AppUser?

    @Query("SELECT * FROM app_users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): AppUser?

    @Query("SELECT COUNT(*) FROM app_users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: AppUser): Long

    @Update
    suspend fun update(user: AppUser)

    @Delete
    suspend fun delete(user: AppUser)

    @Query("UPDATE app_users SET password = :newPassword WHERE id = :userId")
    suspend fun updatePassword(userId: Long, newPassword: String)

    @Query("UPDATE app_users SET isActive = :isActive WHERE id = :userId")
    suspend fun updateStatus(userId: Long, isActive: Boolean)

    @Query("UPDATE app_users SET approvalStatus = :status, isActive = :isActive WHERE id = :userId")
    suspend fun updateApproval(userId: Long, status: String, isActive: Boolean)

    @Query("SELECT * FROM app_users WHERE approvalStatus = :status ORDER BY createdAt DESC")
    fun getUsersByApprovalStatus(status: String): Flow<List<AppUser>>

    @Query("SELECT COUNT(*) FROM app_users WHERE approvalStatus = 'PENDING'")
    fun getPendingApprovalCount(): Flow<Int>

    @Query("UPDATE app_users SET cloudBackupData = :backupJson, lastCloudBackupAt = :backupTime WHERE phone = :phone")
    suspend fun saveCloudBackup(phone: String, backupJson: String, backupTime: Long)

    @Query("DELETE FROM app_users WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM app_users WHERE phone = :phone")
    suspend fun deleteByPhone(phone: String)
}
