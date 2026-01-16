package com.example.mtgutilityapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subsets")
data class SubsetEntity(
    @PrimaryKey val name: String
)
