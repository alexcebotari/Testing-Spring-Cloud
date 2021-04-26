package com.example.demo

import com.google.gson.Gson
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

            authenticationToken = response.data.accessToken
        }
    }

    private var restTemplate: TestRestTemplate = TestRestTemplate()

    @Test
    fun should_get_hello_string_correctly() {
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
            },
            LoggingRequestInterceptor()
        )
        val response = restTemplate.getForObject(
            "https://portal-mobile.uat-newdaycards.com/api/v6/notification?interstitial=true&banner=true",
            Any::class.java
        )
        val responseConcrete = response as? DummyServiceResponse
        val z = 23 * 32
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
    data class Body(var accessToken: String = "")
}