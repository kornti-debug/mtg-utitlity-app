package com.example.mtgutilityapp.data.local

import androidx.room.*
import com.example.mtgutilityapp.data.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY scannedAt DESC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE scanId = :scanId")
    suspend fun getCardByScanId(scanId: Long): CardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()
    
    @Query("SELECT DISTINCT subset FROM cards WHERE subset IS NOT NULL")
    fun getAllSubsets(): Flow<List<String>>
}
