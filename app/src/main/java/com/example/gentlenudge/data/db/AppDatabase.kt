package com.example.gentlenudge.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalMonthColor
import com.example.gentlenudge.data.model.TimeGoalRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [NudgeTask::class, TimeGoal::class, TimeGoalRecord::class, TimeGoalMonthColor::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun nudgeTaskDao(): NudgeTaskDao
    abstract fun timeGoalDao(): TimeGoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE nudge_tasks ADD COLUMN soundType TEXT NOT NULL DEFAULT 'Small nudge'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE nudge_tasks ADD COLUMN ringtoneUri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE nudge_tasks ADD COLUMN ringtoneTitle TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE nudge_tasks ADD COLUMN attachmentsJson TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS time_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        dailyTargetMinutes INTEGER NOT NULL,
                        colorHex TEXT NOT NULL,
                        iconName TEXT NOT NULL,
                        startDate TEXT NOT NULL,
                        isArchived INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        orderIndex INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS time_goal_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goalId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        actualMinutes INTEGER NOT NULL,
                        note TEXT,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_time_goal_records_goalId_date ON time_goal_records (goalId, date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_time_goal_records_date ON time_goal_records (date)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE nudge_tasks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE nudge_tasks ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS time_goal_month_colors (
                        goalId INTEGER NOT NULL,
                        yearMonth TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(goalId, yearMonth)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_time_goal_month_colors_yearMonth ON time_goal_month_colors (yearMonth)")

                // Populate existing goals into time_goal_month_colors for their start month and any recorded months
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO time_goal_month_colors (goalId, yearMonth, colorHex, updatedAt)
                    SELECT g.id, substr(g.startDate, 1, 7), g.colorHex, g.createdAt
                    FROM time_goals g
                    WHERE g.startDate IS NOT NULL AND length(g.startDate) >= 7
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO time_goal_month_colors (goalId, yearMonth, colorHex, updatedAt)
                    SELECT r.goalId, substr(r.date, 1, 7), g.colorHex, r.updatedAt
                    FROM time_goal_records r
                    JOIN time_goals g ON r.goalId = g.id
                    WHERE r.date IS NOT NULL AND length(r.date) >= 7
                    """.trimIndent()
                )
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7
        )

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gentle_nudge_database"
                )
                .addMigrations(*ALL_MIGRATIONS)
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.nudgeTaskDao(), database.timeGoalDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: NudgeTaskDao, timeGoalDao: TimeGoalDao? = null) {
            val seedTasks = listOf(
                NudgeTask(
                    title = "Pick up the laundry",
                    timeLabel = "10:00 AM",
                    dateLabel = "Today",
                    category = "",
                    priority = "Normal",
                    repeat = "Does not repeat",
                    isDone = false,
                    section = "today"
                ),
                NudgeTask(
                    title = "Call mummy",
                    timeLabel = "1:30 PM",
                    dateLabel = "Today",
                    category = "",
                    priority = "Normal",
                    repeat = "Does not repeat",
                    isDone = false,
                    section = "today"
                ),
                NudgeTask(
                    title = "Take notes",
                    timeLabel = "4:00 PM",
                    dateLabel = "Today",
                    category = "",
                    priority = "Normal",
                    repeat = "Does not repeat",
                    isDone = false,
                    section = "today"
                ),
                NudgeTask(
                    title = "Pay the electricity bill",
                    timeLabel = "7:00 PM",
                    dateLabel = "Today",
                    category = "",
                    priority = "Normal",
                    repeat = "Does not repeat",
                    isDone = false,
                    section = "today"
                ),
                NudgeTask(
                    title = "Book the train ticket",
                    timeLabel = "Tomorrow, 10:00 AM",
                    dateLabel = "Tomorrow",
                    category = "",
                    priority = "Normal",
                    repeat = "Does not repeat",
                    isDone = false,
                    section = "later"
                ),
                NudgeTask(
                    title = "Cancel the subscription",
                    timeLabel = "Fri, 2:00 PM",
                    dateLabel = "Friday",
                    category = "",
                    priority = "Normal",
                    repeat = "Does not repeat",
                    isDone = false,
                    section = "later"
                )
            )
            dao.insertAll(seedTasks)

            timeGoalDao?.let { gDao ->
                val now = java.time.LocalDate.now()
                val currentYearMonth = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-", java.util.Locale.US).format(now)
                val startOfMonth = "${currentYearMonth}01"

                val starterGoals = listOf(
                    TimeGoal(
                        id = 1,
                        name = "Reading",
                        dailyTargetMinutes = 120, // 2 hours
                        colorHex = "#7C4DFF",
                        iconName = "book",
                        startDate = startOfMonth,
                        orderIndex = 0
                    ),
                    TimeGoal(
                        id = 2,
                        name = "Exercise",
                        dailyTargetMinutes = 60, // 1 hour
                        colorHex = "#1E88E5",
                        iconName = "fitness",
                        startDate = startOfMonth,
                        orderIndex = 1
                    ),
                    TimeGoal(
                        id = 3,
                        name = "Family Time",
                        dailyTargetMinutes = 60, // 1 hour
                        colorHex = "#E65100",
                        iconName = "community",
                        startDate = startOfMonth,
                        orderIndex = 2
                    ),
                    TimeGoal(
                        id = 4,
                        name = "Personal Projects",
                        dailyTargetMinutes = 120, // 2 hours
                        colorHex = "#2E7D32",
                        iconName = "work",
                        startDate = startOfMonth,
                        orderIndex = 3
                    )
                )
                gDao.insertAllGoals(starterGoals)
                val starterMonthColors = starterGoals.map {
                    TimeGoalMonthColor(
                        goalId = it.id,
                        yearMonth = currentYearMonth.trimEnd('-'),
                        colorHex = it.colorHex
                    )
                }
                gDao.insertAllMonthColors(starterMonthColors)
            }
        }
    }
}
