package com.studentutilityhub.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val startTimeMillis: Long
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val startTimeMillis: Long
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val category: String = "General"
)

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY startTimeMillis ASC")
    fun getAll(): List<ScheduleEntity>

    @Query("DELETE FROM schedules")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<ScheduleEntity>)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY startTimeMillis ASC")
    fun getAll(): List<ReminderEntity>

    @Query("DELETE FROM reminders")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<ReminderEntity>)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY id ASC")
    fun getAll(): List<ExpenseEntity>

    @Query("DELETE FROM expenses")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<ExpenseEntity>)
}

@Database(
    entities = [ScheduleEntity::class, ReminderEntity::class, ExpenseEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun reminderDao(): ReminderDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_utility_hub.db"
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
