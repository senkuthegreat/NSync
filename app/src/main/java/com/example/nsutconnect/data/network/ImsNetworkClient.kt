package com.example.nsutconnect.data.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.nsutconnect.data.model.AttendanceSummary
import com.example.nsutconnect.data.model.WeeklyTimetable
import com.example.nsutconnect.data.parser.ImsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.jsoup.nodes.Document

data class CaptchaSession(
    val sessionId: String,
    val captchaBitmap: Bitmap?,
    val cookies: Map<String, String>,
    val hrandNum: String? = null,
    val encFy: String? = null,
    val fy: String = "2026-27",
    val comp: String = "NETAJI SUBHAS UNIVERSITY OF TECHNOLOGY",
    val hiddenFields: Map<String, String> = emptyMap()
)

sealed class ImsLoginResponse {
    data class Success(
        val cookies: String,
        val studentName: String? = null,
        val branch: String? = null,
        val profileUrl: String? = null,
        val activitiesUrl: String? = null,
        val attendanceUrl: String? = null,
        val timetableUrl: String? = null
    ) : ImsLoginResponse()
    data class Failure(val message: String) : ImsLoginResponse()
}

class ImsNetworkClient {

    companion object {
        const val BASE_URL = "https://www.imsnsit.org/imsnsit/"
        const val STUDENT_LOGIN110_URL = "https://www.imsnsit.org/imsnsit/student_login110.php"
        const val STUDENT_LOGIN_URL = "https://www.imsnsit.org/imsnsit/student_login.php"
        const val PLUM_SCREEN_INIT_URL = "https://www.imsnsit.org/imsnsit/plum5_fw_login.php?t=sw&w=1"
        const val TIMETABLE_URL = "https://www.imsnsit.org/imsnsit/plum_url.php?41uZnOKcTatoQuZ1nS5qVGIuiLOuCZGGw43gNaDJuC6JU+zlhWK6ya/tcFLywtouIo7aWVZdCbDde4Onb0dyebiOoF5CwOCF9cWNylj6wkXd0RX8bVnVGaPKlMtStKmyuAZXCzEdKq+zBvagqtna+Vjw3K+2YkEhpEDLFH9RwuVsL162Qyo3TRed5amA2UTcldG13/T8xEKGB82zR7JxVA=="
        const val ATTENDANCE_URL = "https://www.imsnsit.org/imsnsit/plum_url.php?P59Otr7+3xJIlCBBABoVb0GIgAXiKbA1LIAMUeXV5ZvLND/AfclikCFEYYIO4oq3BkrlNPPPoLsLVLt2ihg6VdTT8AXD3bdKB07WmGbjTQ4PM9NWTedZH8r2ohs9zsBsbFmAs01PXjfJn07m4ARSsaaeuZxTy4F4EJp+SoAk4dHNGvsD3yrO6W3oQUld+8626tyicw9vPYtiH/KgrqEbUsWbLNgbppb9cgWn/IrjvV15vuFRdLZIHnwX5Qndspns"
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.119 Safari/537.36"
    }

    data class DiscoveredPortalLinks(
        val profileUrl: String? = null,
        val activitiesUrl: String? = null,
        val attendanceUrl: String? = null,
        val timetableUrl: String? = null
    )

