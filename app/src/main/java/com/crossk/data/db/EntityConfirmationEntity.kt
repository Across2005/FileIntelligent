package com.crossk.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * v4 (CR-1 user feedback loop): log of each user confirmation or ignore
 * action on an entity. Used to surface "is this entity correctly typed?"
 * in the UI and to learn from the user's corrections over time.
 */
@Entity(
    tableName = "entity_confirmations",
    indices = [Index("entityId"), Index("confirmedAt")],
)
data class EntityConfirmationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityId: String,
    val originalType: String,
    val confirmedType: String?,
    val isIgnored: Boolean,
    val confirmedAt: Long,
)
