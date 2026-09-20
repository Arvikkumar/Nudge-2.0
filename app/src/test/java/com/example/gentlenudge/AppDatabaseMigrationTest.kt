package com.example.gentlenudge

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gentlenudge.data.db.AppDatabase
import com.example.gentlenudge.data.model.NudgeTask
import com.example.gentlenudge.data.model.TimeGoal
import com.example.gentlenudge.data.model.TimeGoalRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private lateinit var context: Context
    private val testDbName = "migration_test_db.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(testDbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(testDbName)
    }

    private fun createHelper(version: Int): SupportSQLiteDatabase {
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(testDbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Intentionally left blank so specific schemas can be constructed in tests
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun testMigration1To2_addsSoundTypeWithDefault() {
        val db = createHelper(1)
        db.execSQL(
            """
            CREATE TABLE nudge_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                timeLabel TEXT NOT NULL,
                dateLabel TEXT NOT NULL,
                category TEXT NOT NULL,
                priority TEXT NOT NULL,
                repeat TEXT NOT NULL,
                isDone INTEGER NOT NULL,
                section TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                completedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO nudge_tasks (title, timeLabel, dateLabel, category, priority, repeat, isDone, section, createdAt)
            VALUES ('Morning Meditation', '07:00 AM', 'Today', 'Health', 'Normal', 'Daily', 0, 'today', 1600000000000)
            """.trimIndent()
        )

        // Execute Migration 1 -> 2
        AppDatabase.MIGRATION_1_2.migrate(db)

        val cursor = db.query("SELECT id, title, soundType FROM nudge_tasks WHERE title = 'Morning Meditation'")
        assertTrue(cursor.moveToFirst())
        val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
        val soundType = cursor.getString(cursor.getColumnIndexOrThrow("soundType"))
        assertEquals("Morning Meditation", title)
        assertEquals("Small nudge", soundType)
        cursor.close()
        db.close()
    }

    @Test
    fun testMigration2To3_addsRingtoneFields() {
        val db = createHelper(2)
        db.execSQL(
            """
            CREATE TABLE nudge_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                timeLabel TEXT NOT NULL,
                dateLabel TEXT NOT NULL,
                category TEXT NOT NULL,
                priority TEXT NOT NULL,
                repeat TEXT NOT NULL,
                soundType TEXT NOT NULL DEFAULT 'Small nudge',
                isDone INTEGER NOT NULL,
                section TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                completedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO nudge_tasks (title, timeLabel, dateLabel, category, priority, repeat, soundType, isDone, section, createdAt)
            VALUES ('Read a Chapter', '09:00 PM', 'Today', 'Personal', 'Normal', 'Does not repeat', 'Small nudge', 0, 'today', 1600000000000)
            """.trimIndent()
        )

        // Execute Migration 2 -> 3
        AppDatabase.MIGRATION_2_3.migrate(db)

        val cursor = db.query("SELECT id, title, ringtoneUri, ringtoneTitle FROM nudge_tasks WHERE title = 'Read a Chapter'")
        assertTrue(cursor.moveToFirst())
        val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
        val ringtoneUri = cursor.getString(cursor.getColumnIndexOrThrow("ringtoneUri"))
        val ringtoneTitle = cursor.getString(cursor.getColumnIndexOrThrow("ringtoneTitle"))
        assertEquals("Read a Chapter", title)
        assertNull(ringtoneUri)
        assertNull(ringtoneTitle)
        cursor.close()
        db.close()
    }

    @Test
    fun testMigration3To4_addsAttachmentsJson() {
        val db = createHelper(3)
        db.execSQL(
            """
            CREATE TABLE nudge_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                timeLabel TEXT NOT NULL,
                dateLabel TEXT NOT NULL,
                category TEXT NOT NULL,
                priority TEXT NOT NULL,
                repeat TEXT NOT NULL,
                soundType TEXT NOT NULL DEFAULT 'Small nudge',
                ringtoneUri TEXT DEFAULT NULL,
                ringtoneTitle TEXT DEFAULT NULL,
                isDone INTEGER NOT NULL,
                section TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                completedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO nudge_tasks (title, timeLabel, dateLabel, category, priority, repeat, soundType, ringtoneUri, ringtoneTitle, isDone, section, createdAt)
            VALUES ('Pick up groceries', '05:00 PM', 'Today', 'Shopping', 'Important', 'Does not repeat', 'Small nudge', NULL, NULL, 0, 'today', 1600000000000)
            """.trimIndent()
        )

        // Execute Migration 3 -> 4
        AppDatabase.MIGRATION_3_4.migrate(db)

        val cursor = db.query("SELECT id, title, attachmentsJson FROM nudge_tasks WHERE title = 'Pick up groceries'")
        assertTrue(cursor.moveToFirst())
        val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
        val attachmentsJson = cursor.getString(cursor.getColumnIndexOrThrow("attachmentsJson"))
        assertEquals("Pick up groceries", title)
        assertNull(attachmentsJson)
        cursor.close()
        db.close()
    }

    @Test
    fun testMigration4To5_createsTimeGoalsAndRecords() {
        val db = createHelper(4)
        db.execSQL(
            """
            CREATE TABLE nudge_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                timeLabel TEXT NOT NULL,
                dateLabel TEXT NOT NULL,
                category TEXT NOT NULL,
                priority TEXT NOT NULL,
                repeat TEXT NOT NULL,
                soundType TEXT NOT NULL DEFAULT 'Small nudge',
                ringtoneUri TEXT DEFAULT NULL,
                ringtoneTitle TEXT DEFAULT NULL,
                attachmentsJson TEXT DEFAULT NULL,
                isDone INTEGER NOT NULL,
                section TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                completedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO nudge_tasks (title, timeLabel, dateLabel, category, priority, repeat, soundType, isDone, section, createdAt)
            VALUES ('Doctor Appointment', '11:00 AM', 'Tomorrow', 'Health', 'Important', 'Does not repeat', 'Small nudge', 0, 'later', 1600000000000)
            """.trimIndent()
        )

        // Execute Migration 4 -> 5
        AppDatabase.MIGRATION_4_5.migrate(db)

        // Verify task data remains intact
        val taskCursor = db.query("SELECT id, title FROM nudge_tasks WHERE title = 'Doctor Appointment'")
        assertTrue(taskCursor.moveToFirst())
        assertEquals("Doctor Appointment", taskCursor.getString(taskCursor.getColumnIndexOrThrow("title")))
        taskCursor.close()

        // Insert and verify time_goals table
        db.execSQL(
            """
            INSERT INTO time_goals (name, dailyTargetMinutes, colorHex, iconName, startDate, isArchived, createdAt, orderIndex)
            VALUES ('Guitar Practice', 45, '#FF7043', 'music', '2026-08-01', 0, 1600000000000, 1)
            """.trimIndent()
        )
        val goalCursor = db.query("SELECT id, name, dailyTargetMinutes FROM time_goals WHERE name = 'Guitar Practice'")
        assertTrue(goalCursor.moveToFirst())
        assertEquals("Guitar Practice", goalCursor.getString(goalCursor.getColumnIndexOrThrow("name")))
        assertEquals(45, goalCursor.getInt(goalCursor.getColumnIndexOrThrow("dailyTargetMinutes")))
        val goalId = goalCursor.getLong(goalCursor.getColumnIndexOrThrow("id"))
        goalCursor.close()

        // Insert and verify time_goal_records table
        db.execSQL(
            """
            INSERT INTO time_goal_records (goalId, date, actualMinutes, note, updatedAt)
            VALUES ($goalId, '2026-08-25', 40, 'Practiced scales', 1600000000000)
            """.trimIndent()
        )
        val recordCursor = db.query("SELECT id, goalId, date, actualMinutes, note FROM time_goal_records WHERE goalId = $goalId")
        assertTrue(recordCursor.moveToFirst())
        assertEquals("2026-08-25", recordCursor.getString(recordCursor.getColumnIndexOrThrow("date")))
        assertEquals(40, recordCursor.getInt(recordCursor.getColumnIndexOrThrow("actualMinutes")))
        assertEquals("Practiced scales", recordCursor.getString(recordCursor.getColumnIndexOrThrow("note")))
        recordCursor.close()

        db.close()
    }

    @Test
    fun testMigration5To6_addsSoftDeleteFields() {
        val db = createHelper(5)
        db.execSQL(
            """
            CREATE TABLE nudge_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                timeLabel TEXT NOT NULL,
                dateLabel TEXT NOT NULL,
                category TEXT NOT NULL,
                priority TEXT NOT NULL,
                repeat TEXT NOT NULL,
                soundType TEXT NOT NULL DEFAULT 'Small nudge',
                ringtoneUri TEXT,
                ringtoneTitle TEXT,
                attachmentsJson TEXT,
                isDone INTEGER NOT NULL,
                section TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                completedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO nudge_tasks (title, timeLabel, dateLabel, category, priority, repeat, soundType, isDone, section, createdAt)
            VALUES ('Pick up groceries', '05:00 PM', 'Today', 'Personal', 'Normal', 'Does not repeat', 'Small nudge', 0, 'today', 1600000000000)
            """.trimIndent()
        )

        // Execute Migration 5 -> 6
        AppDatabase.MIGRATION_5_6.migrate(db)

        val cursor = db.query("SELECT id, title, isDeleted, deletedAt FROM nudge_tasks WHERE title = 'Pick up groceries'")
        assertTrue(cursor.moveToFirst())
        val isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow("isDeleted"))
        val deletedAtIdx = cursor.getColumnIndexOrThrow("deletedAt")
        assertEquals(0, isDeleted)
        assertTrue(cursor.isNull(deletedAtIdx))
        cursor.close()
        db.close()
    }

    @Test
    fun testFullMigrationChainFromV1ToV6_withRoomDatabaseBuilder() = runBlocking {
        // 1. Create a V1 database with an initial task
        val dbV1 = createHelper(1)
        dbV1.execSQL(
            """
            CREATE TABLE nudge_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                timeLabel TEXT NOT NULL,
                dateLabel TEXT NOT NULL,
                category TEXT NOT NULL,
                priority TEXT NOT NULL,
                repeat TEXT NOT NULL,
                isDone INTEGER NOT NULL,
                section TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                completedAt INTEGER
            )
            """.trimIndent()
        )
        dbV1.execSQL(
            """
            INSERT INTO nudge_tasks (title, timeLabel, dateLabel, category, priority, repeat, isDone, section, createdAt)
            VALUES ('Call Grandpa', '04:00 PM', 'Sunday', 'Family', 'Important', 'Does not repeat', 0, 'later', 1600000000000)
            """.trimIndent()
        )
        dbV1.close()

        // 2. Open with Room specifying all migrations
        val roomDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            testDbName
        )
        .addMigrations(*AppDatabase.ALL_MIGRATIONS)
        .build()

        // 3. Verify that old data survives and new schema features work via DAOs
        val taskDao = roomDb.nudgeTaskDao()
        val allTasks = taskDao.getAllTasksList()
        assertEquals(1, allTasks.size)
        val migratedTask = allTasks[0]
        assertEquals("Call Grandpa", migratedTask.title)
        assertEquals("Family", migratedTask.category)
        assertEquals("Small nudge", migratedTask.soundType) // migrated default
        assertNull(migratedTask.ringtoneUri) // migrated nullable
        assertNull(migratedTask.attachmentsJson) // migrated nullable
        assertEquals(false, migratedTask.isDeleted)
        assertNull(migratedTask.deletedAt)

        // 4. Test inserting a new task in V6 format and soft-deleting it
        val newTaskId = taskDao.insertTask(
            NudgeTask(
                title = "Study Room Migrations",
                soundType = "Full ringtone",
                ringtoneTitle = "Chime",
                attachmentsJson = "[]"
            )
        )
        val fetchedNewTask = taskDao.getTaskById(newTaskId)
        assertNotNull(fetchedNewTask)
        assertEquals("Study Room Migrations", fetchedNewTask?.title)
        assertEquals("Full ringtone", fetchedNewTask?.soundType)

        // Test soft-deleting and retrieving via deleted tasks queries
        val now = System.currentTimeMillis()
        taskDao.softDeleteTaskById(newTaskId, now)
        val activeTasksAfterDelete = taskDao.getAllTasksList()
        assertEquals(1, activeTasksAfterDelete.size) // only Grandpa remains in active list

        val deletedTasks = taskDao.getDeletedTasksList()
        assertEquals(1, deletedTasks.size)
        assertEquals("Study Room Migrations", deletedTasks[0].title)
        assertEquals(true, deletedTasks[0].isDeleted)
        assertEquals(now, deletedTasks[0].deletedAt)

        // Test restoring
        taskDao.restoreTaskById(newTaskId)
        assertEquals(2, taskDao.getAllTasksList().size)
        assertEquals(0, taskDao.getDeletedTasksList().size)

        // 5. Test TimeGoal and TimeGoalRecord DAOs on the migrated database
        val goalDao = roomDb.timeGoalDao()
        val goalId = goalDao.insertGoal(
            TimeGoal(
                name = "Deep Work",
                dailyTargetMinutes = 180,
                colorHex = "#3F51B5",
                iconName = "work",
                startDate = "2026-08-01"
            )
        )
        val fetchedGoal = goalDao.getGoalById(goalId)
        assertNotNull(fetchedGoal)
        assertEquals("Deep Work", fetchedGoal?.name)
        assertEquals(180, fetchedGoal?.dailyTargetMinutes)

        goalDao.insertOrUpdateRecord(
            TimeGoalRecord(
                goalId = goalId,
                date = "2026-08-26",
                actualMinutes = 150,
                note = "Migration completed"
            )
        )
        val records = goalDao.getAllRecordsList()
        assertEquals(1, records.size)
        assertEquals("2026-08-26", records[0].date)
        assertEquals(150, records[0].actualMinutes)

        roomDb.close()
    }

    @Test
    fun testMigration6To7_createsTimeGoalMonthColorsTable() = runBlocking {
        val db = createHelper(6)
        db.execSQL(
            """
            CREATE TABLE nudge_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                timeLabel TEXT NOT NULL,
                dateLabel TEXT NOT NULL,
                category TEXT NOT NULL,
                priority TEXT NOT NULL,
                repeat TEXT NOT NULL,
                isDone INTEGER NOT NULL,
                section TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                completedAt INTEGER,
                soundType TEXT NOT NULL DEFAULT 'Small nudge',
                ringtoneTitle TEXT NOT NULL DEFAULT 'Gentle Melody',
                ringtoneUri TEXT,
                vibrationPattern TEXT NOT NULL DEFAULT 'Gentle Pulse',
                attachmentsJson TEXT NOT NULL DEFAULT '[]',
                reminderCount INTEGER NOT NULL DEFAULT 1,
                intervalMinutes INTEGER NOT NULL DEFAULT 0,
                isDeleted INTEGER NOT NULL DEFAULT 0,
                deletedAt INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE time_goals (
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
            CREATE TABLE time_goal_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                goalId INTEGER NOT NULL,
                date TEXT NOT NULL,
                actualMinutes INTEGER NOT NULL,
                note TEXT,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES time_goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Run Migration 6 -> 7
        AppDatabase.MIGRATION_6_7.migrate(db)

        // Verify time_goal_month_colors table was created and can be inserted into
        db.execSQL(
            """
            INSERT INTO time_goal_month_colors (goalId, yearMonth, colorHex, updatedAt)
            VALUES (1, '2026-08', '#FF9800', 1600000000000)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO time_goal_month_colors (goalId, yearMonth, colorHex, updatedAt)
            VALUES (1, '2026-09', '#1E88E5', 1600000001000)
            """.trimIndent()
        )

        val cursor = db.query("SELECT goalId, yearMonth, colorHex FROM time_goal_month_colors WHERE goalId = 1 ORDER BY yearMonth ASC")
        assertTrue(cursor.moveToFirst())
        assertEquals("2026-08", cursor.getString(cursor.getColumnIndexOrThrow("yearMonth")))
        assertEquals("#FF9800", cursor.getString(cursor.getColumnIndexOrThrow("colorHex")))

        assertTrue(cursor.moveToNext())
        assertEquals("2026-09", cursor.getString(cursor.getColumnIndexOrThrow("yearMonth")))
        assertEquals("#1E88E5", cursor.getString(cursor.getColumnIndexOrThrow("colorHex")))
        cursor.close()
        db.close()
    }
}
