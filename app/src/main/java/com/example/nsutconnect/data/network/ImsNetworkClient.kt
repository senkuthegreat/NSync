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
import java.net.HttpURLConnection
import java.net.URL

data class CaptchaSession(
    val sessionId: String,
    val captchaBitmap: Bitmap?,
    val cookies: Map<String, String>,
    val encFy: String? = null,
    val fy: String = "2026-27"
)

sealed class ImsLoginResponse {
    data class Success(
        val cookies: String,
        val studentName: String? = null,
        val branch: String? = null,
        val profileUrl: String? = null,
        val activitiesUrl: String? = null
    ) : ImsLoginResponse()
    data class Failure(val message: String) : ImsLoginResponse()
}

class ImsNetworkClient {

    companion object {
        const val BASE_URL = "https://www.imsnsit.org/imsnsit/"
        const val STUDENT_LOGIN110_URL = "https://www.imsnsit.org/imsnsit/student_login110.php"
        const val PLUM_LOGIN_URL = "https://www.imsnsit.org/imsnsit/plum5_fw_login.php?uid="
        const val PLUM_LOGIN_POST_URL = "https://www.imsnsit.org/imsnsit/plum5_fw_login.php"
        const val TIMETABLE_URL = "https://www.imsnsit.org/imsnsit/plum_url.php?41uZnOKcTatoQuZ1nS5qVGIuiLOuCZGGw43gNaDJuC6JU+zlhWK6ya/tcFLywtouIo7aWVZdCbDde4Onb0dyebiOoF5CwOCF9cWNylj6wkXd0RX8bVnVGaPKlMtStKmyuAZXCzEdKq+zBvagqtna+Vjw3K+2YkEhpEDLFH9RwuVsL162Qyo3TRed5amA2UTcldG13/T8xEKGB82zR7JxVA=="
        const val ATTENDANCE_URL = "https://www.imsnsit.org/imsnsit/plum_url.php?P59Otr7+3xJIlCBBABoVb0GIgAXiKbA1LIAMUeXV5ZvLND/AfclikCFEYYIO4oq3BkrlNPPPoLsLVLt2ihg6VdTT8AXD3bdKB07WmGbjTQ4PM9NWTedZH8r2ohs9zsBsbFmAs01PXjfJn07m4ARSsaaeuZxTy4F4EJp+SoAk4dHNGvsD3yrO6W3oQUld+8626tyicw9vPYtiH/KgrqEbUsWbLNgbppb9cgWn/IrjvV15vuFRdLZIHnwX5Qndspns"
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.119 Safari/537.36"
    }