    private fun resolvePortalLinks(doc: Document, cookies: Map<String, String>): DiscoveredPortalLinks {
        var profileUrl: String? = null
        var activitiesUrl: String? = null
        var attendanceUrl: String? = null
        var timetableUrl: String? = null

        fun scanDoc(d: Document) {
            for (link in d.select("a")) {
                val href = link.attr("href").trim()
                val onclick = link.attr("onclick").trim()
                val text = link.text().trim()
                val combined = "$text $href $onclick".lowercase()

                val resolvedUrl: String? = when {
                    href.startsWith("http") -> href
                    href.isNotBlank() && !href.startsWith("javascript") -> {
                        if (href.startsWith("/")) "https://www.imsnsit.org$href" else BASE_URL + href
                    }
                    onclick.contains("plum_url.php") -> {
                        val match = Regex("plum_url\\.php[^'\" )]+").find(onclick)?.value
                        if (match != null) BASE_URL + match else null
                    }
                    else -> null
                }

                if (resolvedUrl != null) {
                    if (attendanceUrl == null && (combined.contains("attendance") || combined.contains("student_attendance"))) {
                        attendanceUrl = resolvedUrl
                    }
                    if (timetableUrl == null && (combined.contains("time table") || combined.contains("timetable") || combined.contains("student_timetable"))) {
                        timetableUrl = resolvedUrl
                    }
                    if (profileUrl == null && (combined.contains("my profile") || combined.contains("profile") || combined.contains("student_profile"))) {
                        profileUrl = resolvedUrl
                    }
                    if (activitiesUrl == null && (combined.contains("my activities") || combined.contains("activities") || combined.contains("student_activities"))) {
                        activitiesUrl = resolvedUrl
                    }
                }
            }
        }

        scanDoc(doc)

        // If attendance or timetable were not found directly, inspect any child frames (Plumware navigation layout)
        if (attendanceUrl == null || timetableUrl == null || profileUrl == null) {
            val frames = doc.select("frame, iframe")
            for (frame in frames) {
                val src = frame.attr("src").trim()
                if (src.isBlank() || src.startsWith("javascript") || src.equals("about:blank", ignoreCase = true)) continue
                val frameUrl = if (src.startsWith("http")) src else BASE_URL + src
                try {
                    val frameDoc = Jsoup.connect(frameUrl)
                        .userAgent(USER_AGENT)
                        .header("Referer", STUDENT_LOGIN_URL)
                        .cookies(cookies)
                        .timeout(6000)
                        .get()
                    scanDoc(frameDoc)
                } catch (e: Exception) {
                    Log.w("NSyncAuth", "Could not inspect portal frame $frameUrl: ${e.message}")
                }
            }
        }

        Log.i("NSyncAuth", "Resolved portal links: attendance=$attendanceUrl, timetable=$timetableUrl, profile=$profileUrl")
        return DiscoveredPortalLinks(
            profileUrl = profileUrl,
            activitiesUrl = activitiesUrl,
            attendanceUrl = attendanceUrl,
            timetableUrl = timetableUrl
        )
    }

