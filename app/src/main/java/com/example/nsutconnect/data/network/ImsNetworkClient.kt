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
        val timetableUrl: String? = null,
        val coursesUrl: String? = null,
        val semester: String? = null
    ) : ImsLoginResponse()
    data class Failure(val message: String) : ImsLoginResponse()
}

class ImsNetworkClient {

    companion object {
        const val BASE_URL = "https://www.imsnsit.org/imsnsit/"
        const val STUDENT_LOGIN110_URL = "https://www.imsnsit.org/imsnsit/student_login110.php"
        const val STUDENT_LOGIN_URL = "https://www.imsnsit.org/imsnsit/student_login.php"
        const val PLUM_SCREEN_INIT_URL = "https://www.imsnsit.org/imsnsit/plum5_fw_login.php?t=sw&w=1"
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.119 Safari/537.36"
    }

    data class DiscoveredPortalLinks(
        val profileUrl: String? = null,
        val activitiesUrl: String? = null,
        val attendanceUrl: String? = null,
        val timetableUrl: String? = null,
        val coursesUrl: String? = null,
        val resultUrl: String? = null
    )

    fun resolvePortalLinks(doc: Document, cookies: Map<String, String>): DiscoveredPortalLinks {
        var profileUrl: String? = null
        var activitiesUrl: String? = null
        var attendanceUrl: String? = null
        var timetableUrl: String? = null
        var coursesUrl: String? = null
        var resultUrl: String? = null

        fun extractUrlFromElement(elem: org.jsoup.nodes.Element): String? {
            val href = elem.attr("href").trim()
            val onclick = elem.attr("onclick").trim()
            val jsRegex = Regex("""(?:openURL|showURL|window\.open)\s*\(\s*['"]([^'"]+)['"]""")
            val jsMatch = jsRegex.find(href)?.groupValues?.get(1) ?: jsRegex.find(onclick)?.groupValues?.get(1)
            if (jsMatch != null && jsMatch.isNotBlank() && jsMatch != "#") {
                return when {
                    jsMatch.startsWith("http") -> jsMatch
                    jsMatch.startsWith("/") -> "https://www.imsnsit.org$jsMatch"
                    else -> BASE_URL + jsMatch
                }
            }
            return when {
                href.startsWith("http") -> href
                href.isNotBlank() && !href.startsWith("javascript") && !href.startsWith("#") -> {
                    if (href.startsWith("/")) "https://www.imsnsit.org$href" else BASE_URL + href
                }
                href.contains("plum_url.php") -> {
                    val match = Regex("plum_url\\.php[^'\" )]+").find(href)?.value
                    if (match != null) BASE_URL + match else null
                }
                onclick.contains("plum_url.php") -> {
                    val match = Regex("plum_url\\.php[^'\" )]+").find(onclick)?.value
                    if (match != null) BASE_URL + match else null
                }
                else -> null
            }
        }

        fun scanDocument(d: Document, isActivitiesDoc: Boolean = false) {
            for (elem in d.select("a, [onclick], area, td, tr, div, li")) {
                val text = elem.text().trim()
                val textLower = text.lowercase().replace(Regex("[^a-z0-9]"), "")
                val combined = "$text ${elem.attr("href")} ${elem.attr("onclick")}".lowercase()
                val resolvedUrl = extractUrlFromElement(elem)

                if (resolvedUrl != null) {
                    if (attendanceUrl == null && (textLower.contains("attendance") || combined.contains("student_attendance"))) {
                        attendanceUrl = resolvedUrl
                        Log.i("NSyncAuth", "Found attendanceUrl: $attendanceUrl (from text: '$text')")
                    }
                    if (timetableUrl == null && (textLower.contains("timetable") || textLower.contains("facultytimetable") || combined.contains("student_timetable"))) {
                        timetableUrl = resolvedUrl
                        Log.i("NSyncAuth", "Found timetableUrl: $timetableUrl (from text: '$text')")
                    }
                    if (coursesUrl == null && (textLower.contains("coursesregistered") || textLower.contains("registeredcourses") || (textLower.contains("course") && textLower.contains("register")) || combined.contains("courses_registered"))) {
                        coursesUrl = resolvedUrl
                        Log.i("NSyncAuth", "Found coursesUrl: $coursesUrl (from text: '$text')")
                    }
                    if (resultUrl == null && (textLower.contains("result") || combined.contains("examination_result"))) {
                        resultUrl = resolvedUrl
                    }
                    if (!isActivitiesDoc) {
                        if (profileUrl == null && (textLower.contains("profile") || combined.contains("student_profile"))) {
                            profileUrl = resolvedUrl
                        }
                        if (activitiesUrl == null && (textLower.contains("activities") || textLower.contains("myactivities") || combined.contains("student_activities"))) {
                            activitiesUrl = resolvedUrl
                            Log.i("NSyncAuth", "Found activitiesUrl: $activitiesUrl (from text: '$text')")
                        }
                    }
                }
            }
        }

        // Pass 1: Scan main landing doc
        scanDocument(doc, isActivitiesDoc = false)

        // Pass 2: If activitiesUrl or profileUrl wasn't found directly, inspect child frames
        if (activitiesUrl == null || profileUrl == null || attendanceUrl == null || coursesUrl == null) {
            val frames = doc.select("frame, iframe")
            for (frame in frames) {
                val src = frame.attr("src").trim()
                if (src.isBlank() || src.startsWith("javascript") || src.equals("about:blank", ignoreCase = true)) continue
                val frameUrl = when {
                    src.startsWith("http") -> src
                    src.startsWith("/") -> "https://www.imsnsit.org$src"
                    else -> BASE_URL + src
                }
                try {
                    val frameDoc = Jsoup.connect(frameUrl)
                        .userAgent(USER_AGENT)
                        .header("Referer", STUDENT_LOGIN_URL)
                        .cookies(cookies)
                        .timeout(6000)
                        .get()
                    scanDocument(frameDoc, isActivitiesDoc = false)
                } catch (e: Exception) {
                    Log.w("NSyncAuth", "Could not inspect portal frame $frameUrl: ${e.message}")
                }
            }
        }

        // Pass 3: THE KEY TO NSUT IMS: Fetch "My Activities" to resolve dynamic sub-module links!
        // In Plumware, "My Attendance", "Timetable", "Courses" live inside the "My Activities" module page!
        val actUrl = activitiesUrl
        if (!actUrl.isNullOrBlank() && (attendanceUrl == null || timetableUrl == null || coursesUrl == null)) {
            try {
                Log.i("NSyncAuth", "Fetching My Activities page: $actUrl")
                val actDoc = Jsoup.connect(actUrl)
                    .userAgent(USER_AGENT)
                    .header("Referer", STUDENT_LOGIN_URL)
                    .cookies(cookies)
                    .timeout(10000)
                    .followRedirects(true)
                    .get()

                scanDocument(actDoc, isActivitiesDoc = true)

                // Also inspect any frames within My Activities if attendanceUrl still not resolved
                if (attendanceUrl == null || timetableUrl == null || coursesUrl == null) {
                    val actFrames = actDoc.select("frame, iframe")
                    for (frame in actFrames) {
                        val src = frame.attr("src").trim()
                        if (src.isBlank() || src.startsWith("javascript") || src.equals("about:blank", ignoreCase = true)) continue
                        val frameUrl = when {
                            src.startsWith("http") -> src
                            src.startsWith("/") -> "https://www.imsnsit.org$src"
                            else -> BASE_URL + src
                        }
                        try {
                            val fDoc = Jsoup.connect(frameUrl)
                                .userAgent(USER_AGENT)
                                .header("Referer", actUrl)
                                .cookies(cookies)
                                .timeout(6000)
                                .get()
                            scanDocument(fDoc, isActivitiesDoc = true)
                        } catch (e: Exception) {
                            Log.w("NSyncAuth", "Could not inspect activities subframe $frameUrl: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NSyncAuth", "Failed to fetch activitiesUrl $activitiesUrl: ${e.message}")
            }
        }

        Log.i("NSyncAuth", "Resolved portal links: attendance=$attendanceUrl, timetable=$timetableUrl, courses=$coursesUrl, profile=$profileUrl, activities=$activitiesUrl")
        return DiscoveredPortalLinks(
            profileUrl = profileUrl,
            activitiesUrl = activitiesUrl,
            attendanceUrl = attendanceUrl,
            timetableUrl = timetableUrl,
            coursesUrl = coursesUrl,
            resultUrl = resultUrl
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

                    var detectedSem: String? = null
                    if (!discovered.coursesUrl.isNullOrBlank()) {
                        try {
                            val (semFromCourses, _) = fetchEnrolledCourses(updatedCookieHeader, discovered.coursesUrl)
                            detectedSem = semFromCourses
                        } catch (_: Exception) {}
                    }
                    if (detectedSem.isNullOrBlank()) {
                        detectedSem = calculateSemesterFromRoll(rollNumber)
                    }

                    if (isSuccess) {
                        Log.i("NSyncAuth", "Login SUCCESS: name=$studentName, branch=$detectedBranch, attUrl=${discovered.attendanceUrl}, ttUrl=${discovered.timetableUrl}, coursesUrl=${discovered.coursesUrl}, sem=$detectedSem")
                        return@withContext ImsLoginResponse.Success(
                            cookies = updatedCookieHeader,
                            studentName = studentName,
                            branch = detectedBranch,
                            profileUrl = discovered.profileUrl,
                            activitiesUrl = discovered.activitiesUrl,
                            attendanceUrl = discovered.attendanceUrl,
                            timetableUrl = discovered.timetableUrl,
                            coursesUrl = discovered.coursesUrl,
                            semester = detectedSem
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
                        timetableUrl = discovered.timetableUrl,
                        coursesUrl = discovered.coursesUrl,
                        semester = detectedSem
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

            var detectedSem2: String? = null
            if (!discovered2.coursesUrl.isNullOrBlank()) {
                try {
                    val (semFromCourses, _) = fetchEnrolledCourses(cookieHeader, discovered2.coursesUrl)
                    detectedSem2 = semFromCourses
                } catch (_: Exception) {}
            }
            if (detectedSem2.isNullOrBlank()) {
                detectedSem2 = calculateSemesterFromRoll(rollNumber)
            }

            if (discovered2.profileUrl != null || discovered2.activitiesUrl != null || studentName != null || html.contains("Logout")) {
                return@withContext ImsLoginResponse.Success(
                    cookies = cookieHeader,
                    studentName = studentName,
                    branch = detectedBranch2,
                    profileUrl = discovered2.profileUrl,
                    activitiesUrl = discovered2.activitiesUrl,
                    attendanceUrl = discovered2.attendanceUrl,
                    timetableUrl = discovered2.timetableUrl,
                    coursesUrl = discovered2.coursesUrl,
                    semester = detectedSem2
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
                timetableUrl = discovered2.timetableUrl,
                coursesUrl = discovered2.coursesUrl,
                semester = detectedSem2
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
        val targetUrl = timetableUrl?.ifBlank { null } ?: return@withContext null

        try {
            val response = Jsoup.connect(targetUrl)
                .userAgent(USER_AGENT)
                .header("Referer", STUDENT_LOGIN_URL)
                .apply {
                    if (cookies.isNotBlank()) header("Cookie", cookies)
                }
                .timeout(10000)
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

    fun parseCookiesToMap(cookieHeader: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        cookieHeader.split(";").forEach { part ->
            val trimmed = part.trim()
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx > 0) {
                val key = trimmed.substring(0, eqIdx).trim()
                val value = trimmed.substring(eqIdx + 1).trim()
                map[key] = value
            }
        }
        return map
    }

    fun calculateSemesterFromRoll(rollNumber: String): String {
        val clean = rollNumber.trim().uppercase()
        val match = Regex("""^(?:20)?(\d{2})""").find(clean)
        if (match != null) {
            val yr2 = match.groupValues[1].toIntOrNull()
            if (yr2 != null) {
                val admYear = 2000 + yr2
                val now = java.time.LocalDate.now()
                val curYear = now.year
                val curMonth = now.monthValue
                val diff = (curYear - admYear).coerceAtLeast(0)
                val sem = if (curMonth <= 6) {
                    diff * 2
                } else {
                    diff * 2 + 1
                }
                return sem.coerceIn(1, 8).toString()
            }
        }
        return "1"
    }

    suspend fun fetchEnrolledCourses(
        cookies: String,
        coursesUrl: String?
    ): Pair<String?, Map<String, String>> = withContext(Dispatchers.IO) {
        val targetUrl = coursesUrl?.ifBlank { null } ?: return@withContext Pair(null, emptyMap())
        if (cookies.isBlank()) {
            return@withContext Pair(null, emptyMap())
        }
        try {
            val cookiesMap = parseCookiesToMap(cookies)
            val res = Jsoup.connect(targetUrl)
                .userAgent(USER_AGENT)
                .header("Referer", STUDENT_LOGIN_URL)
                .cookies(cookiesMap)
                .timeout(10000)
                .followRedirects(true)
                .get()
            val html = res.html()
            val sem = ImsParser.parseSemesterFromHtml(html)
            val courses = ImsParser.parseEnrolledCoursesHtml(html)
            if (courses.isNotEmpty()) {
                ImsParser.registerSubjectNames(courses)
            }
            Pair(sem, courses)
        } catch (e: Exception) {
            Log.w("NSyncCourses", "Failed to fetch enrolled courses: ${e.message}")
            Pair(null, emptyMap())
        }
    }

    fun getFinancialYear(): String {
        val now = java.time.LocalDate.now()
        val year = now.year
        val month = now.monthValue
        return if (month <= 5) {
            "${year - 1}-${(year % 100).toString().padStart(2, '0')}"
        } else {
            "$year-${((year + 1) % 100).toString().padStart(2, '0')}"
        }
    }

    fun getPreviousFinancialYear(fy: String): String {
        val startYear = fy.substringBefore("-").toIntOrNull() ?: return fy
        val prevStart = startYear - 1
        return "$prevStart-${((prevStart + 1) % 100).toString().padStart(2, '0')}"
    }

    private fun isAttendanceMatrixHtml(html: String): Boolean {
        return html.contains("plum_fieldbig", ignoreCase = true) ||
                html.contains("plum_head", ignoreCase = true) ||
                html.contains("Total Classes", ignoreCase = true) ||
                html.contains("Overall", ignoreCase = true) ||
                (html.contains("Total", ignoreCase = true) && html.contains("Present", ignoreCase = true)) ||
                html.contains("myreport", ignoreCase = true) ||
                html.contains("ABSENT", ignoreCase = true)
    }

    private fun fetchAttendanceHtmlFromPortal(
        targetUrl: String,
        cookies: String,
        rollNumber: String,
        branch: String?,
        semester: String? = null
    ): String? {
        if (targetUrl.isBlank()) return null
        val effectiveUrl = targetUrl
        try {
            val cookiesMap = parseCookiesToMap(cookies)
            Log.i("NSyncAttendance", "Requesting attendance from: $effectiveUrl with roll: $rollNumber")

            // Step 1: Issue GET request to attendance module with session cookies
            val getRes = Jsoup.connect(effectiveUrl)
                .userAgent(USER_AGENT)
                .header("Referer", STUDENT_LOGIN_URL)
                .cookies(cookiesMap)
                .timeout(15000)
                .followRedirects(true)
                .execute()

            val doc = getRes.parse()
            val initialHtml = doc.html()

            if (initialHtml.contains("Session expired", ignoreCase = true) ||
                initialHtml.contains("Please Login Again", ignoreCase = true) ||
                getRes.url().toString().contains("student_login") ||
                getRes.url().toString().contains("plum5_fw_login")) {
                Log.w("NSyncAttendance", "Portal response indicates session expired")
                return null
            }

            // If the initial GET response already rendered the attendance matrix:
            if (isAttendanceMatrixHtml(initialHtml)) {
                Log.i("NSyncAttendance", "Attendance matrix found directly on initial GET response")
                return initialHtml
            }

            // Step 2: Extract form and dynamically submit with student roll and hidden parameters
            val form = doc.selectFirst("form#frm, form[name=frm]")
                ?: doc.select("form").firstOrNull { it.selectFirst("select[name=year], select[name=sem]") != null }
                ?: doc.selectFirst("form")
            if (form != null) {
                val actionAttr = form.attr("action").trim()
                val postUrl = when {
                    actionAttr.startsWith("http") -> actionAttr
                    actionAttr.isNotBlank() && !actionAttr.contains("createpdf", ignoreCase = true) ->
                        if (actionAttr.startsWith("/")) "https://www.imsnsit.org$actionAttr" else BASE_URL + actionAttr
                    else -> effectiveUrl
                }

                val baseParams = mutableMapOf<String, String>()
                for (input in form.select("input")) {
                    val name = input.attr("name").trim()
                    val value = input.attr("value")
                    if (name.isNotBlank()) {
                        baseParams[name] = value
                    }
                }

                // Explicitly extract enc_year and enc_sem like imsnsit
                val encYear = doc.selectFirst("#enc_year, input[name=enc_year]")?.attr("value")?.trim()
                val encSem = doc.selectFirst("#enc_sem, input[name=enc_sem]")?.attr("value")?.trim()
                if (!encYear.isNullOrBlank()) baseParams["enc_year"] = encYear
                if (!encSem.isNullOrBlank()) baseParams["enc_sem"] = encSem

                val formRoll = form.selectFirst("[name=recentitycode]")?.attr("value")?.trim().orEmpty()
                baseParams["recentitycode"] = if (formRoll.isNotBlank()) formRoll else rollNumber

                val formDept = form.selectFirst("[name=dept]")?.attr("value")?.trim().orEmpty()
                if (formDept.isNotBlank()) {
                    baseParams["dept"] = formDept
                } else if (!branch.isNullOrBlank()) {
                    baseParams["dept"] = branch
                }

                val formDegree = form.selectFirst("[name=degree]")?.attr("value")?.trim().orEmpty()
                baseParams["degree"] = if (formDegree.isNotBlank()) formDegree else "B.Tech."

                baseParams["submit"] = "Submit"
                if (!baseParams.containsKey("ename")) baseParams["ename"] = ""
                if (!baseParams.containsKey("ecode")) baseParams["ecode"] = ""

                // Determine Academic / Financial Year candidates
                val yearSelect = form.selectFirst("select[name=year]") ?: doc.selectFirst("select[name=year]")
                val availableYears = yearSelect?.select("option")?.mapNotNull { it.attr("value").trim().ifBlank { null } } ?: emptyList()
                val targetYear = getFinancialYear()
                val candidateYears = mutableListOf<String>()
                if (availableYears.contains(targetYear)) candidateYears.add(targetYear)
                val prevYear = getPreviousFinancialYear(targetYear)
                if (availableYears.contains(prevYear)) candidateYears.add(prevYear)
                for (yr in availableYears) {
                    if (!candidateYears.contains(yr)) candidateYears.add(yr)
                }
                if (candidateYears.isEmpty()) candidateYears.add(targetYear)

                // Determine Candidate Semesters to query
                val resolvedSem = semester?.ifBlank { null } ?: calculateSemesterFromRoll(rollNumber)
                val semSelect = form.selectFirst("select[name=sem]") ?: doc.selectFirst("select[name=sem]")
                val availableSems = semSelect?.select("option")?.mapNotNull { it.attr("value").trim().ifBlank { null } } ?: emptyList()

                val semInt = resolvedSem.toIntOrNull() ?: 1
                val candidateSems = mutableListOf<String>()
                candidateSems.add(resolvedSem)
                if (semInt > 1) {
                    candidateSems.add((semInt - 1).toString())
                }
                if (semInt < 8) {
                    candidateSems.add((semInt + 1).toString())
                }

                val validCandidates = candidateSems.filter { availableSems.isEmpty() || availableSems.contains(it) }.distinct()

                for (candYear in candidateYears) {
                    for (candSem in validCandidates) {
                        val postParams = baseParams.toMutableMap()
                        postParams["year"] = candYear
                        postParams["sem"] = candSem

                        Log.i("NSyncAttendance", "Submitting POST to $postUrl with year=$candYear, sem=$candSem, roll=$rollNumber")

                        val postRes = Jsoup.connect(postUrl)
                            .userAgent(USER_AGENT)
                            .header("Referer", effectiveUrl)
                            .cookies(cookiesMap)
                            .data(postParams)
                            .timeout(15000)
                            .followRedirects(true)
                            .method(Connection.Method.POST)
                            .execute()

                        val postHtml = postRes.body()
                        if (isAttendanceMatrixHtml(postHtml)) {
                            val parsed = ImsParser.parseAttendanceHtml(postHtml, rollNumber)
                            if (parsed.records.isNotEmpty()) {
                                Log.i("NSyncAttendance", "Successfully retrieved live attendance matrix from POST response for year $candYear, sem $candSem with ${parsed.records.size} subjects")
                                return postHtml
                            }
                        }
                    }
                }
            }

            return initialHtml
        } catch (e: Exception) {
            Log.e("NSyncAttendance", "Exception in fetchAttendanceHtmlFromPortal: ${e.message}", e)
            return null
        }
    }

    suspend fun fetchLiveAttendance(
        cookies: String,
        rollNumber: String,
        attendanceUrl: String? = null,
        branch: String? = null,
        semester: String? = null
    ): AttendanceSummary? = withContext(Dispatchers.IO) {
        val targetUrl = attendanceUrl?.ifBlank { null } ?: return@withContext null

        val html = fetchAttendanceHtmlFromPortal(targetUrl, cookies, rollNumber, branch, semester) ?: return@withContext null
        if (isAttendanceMatrixHtml(html)) {
            val summary = ImsParser.parseAttendanceHtml(html, rollNumber)
            if (summary.records.isNotEmpty()) {
                return@withContext summary
            }
        }
        null
    }

    suspend fun fetchLiveAttendanceResult(
        cookies: String,
        rollNumber: String,
        attendanceUrl: String? = null,
        branch: String? = null,
        semester: String? = null
    ): LiveFetchResult<AttendanceSummary> = withContext(Dispatchers.IO) {
        if (cookies.isBlank()) {
            return@withContext LiveFetchResult.SessionExpired
        }
        val targetUrl = attendanceUrl?.ifBlank { null }
            ?: return@withContext LiveFetchResult.Error("Attendance module link not found in portal session")

        val html = fetchAttendanceHtmlFromPortal(targetUrl, cookies, rollNumber, branch, semester)
        if (html == null) {
            return@withContext LiveFetchResult.SessionExpired
        }

        if (isAttendanceMatrixHtml(html)) {
            val summary = ImsParser.parseAttendanceHtml(html, rollNumber)
            if (summary.records.isNotEmpty()) {
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
    }

    suspend fun fetchLiveTimetableResult(
        cookies: String,
        rollNumber: String,
        timetableUrl: String? = null
    ): LiveFetchResult<WeeklyTimetable> = withContext(Dispatchers.IO) {
        if (cookies.isBlank()) {
            return@withContext LiveFetchResult.SessionExpired
        }
        val targetUrl = timetableUrl?.ifBlank { null }
            ?: return@withContext LiveFetchResult.Error("Timetable module link not found in portal session")

        try {
            val response = Jsoup.connect(targetUrl)
                .userAgent(USER_AGENT)
                .header("Referer", STUDENT_LOGIN_URL)
                .header("Cookie", cookies)
                .timeout(10000)
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