    suspend fun fetchCaptchaSession(): CaptchaSession? = withContext(Dispatchers.IO) {
        try {
            val initRes = Jsoup.connect(STUDENT_LOGIN110_URL)
                .userAgent(USER_AGENT)
                .header("Referer", "https://www.imsnsit.org/imsnsit/student.htm")
                .timeout(10000)
                .method(Connection.Method.GET)
                .execute()

            val cookies = initRes.cookies().toMutableMap()

            val loginRes = Jsoup.connect(PLUM_LOGIN_URL)
                .userAgent(USER_AGENT)
                .header("Referer", STUDENT_LOGIN110_URL)
                .cookies(cookies)
                .timeout(10000)
                .method(Connection.Method.GET)
                .execute()

            cookies.putAll(loginRes.cookies())

            val doc = loginRes.parse()
            val captchaImg = doc.selectFirst("img#captchaimg") ?: doc.selectFirst("img[src*='captcha']")
            val captchaSrc = captchaImg?.attr("src") ?: return@withContext null
            val fullCaptchaUrl = if (captchaSrc.startsWith("http")) captchaSrc else "https://www.imsnsit.org/imsnsit/$captchaSrc"

            val encFy = doc.selectFirst("input[name=enc_fy]")?.attr("value")
            val fy = doc.selectFirst("select[name=fy] option[selected]")?.attr("value") ?: "2026-27"

            val imgRes = Jsoup.connect(fullCaptchaUrl)
                .userAgent(USER_AGENT)
                .header("Referer", PLUM_LOGIN_URL)
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
                encFy = encFy,
                fy = fy
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
            val response = Jsoup.connect(PLUM_LOGIN_POST_URL)
                .userAgent(USER_AGENT)
                .header("Referer", PLUM_LOGIN_URL)
                .header("Origin", "https://www.imsnsit.org")
                .cookies(sessionToUse.cookies)
                .data("f", "")
                .data("uid", cleanRoll)
                .data("pwd", cleanPass)
                .data("cap", cleanCap)
                .data("comp", "NETAJI SUBHAS UNIVERSITY OF TECHNOLOGY")
                .data("fy", sessionToUse.fy)
                .apply {
                    sessionToUse.encFy?.let { data("enc_fy", it) }
                }
                .data("login", "Login")
                .timeout(12000)
                .method(Connection.Method.POST)
                .followRedirects(true)
                .execute()

            val cookies = sessionToUse.cookies.toMutableMap()
            cookies.putAll(response.cookies())
            val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

            val html = response.body()
            val doc = response.parse()

            // Check for explicit error messages from IMS
            if (html.contains("Invalid security Number", ignoreCase = true) ||
                html.contains("Please Enter Captcha", ignoreCase = true)
            ) {
                return@withContext ImsLoginResponse.Failure("Invalid security captcha number. Please try again.")
            }

            if (html.contains("Invalid password", ignoreCase = true) ||
                html.contains("password does not match", ignoreCase = true) ||
                html.contains("Please enter valid Userid", ignoreCase = true)
            ) {
                return@withContext ImsLoginResponse.Failure("Incorrect Password or Roll Number. Please verify your credentials.")
            }

            // Check if redirected right back to login form (unauthenticated)
            if (html.contains("Please Login") && html.contains("User ID") && html.contains("Password") && !html.contains("My Profile")) {
                return@withContext ImsLoginResponse.Failure("Authentication failed. Invalid Roll Number or Password.")
            }

            // Look for successful authentication markers
            val links = doc.select("a")
            var profileUrl: String? = null
            var activitiesUrl: String? = null
            for (link in links) {
                val text = link.text().trim()
                if (text.equals("My Profile", ignoreCase = true)) {
                    profileUrl = link.attr("href")
                } else if (text.equals("My Activities", ignoreCase = true)) {
                    activitiesUrl = link.attr("href")
                }
            }

            // Extract student name if present
            val studentName = doc.select("td:contains(Welcome), font:contains(Welcome)").text()
                .substringAfter("Welcome", "")
                .trim()
                .ifBlank { null }

            ImsLoginResponse.Success(
                cookies = cookieHeader,
                studentName = studentName,
                branch = null,
                profileUrl = profileUrl,
                activitiesUrl = activitiesUrl
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ImsLoginResponse.Failure("Network error: ${e.localizedMessage ?: "Failed to reach IMS portal."}")
        }
    }

    suspend fun fetchLiveTimetable(cookies: String, rollNumber: String): WeeklyTimetable? = withContext(Dispatchers.IO) {
        try {
            val response = Jsoup.connect(TIMETABLE_URL)
                .userAgent(USER_AGENT)
                .header("Referer", "https://www.imsnsit.org/imsnsit/student_login.php")
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

    suspend fun fetchLiveAttendance(cookies: String, rollNumber: String): AttendanceSummary? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(ATTENDANCE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", USER_AGENT)
                if (cookies.isNotBlank()) setRequestProperty("Cookie", cookies)
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Referer", ATTENDANCE_URL)
            }

            val postData = "year=2026-27&sem=3&submit=Submit&recentitycode=$rollNumber&dept=ELECTRONICS+AND+COMMUNICATION+ENGINEERING&degree=B.Tech."
            conn.outputStream.use { os ->
                os.write(postData.toByteArray(Charsets.UTF_8))
            }

            val html = conn.inputStream.bufferedReader().use { it.readText() }
            if (html.contains("ABSENT") || html.contains("Total Classes") || html.contains("Overall")) {
                return@withContext ImsParser.parseAttendanceHtml(html, rollNumber)
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
