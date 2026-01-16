package com.example.mtgutilityapp.data.local

import androidx.room.*
import com.example.mtgutilityapp.data.local.entity.SubsetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubsetDao {
    @Query("SELECT * FROM subsets ORDER BY name ASC")
    fun getAllSubsets(): Flow<List<SubsetEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubset(subset: SubsetEntity)

    @Delete
    suspend fun deleteSubset(subset: SubsetEntity)
}
