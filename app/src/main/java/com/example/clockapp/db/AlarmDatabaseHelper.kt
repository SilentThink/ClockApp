package com.example.clockapp.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.clockapp.model.Alarm

/**
 * 闹钟数据库帮助类
 */
class AlarmDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "alarm_db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_ALARMS = "alarms"
        private const val COLUMN_ID = "id"
        private const val COLUMN_HOUR = "hour"
        private const val COLUMN_MINUTE = "minute"
        private const val COLUMN_ENABLED = "enabled"
        private const val COLUMN_LABEL = "label"
        private const val COLUMN_REPEAT_DAYS = "repeat_days"
        private const val COLUMN_VIBRATE = "vibrate"
        private const val COLUMN_SOUND_URI = "sound_uri"
        private const val COLUMN_SNOOZE_MINUTES = "snooze_minutes"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE $TABLE_ALARMS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_HOUR INTEGER,
                $COLUMN_MINUTE INTEGER,
                $COLUMN_ENABLED INTEGER,
                $COLUMN_LABEL TEXT,
                $COLUMN_REPEAT_DAYS TEXT,
                $COLUMN_VIBRATE INTEGER,
                $COLUMN_SOUND_URI TEXT,
                $COLUMN_SNOOZE_MINUTES INTEGER
            )
        """.trimIndent()
        db.execSQL(createTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALARMS")
        onCreate(db)
    }

    /**
     * 添加闹钟
     */
    fun addAlarm(alarm: Alarm): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, alarm.id)
            put(COLUMN_HOUR, alarm.hour)
            put(COLUMN_MINUTE, alarm.minute)
            put(COLUMN_ENABLED, if (alarm.isEnabled) 1 else 0)
            put(COLUMN_LABEL, alarm.label)
            put(COLUMN_REPEAT_DAYS, alarm.repeatDays.joinToString(",") { if (it) "1" else "0" })
            put(COLUMN_VIBRATE, if (alarm.vibrate) 1 else 0)
            put(COLUMN_SOUND_URI, alarm.soundUri)
            put(COLUMN_SNOOZE_MINUTES, alarm.snoozeMinutes)
        }

        val result = db.insert(TABLE_ALARMS, null, values)
        db.close()
        return result
    }

    /**
     * 更新闹钟
     */
    fun updateAlarm(alarm: Alarm): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_HOUR, alarm.hour)
            put(COLUMN_MINUTE, alarm.minute)
            put(COLUMN_ENABLED, if (alarm.isEnabled) 1 else 0)
            put(COLUMN_LABEL, alarm.label)
            put(COLUMN_REPEAT_DAYS, alarm.repeatDays.joinToString(",") { if (it) "1" else "0" })
            put(COLUMN_VIBRATE, if (alarm.vibrate) 1 else 0)
            put(COLUMN_SOUND_URI, alarm.soundUri)
            put(COLUMN_SNOOZE_MINUTES, alarm.snoozeMinutes)
        }

        val result = db.update(
            TABLE_ALARMS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(alarm.id)
        )
        db.close()
        return result
    }

    /**
     * 删除闹钟
     */
    fun deleteAlarm(alarmId: String): Int {
        val db = this.writableDatabase
        val result = db.delete(
            TABLE_ALARMS,
            "$COLUMN_ID = ?",
            arrayOf(alarmId)
        )
        db.close()
        return result
    }

    /**
     * 获取所有闹钟
     */
    fun getAllAlarms(): List<Alarm> {
        val alarmList = mutableListOf<Alarm>()
        val selectQuery = "SELECT * FROM $TABLE_ALARMS ORDER BY $COLUMN_HOUR, $COLUMN_MINUTE ASC"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                alarmList.add(cursorToAlarm(cursor))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return alarmList
    }

    /**
     * 获取单个闹钟
     */
    fun getAlarm(alarmId: String): Alarm? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_ALARMS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(alarmId),
            null, null, null
        )

        var alarm: Alarm? = null
        if (cursor.moveToFirst()) {
            alarm = cursorToAlarm(cursor)
        }

        cursor.close()
        db.close()
        return alarm
    }

    /**
     * 将游标转换为闹钟对象
     */
    private fun cursorToAlarm(cursor: Cursor): Alarm {
        val id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID))
        val hour = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HOUR))
        val minute = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MINUTE))
        val enabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENABLED)) == 1
        val label = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LABEL))
        val repeatDaysString = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPEAT_DAYS))
        val vibrate = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VIBRATE)) == 1
        val soundUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOUND_URI))
        val snoozeMinutes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SNOOZE_MINUTES))

        // 解析重复日
        val repeatDays = BooleanArray(7)
        val repeatDaysList = repeatDaysString.split(",")
        for (i in repeatDaysList.indices) {
            if (i < 7) {
                repeatDays[i] = repeatDaysList[i] == "1"
            }
        }

        return Alarm(
            id, hour, minute, enabled, label, repeatDays,
            vibrate, soundUri, snoozeMinutes
        )
    }
} 