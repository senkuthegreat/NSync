package com.example.nsutconnect.data.sync

import android.content.Context
import android.util.Log
import com.example.nsutconnect.data.calendar.CalendarSyncResult
import com.example.nsutconnect.data.calendar.GoogleCalendarSyncManager
import com.example.nsutconnect.data.local.UserPreferences
import com.example.nsutconnect.data.model.AttendanceSummary
import com.example.nsutconnect.data.model.WeeklyTimetable
import com.example.nsutconnect.data.network.CaptchaSession
import com.example.nsutconnect.data.network.ImsLoginResponse
import com.example.nsutconnect.data.network.ImsNetworkClient
import com.example.nsutconnect.data.network.LiveFetchResult
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

    private val _isSessionExpired = MutableStateFlow(false)
    val isSessionExpired: StateFlow<Boolean> = _isSessionExpired.asStateFlow()

    init {
        // If authentically logged in, initialize with cached data or local provider
        if (prefs.isLoggedIn()) {
            val roll = prefs.getRollNumber()
            val cachedAtt = prefs.getCachedAttendance() ?: com.example.nsutconnect.data.local.ImsLocalDataProvider.getAttendanceForRoll(context, roll)
            val cachedTt = prefs.getCachedTimetable() ?: com.example.nsutconnect.data.local.ImsLocalDataProvider.getTimetableForRoll(context, roll)
            if (prefs.getCachedAttendance() == null && cachedAtt != null) {
                prefs.saveAttendanceSummary(cachedAtt)
            }
            if (prefs.getCachedTimetable() == null && cachedTt != null) {
                prefs.saveTimetable(cachedTt)
            }
            if (cachedAtt != null) _attendance.value = cachedAtt
            if (cachedTt != null) _timetable.value = cachedTt
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
                val attUrl = authResult.attendanceUrl
                val ttUrl = authResult.timetableUrl
                prefs.saveAttendanceUrl(attUrl)
                prefs.saveTimetableUrl(ttUrl)
                prefs.saveActivitiesUrl(authResult.activitiesUrl)
                prefs.saveCoursesUrl(authResult.coursesUrl)

                var resolvedSemester = authResult.semester
                if (!authResult.coursesUrl.isNullOrBlank()) {
                    try {
                        val (semFromCourses, _) = networkClient.fetchEnrolledCourses(
                            authResult.cookies,
                            authResult.coursesUrl
                        )
                        if (!semFromCourses.isNullOrBlank()) {
                            resolvedSemester = semFromCourses
                        }
                    } catch (_: Exception) {}
                }
                if (resolvedSemester.isNullOrBlank()) {
                    resolvedSemester = networkClient.calculateSemesterFromRoll(cleanRoll)
                }
                prefs.saveSemester(resolvedSemester)
                prefs.setLoggedIn(true)

                // Fetch real student attendance and timetable from the portal
                val liveAttendance = networkClient.fetchLiveAttendance(
                    cookies = authResult.cookies,
                    rollNumber = cleanRoll,
                    attendanceUrl = attUrl,
                    branch = authResult.branch,
                    semester = resolvedSemester
                )
                val liveTimetable = networkClient.fetchLiveTimetable(
                    cookies = authResult.cookies,
                    rollNumber = cleanRoll,
                    timetableUrl = ttUrl
                )

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
        var attendanceUrl = prefs.getAttendanceUrl()
        var timetableUrl = prefs.getTimetableUrl()
        val activitiesUrl = prefs.getActivitiesUrl()
        var coursesUrl = prefs.getCoursesUrl()
        var semester = prefs.getSemester()
        val branch = prefs.getBranch()
        var liveAttendance: AttendanceSummary? = null
        var liveTimetable: WeeklyTimetable? = null
        var sessionExpired = false

        // If attendanceUrl, timetableUrl, or coursesUrl is missing, re-resolve dynamically from activitiesUrl
        val needsAttResolution = attendanceUrl.isNullOrBlank()
        val needsTtResolution = timetableUrl.isNullOrBlank()
        if ((needsAttResolution || needsTtResolution || coursesUrl.isNullOrBlank()) && !activitiesUrl.isNullOrBlank() && cookies.isNotBlank()) {
            try {
                val cookiesMap = networkClient.parseCookiesToMap(cookies)
                val actDoc = org.jsoup.Jsoup.connect(activitiesUrl)
                    .userAgent(ImsNetworkClient.USER_AGENT)
                    .header("Referer", ImsNetworkClient.STUDENT_LOGIN_URL)
                    .cookies(cookiesMap)
                    .timeout(10000)
                    .followRedirects(true)
                    .get()
                val discovered = networkClient.resolvePortalLinks(actDoc, cookiesMap)
                if (!discovered.attendanceUrl.isNullOrBlank()) {
                    attendanceUrl = discovered.attendanceUrl
                    prefs.saveAttendanceUrl(discovered.attendanceUrl)
                }
                if (!discovered.timetableUrl.isNullOrBlank()) {
                    timetableUrl = discovered.timetableUrl
                    prefs.saveTimetableUrl(discovered.timetableUrl)
                }
                if (!discovered.coursesUrl.isNullOrBlank()) {
                    coursesUrl = discovered.coursesUrl
                    prefs.saveCoursesUrl(discovered.coursesUrl)
                }
            } catch (e: Exception) {
                Log.w("NSyncSync", "Failed to refresh dynamic links from activitiesUrl: ${e.message}")
            }
        }

        // Ensure semester and enrolled courses are fetched
        if (semester.isNullOrBlank() && !coursesUrl.isNullOrBlank() && cookies.isNotBlank()) {
            try {
                val (semFromCourses, _) = networkClient.fetchEnrolledCourses(cookies, coursesUrl)
                if (!semFromCourses.isNullOrBlank()) {
                    semester = semFromCourses
                    prefs.saveSemester(semFromCourses)
                }
            } catch (_: Exception) {}
        }
        if (semester.isNullOrBlank()) {
            semester = networkClient.calculateSemesterFromRoll(rollNumber)
            prefs.saveSemester(semester)
        }

        try {
            val attResult = networkClient.fetchLiveAttendanceResult(
                cookies = cookies,
                rollNumber = rollNumber,
                attendanceUrl = attendanceUrl,
                branch = branch,
                semester = semester
            )
            when (attResult) {
                is LiveFetchResult.Success -> liveAttendance = attResult.data
                is LiveFetchResult.SessionExpired -> sessionExpired = true
                is LiveFetchResult.Error -> {}
            }

            val ttResult = networkClient.fetchLiveTimetableResult(
                cookies = cookies,
                rollNumber = rollNumber,
                timetableUrl = timetableUrl
            )
            when (ttResult) {
                is LiveFetchResult.Success -> liveTimetable = ttResult.data
                is LiveFetchResult.SessionExpired -> sessionExpired = true
                is LiveFetchResult.Error -> {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (sessionExpired) {
            val cached = prefs.getCachedAttendance() ?: com.example.nsutconnect.data.local.ImsLocalDataProvider.getAttendanceForRoll(context, rollNumber)
            if (prefs.getCachedAttendance() == null && cached != null) {
                prefs.saveAttendanceSummary(cached)
            }
            if (cached != null) {
                _attendance.value = cached
            }
            val tt = prefs.getCachedTimetable() ?: com.example.nsutconnect.data.local.ImsLocalDataProvider.getTimetableForRoll(context, rollNumber)
            if (prefs.getCachedTimetable() == null && tt != null) {
                prefs.saveTimetable(tt)
            }
            if (tt != null) {
                _timetable.value = tt
            }
            updateWidgets()

            if (!isBackground) {
                _isSessionExpired.value = true
                val res = SyncResult.Error("IMS session timed out. Please enter captcha to refresh.")
                _syncState.value = res
                return@withContext res
            } else {
                _isSessionExpired.value = false
                val res = SyncResult.Success("Using offline mode (IMS session timed out)")
                return@withContext res
            }
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

        // If live sync couldn't connect, fallback to previously cached data or local provider
        val cached = prefs.getCachedAttendance() ?: com.example.nsutconnect.data.local.ImsLocalDataProvider.getAttendanceForRoll(context, rollNumber)
        if (prefs.getCachedAttendance() == null && cached != null) {
            prefs.saveAttendanceSummary(cached)
        }
        if (cached != null) {
            _attendance.value = cached
        }
        val tt = prefs.getCachedTimetable() ?: com.example.nsutconnect.data.local.ImsLocalDataProvider.getTimetableForRoll(context, rollNumber)
        if (prefs.getCachedTimetable() == null && tt != null) {
            prefs.saveTimetable(tt)
        }
        if (tt != null) {
            _timetable.value = tt
        }
        updateWidgets()
        val res = SyncResult.Success("Using offline mode (IMS unreachable)")
        if (!isBackground) _syncState.value = res
        return@withContext res

        val err = SyncResult.Error("Unable to reach NSUT IMS server")
        if (!isBackground) _syncState.value = err
        err
    }

    suspend fun reAuthenticateWithCaptcha(
        captcha: String,
        captchaSession: CaptchaSession?
    ): SyncResult = withContext(Dispatchers.IO) {
        val roll = prefs.getRollNumber()
        val pass = prefs.getSavedPassword()
        if (roll.isBlank() || pass.isNullOrBlank()) {
            _isSessionExpired.value = false
            return@withContext SyncResult.Error("Saved credentials not found. Please log in again.")
        }

        val result = login(roll, pass, captcha, captchaSession, autoLogin = true)
        if (result is SyncResult.Success) {
            _isSessionExpired.value = false
        }
        result
    }

    fun dismissSessionExpiredPrompt() {
        _isSessionExpired.value = false
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

    fun logout(forgetCredentials: Boolean = false) {
        if (forgetCredentials) {
            prefs.clearAuth()
        } else {
            prefs.clearSession(keepCredentials = true)
        }
        _isSessionExpired.value = false
        _attendance.value = null
        _timetable.value = null
        _syncState.value = SyncResult.Success("Logged out")
        updateWidgets()
    }
}
