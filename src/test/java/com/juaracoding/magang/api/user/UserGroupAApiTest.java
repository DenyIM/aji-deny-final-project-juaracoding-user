package com.juaracoding.magang.api.user;

import com.juaracoding.magang.api.BaseApiTest;
import com.juaracoding.magang.api.helper.AdminApiHelper;
import com.juaracoding.magang.reports.ExtentTestManager;
import com.juaracoding.magang.utils.Constants;
import com.juaracoding.magang.utils.SecurityUtil;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;

/**
 * Pengujian Otomatisasi REST Assured API — Portal Karyawan Group A (Tester
 * Deny).
 * Mencakup 71 Test Cases (SIT v5.1):
 * - 55 Mandatory SIT API TCs: SIT_USR_API_001–018, 044–049, 053–083
 * - 16 Critical/High Security & Live Endpoint Enrichment TCs:
 * SIT_USR_API_117–132
 * Branch: feature/deny-user-api-a
 */
public class UserGroupAApiTest extends BaseApiTest {

        // Base64 encoded test password for disposable users (zero plaintext rule)
        private static final String TEMP_NEW_PASSWORD_ENCODED = "TmV3UGFzc18yMDI2IQ==";

        private record CleanupRecord(String endpoint, String id) {
        }

        private String adminToken;
        private String adminUserId;
        private String defaultUserToken;
        private String defaultUserId;
        private String defaultUserBffCookie;
        private String companyId;

        private String userAId;
        private String userAEmail;
        private String userAToken;
        private String userACookie;

        private String userBId;
        private String userBEmail;
        private String userBToken;

        private String availablePoolNik;
        private String preferredLeaveTypeId;
        private String validPhotoId;

        private String userBOwnOffPermitId;
        private String userBOwnLatePermitId;
        private String userBOwnEarlyLeaveId;
        private String userBOwnLeaveId;

        private final Set<String> disposableUserEmails = ConcurrentHashMap.newKeySet();
        private final Set<CleanupRecord> disposableRecords = ConcurrentHashMap.newKeySet();

        @BeforeClass(alwaysRun = true)
        public void initGroupAApiContext() {
                setupApiEngine();

                AdminApiHelper.ApiSession adminSession = AdminApiHelper.loginAsHybridAdmin();
                adminToken = adminSession.token();
                adminUserId = adminSession.userId();

                AdminApiHelper.ApiSession defaultSession = AdminApiHelper.loginAsDefaultUser();
                defaultUserToken = defaultSession.token();
                defaultUserId = defaultSession.userId();
                defaultUserBffCookie = authenticateBffSessionCookie(Constants.USER_EMAIL,
                                Constants.USER_PASSWORD_ENCODED);

                Response profileRes = sendFeathers(defaultUserToken, "GET",
                                Constants.API_USERS_ENDPOINT + "?email=" + Constants.USER_EMAIL, null);
                if (profileRes.getStatusCode() == 200) {
                        companyId = profileRes.jsonPath().getString("data[0].company_id");
                }
                if (companyId == null || companyId.isBlank()) {
                        companyId = "72aafd87-9909-49bc-9e7c-5e6d5c41f437";
                }

                availablePoolNik = AdminApiHelper.findAvailableNikFromPool();
                preferredLeaveTypeId = AdminApiHelper.getPreferredLeaveTypeId(defaultUserToken);
                validPhotoId = AdminApiHelper.uploadTestPhoto(defaultUserToken, "valid_photo.jpg");

                // Provision 2 isolated disposable users (User A & User B) for destructive &
                // IDOR checks
                long ts = System.currentTimeMillis();
                userAEmail = "api_deny_a_" + ts + "@mail.com";
                userAId = provisionDisposableUser(availablePoolNik, "SQA API Deny A", userAEmail, 12);
                AdminApiHelper.ApiSession sessA = AdminApiHelper.loginAsUser(userAEmail,
                                Constants.USER_PASSWORD_ENCODED);
                userAToken = sessA.token();
                userACookie = authenticateBffSessionCookie(userAEmail, Constants.USER_PASSWORD_ENCODED);

                userBEmail = "api_deny_b_" + (ts + 1) + "@mail.com";
                userBId = provisionDisposableUser(availablePoolNik, "SQA API Deny B", userBEmail, 12);
                AdminApiHelper.ApiSession sessB = AdminApiHelper.loginAsUser(userBEmail,
                                Constants.USER_PASSWORD_ENCODED);
                userBToken = sessB.token();

                // Seed User B records for cross-user IDOR checks
                userBOwnOffPermitId = AdminApiHelper.createPendingOffPermit(
                                userBToken, AdminApiHelper.futureDateYmd(18), "IDOR_Off_Seed_" + ts);
                trackRecord(Constants.API_OFF_PERMIT_ENDPOINT, userBOwnOffPermitId);

                userBOwnLatePermitId = AdminApiHelper.createPendingLatePermit(
                                userBToken, userBId, AdminApiHelper.isoUtcTimestamp(19, 2), "IDOR_Late_Seed_" + ts);
                trackRecord(Constants.API_LATE_PERMIT_ENDPOINT, userBOwnLatePermitId);

                Response earlyB = sendFeathers(userBToken, "POST", Constants.API_EARLY_LEAVE_ENDPOINT, Map.of(
                                "user_id", userBId,
                                "early_time_out", AdminApiHelper.isoUtcTimestamp(20, 8),
                                "early_leave_reason", "IDOR_Early_Seed_" + ts,
                                "time_zone", "+07:00"));
                if (earlyB.getStatusCode() == 200 || earlyB.getStatusCode() == 201) {
                        userBOwnEarlyLeaveId = earlyB.jsonPath().getString("id");
                        trackRecord(Constants.API_EARLY_LEAVE_ENDPOINT, userBOwnEarlyLeaveId);
                }

                userBOwnLeaveId = AdminApiHelper.createPendingLeave(
                                userBEmail, Constants.USER_PASSWORD_ENCODED, preferredLeaveTypeId,
                                AdminApiHelper.futureDateYmd(21), AdminApiHelper.futureDateYmd(22),
                                "IDOR_Leave_Seed_" + ts);
                trackRecord(Constants.API_REQUEST_LEAVE_ENDPOINT, userBOwnLeaveId);

                logger.info("UserGroupAApiTest ready -> defaultUserId={}, userA={}, userB={}, companyId={}",
                                defaultUserId, userAId, userBId, companyId);
        }

        @AfterClass(alwaysRun = true)
        public void cleanupGroupAApiResources() {
                for (CleanupRecord rec : disposableRecords) {
                        AdminApiHelper.cleanupRecordQuietly(rec.endpoint(), rec.id());
                }
                disposableRecords.clear();
                for (String email : disposableUserEmails) {
                        AdminApiHelper.cleanupUserByEmailQuietly(email);
                }
                disposableUserEmails.clear();
        }

        // =========================================================================
        // INTERNAL DRY HELPERS
        // =========================================================================

        private String provisionDisposableUser(String nik, String fullname, String email, int leaveAmount) {
                String uid = AdminApiHelper.createSelfRegisteredMobileUser(
                                nik, fullname, email, Constants.USER_PASSWORD_ENCODED);
                Assert.assertNotNull(uid, "Disposable user provisioning gagal untuk: " + email);
                disposableUserEmails.add(email);
                boolean ok = AdminApiHelper.activateUserWithAdminUplinerAndLeave(uid, leaveAmount);
                Assert.assertTrue(ok, "Aktivasi disposable user gagal untuk: " + uid);
                return uid;
        }

        private void trackRecord(String endpoint, String id) {
                if (id != null && !id.isBlank()) {
                        disposableRecords.add(new CleanupRecord(endpoint, id));
                }
        }

        private Response sendFeathers(String bearerToken, String method, String path, Object body) {
                return executeWithConnectRetry(() -> {
                        RequestSpecification spec = (bearerToken != null && !bearerToken.isBlank())
                                        ? getAuthenticatedSpec(bearerToken)
                                        : unauthenticatedJsonSpec();
                        if (body != null) {
                                spec = spec.body(body);
                        }
                        return switch (method.toUpperCase()) {
                                case "GET" -> given().spec(spec).when().get(path).then().extract().response();
                                case "POST" -> given().spec(spec).when().post(path).then().extract().response();
                                case "PATCH" -> given().spec(spec).when().patch(path).then().extract().response();
                                case "DELETE" -> given().spec(spec).when().delete(path).then().extract().response();
                                default -> throw new IllegalArgumentException("Unsupported method: " + method);
                        };
                }, method + " " + path);
        }

        private Response sendBff(String cookie, String method, String path, Object body) {
                return executeWithConnectRetry(() -> {
                        RequestSpecification spec = (cookie != null && !cookie.isBlank())
                                        ? getBffAuthenticatedSpec(cookie)
                                        : unauthenticatedBffSpec();
                        if (body != null) {
                                spec = spec.body(body);
                        }
                        return switch (method.toUpperCase()) {
                                case "GET" -> given().spec(spec).when().get(path).then().extract().response();
                                case "POST" -> given().spec(spec).when().post(path).then().extract().response();
                                case "PATCH" -> given().spec(spec).when().patch(path).then().extract().response();
                                case "DELETE" -> given().spec(spec).when().delete(path).then().extract().response();
                                default -> throw new IllegalArgumentException("Unsupported method: " + method);
                        };
                }, "BFF " + method + " " + path);
        }

        private File testDataFile(String fileName) {
                Path p = Paths.get(Constants.TESTDATA_DIR, fileName).toAbsolutePath().normalize();
                Assert.assertTrue(Files.exists(p), "Fixture testdata wajib ada: " + p);
                return p.toFile();
        }

        private String safeBodyPreview(Response res) {
                try {
                        String raw = res.getBody() != null ? res.getBody().asString() : "";
                        String compact = raw.replaceAll("\\s+", " ").trim();
                        return compact.length() > 240 ? compact.substring(0, 240) + "..." : compact;
                } catch (Exception e) {
                        return "<binary>";
                }
        }

        private void assertRejectedAuthOrForbidden(Response res, String ctx) {
                int code = res.getStatusCode();
                Assert.assertTrue(code == 401 || code == 403 || code == 404,
                                ctx + " seharusnya ditolak (401/403/404), aktual HTTP " + code + " | "
                                                + safeBodyPreview(res));
        }

        private void assertRejectedClientError(Response res, String ctx) {
                int code = res.getStatusCode();
                Assert.assertTrue(code >= 400 && code < 500,
                                ctx + " seharusnya ditolak (4xx), aktual HTTP " + code + " | " + safeBodyPreview(res));
        }

        // =========================================================================
        // BLOCK 1: AUTH, JWT SECURITY, RESET PASSWORD & BFF LOGIN (15 TCs)
        // SIT_USR_API_001–010, 081–082, 117–119
        // =========================================================================

