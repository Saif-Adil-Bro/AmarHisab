package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Profiles table.
 * Handles profiles creation, updating, deletion, and default resets.
 */
@Dao
interface ProfileDao {

    /**
     * Inserts a new profile and returns its auto-generated Row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    /**
     * Updates an existing profile details.
     */
    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    /**
     * Deletes a profile. Cascade constraint removes its expenses and shopping lists.
     */
    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    /**
     * Emits all profiles, prioritizing default ones first, then sorting alphabetically.
     */
    @Query("SELECT * FROM profiles ORDER BY isDefault DESC, name ASC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    /**
     * Retrieves the designated default Profile, or null if key unset.
     */
    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): ProfileEntity?

    /**
     * Sets all profiles as non-default, allowing standard single-default enforcement.
     */
    @Query("UPDATE profiles SET isDefault = 0")
    suspend fun clearDefaultProfile()
}
