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

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

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
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(BaseApiTest.class);

    /**
     * Mengeksekusi pemanggilan REST Assured dengan retry aman (maks 3x) khusus saat terjadi
     * gangguan koneksi TCP/timeout sementara pada server staging (tidak memengaruhi error HTTP 4xx/5xx).
     */
    public static Response executeWithConnectRetry(Supplier<Response> requestCall, String operationName) {
        RuntimeException lastEx = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return requestCall.get();
            } catch (RuntimeException ex) {
                if (isTransientNetworkException(ex) && attempt < 3) {
                    lastEx = ex;
                    STATIC_LOGGER.warn("Transient network error on [{}] (attempt {}/3): {}, retrying in 2s...",
                            operationName, attempt, ex.getMessage());
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                    continue;
                }
                throw ex;
            }
        }
        throw lastEx != null ? lastEx : new IllegalStateException("Retry exhausted for " + operationName);
    }

    private static boolean isTransientNetworkException(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof ConnectException || cur instanceof SocketTimeoutException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    public static Response authenticate(String email, String encodedPassword) {
        String plainPassword = SecurityUtil.decode(encodedPassword);

        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", plainPassword);
        payload.put("strategy", "local");

        return executeWithConnectRetry(() -> given()
                .spec(buildDefaultRequestSpec())
                .body(payload)
        .when()
                .post(Constants.API_AUTH_ENDPOINT)
        .then()
                .extract()
                .response(), "POST /authentication (" + email + ")");
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
        payload.put("strategy", "local");
        payload.put("email", email);
        payload.put("password", plainPassword);

        Response response = executeWithConnectRetry(() -> given()
                .spec(buildBffRequestSpec())
                .body(payload)
        .when()
                .post(Constants.BFF_LOGIN_ENDPOINT)
        .then()
                .extract()
                .response(), "POST /api/login (" + email + ")");

        STATIC_LOGGER.info("BFF login for [{}] -> HTTP {}", email, response.getStatusCode());
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

    /**
     * RequestSpecification tanpa Authorization header untuk pengujian unauthenticated FeathersJS.
     */
    protected RequestSpecification unauthenticatedJsonSpec() {
        return buildDefaultRequestSpec();
    }

    /**
     * RequestSpecification tanpa cookie sesi untuk pengujian unauthenticated Next.js BFF.
     */
    protected RequestSpecification unauthenticatedBffSpec() {
        return buildBffRequestSpec();
    }

    /**
     * Mengubah segmen signature JWT valid untuk menguji penolakan Tampered JWT Signature (HTTP 401).
     */
    protected String tamperJwtSignature(String validJwt) {
        if (validJwt == null || !validJwt.contains(".")) {
            return "invalid.jwt.signature";
        }
        String[] parts = validJwt.split("\\.");
        if (parts.length != 3) {
            return validJwt + "tampered";
        }
        String sig = parts[2];
        char flipped = sig.endsWith("A") ? 'B' : 'A';
        String tamperedSig = (sig.length() > 1 ? sig.substring(0, sig.length() - 1) : "") + flipped;
        return parts[0] + "." + parts[1] + "." + tamperedSig;
    }
}
