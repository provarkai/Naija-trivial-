package com.ai4biz.app.data.local

import androidx.room.TypeConverter
import com.ai4biz.app.model.BrandTone
import com.ai4biz.app.model.BusinessGoalType
import com.ai4biz.app.model.MessageRole
import com.ai4biz.app.model.ProductServiceType

/**
 * Room can't persist enums natively -- each Phase 2 enum is stored as its
 * [Enum.name] (TEXT column) and parsed back with [Enum.valueOf]. Registered
 * on [AppDatabase] via `@TypeConverters(Converters::class)`.
 */
class Converters {

    @TypeConverter
    fun brandToneToString(value: BrandTone): String = value.name

    @TypeConverter
    fun stringToBrandTone(value: String): BrandTone = BrandTone.valueOf(value)

    @TypeConverter
    fun productServiceTypeToString(value: ProductServiceType): String = value.name

    @TypeConverter
    fun stringToProductServiceType(value: String): ProductServiceType = ProductServiceType.valueOf(value)

    @TypeConverter
    fun businessGoalTypeToString(value: BusinessGoalType): String = value.name

    @TypeConverter
    fun stringToBusinessGoalType(value: String): BusinessGoalType = BusinessGoalType.valueOf(value)

    @TypeConverter
    fun messageRoleToString(value: MessageRole): String = value.name

    @TypeConverter
    fun stringToMessageRole(value: String): MessageRole = MessageRole.valueOf(value)
}
