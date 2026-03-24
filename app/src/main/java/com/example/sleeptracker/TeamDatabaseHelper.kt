package com.example.sleeptracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class TeamContent(
    val title: String,
    val description: String,
    val imageName: String
)

class TeamDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TEAM_CONTENT (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT UNIQUE NOT NULL,
                $COLUMN_DESCRIPTION TEXT NOT NULL,
                $COLUMN_IMAGE_NAME TEXT NOT NULL
            )
            """.trimIndent()
        )
        seedDefaultData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TEAM_CONTENT")
        onCreate(db)
    }

    fun getContentByTitle(title: String): TeamContent? {
        val database = readableDatabase
        val cursor = database.query(
            TABLE_TEAM_CONTENT,
            arrayOf(COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_IMAGE_NAME),
            "$COLUMN_TITLE = ?",
            arrayOf(title),
            null,
            null,
            null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                TeamContent(
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    imageName = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE_NAME))
                )
            } else {
                null
            }
        }
    }

    private fun seedDefaultData(db: SQLiteDatabase) {
        DEFAULT_TEAM_CONTENT.forEach { content ->
            val values = ContentValues().apply {
                put(COLUMN_TITLE, content.title)
                put(COLUMN_DESCRIPTION, content.description)
                put(COLUMN_IMAGE_NAME, content.imageName)
            }
            db.insert(TABLE_TEAM_CONTENT, null, values)
        }
    }

    companion object {
        private const val DATABASE_NAME = "team_content.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_TEAM_CONTENT = "team_content"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_IMAGE_NAME = "image_name"

        private val DEFAULT_TEAM_CONTENT = listOf(
            TeamContent(
                title = "About Us",
                description = "Sleep Tracker helps users sign in, save profile details, log today's bedtime, and receive useful reminders for health and safety.",
                imageName = "app_logo"
            ),
            TeamContent(
                title = "Team Details",
                description = "Team Name: Sleep Keepers\nApplication Type: Individual Android application\nCore Stack: Kotlin, XML layouts, SharedPreferences, SQLite, services, notifications, location, and SMS.",
                imageName = "app_logo"
            ),
            TeamContent(
                title = "Team Members",
                description = "1. Student Developer\n2. UI and Logic Integrator\n3. Testing and Documentation Support",
                imageName = "member1"
            ),
            TeamContent(
                title = "Member Profiles",
                description = "Developer Profile:\nPicture included above.\nResponsibilities: built login/sign-up, sleep recommendation, swipeable history carousel, reminder scheduling, SQLite pages, location lookup, SMS alert, and playlist support.",
                imageName = "member1"
            ),
            TeamContent(
                title = "Project Description",
                description = "This project records today's sleep plan, recommends wake-up time from the user's preferred sleep hours, stores history, provides carousel-based daily cards, and demonstrates Android course features such as menus, dialogs, services, notifications, SMS, geocoder, and SQLite.",
                imageName = "app_logo"
            )
        )
    }
}
