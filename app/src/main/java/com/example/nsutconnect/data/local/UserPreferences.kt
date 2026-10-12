package com.example.nsutconnect.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.nsutconnect.data.model.AttendanceDateEntry
import com.example.nsutconnect.data.model.AttendanceRecord
import com.example.nsutconnect.data.model.AttendanceSummary
import com.example.nsutconnect.data.model.CalendarCategory
import com.example.nsutconnect.data.model.ClassScheduleItem
import com.example.nsutconnect.data.model.ClassType
import com.example.nsutconnect.data.model.DaySchedule
import com.example.nsutconnect.data.model.ExamDatesheetItem
import com.example.nsutconnect.data.model.WeeklyTimetable
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.time.DayOfWeek
import java.time.LocalDate
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class UserPreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nsut_connect_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ROLL_NUMBER = "key_roll_number"
        private const val KEY_ENCRYPTED_PASSWORD = "key_enc_pass"
        private const val KEY_PASSWORD_IV = "key_pass_iv"
        private const val KEY_AUTO_LOGIN = "key_auto_login"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_LAST_SYNC = "key_last_sync"
        private const val KEY_ATTENDANCE_JSON = "key_attendance_json"
        private const val KEY_TIMETABLE_JSON = "key_timetable_json"
        private const val KEY_STUDENT_NAME = "key_student_name"
        private const val KEY_BRANCH = "key_branch"
        private const val KEY_NOTIFICATION_ENABLED = "key_notif_enabled"
        private const val KEY_THRESHOLD = "key_threshold"
        private const val KEY_PORTAL_COOKIES = "key_portal_cookies"
        private const val KEY_GOOGLE_CALENDAR_SYNC_ENABLED = "key_google_cal_sync_enabled"
        private const val KEY_LAST_CALENDAR_SYNC_TIMESTAMP = "key_last_cal_sync_timestamp"
        private const val KEY_LAST_CALENDAR_SYNC_COUNT = "key_last_cal_sync_count"
        private const val KEY_AVATAR_SEED = "key_avatar_seed"
        private const val KEY_INCLUDE_ACADEMIC_CALENDAR_IN_SCHEDULE = "key_include_acad_cal_schedule"
        private const val KEY_CALENDAR_TAG_FILTERS = "key_calendar_tag_filters"
        private const val KEY_WIDGET_THEME = "key_widget_theme"
        private const val KEY_WIDGET_ACCENT = "key_widget_accent"
        private const val KEY_WIDGET_BUNKS_ENABLED = "key_widget_bunks_enabled"
        private const val KEY_WIDGET_BANNER_ENABLED = "key_widget_banner_enabled"
        private const val KEY_ATTENDANCE_URL = "key_attendance_url"
        private const val KEY_TIMETABLE_URL = "key_timetable_url"
        private const val KEY_ACTIVITIES_URL = "key_activities_url"
        private const val KEY_COURSES_URL = "key_courses_url"
        private const val KEY_SEMESTER = "key_semester"

        private const val KEYSTORE_ALIAS = "NsutAuthKeyAlias"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    init {
        initKeyStore()
    }

    private fun initKeyStore() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val keyGen = KeyGenerator.getInstance(
                    android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGen.init(spec)
                keyGen.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun encrypt(plainText: String): Pair<String, String>? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey ?: return null
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            Pair(
                Base64.encodeToString(cipherText, Base64.NO_WRAP),
                Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            // Fallback obfuscation if keystore is unavailable in restricted test environments
            val fallbackEnc = Base64.encodeToString(plainText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            Pair(fallbackEnc, "plain_b64")
        }
    }

    private fun decrypt(encryptedText: String, ivBase64: String): String? {
        return try {
            if (ivBase64 == "plain_b64") {
                return String(Base64.decode(encryptedText, Base64.NO_WRAP), StandardCharsets.UTF_8)
            }
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey ?: return null
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val plainBytes = cipher.doFinal(Base64.decode(encryptedText, Base64.NO_WRAP))
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun saveCredentials(rollNumber: String, password: String, autoLogin: Boolean) {
        val currentRoll = getRollNumber()
        val newRoll = rollNumber.trim().uppercase()
        if (currentRoll.isNotBlank() && !currentRoll.equals(newRoll, ignoreCase = true)) {
            prefs.edit()
                .remove(KEY_ATTENDANCE_JSON)
                .remove(KEY_TIMETABLE_JSON)
                .remove(KEY_ATTENDANCE_URL)
                .remove(KEY_TIMETABLE_URL)
                .remove(KEY_STUDENT_NAME)
                .remove(KEY_BRANCH)
                .apply()
        }

        val encrypted = encrypt(password)
        val editor = prefs.edit()
            .putString(KEY_ROLL_NUMBER, newRoll)
            .putBoolean(KEY_AUTO_LOGIN, autoLogin)
            .putBoolean(KEY_IS_LOGGED_IN, true)

        if (encrypted != null) {
            editor.putString(KEY_ENCRYPTED_PASSWORD, encrypted.first)
            editor.putString(KEY_PASSWORD_IV, encrypted.second)
        }
        editor.apply()
    }

    fun getRollNumber(): String = prefs.getString(KEY_ROLL_NUMBER, "") ?: ""

    fun getAttendanceUrl(): String? = prefs.getString(KEY_ATTENDANCE_URL, null)
    fun saveAttendanceUrl(url: String?) {
        prefs.edit().putString(KEY_ATTENDANCE_URL, url).apply()
    }

    fun getTimetableUrl(): String? = prefs.getString(KEY_TIMETABLE_URL, null)
    fun saveTimetableUrl(url: String?) {
        prefs.edit().putString(KEY_TIMETABLE_URL, url).apply()
    }

    fun getActivitiesUrl(): String? = prefs.getString(KEY_ACTIVITIES_URL, null)
    fun saveActivitiesUrl(url: String?) {
        prefs.edit().putString(KEY_ACTIVITIES_URL, url).apply()
    }

    fun getCoursesUrl(): String? = prefs.getString(KEY_COURSES_URL, null)
    fun saveCoursesUrl(url: String?) {
        prefs.edit().putString(KEY_COURSES_URL, url).apply()
    }

    fun getSemester(): String? = prefs.getString(KEY_SEMESTER, null)
    fun saveSemester(semester: String?) {
        prefs.edit().putString(KEY_SEMESTER, semester).apply()
    }

    fun getSavedPassword(): String? {
        val enc = prefs.getString(KEY_ENCRYPTED_PASSWORD, null) ?: return null
        val iv = prefs.getString(KEY_PASSWORD_IV, null) ?: return null
        return decrypt(enc, iv)
    }

    fun isAutoLoginEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_LOGIN, true)

    fun setAutoLoginEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LOGIN, enabled).apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
    }

    fun clearSession(keepCredentials: Boolean = true) {
        val editor = prefs.edit()
            .remove(KEY_PORTAL_COOKIES)
            .remove(KEY_ATTENDANCE_URL)
            .remove(KEY_TIMETABLE_URL)
            .remove(KEY_ACTIVITIES_URL)
            .remove(KEY_COURSES_URL)
            .putBoolean(KEY_IS_LOGGED_IN, false)
        if (!keepCredentials) {
            editor.remove(KEY_ROLL_NUMBER)
                .remove(KEY_ENCRYPTED_PASSWORD)
                .remove(KEY_PASSWORD_IV)
                .remove(KEY_ATTENDANCE_JSON)
                .remove(KEY_TIMETABLE_JSON)
                .remove(KEY_STUDENT_NAME)
                .remove(KEY_BRANCH)
                .remove(KEY_SEMESTER)
        }
        editor.apply()
    }

    fun hasSavedCredentials(): Boolean {
        return getRollNumber().isNotBlank() && !getSavedPassword().isNullOrBlank()
    }

    fun clearAuth() {
        clearSession(keepCredentials = false)
    }

    fun getBranch(): String = prefs.getString(KEY_BRANCH, "B.Tech.") ?: "B.Tech."
    fun setBranch(branch: String) {
        prefs.edit().putString(KEY_BRANCH, branch).apply()
    }

    fun getStudentName(): String = prefs.getString(KEY_STUDENT_NAME, "STUDENT") ?: "STUDENT"
    fun setStudentName(name: String) {
        prefs.edit().putString(KEY_STUDENT_NAME, name).apply()
    }

    fun savePortalCookies(cookies: String) {
        prefs.edit().putString(KEY_PORTAL_COOKIES, cookies).apply()
    }

    fun getPortalCookies(): String = prefs.getString(KEY_PORTAL_COOKIES, "") ?: ""

    fun getAttendanceThreshold(): Int = prefs.getInt(KEY_THRESHOLD, 75)

    fun setAttendanceThreshold(threshold: Int) {
        prefs.edit().putInt(KEY_THRESHOLD, threshold).apply()
    }

    fun isNotificationEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)

    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
    }

    // --- Cached Attendance Serialization ---

    fun saveAttendanceSummary(summary: AttendanceSummary) {
        try {
            val root = JSONObject().apply {
                put("studentName", summary.studentName)
                put("rollNumber", summary.rollNumber)
                put("branch", summary.branch)
                put("semester", summary.semester)
                put("lastSyncedTimestamp", summary.lastSyncedTimestamp)

                val arr = JSONArray()
                for (rec in summary.records) {
                    val obj = JSONObject().apply {
                        put("subjectCode", rec.subjectCode)
                        put("subjectName", rec.subjectName)
                        put("attendedClasses", rec.attendedClasses)
                        put("totalClasses", rec.totalClasses)
                        put("facultyName", rec.facultyName)
                        put("subjectType", rec.subjectType)

                        val histArr = JSONArray()
                        for (h in rec.history) {
                            val hObj = JSONObject().apply {
                                put("date", h.date)
                                put("statusText", h.statusText)
                                put("statusCode", h.statusCode)
                                if (h.isPresent != null) put("isPresent", h.isPresent)
                            }
                            histArr.put(hObj)
                        }
                        put("history", histArr)
                    }
                    arr.put(obj)
                }
                put("records", arr)
            }
            prefs.edit()
                .putString(KEY_ATTENDANCE_JSON, root.toString())
                .putLong(KEY_LAST_SYNC, summary.lastSyncedTimestamp)
                .putString(KEY_STUDENT_NAME, summary.studentName)
                .putString(KEY_BRANCH, summary.branch)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCachedAttendance(): AttendanceSummary? {
        val json = prefs.getString(KEY_ATTENDANCE_JSON, null) ?: return null
        return try {
            val root = JSONObject(json)
            val cachedRoll = root.optString("rollNumber", "")
            val currentRoll = getRollNumber()
            if (currentRoll.isNotBlank() && cachedRoll.isNotBlank() && !cachedRoll.equals(currentRoll, ignoreCase = true)) {
                return null
            }
            val records = mutableListOf<AttendanceRecord>()
            val arr = root.optJSONArray("records")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val histList = mutableListOf<AttendanceDateEntry>()
                    val histArr = obj.optJSONArray("history")
                    if (histArr != null) {
                        for (j in 0 until histArr.length()) {
                            val hObj = histArr.getJSONObject(j)
                            val isPres = if (hObj.has("isPresent")) hObj.getBoolean("isPresent") else null
                            histList.add(
                                AttendanceDateEntry(
                                    date = hObj.optString("date", ""),
                                    statusText = hObj.optString("statusText", ""),
                                    statusCode = hObj.optString("statusCode", ""),
                                    isPresent = isPres
                                )
                            )
                        }
                    }
                    val subCode = obj.optString("subjectCode", "")
                    var fac = obj.optString("facultyName", "")
                    if (subCode == "VTECC305" && (fac.contains("Rohit", ignoreCase = true) || fac.contains("Neetu", ignoreCase = true) || fac.isBlank())) {
                        fac = "Dr. Neeraj Goel"
                    } else if (subCode == "VTECC304" && (fac.contains("Varun", ignoreCase = true) || fac.contains("Kapil", ignoreCase = true) || fac.contains("Savita", ignoreCase = true) || fac.contains("Sandeep", ignoreCase = true) || fac.isBlank())) {
                        fac = "Prof. Raj Senani"
                    }
                    records.add(
                        AttendanceRecord(
                            subjectCode = subCode,
                            subjectName = obj.optString("subjectName", ""),
                            attendedClasses = obj.optInt("attendedClasses", 0),
                            totalClasses = obj.optInt("totalClasses", 0),
                            facultyName = fac,
                            subjectType = obj.optString("subjectType", "Theory"),
                            history = histList
                        )
                    )
                }
            }
            AttendanceSummary(
                studentName = root.optString("studentName", "Student"),
                rollNumber = root.optString("rollNumber", getRollNumber()),
                branch = root.optString("branch", "Engineering"),
                semester = root.optString("semester", "Current Semester"),
                records = records,
                lastSyncedTimestamp = root.optLong("lastSyncedTimestamp", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }

    // --- Cached Timetable Serialization ---

    fun saveTimetable(timetable: WeeklyTimetable) {
        try {
            val root = JSONObject().apply {
                put("rollNumber", getRollNumber())
                put("studentBranch", timetable.studentBranch)
                put("section", timetable.section)
                put("semester", timetable.semester)
                put("lastSyncedTimestamp", timetable.lastSyncedTimestamp)

                val daysArr = JSONArray()
                for (day in timetable.days) {
                    val dayObj = JSONObject().apply {
                        put("dayOfWeek", day.dayOfWeek.name)
                        val classArr = JSONArray()
                        for (item in day.classes) {
                            val cObj = JSONObject().apply {
                                put("id", item.id)
                                put("dayOfWeek", item.dayOfWeek.name)
                                put("subjectCode", item.subjectCode)
                                put("subjectName", item.subjectName)
                                put("roomNumber", item.roomNumber)
                                put("facultyName", item.facultyName)
                                put("startTime", item.startTime)
                                put("endTime", item.endTime)
                                put("classType", item.classType.name)
                            }
                            classArr.put(cObj)
                        }
                        put("classes", classArr)
                    }
                    daysArr.put(dayObj)
                }
                put("days", daysArr)
            }
            prefs.edit().putString(KEY_TIMETABLE_JSON, root.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCachedTimetable(): WeeklyTimetable? {
        val json = prefs.getString(KEY_TIMETABLE_JSON, null) ?: return null
        return try {
            val root = JSONObject(json)
            val cachedRoll = root.optString("rollNumber", "")
            val currentRoll = getRollNumber()
            if (currentRoll.isNotBlank() && cachedRoll.isNotBlank() && !cachedRoll.equals(currentRoll, ignoreCase = true)) {
                return null
            }
            val daysList = mutableListOf<DaySchedule>()
            val daysArr = root.optJSONArray("days")
            if (daysArr != null) {
                for (i in 0 until daysArr.length()) {
                    val dayObj = daysArr.getJSONObject(i)
                    val dayName = dayObj.optString("dayOfWeek", "MONDAY")
                    val dayOfWeek = try { DayOfWeek.valueOf(dayName) } catch (e: Exception) { DayOfWeek.MONDAY }
                    val classesList = mutableListOf<ClassScheduleItem>()
                    val cArr = dayObj.optJSONArray("classes")
                    if (cArr != null) {
                        for (j in 0 until cArr.length()) {
                            val cObj = cArr.getJSONObject(j)
                            val typeName = cObj.optString("classType", "LECTURE")
                            val cType = try { ClassType.valueOf(typeName) } catch (e: Exception) { ClassType.LECTURE }
                            val subCode = cObj.optString("subjectCode", "")
                            var facName = cObj.optString("facultyName", "")
                            if (subCode == "VTECC305" && (facName.contains("Rohit", ignoreCase = true) || facName.contains("Neetu", ignoreCase = true) || facName.isBlank())) {
                                facName = "Dr. Neeraj Goel"
                            } else if (subCode == "VTECC304" && (facName.contains("Varun", ignoreCase = true) || facName.contains("Kapil", ignoreCase = true) || facName.contains("Savita", ignoreCase = true) || facName.contains("Sandeep", ignoreCase = true) || facName.isBlank())) {
                                facName = "Prof. Raj Senani"
                            }
                            classesList.add(
                                ClassScheduleItem(
                                    id = cObj.optString("id", "${dayOfWeek}_$j"),
                                    dayOfWeek = dayOfWeek,
                                    subjectCode = subCode,
                                    subjectName = cObj.optString("subjectName", ""),
                                    roomNumber = cObj.optString("roomNumber", ""),
                                    facultyName = facName,
                                    startTime = cObj.optString("startTime", "09:00"),
                                    endTime = cObj.optString("endTime", "10:00"),
                                    classType = cType
                                )
                            )
                        }
                    }
                    daysList.add(DaySchedule(dayOfWeek, classesList))
                }
            }
            WeeklyTimetable(
                studentBranch = root.optString("studentBranch", "Engineering"),
                section = root.optString("section", "Section A"),
                semester = root.optString("semester", "Semester"),
                days = daysList,
                lastSyncedTimestamp = root.optLong("lastSyncedTimestamp", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }

    fun isGoogleCalendarSyncEnabled(): Boolean = prefs.getBoolean(KEY_GOOGLE_CALENDAR_SYNC_ENABLED, true)

    fun setGoogleCalendarSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GOOGLE_CALENDAR_SYNC_ENABLED, enabled).apply()
    }

    fun getLastCalendarSyncTimestamp(): Long = prefs.getLong(KEY_LAST_CALENDAR_SYNC_TIMESTAMP, 0L)

    fun getLastCalendarSyncCount(): Int = prefs.getInt(KEY_LAST_CALENDAR_SYNC_COUNT, 0)

    fun setLastCalendarSync(timestamp: Long, count: Int) {
        prefs.edit()
            .putLong(KEY_LAST_CALENDAR_SYNC_TIMESTAMP, timestamp)
            .putInt(KEY_LAST_CALENDAR_SYNC_COUNT, count)
            .apply()
    }

    fun getAvatarSeed(): String {
        val saved = prefs.getString(KEY_AVATAR_SEED, null)
        if (!saved.isNullOrBlank()) return saved
        val fallback = getRollNumber().ifBlank { getStudentName().ifBlank { "Student" } }
        return fallback.filter { it.isLetterOrDigit() }.ifBlank { "Student" }
    }

    fun setAvatarSeed(seed: String) {
        prefs.edit().putString(KEY_AVATAR_SEED, seed).apply()
    }

    fun isAcademicCalendarInScheduleEnabled(): Boolean =
        prefs.getBoolean(KEY_INCLUDE_ACADEMIC_CALENDAR_IN_SCHEDULE, true)

    fun setAcademicCalendarInScheduleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_ACADEMIC_CALENDAR_IN_SCHEDULE, enabled).apply()
    }

    fun getCalendarCategoryFilters(): Set<CalendarCategory> {
        val defaultSet = CalendarCategory.values().map { it.name }.toSet()
        val saved = prefs.getStringSet(KEY_CALENDAR_TAG_FILTERS, defaultSet) ?: defaultSet
        val parsed = saved.mapNotNull { name ->
            try { CalendarCategory.valueOf(name) } catch (e: Exception) { null }
        }.toSet()
        return if (parsed.isEmpty()) CalendarCategory.values().toSet() else parsed
    }

    fun setCalendarCategoryFilters(categories: Set<CalendarCategory>) {
        val names = categories.map { it.name }.toSet()
        prefs.edit().putStringSet(KEY_CALENDAR_TAG_FILTERS, names).apply()
    }

    fun isCalendarCategoryEnabled(category: CalendarCategory): Boolean {
        return getCalendarCategoryFilters().contains(category)
    }

    fun toggleCalendarCategoryFilter(category: CalendarCategory): Set<CalendarCategory> {
        val current = getCalendarCategoryFilters().toMutableSet()
        if (current.contains(category)) {
            current.remove(category)
        } else {
            current.add(category)
        }
        setCalendarCategoryFilters(current)
        return current
    }

    fun saveDatesheet(cycle: String, items: List<ExamDatesheetItem>) {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("courseCode", item.courseCode)
                put("courseName", item.courseName)
                put("date", item.date)
                put("day", item.day)
                put("timeSlot", item.timeSlot)
                put("shift", item.shift)
                put("isUpcoming", item.isUpcoming)
                put("credits", item.credits)
                put("examType", item.examType)
                put("examDateRaw", item.examDateRaw?.toString() ?: "")
                put("roomAllotment", item.roomAllotment)
                put("reportingTime", item.reportingTime)
                put("examBlock", item.examBlock)
            }
            arr.put(obj)
        }
        prefs.edit().putString("key_datesheet_$cycle", arr.toString()).apply()
    }

    fun getCachedDatesheet(cycle: String): List<ExamDatesheetItem>? {
        val json = prefs.getString("key_datesheet_$cycle", null) ?: return null
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<ExamDatesheetItem>()
            val today = LocalDate.now()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawDateStr = obj.optString("examDateRaw", "")
                val rawDate = if (rawDateStr.isNotBlank()) LocalDate.parse(rawDateStr) else null
                list.add(
                    ExamDatesheetItem(
                        courseCode = obj.getString("courseCode"),
                        courseName = obj.getString("courseName"),
                        date = obj.getString("date"),
                        day = obj.getString("day"),
                        timeSlot = obj.optString("timeSlot", "10:00 AM - 11:30 AM"),
                        shift = obj.optString("shift", "Shift 1 (Morning)"),
                        isUpcoming = rawDate?.let { !it.isBefore(today) } ?: obj.optBoolean("isUpcoming", false),
                        credits = obj.optInt("credits", 4),
                        examType = obj.optString("examType", cycle),
                        examDateRaw = rawDate,
                        roomAllotment = obj.optString("roomAllotment", "Released 1 day prior"),
                        reportingTime = obj.optString("reportingTime", "09:30 AM (30 min prior)"),
                        examBlock = obj.optString("examBlock", "Main Campus")
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    fun getWidgetTheme(): String = prefs.getString(KEY_WIDGET_THEME, "oled") ?: "oled"
    fun setWidgetTheme(theme: String) = prefs.edit().putString(KEY_WIDGET_THEME, theme).apply()

    fun getWidgetAccent(): String = prefs.getString(KEY_WIDGET_ACCENT, "indigo") ?: "indigo"
    fun setWidgetAccent(accent: String) = prefs.edit().putString(KEY_WIDGET_ACCENT, accent).apply()

    fun isWidgetBunksEnabled(): Boolean = prefs.getBoolean(KEY_WIDGET_BUNKS_ENABLED, true)
    fun setWidgetBunksEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_WIDGET_BUNKS_ENABLED, enabled).apply()

    fun isWidgetBannerEnabled(): Boolean = prefs.getBoolean(KEY_WIDGET_BANNER_ENABLED, true)
    fun setWidgetBannerEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_WIDGET_BANNER_ENABLED, enabled).apply()
}
