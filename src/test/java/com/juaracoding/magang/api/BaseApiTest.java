package com.juaracoding.magang.api;

import com.juaracoding.magang.listeners.TestListener;
import com.juaracoding.magang.reports.ExtentTestManager;
import com.juaracoding.magang.utils.Constants;
import com.juaracoding.magang.utils.SecurityUtil;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Kelas induk untuk seluruh pengujian API (REST Assured) pada backend FeathersJS HADIR
 * serta rute Next.js BFF (magang.dikahadir.com/api/*).
 * Mengaktifkan relaxedHTTPSValidation untuk sertifikat TLS staging dan menyediakan
 * utilitas autentikasi JWT (POST /authentication) maupun sesi cookie BFF (POST /api/login).
 */
@Listeners(TestListener.class)
public abstract class BaseApiTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected static RequestSpecification defaultRequestSpec;

    @BeforeClass(alwaysRun = true)
    public void setupApiEngine() {
        defaultRequestSpec = buildDefaultRequestSpec();
        logger.info("REST Assured initialized with API Base URI: {}", Constants.API_BASE_URI);
    }

    /**
     * Membangun RequestSpecification standar FeathersJS dengan relaxedHTTPSValidation agar dapat
     * dipanggil baik dari subclass BaseApiTest maupun helper statis Hybrid @Before.
     *
     * @return RequestSpecification terkonfigurasi untuk API_BASE_URI
     */
    public static RequestSpecification buildDefaultRequestSpec() {
        RestAssured.baseURI = Constants.API_BASE_URI;
        RestAssured.useRelaxedHTTPSValidation();

        return new RequestSpecBuilder()
                .setBaseUri(Constants.API_BASE_URI)
                .setRelaxedHTTPSValidation()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.METHOD)
                .log(LogDetail.URI)
                .build();
    }

    /**
     * Membangun RequestSpecification khusus rute Next.js BFF pada BASE_URL (magang.dikahadir.com).
     *
     * @return RequestSpecification terkonfigurasi untuk BASE_URL
     */
    public static RequestSpecification buildBffRequestSpec() {
        RestAssured.useRelaxedHTTPSValidation();

        return new RequestSpecBuilder()
                .setBaseUri(Constants.BASE_URL)
                .setRelaxedHTTPSValidation()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.METHOD)
                .log(LogDetail.URI)
                .build();
    }

    /**
     * Melakukan login ke POST /authentication menggunakan kredensial Base64 dan mengembalikan
     * response lengkap (berisi accessToken dan data user).
     *
     * @param email           email akun
     * @param encodedPassword password ter-encode Base64
     * @return Response REST Assured
     */
    public static Response authenticate(String email, String encodedPassword) {
        String plainPassword = SecurityUtil.decode(encodedPassword);

        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", plainPassword);
        payload.put("strategy", "local");

        return given()
                .spec(buildDefaultRequestSpec())
                .body(payload)
        .when()
                .post(Constants.API_AUTH_ENDPOINT)
        .then()
                .extract()
                .response();
    }

    /**
     * Melakukan login ke Next.js BFF (POST https://magang.dikahadir.com/api/login) menggunakan
     * kredensial Base64 dan mengembalikan nilai cookie sesi hadir_app_magang.
     *
     * @param email           email akun
     * @param encodedPassword password ter-encode Base64
     * @return nilai cookie sesi hadir_app_magang atau null jika gagal
     */
    public static String authenticateBffSessionCookie(String email, String encodedPassword) {
        String plainPassword = SecurityUtil.decode(encodedPassword);

        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", plainPassword);

        Response response = given()
                .spec(buildBffRequestSpec())
                .body(payload)
        .when()
                .post(Constants.BFF_LOGIN_ENDPOINT)
        .then()
                .extract()
                .response();

        return response.getCookie(Constants.BFF_SESSION_COOKIE_NAME);
    }

    /**
     * Melakukan login FeathersJS dan mengekstrak JWT accessToken.
     *
     * @param email           email akun
     * @param encodedPassword password ter-encode Base64
     * @return JWT accessToken atau null jika gagal
     */
    protected String loginAndGetToken(String email, String encodedPassword) {
        Response response = authenticate(email, encodedPassword);
        int statusCode = response.getStatusCode();
        logger.info("Authentication response for [{}]: HTTP {}", email, statusCode);

        if (statusCode == 200 || statusCode == 201) {
            String token = response.jsonPath().getString("accessToken");
            if (token == null) {
                token = response.jsonPath().getString("token");
            }
            ExtentTestManager.logInfo("API login succeeded for: " + email);
            return token;
        }

        logger.warn("API login failed for [{}]: HTTP {} - {}", email, statusCode, response.getBody().asString());
        return null;
    }

    /**
     * Membentuk RequestSpecification FeathersJS dengan header Authorization: Bearer &lt;token&gt;.
     *
     * @param token JWT accessToken
     * @return RequestSpecification terautentikasi
     */
    protected RequestSpecification getAuthenticatedSpec(String token) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .addRequestSpecification(defaultRequestSpec != null ? defaultRequestSpec : buildDefaultRequestSpec());
        if (token != null && !token.trim().isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token.trim());
        }
        return builder.build();
    }

    /**
     * Membentuk RequestSpecification Next.js BFF dengan cookie sesi hadir_app_magang.
     *
     * @param sessionCookie nilai cookie sesi BFF
     * @return RequestSpecification BFF terautentikasi
     */
    protected RequestSpecification getBffAuthenticatedSpec(String sessionCookie) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .addRequestSpecification(buildBffRequestSpec());
        if (sessionCookie != null && !sessionCookie.trim().isEmpty()) {
            builder.addCookie(Constants.BFF_SESSION_COOKIE_NAME, sessionCookie.trim());
        }
        return builder.build();
    }
}