        @Test(priority = 1, description = "SIT_USR_API_001: Login User Magang dengan kredensial valid (hadirsqa1@gmail.com)", groups = {
                        "smoke", "regression", "positive" })
        public void testSIT_USR_API_001_ValidUserLoginReturnsJwt() {
                Response res = authenticate(Constants.USER_EMAIL, Constants.USER_PASSWORD_ENCODED);
                int status = res.getStatusCode();
                ExtentTestManager.logInfo("SIT_USR_API_001 [Medium|P2] POST /authentication -> HTTP " + status);

                Assert.assertTrue(status == 200 || status == 201, "Login user valid harus 200/201, aktual: " + status);
                String token = res.jsonPath().getString("accessToken");
                Assert.assertNotNull(token, "accessToken tidak boleh null.");
                Assert.assertEquals(token.split("\\.").length, 3, "JWT accessToken wajib terdiri dari 3 segmen.");
        }

        @Test(priority = 2, description = "SIT_USR_API_002: Login User Magang dengan password salah", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_002_LoginWrongPasswordRejected() {
                Response res = sendFeathers(null, "POST", Constants.API_AUTH_ENDPOINT, Map.of(
                                "email", Constants.USER_EMAIL,
                                "password", "WrongPass_Invalid999!",
                                "strategy", "local"));
                int status = res.getStatusCode();
                ExtentTestManager.logInfo(
                                "SIT_USR_API_002 [Medium|P2] POST /authentication wrong pass -> HTTP " + status);

                Assert.assertTrue(status == 401 || status == 400 || status == 500,
                                "Login password salah wajib ditolak, aktual: " + status);
                Assert.assertNull(res.jsonPath().getString("accessToken"), "accessToken tidak boleh diterbitkan.");
        }

        @Test(priority = 3, description = "SIT_USR_API_003: Login User Magang dengan format email tidak valid", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_003_LoginInvalidEmailFormatRejected() {
                Response res = sendFeathers(null, "POST", Constants.API_AUTH_ENDPOINT, Map.of(
                                "email", "bukan_email_valid",
                                "password", "AnyPass123!",
                                "strategy", "local"));
                int status = res.getStatusCode();
                ExtentTestManager.logInfo(
                                "SIT_USR_API_003 [Medium|P2] POST /authentication invalid email -> HTTP " + status);

                Assert.assertTrue(status >= 400, "Format email tidak valid wajib ditolak, aktual: " + status);
                Assert.assertNull(res.jsonPath().getString("accessToken"));
        }

        @Test(priority = 4, description = "SIT_USR_API_004: Login User Magang dengan field email kosong", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_004_LoginEmptyEmailRejected() {
                Response res = sendFeathers(null, "POST", Constants.API_AUTH_ENDPOINT, Map.of(
                                "email", "",
                                "password", SecurityUtil.decode(Constants.USER_PASSWORD_ENCODED),
                                "strategy", "local"));
                ExtentTestManager.logInfo("SIT_USR_API_004 [Medium|P2] POST /authentication empty email -> HTTP "
                                + res.getStatusCode());

                Assert.assertTrue(res.getStatusCode() >= 400,
                                "Email kosong wajib ditolak, aktual: " + res.getStatusCode());
                Assert.assertNull(res.jsonPath().getString("accessToken"));
        }

        @Test(priority = 5, description = "SIT_USR_API_005: Login User Magang dengan field password kosong", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_005_LoginEmptyPasswordRejected() {
                Response res = sendFeathers(null, "POST", Constants.API_AUTH_ENDPOINT, Map.of(
                                "email", Constants.USER_EMAIL,
                                "password", "",
                                "strategy", "local"));
                ExtentTestManager.logInfo("SIT_USR_API_005 [Medium|P2] POST /authentication empty password -> HTTP "
                                + res.getStatusCode());

                Assert.assertTrue(res.getStatusCode() >= 400,
                                "Password kosong wajib ditolak, aktual: " + res.getStatusCode());
                Assert.assertNull(res.jsonPath().getString("accessToken"));
        }

        @Test(priority = 6, description = "SIT_USR_API_006: Login menggunakan akun User yang telah dinonaktifkan (Suspended)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_006_LoginSuspendedAccountRejected() {
                String suspEmail = "api_susp_" + System.currentTimeMillis() + "@mail.com";
                String suspId = provisionDisposableUser(availablePoolNik, "SQA Suspended", suspEmail, -1);
                Assert.assertTrue(AdminApiHelper.updateUserActiveStatusAsAdmin(suspId, false),
                                "Prasyarat nonaktifkan user uji harus berhasil.");

                Response res = authenticate(suspEmail, Constants.USER_PASSWORD_ENCODED);
                ExtentTestManager.logInfo("SIT_USR_API_006 [Medium|P2] Login akun is_active=false -> HTTP "
                                + res.getStatusCode());

                Assert.assertTrue(
                                res.getStatusCode() == 401 || res.getStatusCode() == 403 || res.getStatusCode() == 409,
                                "Akun non-aktif wajib ditolak login (401/403/409), aktual HTTP " + res.getStatusCode());
                Assert.assertNull(res.jsonPath().getString("accessToken"),
                                "accessToken tidak boleh diterbitkan untuk akun suspended.");
        }

        @Test(priority = 7, description = "SIT_USR_API_007: Request reset password melalui API dengan email terdaftar", groups = {
                        "regression", "positive" })
        public void testSIT_USR_API_007_ResetPasswordRequestRegisteredEmail() {
                Response res = sendFeathers(null, "POST", Constants.API_AUTH_MANAGEMENT_ENDPOINT, Map.of(
                                "action", "sendResetPwd",
                                "value", Map.of("email", userAEmail)));
                int status = res.getStatusCode();
                ExtentTestManager.logInfo(
                                "SIT_USR_API_007 [Low|P3] POST /auth-management sendResetPwd (registered) -> HTTP "
                                                + status + " body=" + safeBodyPreview(res));

                Assert.assertTrue(status == 200 || status == 201,
                                "Request reset password email terdaftar harus 200/201, aktual: " + status);
        }

        @Test(priority = 8, description = "SIT_USR_API_008: Request reset password melalui API dengan email tidak terdaftar", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_008_ResetPasswordRequestUnregisteredEmailRejected() {
                Response res = sendFeathers(null, "POST", Constants.API_AUTH_MANAGEMENT_ENDPOINT, Map.of(
                                "action", "sendResetPwd",
                                "value",
                                Map.of("email", "unregistered_api_999_" + System.currentTimeMillis() + "@hadir.com")));
                ExtentTestManager.logInfo(
                                "SIT_USR_API_008 [Low|P3] POST /auth-management sendResetPwd (unregistered) -> HTTP "
                                                + res.getStatusCode() + " body=" + safeBodyPreview(res));

                Assert.assertTrue(res.getStatusCode() >= 400,
                                "Email tidak terdaftar harus ditolak server (4xx), aktual: " + res.getStatusCode());
        }

        @Test(priority = 9, description = "SIT_USR_API_009: Submit password baru menggunakan token reset tidak valid", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_009_ResetPasswordWithInvalidTokenRejected() {
                Response res = sendFeathers(null, "POST", Constants.API_AUTH_MANAGEMENT_ENDPOINT, Map.of(
                                "action", "resetPwdShort",
                                "value", Map.of(
                                                "user", Map.of("email", userAEmail),
                                                "token", "invalid_reset_token_xyz",
                                                "password", SecurityUtil.decode(TEMP_NEW_PASSWORD_ENCODED))));
                ExtentTestManager.logInfo(
                                "SIT_USR_API_009 [Low|P3] POST /auth-management resetPwdShort invalid token -> HTTP "
                                                + res.getStatusCode());

                assertRejectedClientError(res, "Reset password token invalid");
        }

        @Test(priority = 10, description = "SIT_USR_API_010: Verifikasi klaim userId dan role di dalam payload JWT User Magang", groups = {
                        "regression", "positive" })
        public void testSIT_USR_API_010_VerifyJwtPayloadClaims() {
                String[] segments = defaultUserToken.split("\\.");
                Assert.assertEquals(segments.length, 3, "Token JWT harus punya header.payload.signature.");

                String payloadJson = new String(Base64.getUrlDecoder().decode(segments[1]), StandardCharsets.UTF_8);
                ExtentTestManager.logInfo("SIT_USR_API_010 [Medium|P2] Decoded JWT payload: " + payloadJson);

                Assert.assertTrue(payloadJson.contains(defaultUserId) || payloadJson.contains("sub"),
                                "Payload JWT wajib memuat userId / klaim sub. Aktual: " + payloadJson);
                Assert.assertTrue(payloadJson.contains("iat") && payloadJson.contains("exp"),
                                "Payload JWT wajib memuat timestamp iat dan exp. Aktual: " + payloadJson);
        }

        @Test(priority = 81, description = "SIT_USR_API_081: Tolak verifikasi reset password dengan kode OTP salah melalui POST /auth-management", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_081_ResetPasswordWrongOtpRejected() {
                Response res = sendFeathers(null, "POST", Constants.API_AUTH_MANAGEMENT_ENDPOINT, Map.of(
                                "action", "resetPwdShort",
                                "value", Map.of(
                                                "user", Map.of("email", userAEmail),
                                                "token", "000000",
                                                "password", SecurityUtil.decode(TEMP_NEW_PASSWORD_ENCODED))));
                ExtentTestManager.logInfo("SIT_USR_API_081 [High|P1] POST /auth-management OTP salah -> HTTP "
                                + res.getStatusCode());
                assertRejectedClientError(res, "OTP reset password salah");
        }

        @Test(priority = 82, description = "SIT_USR_API_082: Tolak pemanggilan /auth-management dengan action tidak dikenal ('deleteAllUsers')", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_082_AuthManagementUnknownActionRejected() {
                Response res = sendFeathers(null, "POST", Constants.API_AUTH_MANAGEMENT_ENDPOINT, Map.of(
                                "action", "deleteAllUsers",
                                "value", Map.of()));
                ExtentTestManager.logInfo("SIT_USR_API_082 [Critical|P1] POST /auth-management unknown action -> HTTP "
                                + res.getStatusCode());
                Assert.assertTrue(res.getStatusCode() >= 400,
                                "Action tidak dikenal harus ditolak server, aktual HTTP " + res.getStatusCode());
        }

        @Test(priority = 117, description = "SIT_USR_API_117: Akses endpoint terproteksi menggunakan JWT token dengan signature dimodifikasi (Tampered JWT)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_117_TamperedJwtSignatureRejected() {
                String tampered = tamperJwtSignature(defaultUserToken);
                Response usersRes = sendFeathers(tampered, "GET", Constants.API_USERS_ENDPOINT, null);
                Response reportRes = sendFeathers(tampered, "POST", Constants.API_SHARE_REPORT_ENDPOINT, Map.of(
                                "date_from", AdminApiHelper.futureDateYmd(0),
                                "date_to", AdminApiHelper.futureDateYmd(0),
                                "company_id", companyId));
                ExtentTestManager.logInfo(
                                "SIT_USR_API_117 [Critical|P1] Tampered JWT -> /users=" + usersRes.getStatusCode()
                                                + ", /share-report=" + reportRes.getStatusCode());

                Assert.assertEquals(usersRes.getStatusCode(), 401, "Tampered JWT pada GET /users wajib 401.");
                Assert.assertEquals(reportRes.getStatusCode(), 401, "Tampered JWT pada POST /share-report wajib 401.");
        }

        @Test(priority = 118, description = "SIT_USR_API_118: Kirim payload JSON kosong {}, malformed JSON, atau HTTP GET ke endpoint /authentication", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_118_AuthenticationMalformedEmptyAndWrongMethodRejected() {
                Response emptyRes = sendFeathers(null, "POST", Constants.API_AUTH_ENDPOINT, Map.of());
                Response malformedRes = executeWithConnectRetry(() -> given()
                                .spec(unauthenticatedJsonSpec())
                                .body("{malformed-json")
                                .when().post(Constants.API_AUTH_ENDPOINT)
                                .then().extract().response(), "POST /authentication malformed");
                Response getRes = sendFeathers(null, "GET", Constants.API_AUTH_ENDPOINT, null);

                ExtentTestManager.logInfo("SIT_USR_API_118 [High|P1] /authentication empty=" + emptyRes.getStatusCode()
                                + ", malformed=" + malformedRes.getStatusCode() + ", GET=" + getRes.getStatusCode());

                Assert.assertTrue(emptyRes.getStatusCode() >= 400 && emptyRes.getStatusCode() < 500,
                                "Empty JSON {} harus ditolak 4xx, aktual: " + emptyRes.getStatusCode());
                Assert.assertTrue(malformedRes.getStatusCode() >= 400 && malformedRes.getStatusCode() < 500,
                                "Malformed JSON harus ditolak 4xx, aktual: " + malformedRes.getStatusCode());
                Assert.assertTrue(
                                getRes.getStatusCode() == 404 || getRes.getStatusCode() == 405
                                                || getRes.getStatusCode() == 401,
                                "GET /authentication harus ditolak (404/405/401), aktual: " + getRes.getStatusCode());
        }

        @Test(priority = 119, description = "SIT_USR_API_119: Login Next.js BFF POST /api/login dengan password salah atau email tidak terdaftar", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_119_BffLoginWrongCredentialsRejected() {
                Response res = sendBff(null, "POST", Constants.BFF_LOGIN_ENDPOINT, Map.of(
                                "strategy", "local",
                                "email", Constants.USER_EMAIL,
                                "password", "WrongBffPass_999!"));
                String cookie = res.getCookie(Constants.BFF_SESSION_COOKIE_NAME);
                ExtentTestManager.logInfo("SIT_USR_API_119 [High|P1] BFF POST /api/login wrong pass -> HTTP "
                                + res.getStatusCode() + ", cookie=" + cookie);

                Assert.assertTrue(
                                res.getStatusCode() == 401 || res.getStatusCode() == 403 || res.getStatusCode() == 400,
                                "BFF /api/login kredensial salah wajib ditolak, aktual: " + res.getStatusCode());
                Assert.assertTrue(cookie == null || cookie.isBlank(),
                                "Cookie hadir_web_magang tidak boleh diterbitkan saat BFF login gagal.");
        }

        // =========================================================================
        // BLOCK 2: REGISTRATION & NIK CHECK (/users) (8 TCs)
        // SIT_USR_API_011–018
        // =========================================================================

        @Test(priority = 11, description = "SIT_USR_API_011: Cek ketersediaan NIK valid dari pool Excel (F1240001)", groups = {
                        "regression", "positive" })
        public void testSIT_USR_API_011_CheckAvailableNikFromPool() {
                Response res = sendFeathers(defaultUserToken, "GET",
                                Constants.API_USERS_ENDPOINT + "?nik=" + availablePoolNik, null);
                ExtentTestManager.logInfo("SIT_USR_API_011 [Medium|P2] GET /users?nik=" + availablePoolNik + " -> HTTP "
                                + res.getStatusCode());

                Assert.assertEquals(res.getStatusCode(), 200, "GET /users?nik harus HTTP 200.");
                List<Map<String, Object>> rows = res.jsonPath().getList("data");
                Assert.assertNotNull(rows, "Field 'data' pada respons /users?nik tidak boleh null.");
                for (Map<String, Object> row : rows) {
                        Assert.assertEquals(String.valueOf(row.get("nik")), availablePoolNik,
                                        "Record /users?nik harus sesuai dengan filter NIK yang diminta.");
                }
                ExtentTestManager.logInfo("SIT_USR_API_011 catatan staging: " + rows.size()
                                + " record histori ditemukan untuk pool NIK " + availablePoolNik);
        }

        @Test(priority = 12, description = "SIT_USR_API_012: Cek NIK yang tidak terdaftar di master data (INVALID_NIK_999)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_012_CheckInvalidNikReturnsEmpty() {
                Response res = sendFeathers(defaultUserToken, "GET",
                                Constants.API_USERS_ENDPOINT + "?nik=INVALID_NIK_999", null);
                ExtentTestManager.logInfo("SIT_USR_API_012 [Medium|P2] GET /users?nik=INVALID_NIK_999 -> HTTP "
                                + res.getStatusCode());

                Assert.assertEquals(res.getStatusCode(), 200);
                List<?> rows = res.jsonPath().getList("data");
                Assert.assertTrue(rows == null || rows.isEmpty(),
                                "NIK INVALID_NIK_999 tidak boleh mengembalikan data user.");
        }

        @Test(priority = 13, description = "SIT_USR_API_013: Cek NIK yang sudah diregistrasikan oleh user aktif (D8240010)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_013_CheckAlreadyRegisteredActiveNik() {
                Response res = sendFeathers(defaultUserToken, "GET",
                                Constants.API_USERS_ENDPOINT + "?nik=" + Constants.USER_NIK, null);
                ExtentTestManager.logInfo("SIT_USR_API_013 [Medium|P2] GET /users?nik=" + Constants.USER_NIK
                                + " -> HTTP " + res.getStatusCode());

                Assert.assertEquals(res.getStatusCode(), 200);
                List<String> niks = res.jsonPath().getList("data.nik");
                Assert.assertTrue(niks != null && niks.contains(Constants.USER_NIK),
                                "Query NIK aktif " + Constants.USER_NIK + " wajib menunjukkan akun sudah terdaftar.");
        }

        @Test(priority = 14, description = "SIT_USR_API_014: Pendaftaran mandiri akun baru dengan NIK valid & payload lengkap", groups = {
                        "smoke", "regression", "positive" })
        public void testSIT_USR_API_014_ValidSelfRegistrationSuccess() {
                String regEmail = "api_reg_valid_" + System.currentTimeMillis() + "@mail.com";
                disposableUserEmails.add(regEmail);

                Response res = sendFeathers(null, "POST", Constants.API_USERS_ENDPOINT, Map.of(
                                "nik", availablePoolNik,
                                "fullname", "SQA API SelfReg",
                                "email", regEmail,
                                "password", SecurityUtil.decode(Constants.USER_PASSWORD_ENCODED),
                                "company_code", "DIKA",
                                "is_mobile", true,
                                "photo", validPhotoId != null ? validPhotoId : ""));
                int status = res.getStatusCode();
                ExtentTestManager.logInfo("SIT_USR_API_014 [High|P1] POST /users self-register -> HTTP " + status);

                Assert.assertTrue(status == 200 || status == 201,
                                "Registrasi mandiri payload lengkap harus HTTP 200/201, aktual: " + status + " | "
                                                + safeBodyPreview(res));
                Assert.assertNotNull(res.jsonPath().getString("id"),
                                "Response registrasi harus mengembalikan user id.");
        }

        @Test(priority = 15, description = "SIT_USR_API_015: Pendaftaran mandiri tanpa menyertakan field NIK", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_015_RegisterWithoutNikRejected() {
                String email = "api_reg_nonik_" + System.currentTimeMillis() + "@mail.com";
                disposableUserEmails.add(email);

                Response res = sendFeathers(null, "POST", Constants.API_USERS_ENDPOINT, Map.of(
                                "fullname", "No NIK User",
                                "email", email,
                                "password", SecurityUtil.decode(Constants.USER_PASSWORD_ENCODED),
                                "company_code", "DIKA",
                                "is_mobile", true));
                ExtentTestManager.logInfo(
                                "SIT_USR_API_015 [Medium|P2] POST /users without NIK -> HTTP " + res.getStatusCode());
                assertRejectedClientError(res, "Registrasi tanpa NIK");
        }

        @Test(priority = 16, description = "SIT_USR_API_016: Pendaftaran mandiri menggunakan email yang sudah terpakai", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_016_RegisterDuplicateEmailRejected() {
                Response res = sendFeathers(null, "POST", Constants.API_USERS_ENDPOINT, Map.of(
                                "nik", availablePoolNik,
                                "fullname", "Dup Email User",
                                "email", Constants.USER_EMAIL,
                                "password", SecurityUtil.decode(Constants.USER_PASSWORD_ENCODED),
                                "company_code", "DIKA",
                                "is_mobile", true));
                ExtentTestManager.logInfo("SIT_USR_API_016 [Medium|P2] POST /users duplicate email -> HTTP "
                                + res.getStatusCode());
                Assert.assertTrue(res.getStatusCode() >= 400,
                                "Registrasi email duplikat wajib ditolak, aktual: " + res.getStatusCode());
        }

        @Test(priority = 17, description = "SIT_USR_API_017: Pendaftaran mandiri dengan password terlalu pendek ('12')", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_017_RegisterShortPasswordRejected() {
                String email = "api_reg_short_" + System.currentTimeMillis() + "@mail.com";
                disposableUserEmails.add(email);

                Response res = sendFeathers(null, "POST", Constants.API_USERS_ENDPOINT, Map.of(
                                "nik", availablePoolNik,
                                "fullname", "Short Pass User",
                                "email", email,
                                "password", "12",
                                "company_code", "DIKA",
                                "is_mobile", true));
                ExtentTestManager.logInfo("SIT_USR_API_017 [Medium|P2] POST /users short password -> HTTP "
                                + res.getStatusCode());
                assertRejectedClientError(res, "Registrasi password < min length");
        }

        @Test(priority = 18, description = "SIT_USR_API_018: Pendaftaran mandiri dengan payload XSS script pada field nama", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_018_RegisterXssPayloadInFullname() {
                String xssName = "<script>alert('xss')</script>";
                String email = "api_reg_xss_" + System.currentTimeMillis() + "@mail.com";
                disposableUserEmails.add(email);

                Response res = sendFeathers(null, "POST", Constants.API_USERS_ENDPOINT, Map.of(
                                "nik", availablePoolNik,
                                "fullname", xssName,
                                "email", email,
                                "password", SecurityUtil.decode(Constants.USER_PASSWORD_ENCODED),
                                "company_code", "DIKA",
                                "is_mobile", true));
                int status = res.getStatusCode();
                String returnedName;
                try {
                        returnedName = res.jsonPath().getString("fullname");
                } catch (Exception ignored) {
                        returnedName = safeBodyPreview(res);
                }
                ExtentTestManager.logInfo("SIT_USR_API_018 [Critical|P1] POST /users XSS name -> HTTP "
                                + status + ", returnedName=" + returnedName);

                boolean sanitizedOrRejected = (status >= 400 && status < 500)
                                || ((status == 200 || status == 201)
                                                && (returnedName == null || !returnedName.contains("<script>")));
                Assert.assertTrue(sanitizedOrRejected,
                                "[KNOWN SECURITY] Backend tidak menolak/mensanitasi raw <script> tag di fullname (HTTP "
                                                + status + ", fullname=" + returnedName + ").");
        }

        // =========================================================================
        // BLOCK 3: PERMITS (/off-permit, /late-permit, /early-leave) (14 TCs)
        // SIT_USR_API_044, 056–063, 120–124
        // =========================================================================

        @Test(priority = 44, description = "SIT_USR_API_044: Buat pengajuan Izin Off valid melalui POST /off-permit", groups = {
                        "smoke", "regression", "positive" })
        public void testSIT_USR_API_044_CreateValidOffPermit() {
                Response res = sendFeathers(userAToken, "POST", Constants.API_OFF_PERMIT_ENDPOINT, Map.of(
                                "date_off", AdminApiHelper.futureDateYmd(25),
                                "reason", "Izin Off Valid API " + System.currentTimeMillis()));
                int status = res.getStatusCode();
                if (status == 200 || status == 201) {
                        trackRecord(Constants.API_OFF_PERMIT_ENDPOINT, res.jsonPath().getString("id"));
                }
                ExtentTestManager.logInfo("SIT_USR_API_044 [Medium|P2] POST /off-permit -> HTTP " + status);
                Assert.assertTrue(status == 200 || status == 201,
                                "POST /off-permit valid harus 200/201, aktual: " + status);
        }

        @Test(priority = 56, description = "SIT_USR_API_056: Karyawan mengajukan Izin Terlambat valid melalui POST /late-permit", groups = {
                        "smoke", "regression", "positive" })
        public void testSIT_USR_API_056_CreateValidLatePermit() {
                Response res = sendFeathers(userAToken, "POST", Constants.API_LATE_PERMIT_ENDPOINT, Map.of(
                                "user_id", userAId,
                                "late_time_in", AdminApiHelper.isoUtcTimestamp(25, 2),
                                "late_reason", "Ban bocor API " + System.currentTimeMillis(),
                                "time_zone", "+07:00"));
                int status = res.getStatusCode();
                if (status == 200 || status == 201) {
                        trackRecord(Constants.API_LATE_PERMIT_ENDPOINT, res.jsonPath().getString("id"));
                }
                ExtentTestManager.logInfo("SIT_USR_API_056 [High|P1] POST /late-permit -> HTTP " + status);
                Assert.assertTrue(status == 200 || status == 201,
                                "POST /late-permit valid harus 200/201, aktual: " + status);
        }

        @Test(priority = 57, description = "SIT_USR_API_057: Tolak pengajuan Izin Terlambat tanpa menyertakan alasan/jam keterlambatan", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_057_CreateEmptyLatePermitRejected() {
                Response res = sendFeathers(userAToken, "POST", Constants.API_LATE_PERMIT_ENDPOINT, Map.of(
                                "user_id", userAId,
                                "late_time_in", "",
                                "late_reason", "",
                                "time_zone", "+07:00"));
                if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
                        trackRecord(Constants.API_LATE_PERMIT_ENDPOINT, res.jsonPath().getString("id"));
                }
                ExtentTestManager.logInfo(
                                "SIT_USR_API_057 [High|P2] POST /late-permit empty -> HTTP " + res.getStatusCode());
                assertRejectedClientError(res, "POST /late-permit field kosong");
        }

        @Test(priority = 58, description = "SIT_USR_API_058: Ambil daftar riwayat pengajuan Izin Terlambat milik user login", groups = {
                        "regression", "positive" })
        public void testSIT_USR_API_058_GetOwnLatePermits() {
                Response res = sendFeathers(defaultUserToken, "GET",
                                Constants.API_LATE_PERMIT_ENDPOINT + "?user_id=" + defaultUserId + "&$limit=5", null);
                ExtentTestManager
                                .logInfo("SIT_USR_API_058 [Medium|P2] GET /late-permit -> HTTP " + res.getStatusCode());

                Assert.assertEquals(res.getStatusCode(), 200);
                Assert.assertNotNull(res.jsonPath().get("data"), "Response GET /late-permit wajib memuat array data.");
        }

        @Test(priority = 59, description = "SIT_USR_API_059: Akses GET /late-permit tanpa Bearer Token ditolak server", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_059_GetLatePermitWithoutTokenRejected() {
                Response res = sendFeathers(null, "GET", Constants.API_LATE_PERMIT_ENDPOINT, null);
                ExtentTestManager.logInfo(
                                "SIT_USR_API_059 [High|P1] GET /late-permit no token -> HTTP " + res.getStatusCode());
                Assert.assertEquals(res.getStatusCode(), 401);
        }

        @Test(priority = 60, description = "SIT_USR_API_060: Karyawan mengajukan Izin Pulang Cepat valid melalui POST /early-leave", groups = {
                        "smoke", "regression", "positive" })
        public void testSIT_USR_API_060_CreateValidEarlyLeave() {
                Response res = sendFeathers(userAToken, "POST", Constants.API_EARLY_LEAVE_ENDPOINT, Map.of(
                                "user_id", userAId,
                                "early_time_out", AdminApiHelper.isoUtcTimestamp(25, 8),
                                "early_leave_reason", "Keperluan keluarga API " + System.currentTimeMillis(),
                                "time_zone", "+07:00"));
                int status = res.getStatusCode();
                if (status == 200 || status == 201) {
                        trackRecord(Constants.API_EARLY_LEAVE_ENDPOINT, res.jsonPath().getString("id"));
                }
                ExtentTestManager.logInfo("SIT_USR_API_060 [High|P1] POST /early-leave -> HTTP " + status);
                Assert.assertTrue(status == 200 || status == 201,
                                "POST /early-leave valid harus 200/201, aktual: " + status);
        }

        @Test(priority = 61, description = "SIT_USR_API_061: Tolak pengajuan Izin Pulang Cepat dengan alasan kosong atau format jam invalid", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_061_CreateInvalidEarlyLeaveRejected() {
                Response res = sendFeathers(userAToken, "POST", Constants.API_EARLY_LEAVE_ENDPOINT, Map.of(
                                "user_id", userAId,
                                "early_time_out", "invalid-time",
                                "early_leave_reason", "",
                                "time_zone", "+07:00"));
                if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
                        trackRecord(Constants.API_EARLY_LEAVE_ENDPOINT, res.jsonPath().getString("id"));
                }
                ExtentTestManager.logInfo(
                                "SIT_USR_API_061 [High|P2] POST /early-leave invalid -> HTTP " + res.getStatusCode());
                Assert.assertTrue(res.getStatusCode() >= 400,
                                "Input early-leave invalid wajib ditolak, aktual: " + res.getStatusCode());
        }

        @Test(priority = 62, description = "SIT_USR_API_062: Ambil daftar riwayat pengajuan Izin Pulang Cepat milik user login", groups = {
                        "regression", "positive" })
        public void testSIT_USR_API_062_GetOwnEarlyLeaves() {
                Response res = sendFeathers(defaultUserToken, "GET",
                                Constants.API_EARLY_LEAVE_ENDPOINT + "?user_id=" + defaultUserId + "&$limit=5", null);
                ExtentTestManager
                                .logInfo("SIT_USR_API_062 [Medium|P2] GET /early-leave -> HTTP " + res.getStatusCode());

                Assert.assertEquals(res.getStatusCode(), 200);
                Assert.assertNotNull(res.jsonPath().get("data"));
        }

        @Test(priority = 63, description = "SIT_USR_API_063: Akses GET /early-leave tanpa menyertakan Authorization Bearer Token", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_063_GetEarlyLeaveWithoutTokenRejected() {
                Response res = sendFeathers(null, "GET", Constants.API_EARLY_LEAVE_ENDPOINT, null);
                ExtentTestManager.logInfo(
                                "SIT_USR_API_063 [High|P1] GET /early-leave no token -> HTTP " + res.getStatusCode());
                Assert.assertEquals(res.getStatusCode(), 401);
        }

        @Test(priority = 120, description = "SIT_USR_API_120: Ambil daftar riwayat pengajuan Izin Off milik user login via GET /off-permit", groups = {
                        "regression", "positive" })
        public void testSIT_USR_API_120_GetOwnOffPermits() {
                Response res = sendFeathers(userBToken, "GET",
                                Constants.API_OFF_PERMIT_ENDPOINT + "?user_id=" + userBId + "&$limit=10", null);
                ExtentTestManager.logInfo("SIT_USR_API_120 [High|P1] GET /off-permit -> HTTP " + res.getStatusCode());

                Assert.assertEquals(res.getStatusCode(), 200);
                Assert.assertNotNull(res.jsonPath().get("data"));
        }

        @Test(priority = 121, description = "SIT_USR_API_121: Akses GET /off-permit tanpa Bearer Token & tolak POST /off-permit dengan field wajib kosong", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_121_OffPermitUnauthAndEmptyPayloadRejected() {
                Response unauthGet = sendFeathers(null, "GET", Constants.API_OFF_PERMIT_ENDPOINT, null);
                Response emptyPost = sendFeathers(userAToken, "POST", Constants.API_OFF_PERMIT_ENDPOINT, Map.of(
                                "date_off", "",
                                "reason", ""));
                if (emptyPost.getStatusCode() == 200 || emptyPost.getStatusCode() == 201) {
                        trackRecord(Constants.API_OFF_PERMIT_ENDPOINT, emptyPost.jsonPath().getString("id"));
                }
                ExtentTestManager.logInfo("SIT_USR_API_121 [High|P1] /off-permit noToken=" + unauthGet.getStatusCode()
                                + ", emptyPost=" + emptyPost.getStatusCode());

                Assert.assertEquals(unauthGet.getStatusCode(), 401);
                Assert.assertTrue(emptyPost.getStatusCode() >= 400, "POST /off-permit field kosong wajib ditolak.");
        }

        @Test(priority = 122, description = "SIT_USR_API_122: User A mencoba membaca detail, mengubah, atau menghapus Izin Off milik User B (Cross-User IDOR)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_122_OffPermitCrossUserIdorRejected() {
                Assert.assertNotNull(userBOwnOffPermitId, "Prasyarat seed off-permit User B harus ada.");
                String path = Constants.API_OFF_PERMIT_ENDPOINT + "/" + userBOwnOffPermitId;
                Response getRes = sendFeathers(userAToken, "GET", path, null);
                Response patchRes = sendFeathers(userAToken, "PATCH", path, Map.of("reason", "IDOR Tamper"));
                Response delRes = sendFeathers(userAToken, "DELETE", path, null);

                ExtentTestManager.logInfo("SIT_USR_API_122 [Critical|P1] OffPermit IDOR UserA->UserB: GET="
                                + getRes.getStatusCode() + ", PATCH=" + patchRes.getStatusCode() + ", DELETE="
                                + delRes.getStatusCode());

                assertRejectedAuthOrForbidden(getRes, "IDOR GET /off-permit/{other_id}");
                assertRejectedAuthOrForbidden(patchRes, "IDOR PATCH /off-permit/{other_id}");
                assertRejectedAuthOrForbidden(delRes, "IDOR DELETE /off-permit/{other_id}");
        }

        @Test(priority = 123, description = "SIT_USR_API_123: Karyawan biasa mencoba Self-Approval Izin Terlambat & Pulang Cepat miliknya sendiri via PATCH", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_123_SelfApprovalLateAndEarlyPermitRejected() {
                String ownLateId = AdminApiHelper.createPendingLatePermit(
                                userAToken, userAId, AdminApiHelper.isoUtcTimestamp(26, 2),
                                "SelfApproveLate_" + System.currentTimeMillis());
                trackRecord(Constants.API_LATE_PERMIT_ENDPOINT, ownLateId);
                Assert.assertNotNull(ownLateId, "Seed late-permit User A harus berhasil.");

                Response earlyCreate = sendFeathers(userAToken, "POST", Constants.API_EARLY_LEAVE_ENDPOINT, Map.of(
                                "user_id", userAId,
                                "early_time_out", AdminApiHelper.isoUtcTimestamp(26, 8),
                                "early_leave_reason", "SelfApproveEarly_" + System.currentTimeMillis(),
                                "time_zone", "+07:00"));
                String ownEarlyId = earlyCreate.jsonPath().getString("id");
                trackRecord(Constants.API_EARLY_LEAVE_ENDPOINT, ownEarlyId);
                Assert.assertNotNull(ownEarlyId, "Seed early-leave User A harus berhasil.");

                Response patchLate = sendFeathers(userAToken, "PATCH",
                                Constants.API_LATE_PERMIT_ENDPOINT + "/" + ownLateId, Map.of("is_approved_late", true));
                Response patchEarly = sendFeathers(userAToken, "PATCH",
                                Constants.API_EARLY_LEAVE_ENDPOINT + "/" + ownEarlyId,
                                Map.of("is_approved_early_leave", true));

                ExtentTestManager.logInfo("SIT_USR_API_123 [Critical|P1] Self-Approval: late="
                                + patchLate.getStatusCode() + ", early=" + patchEarly.getStatusCode());

                assertRejectedAuthOrForbidden(patchLate, "Self-Approval PATCH /late-permit/{own_id}");
                assertRejectedAuthOrForbidden(patchEarly, "Self-Approval PATCH /early-leave/{own_id}");
        }

        @Test(priority = 124, description = "SIT_USR_API_124: User A mencoba mengubah atau menghapus pengajuan Izin Terlambat & Pulang Cepat milik User B (IDOR)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_124_LateAndEarlyPermitCrossUserIdorRejected() {
                Assert.assertNotNull(userBOwnLatePermitId);
                Assert.assertNotNull(userBOwnEarlyLeaveId);

                Response patchLate = sendFeathers(userAToken, "PATCH",
                                Constants.API_LATE_PERMIT_ENDPOINT + "/" + userBOwnLatePermitId,
                                Map.of("late_reason", "IDOR"));
                Response delLate = sendFeathers(userAToken, "DELETE",
                                Constants.API_LATE_PERMIT_ENDPOINT + "/" + userBOwnLatePermitId, null);
                Response patchEarly = sendFeathers(userAToken, "PATCH",
                                Constants.API_EARLY_LEAVE_ENDPOINT + "/" + userBOwnEarlyLeaveId,
                                Map.of("early_leave_reason", "IDOR"));
                Response delEarly = sendFeathers(userAToken, "DELETE",
                                Constants.API_EARLY_LEAVE_ENDPOINT + "/" + userBOwnEarlyLeaveId, null);

                ExtentTestManager.logInfo("SIT_USR_API_124 [Critical|P1] IDOR Late/Early: patchLate="
                                + patchLate.getStatusCode()
                                + ", delLate=" + delLate.getStatusCode() + ", patchEarly=" + patchEarly.getStatusCode()
                                + ", delEarly=" + delEarly.getStatusCode());

                assertRejectedAuthOrForbidden(patchLate, "IDOR PATCH /late-permit/{other_id}");
                assertRejectedAuthOrForbidden(delLate, "IDOR DELETE /late-permit/{other_id}");
                assertRejectedAuthOrForbidden(patchEarly, "IDOR PATCH /early-leave/{other_id}");
                assertRejectedAuthOrForbidden(delEarly, "IDOR DELETE /early-leave/{other_id}");
        }

        // =========================================================================
        // BLOCK 4: UPLOAD SECURITY (/upload, /uploads/*) & SICK BFF (12 TCs)
        // SIT_USR_API_048–049, 064–071, 130–131
        // =========================================================================

        @Test(priority = 64, description = "SIT_USR_API_064: Karyawan mengupload gambar bukti surat dokter (< 2 MB) ke POST /upload", groups = {
                        "smoke", "regression", "positive" })
        public void testSIT_USR_API_064_UploadValidImageSuccess() {
                String uploadedId = AdminApiHelper.uploadTestPhoto(userAToken, "valid_photo.jpg");
                ExtentTestManager.logInfo("SIT_USR_API_064 [High|P1] POST /upload valid_photo.jpg -> id=" + uploadedId);
                Assert.assertNotNull(uploadedId, "POST /upload gambar valid <2MB harus mengembalikan ID/path file.");
        }

        @Test(priority = 65, description = "SIT_USR_API_065: Tolak upload file bukti sakit/selfie berukuran > 2 MB sebagai User", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_065_UploadOversizedImageRejected() {
                File oversized = testDataFile("oversized_photo.jpg");
                Response res = executeWithConnectRetry(() -> given()
                                .baseUri(Constants.API_BASE_URI)
                                .relaxedHTTPSValidation()
                                .header("Authorization", "Bearer " + userAToken)
                                .multiPart("uri", oversized, "image/jpeg")
                                .when().post(Constants.API_UPLOAD_ENDPOINT)
                                .then().extract().response(), "POST /upload oversized");
                ExtentTestManager.logInfo("SIT_USR_API_065 [High|P2] POST /upload oversized_photo.jpg ("
                                + oversized.length() + " bytes) -> HTTP " + res.getStatusCode());

                Assert.assertTrue(
                                res.getStatusCode() == 400 || res.getStatusCode() == 413 || res.getStatusCode() == 422,
                                "Upload file >2MB wajib ditolak (400/413/422), aktual HTTP " + res.getStatusCode());
        }

        @Test(priority = 66, description = "SIT_USR_API_066: Tolak upload file non-gambar (.txt/.pdf) untuk foto selfie/dokumen User", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_066_UploadNonImageRejected() {
                File pdfFile = testDataFile("invalid_upload.pdf");
                Response res = executeWithConnectRetry(() -> given()
                                .baseUri(Constants.API_BASE_URI)
                                .relaxedHTTPSValidation()
                                .header("Authorization", "Bearer " + userAToken)
                                .multiPart("uri", pdfFile, "application/pdf")
                                .when().post(Constants.API_UPLOAD_ENDPOINT)
                                .then().extract().response(), "POST /upload pdf");
                ExtentTestManager.logInfo("SIT_USR_API_066 [Critical|P1] POST /upload invalid_upload.pdf -> HTTP "
                                + res.getStatusCode());
                Assert.assertTrue(res.getStatusCode() >= 400,
                                "Upload file .pdf wajib ditolak server, aktual: " + res.getStatusCode());
        }

        @Test(priority = 67, description = "SIT_USR_API_067: Tolak upload file ke POST /upload tanpa Authorization Token", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_067_UploadWithoutTokenRejected() {
                File validImg = testDataFile("valid_photo.jpg");
                Response res = executeWithConnectRetry(() -> given()
                                .baseUri(Constants.API_BASE_URI)
                                .relaxedHTTPSValidation()
                                .multiPart("uri", validImg, "image/jpeg")
                                .when().post(Constants.API_UPLOAD_ENDPOINT)
                                .then().extract().response(), "POST /upload no token");
                ExtentTestManager.logInfo("SIT_USR_API_067 [Critical|P1] POST /upload tanpa token -> HTTP "
                                + res.getStatusCode());

                Assert.assertTrue(res.getStatusCode() == 401 || res.getStatusCode() == 403,
                                "POST /upload tanpa Authorization wajib ditolak 401/403, aktual HTTP "
                                                + res.getStatusCode());
        }

        @Test(priority = 48, description = "SIT_USR_API_048: Buat pengajuan Klaim Sakit beserta referensi dokumen surat dokter via POST /api/sick", groups = {
                        "smoke", "regression", "positive" })
        public void testSIT_USR_API_048_BffCreateSickRequestSuccess() {
                String d = AdminApiHelper.futureDateYmd(15);
                Response res = sendBff(userACookie, "POST", Constants.BFF_SICK_ENDPOINT, Map.of(
                                "request_type", "SAKIT",
                                "sick_request_date_from", d,
                                "sick_request_date_to", d,
                                "time_zone", "+07:00",
                                "user_sick_document", List.of(Map.of("document", validPhotoId))));
                String sickId = AdminApiHelper.findLatestSickActivityId(userAToken, userAId);
                trackRecord(Constants.API_ACTIVITY_ENDPOINT, sickId);
                ExtentTestManager.logInfo("SIT_USR_API_048 [Medium|P2] BFF POST /api/sick -> HTTP "
                                + res.getStatusCode() + ", id=" + sickId);

                Assert.assertTrue(res.getStatusCode() == 200 || res.getStatusCode() == 201,
                                "POST /api/sick valid harus 200/201, aktual: " + res.getStatusCode() + " | "
                                                + safeBodyPreview(res));
        }

        @Test(priority = 49, description = "SIT_USR_API_049: Ambil daftar riwayat pengajuan Sakit milik user login via GET /api/activity/sick", groups = {
                        "regression", "positive" })
        public void testSIT_USR_API_049_BffGetOwnSickHistory() {
                Response res = sendBff(userACookie, "GET",
                                Constants.BFF_ACTIVITY_SICK_ENDPOINT + "?user_id=" + userAId + "&$skip=0", null);
                ExtentTestManager.logInfo(
                                "SIT_USR_API_049 [High|P1] BFF GET /api/activity/sick -> HTTP " + res.getStatusCode());
                Assert.assertEquals(res.getStatusCode(), 200, "GET /api/activity/sick harus HTTP 200.");
        }

        @Test(priority = 68, description = "SIT_USR_API_068: Karyawan mengajukan Klaim Sakit beserta document ID hasil upload /upload", groups = {
                        "regression", "positive" })
        public void testSIT_USR_API_068_BffCreateSickWithFreshUploadId() {
                String docId = AdminApiHelper.uploadTestPhoto(userAToken, "valid_selfie.jpg");
                Assert.assertNotNull(docId, "Upload SKD valid_selfie.jpg harus mengembalikan ID.");
                String d = AdminApiHelper.futureDateYmd(16);

                Response res = sendBff(userACookie, "POST", Constants.BFF_SICK_ENDPOINT, Map.of(
                                "request_type", "SAKIT",
                                "sick_request_date_from", d,
                                "sick_request_date_to", d,
                                "time_zone", "+07:00",
                                "user_sick_document", List.of(Map.of("document", docId))));
                trackRecord(Constants.API_ACTIVITY_ENDPOINT,
                                AdminApiHelper.findLatestSickActivityId(userAToken, userAId));
                ExtentTestManager.logInfo("SIT_USR_API_068 [High|P1] BFF POST /api/sick (fresh docId=" + docId
                                + ") -> HTTP " + res.getStatusCode());

                Assert.assertTrue(res.getStatusCode() == 200 || res.getStatusCode() == 201);
        }

        @Test(priority = 69, description = "SIT_USR_API_069: Tolak pengajuan Sakit jika tanggal selesai lebih awal dari tanggal mulai", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_069_BffSickReversedDateRangeRejected() {
                Response res = sendBff(userACookie, "POST", Constants.BFF_SICK_ENDPOINT, Map.of(
                                "request_type", "SAKIT",
                                "sick_request_date_from", AdminApiHelper.futureDateYmd(20),
                                "sick_request_date_to", AdminApiHelper.futureDateYmd(17),
                                "time_zone", "+07:00",
                                "user_sick_document", List.of(Map.of("document", validPhotoId))));
                ExtentTestManager.logInfo("SIT_USR_API_069 [Medium|P2] BFF POST /api/sick reversed dates -> HTTP "
                                + res.getStatusCode());
                Assert.assertTrue(res.getStatusCode() >= 400,
                                "Tanggal sakit terbalik wajib ditolak server, aktual: " + res.getStatusCode());
        }

        @Test(priority = 70, description = "SIT_USR_API_070: Tolak pengajuan Sakit tanpa melampirkan dokumen surat dokter wajib", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_070_BffSickWithoutDocumentRejected() {
                String d = AdminApiHelper.futureDateYmd(14);
                Response res = sendBff(userACookie, "POST", Constants.BFF_SICK_ENDPOINT, Map.of(
                                "request_type", "SAKIT",
                                "sick_request_date_from", d,
                                "sick_request_date_to", d,
                                "time_zone", "+07:00",
                                "user_sick_document", List.of()));
                if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
                        trackRecord(Constants.API_ACTIVITY_ENDPOINT,
                                        AdminApiHelper.findLatestSickActivityId(userAToken, userAId));
                }
                ExtentTestManager.logInfo("SIT_USR_API_070 [High|P2] BFF POST /api/sick without doc -> HTTP "
                                + res.getStatusCode());
                Assert.assertTrue(res.getStatusCode() >= 400,
                                "[KNOWN DEFECT] Pengajuan sakit tanpa dokumen SKD wajib ditolak (400/422), aktual HTTP "
                                                + res.getStatusCode());
        }

        @Test(priority = 71, description = "SIT_USR_API_071: Verifikasi riwayat pengajuan sakit terisolasi hanya untuk akun user yang login (IDOR Check)", groups = {
                        "regression", "positive" })
        public void testSIT_USR_API_071_BffSickHistoryIsolatedPerUserIdor() {
                Response res = sendBff(userACookie, "GET",
                                Constants.BFF_ACTIVITY_SICK_ENDPOINT + "?user_id=" + userBId + "&$skip=0", null);
                int code = res.getStatusCode();
                List<String> returnedUserIds = (code == 200) ? res.jsonPath().getList("data.user_id") : List.of();
                ExtentTestManager.logInfo(
                                "SIT_USR_API_071 [Critical|P1] BFF GET /api/activity/sick?user_id=userB -> HTTP "
                                                + code + ", userIds=" + returnedUserIds);

                boolean isolated = (code == 401 || code == 403)
                                || (returnedUserIds == null
                                                || returnedUserIds.stream().noneMatch(uid -> userBId.equals(uid)));
                Assert.assertTrue(isolated, "IDOR Check: BFF /api/activity/sick membocorkan data sakit milik User B!");
        }

        @Test(priority = 130, description = "SIT_USR_API_130: Verifikasi aksesibilitas file gambar yang baru diupload via POST /upload pada static host /uploads", groups = {
                        "regression", "positive" })
        public void testSIT_USR_API_130_UploadedAssetAccessibleOnStaticHost() {
                String id = AdminApiHelper.uploadTestPhoto(userAToken, "valid_photo.jpg");
                Assert.assertNotNull(id, "Prasyarat POST /upload harus mengembalikan asset ID/path.");
                int staticStatus = AdminApiHelper.checkStaticUploadStatus(id);
                ExtentTestManager.logInfo("SIT_USR_API_130 [High|P1] Static host check for uploaded asset [" + id
                                + "] -> HTTP " + staticStatus);

                Assert.assertEquals(staticStatus, 200,
                                "[KNOWN DEFECT] Aset hasil upload (/uploads/" + id
                                                + ") wajib dapat diakses (HTTP 200), aktual HTTP " + staticStatus);
        }

        @Test(priority = 131, description = "SIT_USR_API_131: Tolak upload file dengan MIME spoofing (konten HTML/script berkedok image/jpeg) & multipart tanpa field uri", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_131_UploadMimeSpoofingAndMissingFieldRejected() throws Exception {
                byte[] htmlPayload = "<html><script>alert('xss')</script></html>".getBytes(StandardCharsets.UTF_8);
                Response spoofRes = executeWithConnectRetry(() -> given()
                                .baseUri(Constants.API_BASE_URI)
                                .relaxedHTTPSValidation()
                                .header("Authorization", "Bearer " + userAToken)
                                .multiPart("uri", "fake_spoof.jpg", htmlPayload, "image/jpeg")
                                .when().post(Constants.API_UPLOAD_ENDPOINT)
                                .then().extract().response(), "POST /upload MIME spoof");

                Response wrongFieldRes = executeWithConnectRetry(() -> given()
                                .baseUri(Constants.API_BASE_URI)
                                .relaxedHTTPSValidation()
                                .header("Authorization", "Bearer " + userAToken)
                                .multiPart("file", testDataFile("valid_photo.jpg"), "image/jpeg")
                                .when().post(Constants.API_UPLOAD_ENDPOINT)
                                .then().extract().response(), "POST /upload wrong field");

                ExtentTestManager.logInfo("SIT_USR_API_131 [Critical|P1] Spoof HTML-as-JPEG -> HTTP "
                                + spoofRes.getStatusCode() + ", wrong multipart field -> HTTP "
                                + wrongFieldRes.getStatusCode());

                Assert.assertTrue(spoofRes.getStatusCode() >= 400,
                                "[KNOWN SECURITY] Server menerima konten HTML/script yang disamarkan sebagai image/jpeg (HTTP "
                                                + spoofRes.getStatusCode() + ").");
                Assert.assertTrue(wrongFieldRes.getStatusCode() >= 400,
                                "Multipart tanpa field 'uri' wajib ditolak (400/422), aktual HTTP "
                                                + wrongFieldRes.getStatusCode());
        }

        // =========================================================================
        // BLOCK 5: SHARE REPORT EXPORT & IDOR (/share-report) (4 TCs)
        // SIT_USR_API_072–075
        // =========================================================================

        @Test(priority = 72, description = "SIT_USR_API_072: Karyawan mengunduh laporan pribadi (/share-report) dengan rentang tanggal valid", groups = {
                        "smoke", "regression", "positive" })
        public void testSIT_USR_API_072_ShareReportValidExportXlsx() {
                String today = AdminApiHelper.futureDateYmd(0);
                Response res = AdminApiHelper.postShareReport(defaultUserToken, companyId, defaultUserId, today, today);
                int status = res.getStatusCode();
                byte[] bytes = res.asByteArray();
                ExtentTestManager.logInfo("SIT_USR_API_072 [High|P1] POST /share-report -> HTTP "
                                + status + ", bytes=" + (bytes != null ? bytes.length : 0));

                Assert.assertTrue(status == 200 || status == 201,
                                "POST /share-report valid harus 200/201, aktual: " + status);
                Assert.assertTrue(bytes != null && bytes.length > 100 && bytes[0] == 0x50 && bytes[1] == 0x4B,
                                "Response /share-report wajib berupa file binary XLSX valid (magic header 'PK').");
        }

        @Test(priority = 73, description = "SIT_USR_API_073: Tolak request /share-report dengan rentang tanggal terbalik (endDate < startDate)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_073_ShareReportReversedDateRangeRejected() {
                Response res = AdminApiHelper.postShareReport(
                                defaultUserToken, companyId, defaultUserId,
                                AdminApiHelper.futureDateYmd(5), AdminApiHelper.futureDateYmd(0));
                ExtentTestManager.logInfo("SIT_USR_API_073 [Medium|P2] POST /share-report reversed range -> HTTP "
                                + res.getStatusCode());
                Assert.assertTrue(res.getStatusCode() >= 400,
                                "Rentang tanggal terbalik pada /share-report wajib ditolak, aktual: "
                                                + res.getStatusCode());
        }

        @Test(priority = 74, description = "SIT_USR_API_074: Tolak request /share-report tanpa menyertakan Bearer Token", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_074_ShareReportWithoutTokenRejected() {
                String today = AdminApiHelper.futureDateYmd(0);
                Response res = sendFeathers(null, "POST", Constants.API_SHARE_REPORT_ENDPOINT, Map.of(
                                "date_from", today,
                                "date_to", today,
                                "company_id", companyId));
                ExtentTestManager.logInfo(
                                "SIT_USR_API_074 [High|P1] POST /share-report no token -> HTTP " + res.getStatusCode());
                Assert.assertEquals(res.getStatusCode(), 401);
        }

        @Test(priority = 75, description = "SIT_USR_API_075: Karyawan mencoba mengekspor laporan milik karyawan lain via parameter userId (IDOR)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_075_ShareReportOtherUserIdIdorBlocked() {
                String today = AdminApiHelper.futureDateYmd(0);
                Response res = sendFeathers(userAToken, "POST", Constants.API_SHARE_REPORT_ENDPOINT, Map.of(
                                "date_from", today,
                                "date_to", today,
                                "company_id", companyId,
                                "user_id", defaultUserId,
                                "upliner_id", adminUserId));
                int status = res.getStatusCode();
                ExtentTestManager.logInfo(
                                "SIT_USR_API_075 [Critical|P1] POST /share-report UserA targeting defaultUser upliner=admin -> HTTP "
                                                + status + ", bytes="
                                                + (res.asByteArray() != null ? res.asByteArray().length : 0));

                // Non-leader karyawan tidak boleh mengekspor data karyawan di luar scope-nya
                // atau ditolak 403
                Assert.assertTrue(status == 401 || status == 403 || status == 400
                                || (status == 201 && res.asByteArray() != null),
                                "Response /share-report IDOR harus terisolasi atau ditolak.");
                if (status == 200 || status == 201) {
                        Assert.assertNotEquals(userAToken, defaultUserToken);
                        // If non-upliner receives an arbitrary company/other-user report -> flag if
                        // unexpected 200/201 for other upliner_id
                        Assert.assertTrue(status == 401 || status == 403,
                                        "[KNOWN SECURITY] Karyawan biasa (User A) berhasil men-export report menggunakan user_id/upliner_id pihak lain (HTTP "
                                                        + status + ", " + res.asByteArray().length + " bytes).");
                }
        }

        // =========================================================================
        // BLOCK 6: LEAVES, KUOTA, PROFILE IDOR & MASS ASSIGNMENT (18 TCs)
        // SIT_USR_API_045–047, 053–055, 076–080, 083, 125–129, 132
        // =========================================================================

        @Test(priority = 45, description = "SIT_USR_API_045: Buat pengajuan Cuti dengan tipe & rentang tanggal valid melalui POST /api/leave", groups = {
                        "smoke", "regression", "positive" })
        public void testSIT_USR_API_045_BffCreateValidLeaveRequest() {
                Response res = sendBff(userACookie, "POST", Constants.BFF_LEAVE_ENDPOINT, Map.of(
                                "leave_type_id", preferredLeaveTypeId,
                                "leave_date_from", AdminApiHelper.futureDateYmd(30),
                                "leave_date_to", AdminApiHelper.futureDateYmd(31),
                                "time_zone", "+07:00",
                                "notes", "Cuti API Valid " + System.currentTimeMillis()));
                int status = res.getStatusCode();
                if (status == 200 || status == 201) {
                        trackRecord(Constants.API_REQUEST_LEAVE_ENDPOINT, res.jsonPath().getString("id"));
                }
                ExtentTestManager.logInfo("SIT_USR_API_045 [Medium|P2] BFF POST /api/leave -> HTTP " + status);
                Assert.assertTrue(status == 200 || status == 201,
                                "POST /api/leave valid harus 200/201, aktual: " + status);
        }

        @Test(priority = 46, description = "SIT_USR_API_046: Buat pengajuan Cuti dengan leave_date_to lebih kecil dari leave_date_from", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_046_BffLeaveReversedDateRangeRejected() {
                Response res = sendBff(userACookie, "POST", Constants.BFF_LEAVE_ENDPOINT, Map.of(
                                "leave_type_id", preferredLeaveTypeId,
                                "leave_date_from", AdminApiHelper.futureDateYmd(35),
                                "leave_date_to", AdminApiHelper.futureDateYmd(33),
                                "time_zone", "+07:00",
                                "notes", "Tanggal terbalik API"));
                if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
                        trackRecord(Constants.API_REQUEST_LEAVE_ENDPOINT, res.jsonPath().getString("id"));
                }
                ExtentTestManager.logInfo("SIT_USR_API_046 [Medium|P2] BFF POST /api/leave reversed -> HTTP "
                                + res.getStatusCode());
                Assert.assertTrue(res.getStatusCode() >= 400, "Tanggal cuti terbalik wajib ditolak server.");
        }

        @Test(priority = 47, description = "SIT_USR_API_047: Buat pengajuan Cuti melebihi sisa kuota cuti tahunan (60 hari) [KNOWN DEFECT]", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_047_BffLeaveExceedingQuota60Days_KnownDefect() {
                // day_limit=90 (persalinan) dipakai agar melewati validasi tipe cuti & menguji
                // batas sisa_cuti=12
                Response res = sendBff(userACookie, "POST", Constants.BFF_LEAVE_ENDPOINT, Map.of(
                                "leave_type_id", "d69b6b99-047e-4d9d-80e2-20c3967c01b3",
                                "leave_date_from", AdminApiHelper.futureDateYmd(40),
                                "leave_date_to", AdminApiHelper.futureDateYmd(99),
                                "time_zone", "+07:00",
                                "notes", "Quota60Days API " + System.currentTimeMillis()));
                int status = res.getStatusCode();
                if (status == 200 || status == 201) {
                        trackRecord(Constants.API_REQUEST_LEAVE_ENDPOINT, res.jsonPath().getString("id"));
                }
                ExtentTestManager.logInfo(
                                "SIT_USR_API_047 [High|P1 Known Defect] POST /api/leave 60 hari -> HTTP " + status);
                assertRejectedClientError(res,
                                "[KNOWN DEFECT] Sistem wajib menolak pengajuan cuti 60 hari yang melebihi sisa kuota tahunan (12 hari)");
        }

        @Test(priority = 53, description = "SIT_USR_API_053: Ambil detail profil identitas & sisa kuota cuti user yang sedang login", groups = {
                        "smoke", "regression", "positive" })
        public void testSIT_USR_API_053_GetOwnProfileDetails() {
                Response res = sendFeathers(defaultUserToken, "GET",
                                Constants.API_USERS_ENDPOINT + "?email=" + Constants.USER_EMAIL, null);
                ExtentTestManager.logInfo(
                                "SIT_USR_API_053 [Medium|P2] GET /users own profile -> HTTP " + res.getStatusCode());

                Assert.assertEquals(res.getStatusCode(), 200);
                Assert.assertEquals(res.jsonPath().getString("data[0].nik"), Constants.USER_NIK,
                                "Profil user aktif harus mengembalikan NIK " + Constants.USER_NIK);
        }

        @Test(priority = 54, description = "SIT_USR_API_054: Ubah kata sandi user melalui PATCH /users/{id} dengan Bearer Token & password valid", groups = {
                        "regression", "positive" })
        public void testSIT_USR_API_054_ChangeOwnPasswordAndRestore() {
                String tempPwdEmail = "api_pwd_" + System.currentTimeMillis() + "@mail.com";
                String uid = provisionDisposableUser(availablePoolNik, "SQA ChangePwd", tempPwdEmail, -1);
                String tok = AdminApiHelper.loginAsUser(tempPwdEmail, Constants.USER_PASSWORD_ENCODED).token();

                String newPass = SecurityUtil.decode(TEMP_NEW_PASSWORD_ENCODED);
                Response patchRes = sendFeathers(tok, "PATCH", Constants.API_USERS_ENDPOINT + "/" + uid,
                                Map.of("password", newPass));
                ExtentTestManager.logInfo("SIT_USR_API_054 [Medium|P2] PATCH /users/" + uid + " change pwd -> HTTP "
                                + patchRes.getStatusCode());
                Assert.assertEquals(patchRes.getStatusCode(), 200,
                                "Ubah password sendiri dengan token valid harus HTTP 200.");

                // Verifikasi login dengan password baru berhasil, lalu pulihkan ke default
                // password
                Response loginNew = authenticate(tempPwdEmail, TEMP_NEW_PASSWORD_ENCODED);
                Assert.assertTrue(loginNew.getStatusCode() == 200 || loginNew.getStatusCode() == 201,
                                "Login pasca ganti password wajib berhasil dengan password baru.");
                sendFeathers(loginNew.jsonPath().getString("accessToken"), "PATCH",
                                Constants.API_USERS_ENDPOINT + "/" + uid,
                                Map.of("password", SecurityUtil.decode(Constants.USER_PASSWORD_ENCODED)));
        }

        @Test(priority = 55, description = "SIT_USR_API_055: Ubah kata sandi user melalui PATCH /users/{id} tanpa Bearer Token atau payload kosong", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_055_ChangePasswordUnauthenticatedRejected() {
                Response res = sendFeathers(null, "PATCH", Constants.API_USERS_ENDPOINT + "/" + userAId,
                                Map.of("password", ""));
                ExtentTestManager.logInfo("SIT_USR_API_055 [Medium|P2] PATCH /users/{id} no token -> HTTP "
                                + res.getStatusCode());
                Assert.assertTrue(
                                res.getStatusCode() == 401 || res.getStatusCode() == 400 || res.getStatusCode() == 403);
        }

        @Test(priority = 76, description = "SIT_USR_API_076: Ambil informasi kuota cuti aktif user yang sedang login via GET /project-leave", groups = {
                        "smoke", "regression", "positive" })
        public void testSIT_USR_API_076_GetOwnProjectLeaveQuota() {
                Response res = AdminApiHelper.getProjectLeaveSummaryByUserId(userAToken, userAId);
                ExtentTestManager.logInfo("SIT_USR_API_076 [High|P1] GET /project-leave/" + userAId + " -> HTTP "
                                + res.getStatusCode());

                Assert.assertEquals(res.getStatusCode(), 200);
                Assert.assertNotNull(res.jsonPath().get("total_cuti"));
                Assert.assertNotNull(res.jsonPath().get("cuti_available"));
                Assert.assertNotNull(res.jsonPath().get("sisa_cuti"));
        }

        @Test(priority = 77, description = "SIT_USR_API_077: Coba akses kuota cuti milik user lain melalui path ID (IDOR Check)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_077_BffGetOtherUserProjectLeaveIdorRejected() {
                Response res = sendBff(userACookie, "GET", Constants.BFF_PROJECT_LEAVE_ENDPOINT + "/" + userBId, null);
                ExtentTestManager.logInfo("SIT_USR_API_077 [Critical|P1] BFF GET /api/leave/project-leave/" + userBId
                                + " by UserA -> HTTP "
                                + res.getStatusCode() + " body=" + safeBodyPreview(res));
                assertRejectedAuthOrForbidden(res, "[KNOWN SECURITY] IDOR GET /api/leave/project-leave/{other_id}");
        }

        @Test(priority = 78, description = "SIT_USR_API_078: User A mencoba mengganti password / profil akun User B via PATCH /users/{other_id} (Critical IDOR)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_078_PatchOtherUserPasswordIdorRejected() {
                Response res = sendFeathers(userAToken, "PATCH", Constants.API_USERS_ENDPOINT + "/" + userBId,
                                Map.of("password", SecurityUtil.decode(TEMP_NEW_PASSWORD_ENCODED)));
                ExtentTestManager.logInfo("SIT_USR_API_078 [Critical|P1] IDOR PATCH /users/" + userBId
                                + " by UserA -> HTTP " + res.getStatusCode());
                assertRejectedAuthOrForbidden(res, "[KNOWN SECURITY] Critical IDOR PATCH /users/{other_id}");
        }

        @Test(priority = 79, description = "SIT_USR_API_079: User mencoba mengubah role / is_active / leave_amount dirinya sendiri via PATCH (Mass Assignment)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_079_SelfPrivilegeEscalationMassAssignmentBlocked() {
                Response res = sendFeathers(userAToken, "PATCH", Constants.API_USERS_ENDPOINT + "/" + userAId, Map.of(
                                "role", "admin",
                                "is_admin", true,
                                "leave_amount", 999));
                int status = res.getStatusCode();
                ExtentTestManager.logInfo("SIT_USR_API_079 [Critical|P1] Mass Assignment PATCH /users/{own_id} -> HTTP "
                                + status);

                if (status == 200 || status == 201) {
                        Response check = sendFeathers(adminToken, "GET", Constants.API_USERS_ENDPOINT + "/" + userAId,
                                        null);
                        Integer actualLeaveAmount = check.jsonPath().get("leave_amount");
                        Object actualRole = check.jsonPath().get("role");
                        Assert.assertNotEquals(actualLeaveAmount, Integer.valueOf(999),
                                        "[KNOWN SECURITY] Mass Assignment berhasil mengubah leave_amount menjadi 999!");
                        Assert.assertNotEquals(String.valueOf(actualRole), "admin",
                                        "[KNOWN SECURITY] Mass Assignment berhasil menaikkan role menjadi admin!");
                } else {
                        Assert.assertTrue(status == 400 || status == 401 || status == 403,
                                        "Mass Assignment harus ditolak (400/401/403), aktual HTTP " + status);
                }
        }

        @Test(priority = 80, description = "SIT_USR_API_080: Tolak ubah password user dengan panjang password terlalu pendek (< 6 karakter)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_080_ChangePasswordTooShortRejected() {
                Response res = sendFeathers(userAToken, "PATCH", Constants.API_USERS_ENDPOINT + "/" + userAId,
                                Map.of("password", "123"));
                ExtentTestManager.logInfo("SIT_USR_API_080 [Medium|P2] PATCH /users/{own_id} short pwd -> HTTP "
                                + res.getStatusCode());
                assertRejectedClientError(res, "[KNOWN DEFECT] Backend menerima ubah password < 6 karakter ('123')");
        }

        @Test(priority = 83, description = "SIT_USR_API_083: Karyawan biasa mencoba menghapus akunnya sendiri atau akun user lain via DELETE /users/{id}", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_083_EmployeeDeleteAccountRejected() {
                long ts = System.currentTimeMillis();
                String delEmail = "api_del_iso_" + ts + "@mail.com";
                String delUserId = provisionDisposableUser(availablePoolNik, "SQA Delete Isolation", delEmail, 0);
                AdminApiHelper.ApiSession delSess = AdminApiHelper.loginAsUser(delEmail,
                                Constants.USER_PASSWORD_ENCODED);

                String otherEmail = "api_del_target_" + (ts + 1) + "@mail.com";
                String otherTargetUserId = provisionDisposableUser(availablePoolNik, "SQA Delete Target", otherEmail,
                                0);

                Response delSelf = sendFeathers(delSess.token(), "DELETE",
                                Constants.API_USERS_ENDPOINT + "/" + delUserId, null);
                Response delOther = sendFeathers(userAToken, "DELETE",
                                Constants.API_USERS_ENDPOINT + "/" + otherTargetUserId, null);
                ExtentTestManager.logInfo("SIT_USR_API_083 [Critical|P1] DELETE /users own(" + delUserId + ")="
                                + delSelf.getStatusCode() + ", other(" + otherTargetUserId + ")="
                                + delOther.getStatusCode());

                assertRejectedAuthOrForbidden(delSelf, "[KNOWN SECURITY] DELETE /users/{own_id} oleh user biasa");
                assertRejectedAuthOrForbidden(delOther, "[KNOWN SECURITY] DELETE /users/{other_id} oleh user biasa");
        }

        @Test(priority = 125, description = "SIT_USR_API_125: Ambil daftar tipe cuti aktif beserta day_limit melalui GET /leave-type & tolak akses tanpa token", groups = {
                        "regression", "positive" })
        public void testSIT_USR_API_125_GetLeaveTypesAuthenticatedAndRejectUnauth() {
                Response authRes = sendFeathers(defaultUserToken, "GET", Constants.API_LEAVE_TYPE_ENDPOINT, null);
                Response unauthRes = sendFeathers(null, "GET", Constants.API_LEAVE_TYPE_ENDPOINT, null);
                ExtentTestManager.logInfo("SIT_USR_API_125 [High|P1] GET /leave-type auth="
                                + authRes.getStatusCode() + ", noToken=" + unauthRes.getStatusCode());

                Assert.assertEquals(authRes.getStatusCode(), 200);
                List<?> types = authRes.jsonPath().getList("data");
                Assert.assertTrue(types != null && !types.isEmpty(),
                                "Daftar tipe cuti /leave-type tidak boleh kosong.");
                Assert.assertEquals(unauthRes.getStatusCode(), 401);
        }

        @Test(priority = 126, description = "SIT_USR_API_126: User A mencoba membaca detail atau menghapus pengajuan cuti milik User B (IDOR Cuti)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_126_RequestLeaveCrossUserIdorRejected() {
                Assert.assertNotNull(userBOwnLeaveId, "Prasyarat leave ID milik User B harus ada.");
                String path = Constants.API_REQUEST_LEAVE_ENDPOINT + "/" + userBOwnLeaveId;
                Response getRes = sendFeathers(userAToken, "GET", path, null);
                Response delRes = sendFeathers(userAToken, "DELETE", path, null);

                ExtentTestManager.logInfo("SIT_USR_API_126 [Critical|P1] RequestLeave IDOR UserA->UserB: GET="
                                + getRes.getStatusCode() + ", DELETE=" + delRes.getStatusCode());

                assertRejectedAuthOrForbidden(getRes, "IDOR GET /request-leave/{other_id}");
                assertRejectedAuthOrForbidden(delRes, "IDOR DELETE /request-leave/{other_id}");
        }

        @Test(priority = 127, description = "SIT_USR_API_127: Karyawan mencoba Mass Assignment (is_approved=true) saat ajukan cuti & Self-Approval cuti sendiri", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_127_LeaveMassAssignmentAndSelfApprovalBlocked() {
                Response createRes = sendBff(userACookie, "POST", Constants.BFF_LEAVE_ENDPOINT, Map.of(
                                "leave_type_id", preferredLeaveTypeId,
                                "leave_date_from", AdminApiHelper.futureDateYmd(45),
                                "leave_date_to", AdminApiHelper.futureDateYmd(46),
                                "time_zone", "+07:00",
                                "notes", "MassAssignLeave_" + System.currentTimeMillis(),
                                "is_approved", true));
                String createdId = (createRes.getStatusCode() == 200 || createRes.getStatusCode() == 201)
                                ? createRes.jsonPath().getString("id")
                                : null;
                trackRecord(Constants.API_REQUEST_LEAVE_ENDPOINT, createdId);

                if (createdId != null) {
                        Response checkCreated = AdminApiHelper.getRequestLeaveById(createdId);
                        Assert.assertNull(checkCreated.jsonPath().get("is_approved"),
                                        "[KNOWN SECURITY] Mass Assignment membuat pengajuan cuti langsung is_approved="
                                                        + checkCreated.jsonPath().get("is_approved"));

                        Response selfApproveRes = sendFeathers(userAToken, "PATCH",
                                        Constants.API_REQUEST_LEAVE_ENDPOINT + "/" + createdId,
                                        Map.of("is_approved", true));
                        ExtentTestManager.logInfo("SIT_USR_API_127 [Critical|P1] Leave Self-Approval -> HTTP "
                                        + selfApproveRes.getStatusCode());
                        assertRejectedAuthOrForbidden(selfApproveRes, "Self-Approval PATCH /request-leave/{own_id}");
                }
        }

        @Test(priority = 128, description = "SIT_USR_API_128: Karyawan biasa non-admin mencoba membatalkan transaksi cuti via POST /api/cancel/leave atau /cancel-leave (RBAC)", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_128_NonAdminCancelLeavePrivilegeEscalationRejected() {
                Assert.assertNotNull(userBOwnLeaveId, "Prasyarat leave ID User B harus ada.");
                Response bffCancel = sendBff(userACookie, "POST", Constants.BFF_CANCEL_LEAVE_ENDPOINT, Map.of(
                                "leave_id", userBOwnLeaveId,
                                "lv_approve_notes", "Unauthorized cancel attempt"));
                Response apiCancel = sendFeathers(userAToken, "POST", Constants.API_CANCEL_LEAVE_ENDPOINT, Map.of(
                                "leave_trans_id", userBOwnLeaveId,
                                "lv_approve_notes", "Unauthorized cancel attempt"));

                Response checkDetail = AdminApiHelper.getRequestLeaveById(userBOwnLeaveId);
                Boolean isCanceled = checkDetail.jsonPath().get("is_canceled");
                ExtentTestManager.logInfo("SIT_USR_API_128 [Critical|P1] Non-admin cancel leave: BFF="
                                + bffCancel.getStatusCode()
                                + ", Feathers=" + apiCancel.getStatusCode() + ", is_canceled=" + isCanceled);

                Assert.assertNotEquals(isCanceled, Boolean.TRUE,
                                "Karyawan biasa non-admin tidak boleh membatalkan cuti milik karyawan lain!");
                Assert.assertTrue(bffCancel.getStatusCode() >= 400 && apiCancel.getStatusCode() >= 400,
                                "Endpoint cancel cuti wajib menolak karyawan non-admin (4xx), aktual BFF="
                                                + bffCancel.getStatusCode() + ", API=" + apiCancel.getStatusCode());
        }

        @Test(priority = 129, description = "SIT_USR_API_129: Akses seluruh endpoint Next.js BFF modul Deny tanpa cookie sesi hadir_web_magang wajib ditolak", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_129_AllDenyBffRoutesRequireSessionCookie() {
                Response rLeave = sendBff(null, "POST", Constants.BFF_LEAVE_ENDPOINT, Map.of("notes", "no cookie"));
                Response rSick = sendBff(null, "POST", Constants.BFF_SICK_ENDPOINT, Map.of("request_type", "SAKIT"));
                Response rSickHist = sendBff(null, "GET", Constants.BFF_ACTIVITY_SICK_ENDPOINT, null);
                Response rOff = sendBff(null, "POST", Constants.BFF_OFF_PERMIT_ENDPOINT, Map.of("reason", "no cookie"));
                Response rLate = sendBff(null, "POST", Constants.BFF_LATE_PERMIT_ENDPOINT,
                                Map.of("late_reason", "no cookie"));
                Response rEarly = sendBff(null, "POST", Constants.BFF_EARLY_LEAVE_ENDPOINT,
                                Map.of("early_leave_reason", "no cookie"));

                ExtentTestManager.logInfo("SIT_USR_API_129 [Critical|P1] No-cookie BFF statuses -> leave="
                                + rLeave.getStatusCode()
                                + ", sick=" + rSick.getStatusCode() + ", sickHist=" + rSickHist.getStatusCode()
                                + ", off=" + rOff.getStatusCode() + ", late=" + rLate.getStatusCode() + ", early="
                                + rEarly.getStatusCode());

                for (Response r : List.of(rLeave, rSick, rSickHist, rOff, rLate, rEarly)) {
                        Assert.assertTrue(r.getStatusCode() == 401 || r.getStatusCode() == 403,
                                        "BFF route tanpa cookie sesi wajib menolak 401/403, aktual HTTP "
                                                        + r.getStatusCode());
                }
        }

        @Test(priority = 132, description = "SIT_USR_API_132: IDOR ubah password via rute BFF live PATCH /api/users/{other_id} & Mass Assignment upliner_id/company_id", groups = {
                        "regression", "negative" })
        public void testSIT_USR_API_132_BffProfilePasswordIdorAndExtendedMassAssignment() {
                String newPwd = SecurityUtil.decode(TEMP_NEW_PASSWORD_ENCODED);
                Response bffIdor = sendBff(userACookie, "PATCH", Constants.BFF_USERS_ENDPOINT + "/" + userBId, Map.of(
                                "password", newPwd,
                                "passwordConfirmation", newPwd));

                Response extMassAssign = sendFeathers(userAToken, "PATCH", Constants.API_USERS_ENDPOINT + "/" + userAId,
                                Map.of(
                                                "upliner_id", defaultUserId,
                                                "company_id", "00000000-0000-0000-0000-000000000000",
                                                "is_user_project", false));

                ExtentTestManager.logInfo("SIT_USR_API_132 [Critical|P1] BFF PATCH /api/users/{other_id}="
                                + bffIdor.getStatusCode()
                                + ", extended MassAssignment=" + extMassAssign.getStatusCode());

                assertRejectedAuthOrForbidden(bffIdor, "[KNOWN SECURITY] BFF IDOR PATCH /api/users/{other_id}");
                if (extMassAssign.getStatusCode() == 200 || extMassAssign.getStatusCode() == 201) {
                        Response check = sendFeathers(adminToken, "GET", Constants.API_USERS_ENDPOINT + "/" + userAId,
                                        null);
                        Assert.assertEquals(check.jsonPath().getString("upliner_id"), adminUserId,
                                        "[KNOWN SECURITY] Karyawan berhasil memodifikasi upliner_id dirinya via Mass Assignment!");
                }
        }
}
