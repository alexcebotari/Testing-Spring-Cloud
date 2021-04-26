package com.example.demo

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.test.context.junit.jupiter.SpringExtension
import wiremock.org.apache.commons.io.IOUtils
import java.io.IOException


@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureStubRunner(
//    stubsMode = StubRunnerProperties.StubsMode.LOCAL,
//    ids = ["https://portal-mobile.uat-newdaycards.com"]
//)
class OurNewTest {

    companion object {
        var authenticationToken = ""

        @BeforeAll
        @JvmStatic
        fun once() {
            val authUrl = "https://portal-mobile.uat-newdaycards.com/api/v6/verifyusername"
            val credentials = UserCredentials("opus", "a1105000000910739", "121212")
            val serializedCredentials = Gson().toJson(credentials)
            val httHeaders = HttpHeaders()
            httHeaders.contentType = MediaType.APPLICATION_JSON
            val response = TestRestTemplate().postForObject(
                authUrl,
                HttpEntity(serializedCredentials, HttpHeaders().apply {
                    add("Content-Type", "application/json")
                }),
                LoginResponse::class.java
            )

            val tempAccessToken = response.data.accessToken

            val sendOtpUrl = "https://portal-mobile.uat-newdaycards.com/api/v6/otp/send"
            val credentialsSendOtp = OtpSendCredentials("mobile")
            val serializedCredentialsSendOtp = Gson().toJson(credentialsSendOtp)
            val testRestTemplate = TestRestTemplate()
//            testRestTemplate.restTemplate.interceptors = listOf(LoggingRequestInterceptor())
            val responseSendOtp = testRestTemplate.postForObject(
                sendOtpUrl,
                HttpEntity(serializedCredentialsSendOtp, HttpHeaders().apply {
                    add("Content-Type", "application/json")
                    add("Authorization", "Bearer $tempAccessToken")
                }),
                SendOtpResponse::class.java
            )

            val sessionData = responseSendOtp.data.sessionData

            val digits = response.data.passcodeChallenge.map {
                // 4 - 2
                // 5 - 1
                val digitCode = if (it % 2 == 0) 2 else 1
                PasscodeDigit(it, digitCode)
            }

            val signInOTPUrl = "https://portal-mobile.uat-newdaycards.com/api/v6/signinbypasscodeotptoken"
            val credentialsSignInOTPUrl = SignInByOtpCredentials(
                sessionData = sessionData,
                passcodeDetails = digits,
                passcodeIndexesKey = response.data.passcodeIndexesKey
            )
            val serializedCredentialsSignInOTPUrl = Gson().toJson(credentialsSignInOTPUrl)
            val testRestTemplate1 = TestRestTemplate()
//            testRestTemplate1.restTemplate.interceptors = listOf(LoggingRequestInterceptor())
            val responseSignInOTPUrl = testRestTemplate1.postForObject(
                signInOTPUrl,
                HttpEntity(serializedCredentialsSignInOTPUrl, HttpHeaders().apply {
                    add("Content-Type", "application/json")
                    add("Authorization", "Bearer $tempAccessToken")
                }),
                SignInByOtpResponse::class.java
            )

            authenticationToken = responseSignInOTPUrl.data.boundToken.accessToken

        }
    }

    private var restTemplate: TestRestTemplate = TestRestTemplate()

    @Test
    fun `Ensure we have correct contract for interstitials and banners`() {
        restTemplate.restTemplate.interceptors = listOf(
            object : ClientHttpRequestInterceptor {
                override fun intercept(
                    request: HttpRequest,
                    body: ByteArray,
                    execution: ClientHttpRequestExecution
                ): ClientHttpResponse {
                    request.headers.add(
                        "Authorization",
                        "Bearer $authenticationToken"
                    )
                    return execution.execute(request, body)
                }
            }
//            LoggingRequestInterceptor()
        )
        val response = restTemplate.getForObject(
            "https://portal-mobile.uat-newdaycards.com/api/v6/notification?interstitial=true&banner=true",
            Any::class.java
        )
        val jsonObject = Gson().toJson(response)
        val dataJsonObject = JsonParser().parse(jsonObject).asJsonObject["data"] as JsonObject

        val bannersArray = dataJsonObject["banners"].asJsonArray
        val interstitials =  dataJsonObject["interstitials"]
        assert(interstitials != null)
        assert(bannersArray != null)
    }
}

class LoggingRequestInterceptor : ClientHttpRequestInterceptor {

    override fun intercept(p0: HttpRequest, p1: ByteArray, p2: ClientHttpRequestExecution): ClientHttpResponse {
        val response = p2.execute(p0, p1)

        log(p0, response)

        return response
    }

    private fun log(request: HttpRequest, response: ClientHttpResponse) {
        println("Method: " + request.method.toString());
        println("URI: " + request.uri.toString());
//         println("Request Body: " + String(body));
        println("Response body: " + IOUtils.toString(response.body));
    }
}

class UserCredentials(
    val brand: String,
    val username: String,
    val deviceId: String = "e66bc58192c269b6",
    val deviceInfo: String = "Pixel 2"
)

data class LoginResponse(var status: String = "", var data: Body = Body()) {
    data class Body(
        var accessToken: String = "",
        var passcodeChallenge: List<Int> = emptyList(),
        var passcodeIndexesKey: String = ""
    )
}

class OtpSendCredentials(val phoneNumberType: String)

data class SendOtpResponse(
    var data: Data = Data()
) {
    data class Data(var sessionData: String = "")
}

data class PasscodeDigit(val key: Int, val value: Int)

data class DeviceInfoPair(val key: String, val value: String)

class SignInByOtpCredentials(
    var clientId: String = "D3F9T79PVS2THZBD9YIXNJDO73EWFDY5",
    var clientSecret: String = "BMI76CPB54Q44I25TQ5QVTRN4AEH4LLD",
    var description: String = "Google Pixel 2",
    var passcodeDetails: List<PasscodeDigit> = listOf(PasscodeDigit(1, 1), PasscodeDigit(2, 2), PasscodeDigit(6, 2)),
    var passcodeIndexesKey: String = "akey",
    var nullable: Boolean = true,
    var sessionData: String = "",
    var otp: String = "000000",
    val deviceInfo: List<DeviceInfoPair> = listOf(
        DeviceInfoPair(key = "device_locale", value = "GB"),
        DeviceInfoPair("carrier", "unknown"),
        DeviceInfoPair(key = "screen_width", value = "1080"),
        DeviceInfoPair(key = "screen_height", value = "1794"),
        DeviceInfoPair(key = "app_version", value = "4.5.0"),
        DeviceInfoPair(key = "app_build_number", value = "2011050024"),
        DeviceInfoPair(key = "description", value = "Google Pixel 2"),
        DeviceInfoPair(key = "model", value = "Pixel 2"),
        DeviceInfoPair(key = "android_version", value = "30"),
        DeviceInfoPair(key = "android_version", value = "30"),
        DeviceInfoPair(key = "manufacturer", value = "Google")
    )
)

data class PartBoundToken(
    var accessToken: String = "",
    var expiresIn: Int = 0,
    var tokenType: String = "",
    var refreshToken: String = ""
)

data class BoundToken(
    var accessToken: String = "",
    var expiresIn: Int = 0,
    var tokenType: String = "",
    var refreshToken: String = ""
)

data class SignInByOtpResponse(
    var data: Data = Data()
) {
    data class Data(
        var boundToken: BoundToken = BoundToken(),
        var partBoundToken: PartBoundToken = PartBoundToken()
    )
}