    suspend fun fetchCaptchaSession(): CaptchaSession? = withContext(Dispatchers.IO) {
        try {
            // Step 1: Base session genesis
            val baseRes = Jsoup.connect(BASE_URL)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .method(Connection.Method.GET)
                .execute()

            val cookies = baseRes.cookies().toMutableMap()

            // Step 2: Plumware screen resolution check (w=1) - required for Plumware to render student login form
            try {
                val swRes = Jsoup.connect(PLUM_SCREEN_INIT_URL)
                    .userAgent(USER_AGENT)
                    .header("Referer", BASE_URL)
                    .cookies(cookies)
                    .timeout(8000)
                    .method(Connection.Method.GET)
                    .execute()
                cookies.putAll(swRes.cookies())
            } catch (_: Exception) {
            }

            // Step 3: Student login redirect sequence (student_login110.php -> student_login.php)
            val r110Res = Jsoup.connect(STUDENT_LOGIN110_URL)
                .userAgent(USER_AGENT)
                .header("Referer", "https://www.imsnsit.org/imsnsit/student.htm")
                .cookies(cookies)
                .timeout(10000)
                .followRedirects(true)
                .method(Connection.Method.GET)
                .execute()

            cookies.putAll(r110Res.cookies())

            // Step 4: Ensure student_login.php is fetched and parsed
            val loginDoc = if (r110Res.url().toString().contains("student_login.php") && r110Res.body().length > 1000) {
                r110Res.parse()
            } else {
                val loginRes = Jsoup.connect(STUDENT_LOGIN_URL)
                    .userAgent(USER_AGENT)
                    .header("Referer", "https://www.imsnsit.org/imsnsit/student.htm")
                    .cookies(cookies)
                    .timeout(10000)
                    .method(Connection.Method.GET)
                    .execute()
                cookies.putAll(loginRes.cookies())
                loginRes.parse()
            }

            val captchaImg = loginDoc.selectFirst("img#captchaimg") ?: loginDoc.selectFirst("img[src*='captcha']")
            val captchaSrc = captchaImg?.attr("src") ?: return@withContext null
            val fullCaptchaUrl = if (captchaSrc.startsWith("http")) captchaSrc else "https://www.imsnsit.org/imsnsit/$captchaSrc"

            val hiddenFields = mutableMapOf<String, String>()
            loginDoc.select("form[name=f1] input[type=hidden], form input[type=hidden]").forEach { input ->
                val name = input.attr("name")
                val value = input.attr("value")
                if (name.isNotBlank()) {
                    hiddenFields[name] = value
                }
            }

            val hrandNum = hiddenFields["HRAND_NUM"] ?: loginDoc.selectFirst("input[name=HRAND_NUM]")?.attr("value")
            val fy = hiddenFields["fy"] ?: loginDoc.selectFirst("input[name=fy]")?.attr("value") ?: "2026-27"
            val comp = hiddenFields["comp"] ?: loginDoc.selectFirst("input[name=comp]")?.attr("value") ?: "NETAJI SUBHAS UNIVERSITY OF TECHNOLOGY"
            val encFy = hiddenFields["enc_fy"]

            val imgRes = Jsoup.connect(fullCaptchaUrl)
                .userAgent(USER_AGENT)
                .header("Referer", STUDENT_LOGIN_URL)
                .cookies(cookies)
                .timeout(10000)
                .ignoreContentType(true)
                .execute()

            cookies.putAll(imgRes.cookies())

            val bytes = imgRes.bodyAsBytes()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            CaptchaSession(
                sessionId = cookies["PHPSESSID"] ?: "",
                captchaBitmap = bitmap,
                cookies = cookies,
                hrandNum = hrandNum,
                encFy = encFy,
                fy = fy,
                comp = comp,
                hiddenFields = hiddenFields
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun authenticate(
        rollNumber: String,
        password: String,
        captcha: String,
        session: CaptchaSession?
    ): ImsLoginResponse = withContext(Dispatchers.IO) {
        val cleanRoll = rollNumber.trim().uppercase()
        val cleanPass = password.trim()
        val cleanCap = captcha.trim()

        if (cleanRoll.isBlank() || cleanPass.isBlank()) {
            return@withContext ImsLoginResponse.Failure("Roll Number and Password cannot be blank.")
        }

        val sessionToUse = session ?: fetchCaptchaSession()
        if (sessionToUse == null) {
            return@withContext ImsLoginResponse.Failure("Unable to connect to NSUT IMS. Please check your internet connection.")
        }

        try {
            val conn = Jsoup.connect(STUDENT_LOGIN_URL)
                .userAgent(USER_AGENT)
                .header("Referer", STUDENT_LOGIN_URL)
                .header("Origin", "https://www.imsnsit.org")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "frame")
                .cookies(sessionToUse.cookies)

            val postParams = mutableMapOf<String, String>()
            // Forward all parsed hidden fields from the session
            postParams.putAll(sessionToUse.hiddenFields)

            // Explicitly set credentials, captcha, and required system fields for student login
            postParams["f"] = ""
            postParams["uid"] = cleanRoll
            postParams["pwd"] = cleanPass
            postParams["cap"] = cleanCap
            sessionToUse.hrandNum?.let { postParams["HRAND_NUM"] = it }
            postParams["fy"] = sessionToUse.fy
            postParams["comp"] = sessionToUse.comp
            postParams["login"] = "Login"
            postParams["logintype"] = "student"

            postParams.forEach { (name, value) ->
                conn.data(name, value)
            }

            Log.i("NSyncAuth", "Submitting POST to $STUDENT_LOGIN_URL with params: ${postParams.map { if (it.key == "pwd") "pwd=***" else "${it.key}=${it.value}" }}")

            // Do NOT follow redirects automatically so we can inspect the exact 302 Location header
            val response = conn
                .timeout(15000)
                .method(Connection.Method.POST)
                .followRedirects(false)
                .execute()

            val cookies = sessionToUse.cookies.toMutableMap()
            cookies.putAll(response.cookies())
            val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

            val statusCode = response.statusCode()
            val location = response.header("Location") ?: ""
            Log.i("NSyncAuth", "POST response status: $statusCode, location: '$location'")

            // Plumware returns 302 Found to communicate login outcome:
            if (statusCode in 300..399) {
                // If Location contains "?uid=", "rty=captcha", or redirects to "blank.htm", Captcha was rejected by the portal
                if (location.contains("?uid=") || location.contains("rty=captcha") || location.equals("blank.htm", ignoreCase = true) || location.endsWith("/blank.htm")) {
                    Log.w("NSyncAuth", "Location '$location' indicates Captcha failure")
                    return@withContext ImsLoginResponse.Failure("Invalid Captcha number. Please enter the fresh 5-digit code.")
                }

                // Follow the redirect to the target page with session cookies
                val targetUrl = if (location.startsWith("http")) location else BASE_URL + location
                Log.i("NSyncAuth", "Following 302 redirect to $targetUrl")
                try {
                    val followRes = Jsoup.connect(targetUrl)
                        .userAgent(USER_AGENT)
                        .header("Referer", STUDENT_LOGIN_URL)
                        .cookies(cookies)
                        .timeout(15000)
                        .followRedirects(true)
                        .execute()

                    cookies.putAll(followRes.cookies())
                    val updatedCookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                    val targetDoc = followRes.parse()
                    val targetHtml = targetDoc.html()
                    Log.i("NSyncAuth", "Target page URL: ${followRes.url()}, status: ${followRes.statusCode()}, HTML len: ${targetHtml.length}")

                    val c1 = targetHtml.contains("Your password does not match", ignoreCase = true)
                    val c2 = targetHtml.contains("password does not match", ignoreCase = true)
                    val c3 = targetHtml.contains("Login Failed", ignoreCase = true)
                    val c4 = targetHtml.contains("Invalid password", ignoreCase = true)
                    Log.i("NSyncAuth", "Credential checks: c1=$c1, c2=$c2, c3=$c3, c4=$c4")

                    // Check for invalid password / username error on redirected page
                    if (c1 || c2 || c3 || c4) {
                        Log.w("NSyncAuth", "Target HTML contains invalid credentials error")
                        return@withContext ImsLoginResponse.Failure("Incorrect Password or Roll Number. Please verify your credentials.")
                    }

                    // Check for server-side attempt error (do NOT check static form strings like 'Invalid Security' or 'Please Enter Captcha')
                    if (targetHtml.contains("Invalid Attempt", ignoreCase = true)) {
                        Log.w("NSyncAuth", "Target HTML contains Invalid Attempt error")
                        return@withContext ImsLoginResponse.Failure("Invalid Captcha number. Please enter the fresh 5-digit code.")
                    }

                    // Resolve dynamic student URLs (Attendance, Timetable, Profile, Activities)
                    val discovered = resolvePortalLinks(targetDoc, cookies)
                    var detectedBranch: String? = null
                    discovered.profileUrl?.let { pUrl ->
                        try {
                            val pDoc = Jsoup.connect(pUrl).userAgent(USER_AGENT).cookies(cookies).timeout(6000).get()
                            val bText = pDoc.select("td:contains(Department), td:contains(Branch)").text()
                            if (bText.isNotBlank()) {
                                detectedBranch = bText.substringAfter(":", "").trim().ifBlank { null }
                            }
                        } catch (_: Exception) {}
                    }

                    val studentName = targetDoc.select("td:contains(Welcome), font:contains(Welcome), b:contains(Welcome)").text()
                        .substringAfter("Welcome", "")
                        .trim()
                        .ifBlank { null }

                    val isSuccess = discovered.profileUrl != null || discovered.activitiesUrl != null || studentName != null ||
                        targetHtml.contains("Logout", ignoreCase = true) ||
                        targetHtml.contains("My Profile", ignoreCase = true) ||
                        targetHtml.contains("My Activities", ignoreCase = true) ||
                        targetHtml.contains("student_profile", ignoreCase = true) ||
                        targetHtml.contains("student_attendance", ignoreCase = true)

                    if (isSuccess) {
                        Log.i("NSyncAuth", "Login SUCCESS: name=$studentName, branch=$detectedBranch, attUrl=${discovered.attendanceUrl}, ttUrl=${discovered.timetableUrl}")
                        return@withContext ImsLoginResponse.Success(
                            cookies = updatedCookieHeader,
                            studentName = studentName,
                            branch = detectedBranch,
                            profileUrl = discovered.profileUrl,
                            activitiesUrl = discovered.activitiesUrl,
                            attendanceUrl = discovered.attendanceUrl,
                            timetableUrl = discovered.timetableUrl
                        )
                    }

                    // If redirected to login form with both user and password fields and no success markers
                    val hasLoginFormInputs = targetDoc.selectFirst("input[name=uid]") != null && targetDoc.selectFirst("input[name=pwd]") != null
                    if (hasLoginFormInputs && followRes.url().toString().contains("plum5_fw_login")) {
                        Log.w("NSyncAuth", "Landed back on login page without success markers")
                        return@withContext ImsLoginResponse.Failure("Authentication failed. Please verify your Roll Number, Password, and enter the fresh 5-digit Captcha.")
                    }

                    Log.i("NSyncAuth", "Target does not indicate error, treating as Success: ${followRes.url()}")
                    return@withContext ImsLoginResponse.Success(
                        cookies = updatedCookieHeader,
                        studentName = studentName,
                        branch = detectedBranch,
                        profileUrl = discovered.profileUrl,
                        activitiesUrl = discovered.activitiesUrl,
                        attendanceUrl = discovered.attendanceUrl,
                        timetableUrl = discovered.timetableUrl
                    )
                } catch (e: Exception) {
                    Log.e("NSyncAuth", "Error following redirect: ${e.message}", e)
                    return@withContext ImsLoginResponse.Failure("Connection error following login redirect: ${e.message}")
                }
            }

            val doc = response.parse()
            val html = doc.html()

            // Differentiate errors if a 200 OK body was returned
            if (html.contains("Invalid Attempt", ignoreCase = true)) {
                return@withContext ImsLoginResponse.Failure("Invalid Captcha number. Please enter the fresh 5-digit code.")
            }

            if (html.contains("Your password does not match", ignoreCase = true) ||
                html.contains("password does not match", ignoreCase = true) ||
                html.contains("Login Failed", ignoreCase = true) ||
                html.contains("Invalid password", ignoreCase = true)
            ) {
                return@withContext ImsLoginResponse.Failure("Incorrect Password or Roll Number. Please verify your credentials.")
            }

            // Look for successful authentication markers and links
            val fallbackCookies = sessionToUse.cookies.toMutableMap().apply { putAll(response.cookies()) }
            val discovered2 = resolvePortalLinks(doc, fallbackCookies)
            var detectedBranch2: String? = null
            discovered2.profileUrl?.let { pUrl ->
                try {
                    val pDoc = Jsoup.connect(pUrl).userAgent(USER_AGENT).cookies(fallbackCookies).timeout(6000).get()
                    val bText = pDoc.select("td:contains(Department), td:contains(Branch)").text()
                    if (bText.isNotBlank()) {
                        detectedBranch2 = bText.substringAfter(":", "").trim().ifBlank { null }
                    }
                } catch (_: Exception) {}
            }

            // Extract student name if present
            val studentName = doc.select("td:contains(Welcome), font:contains(Welcome)").text()
                .substringAfter("Welcome", "")
                .trim()
                .ifBlank { null }

            if (discovered2.profileUrl != null || discovered2.activitiesUrl != null || studentName != null || html.contains("Logout")) {
                return@withContext ImsLoginResponse.Success(
                    cookies = cookieHeader,
                    studentName = studentName,
                    branch = detectedBranch2,
                    profileUrl = discovered2.profileUrl,
                    activitiesUrl = discovered2.activitiesUrl,
                    attendanceUrl = discovered2.attendanceUrl,
                    timetableUrl = discovered2.timetableUrl
                )
            }

            val isBackOnLoginPage = (html.contains("Please Login") && html.contains("User ID") && html.contains("Password")) ||
                    response.url().toString().contains("student_login") ||
                    response.url().toString().contains("plum5_fw_login")

            if (isBackOnLoginPage) {
                return@withContext ImsLoginResponse.Failure("Authentication failed. Please verify your Roll Number, Password, and enter the fresh 5-digit Captcha.")
            }

            ImsLoginResponse.Success(
                cookies = cookieHeader,
                studentName = studentName,
                branch = detectedBranch2,
                profileUrl = discovered2.profileUrl,
                activitiesUrl = discovered2.activitiesUrl,
                attendanceUrl = discovered2.attendanceUrl,
                timetableUrl = discovered2.timetableUrl
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ImsLoginResponse.Failure("Network error: ${e.localizedMessage ?: "Failed to reach IMS portal."}")
        }
    }

    suspend fun fetchLiveTimetable(
        cookies: String,
        rollNumber: String,
        timetableUrl: String? = null
    ): WeeklyTimetable? = withContext(Dispatchers.IO) {
        val targetUrl = timetableUrl?.ifBlank { null } ?: run {
            if (rollNumber.equals("2024UCO1500", ignoreCase = true)) TIMETABLE_URL else null
        } ?: return@withContext null

        try {
            val response = Jsoup.connect(targetUrl)
                .userAgent(USER_AGENT)
                .header("Referer", STUDENT_LOGIN_URL)
                .apply {
                    if (cookies.isNotBlank()) header("Cookie", cookies)
                }
                .timeout(8000)
                .get()

            val html = response.html()
            if (html.contains("Time Table") || html.contains("T1")) {
                return@withContext ImsParser.parseTimetableHtml(html, rollNumber)
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchLiveAttendance(
        cookies: String,
        rollNumber: String,
        attendanceUrl: String? = null,
        branch: String? = null
    ): AttendanceSummary? = withContext(Dispatchers.IO) {
        val targetUrl = attendanceUrl?.ifBlank { null } ?: run {
            if (rollNumber.equals("2024UCO1500", ignoreCase = true)) ATTENDANCE_URL else null
        } ?: return@withContext null

        try {
            val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", USER_AGENT)
                if (cookies.isNotBlank()) setRequestProperty("Cookie", cookies)
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Referer", targetUrl)
            }

            val dept = URLEncoder.encode(branch?.ifBlank { null } ?: "ELECTRONICS AND COMMUNICATION ENGINEERING", "UTF-8")
            val postData = "year=2026-27&sem=3&submit=Submit&recentitycode=$rollNumber&dept=$dept&degree=B.Tech."
            conn.outputStream.use { os ->
                os.write(postData.toByteArray(Charsets.UTF_8))
            }

            val html = conn.inputStream.bufferedReader().use { it.readText() }
            if (html.contains("ABSENT") || html.contains("Total Classes") || html.contains("Overall")) {
                val summary = ImsParser.parseAttendanceHtml(html, rollNumber)
                if (summary != null && summary.rollNumber.isNotBlank() && !summary.rollNumber.equals(rollNumber, ignoreCase = true)) {
                    Log.w("NSyncAuth", "Cross-student attendance mismatch in fetch: got ${summary.rollNumber}, expected $rollNumber")
                    return@withContext null
                }
                return@withContext summary
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchLiveAttendanceResult(
        cookies: String,
        rollNumber: String,
        attendanceUrl: String? = null,
        branch: String? = null
    ): LiveFetchResult<AttendanceSummary> = withContext(Dispatchers.IO) {
        if (cookies.isBlank()) {
            return@withContext LiveFetchResult.SessionExpired
        }
        val targetUrl = attendanceUrl?.ifBlank { null } ?: run {
            if (rollNumber.equals("2024UCO1500", ignoreCase = true)) ATTENDANCE_URL else null
        }
        if (targetUrl == null) {
            return@withContext LiveFetchResult.Error("Attendance portal link not available for this session. Please log in again.")
        }

        try {
            val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Cookie", cookies)
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Referer", targetUrl)
            }

            val dept = URLEncoder.encode(branch?.ifBlank { null } ?: "ELECTRONICS AND COMMUNICATION ENGINEERING", "UTF-8")
            val postData = "year=2026-27&sem=3&submit=Submit&recentitycode=$rollNumber&dept=$dept&degree=B.Tech."
            conn.outputStream.use { os ->
                os.write(postData.toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            if (responseCode == 302 || responseCode == 301) {
                return@withContext LiveFetchResult.SessionExpired
            }

            val html = conn.inputStream.bufferedReader().use { it.readText() }
            if (html.contains("ABSENT") || html.contains("Total Classes") || html.contains("Overall")) {
                val summary = ImsParser.parseAttendanceHtml(html, rollNumber)
                if (summary != null) {
                    if (summary.rollNumber.isNotBlank() && !summary.rollNumber.equals(rollNumber, ignoreCase = true)) {
                        Log.w("NSyncAuth", "Cross-student attendance mismatch: got ${summary.rollNumber}, expected $rollNumber")
                        return@withContext LiveFetchResult.Error("Security mismatch: session student entity differs from profile.")
                    }
                    return@withContext LiveFetchResult.Success(summary)
                }
            }

            if (html.contains("Session Expired", ignoreCase = true) ||
                html.contains("student_login", ignoreCase = true) ||
                html.contains("plum5_fw_login", ignoreCase = true) ||
                html.contains("Please Login Again", ignoreCase = true)) {
                return@withContext LiveFetchResult.SessionExpired
            }

            LiveFetchResult.Error("Could not parse attendance data from portal")
        } catch (e: Exception) {
            e.printStackTrace()
            LiveFetchResult.Error("Network error: ${e.localizedMessage ?: "Failed to connect"}")
        }
    }

    suspend fun fetchLiveTimetableResult(
        cookies: String,
        rollNumber: String,
        timetableUrl: String? = null
    ): LiveFetchResult<WeeklyTimetable> = withContext(Dispatchers.IO) {
        if (cookies.isBlank()) {
            return@withContext LiveFetchResult.SessionExpired
        }
        val targetUrl = timetableUrl?.ifBlank { null } ?: run {
            if (rollNumber.equals("2024UCO1500", ignoreCase = true)) TIMETABLE_URL else null
        }
        if (targetUrl == null) {
            return@withContext LiveFetchResult.Error("Timetable portal link not available for this session. Please log in again.")
        }

        try {
            val response = Jsoup.connect(targetUrl)
                .userAgent(USER_AGENT)
                .header("Referer", STUDENT_LOGIN_URL)
                .header("Cookie", cookies)
                .timeout(8000)
                .followRedirects(false)
                .get()

            val html = response.html()
            if (html.contains("Time Table") || html.contains("T1")) {
                val tt = ImsParser.parseTimetableHtml(html, rollNumber)
                if (tt != null) return@withContext LiveFetchResult.Success(tt)
            }

            if (html.contains("Session Expired", ignoreCase = true) ||
                html.contains("student_login", ignoreCase = true) ||
                html.contains("plum5_fw_login", ignoreCase = true)) {
                return@withContext LiveFetchResult.SessionExpired
            }

            LiveFetchResult.Error("Could not parse timetable")
        } catch (e: Exception) {
            e.printStackTrace()
            LiveFetchResult.Error("Network error: ${e.localizedMessage ?: "Failed to connect"}")
        }
    }
}

sealed class LiveFetchResult<out T> {
    data class Success<out T>(val data: T) : LiveFetchResult<T>()
    object SessionExpired : LiveFetchResult<Nothing>()
    data class Error(val message: String) : LiveFetchResult<Nothing>()
}
