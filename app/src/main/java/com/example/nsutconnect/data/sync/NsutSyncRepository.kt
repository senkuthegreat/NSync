package com.example.nsutconnect.data.sync

import android.content.Context
import com.example.nsutconnect.data.calendar.CalendarSyncResult
import com.example.nsutconnect.data.calendar.GoogleCalendarSyncManager
import com.example.nsutconnect.data.local.UserPreferences
import com.example.nsutconnect.data.model.AttendanceSummary
import com.example.nsutconnect.data.model.WeeklyTimetable
import com.example.nsutconnect.data.network.CaptchaSession
import com.example.nsutconnect.data.network.ImsLoginResponse
import com.example.nsutconnect.data.network.ImsNetworkClient
import com.example.nsutconnect.widget.NsutAttendanceWidgetReceiver
import com.example.nsutconnect.widget.NsutScheduleWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Error(val error: String) : SyncResult()
    object Loading : SyncResult()
}

class NsutSyncRepository(private val context: Context) {

    private val prefs = UserPreferences(context)
    private val networkClient = ImsNetworkClient()

    private val _attendance = MutableStateFlow<AttendanceSummary?>(prefs.getCachedAttendance())
    val attendance: StateFlow<AttendanceSummary?> = _attendance.asStateFlow()

    private val _timetable = MutableStateFlow<WeeklyTimetable?>(prefs.getCachedTimetable())
    val timetable: StateFlow<WeeklyTimetable?> = _timetable.asStateFlow()

    private val _syncState = MutableStateFlow<SyncResult>(SyncResult.Success("Ready"))
    val syncState: StateFlow<SyncResult> = _syncState.asStateFlow()

    init {
        // If authentically logged in, initialize with cached data only
        if (prefs.isLoggedIn()) {
            _attendance.value = prefs.getCachedAttendance()
            _timetable.value = prefs.getCachedTimetable()
        }
    }

    suspend fun getCaptchaSession(): CaptchaSession? = withContext(Dispatchers.IO) {
        networkClient.fetchCaptchaSession()
    }

    suspend fun login(
        rollNumber: String,
        password: String,
        captcha: String,
        captchaSession: CaptchaSession?,
        autoLogin: Boolean
    ): SyncResult = withContext(Dispatchers.IO) {
        _syncState.value = SyncResult.Loading

        val cleanRoll = rollNumber.trim().uppercase()
        val cleanPass = password.trim()
        val cleanCap = captcha.trim()

        if (cleanRoll.isBlank() || cleanPass.isBlank()) {
            val err = SyncResult.Error("Roll Number and Password cannot be empty")
            _syncState.value = err
            return@withContext err
        }

        if (cleanCap.isBlank()) {
            val err = SyncResult.Error("Please enter the security captcha code")
            _syncState.value = err
            return@withContext err
        }

        // Authentic verification against NSUT IMS portal
        val authResult = networkClient.authenticate(cleanRoll, cleanPass, cleanCap, captchaSession)
        when (authResult) {
            is ImsLoginResponse.Failure -> {
                val err = SyncResult.Error(authResult.message)
                _syncState.value = err
                return@withContext err
            }
            is ImsLoginResponse.Success -> {
                // Authenticated successfully! Save credentials and mark logged in
                prefs.saveCredentials(cleanRoll, cleanPass, autoLogin)
                prefs.savePortalCookies(authResult.cookies)
                authResult.studentName?.let { prefs.setStudentName(it) }
                authResult.branch?.let { prefs.setBranch(it) }
                prefs.setLoggedIn(true)

                // Fetch real student attendance and timetable from the portal
                val liveAttendance = networkClient.fetchLiveAttendance(authResult.cookies, cleanRoll)
                val liveTimetable = networkClient.fetchLiveTimetable(authResult.cookies, cleanRoll)

                if (liveAttendance != null) {
                    prefs.saveAttendanceSummary(liveAttendance)
                    _attendance.value = liveAttendance
                }
                if (liveTimetable != null) {
                    prefs.saveTimetable(liveTimetable)
                    _timetable.value = liveTimetable
                }

                updateWidgets()
                autoSyncCalendarIfPermitted()

                val success = SyncResult.Success("Signed in successfully as $cleanRoll")
                _syncState.value = success
                return@withContext success
            }
        }
    }

    suspend fun syncData(isBackground: Boolean = false): SyncResult = withContext(Dispatchers.IO) {
        if (!isBackground) {
            _syncState.value = SyncResult.Loading
        }

        val rollNumber = prefs.getRollNumber()
        val password = prefs.getSavedPassword()

        if (rollNumber.isBlank() || password.isNullOrBlank()) {
            val cached = prefs.getCachedAttendance()
            if (cached != null) {
                _attendance.value = cached
                _timetable.value = prefs.getCachedTimetable()
                updateWidgets()
                val res = SyncResult.Success("Loaded cached data")
                if (!isBackground) _syncState.value = res
                return@withContext res
            }
            val err = SyncResult.Error("Please log in to sync NSUT data")
            if (!isBackground) _syncState.value = err
            return@withContext err
        }

        val cookies = prefs.getPortalCookies()
        var liveAttendance: AttendanceSummary? = null
        var liveTimetable: WeeklyTimetable? = null

        try {
            liveAttendance = networkClient.fetchLiveAttendance(cookies, rollNumber)
            liveTimetable = networkClient.fetchLiveTimetable(cookies, rollNumber)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (liveAttendance != null) {
            prefs.saveAttendanceSummary(liveAttendance)
            _attendance.value = liveAttendance
        }
        if (liveTimetable != null) {
            prefs.saveTimetable(liveTimetable)
            _timetable.value = liveTimetable
        }

        if (liveAttendance != null || liveTimetable != null) {
            updateWidgets()
            autoSyncCalendarIfPermitted()
            val res = SyncResult.Success("Live synced with NSUT IMS")
            if (!isBackground) _syncState.value = res
            return@withContext res
        }

        // If live sync couldn't connect, fallback to previously cached authentic data
        val cached = prefs.getCachedAttendance()
        if (cached != null) {
            _attendance.value = cached
            _timetable.value = prefs.getCachedTimetable()
            updateWidgets()
            val res = SyncResult.Success("Using offline cache (IMS unreachable)")
            if (!isBackground) _syncState.value = res
            return@withContext res
        }

        val err = SyncResult.Error("Unable to reach NSUT IMS server")
        if (!isBackground) _syncState.value = err
        err
    }

    fun updateWidgets() {
        try {
            NsutAttendanceWidgetReceiver.updateAll(context)
            NsutScheduleWidgetReceiver.updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun autoSyncCalendarIfPermitted() {
        try {
            if (prefs.isGoogleCalendarSyncEnabled()) {
                val tt = _timetable.value
                if (tt != null) {
                    GoogleCalendarSyncManager.syncTimetableToCalendar(context, tt)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logout() {
        prefs.clearAuth()
        _attendance.value = null
        _timetable.value = null
        _syncState.value = SyncResult.Success("Logged out")
        updateWidgets()
    }
}
