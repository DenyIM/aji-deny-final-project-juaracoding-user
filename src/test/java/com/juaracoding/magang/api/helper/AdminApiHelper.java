package com.juaracoding.magang.api.helper;

import com.juaracoding.magang.api.BaseApiTest;
import com.juaracoding.magang.utils.Constants;
import com.juaracoding.magang.utils.SecurityUtil;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Helper untuk pola Hybrid API + UI di proyek Karyawan (100% selaras dengan EmployeeApiHelper di proyek Admin).
 * Menggunakan REST Assured pada tahap @BeforeMethod / @BeforeClass untuk:
 * 1. Seeding pengajuan Karyawan (Izin Off, Izin Terlambat, Pulang Cepat, Koreksi Absen, Lembur, Dinas, Upload Foto).
 * 2. Menyetujui (Approve), Menolak (Reject), atau Cancel Cuti menggunakan kredensial Admin HRD
 *    (HYBRID_ADMIN_PASSWORD_ENCODED via SecurityUtil.decode) tanpa menyimpan password plaintext.
 */
public final class AdminApiHelper {

    private static final Logger logger = LoggerFactory.getLogger(AdminApiHelper.class);

    public record ApiSession(String token, String userId) {}

    private AdminApiHelper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Login sebagai Karyawan utama (hadirsqa1@gmail.com) via POST /authentication.
     *
     * @return ApiSession berisi JWT accessToken dan userId
     */
    public static ApiSession loginAsDefaultUser() {
        Response res = BaseApiTest.authenticate(
                Constants.USER_EMAIL,
                Constants.USER_PASSWORD_ENCODED
        );
        int status = res.getStatusCode();
        if (status != 200 && status != 201) {
            throw new IllegalStateException("User API authentication failed with HTTP " + status + ": " + res.asString());
        }
        String token = res.jsonPath().getString("accessToken");
        String userId = res.jsonPath().getString("user.id");
        logger.info("Hybrid User session acquired: userId={}", userId);
        return new ApiSession(token, userId);
    }

    /**
     * Login sebagai Admin HRD via POST /authentication untuk melakukan approval/rejection/cancel
     * pada tahap @Before atau cleanup pada tahap @After.
     *
     * @return ApiSession berisi JWT accessToken Admin dan userId
     */
    public static ApiSession loginAsHybridAdmin() {
        Response res = BaseApiTest.authenticate(
                Constants.HYBRID_ADMIN_EMAIL,
                Constants.HYBRID_ADMIN_PASSWORD_ENCODED
        );
        int status = res.getStatusCode();
        if (status != 200 && status != 201) {
            throw new IllegalStateException("Hybrid Admin API authentication failed with HTTP " + status + ": " + res.asString());
        }
        String token = res.jsonPath().getString("accessToken");
        String userId = res.jsonPath().getString("user.id");
        logger.info("Hybrid Admin session acquired: userId={}", userId);
        return new ApiSession(token, userId);
    }

    private static RequestSpecification authSpec(String token) {
        return new RequestSpecBuilder()
                .addRequestSpecification(BaseApiTest.buildDefaultRequestSpec())
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    /**
     * Mengunggah file gambar uji ke POST /upload dengan MIME type eksplisit ("image/jpeg" atau "image/png")
     * pada field multipart "uri".
     *
     * @param fileName nama file di src/test/resources/testdata/
     * @return ID file terunggah atau null jika gagal
     */
    public static String uploadTestPhoto(String fileName) {
        return uploadTestPhoto(null, fileName);
    }

    /**
     * Mengunggah file gambar uji ke POST /upload dengan dukungan opsional Authorization Bearer token
     * serta MIME type eksplisit ("image/jpeg" atau "image/png") pada field multipart "uri".
     *
     * @param bearerToken JWT token opsional (boleh null untuk public upload saat self-register)
     * @param fileName    nama file di src/test/resources/testdata/
     * @return ID file terunggah atau null jika gagal
     */
    public static String uploadTestPhoto(String bearerToken, String fileName) {
        Path filePath = Paths.get(Constants.TESTDATA_DIR, fileName).toAbsolutePath().normalize();
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("Testdata file not found: " + filePath);
        }
        String mimeType = fileName.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";

        Response res = BaseApiTest.executeWithConnectRetry(() -> {
            RequestSpecification req = given()
                    .baseUri(Constants.API_BASE_URI)
                    .relaxedHTTPSValidation()
                    .multiPart("uri", filePath.toFile(), mimeType);
            if (bearerToken != null && !bearerToken.isBlank()) {
                req.header("Authorization", "Bearer " + bearerToken.trim());
            }
            return req
                    .when()
                    .post(Constants.API_UPLOAD_ENDPOINT)
                    .then()
                    .extract()
                    .response();
        }, "POST /upload (" + fileName + ")");

        logger.info("Hybrid POST /upload [{}] (mime={}) -> HTTP {}", fileName, mimeType, res.getStatusCode());
        if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
            return res.jsonPath().getString("id");
        }
        logger.warn("Upload failed: {}", res.asString());
        return null;
    }

    /**
     * Seeding pengajuan Izin Off sebagai Karyawan default via POST /off-permit.
     *
     * @param dateOff tanggal izin (yyyy-MM-dd)
     * @param reason  alasan izin
     * @return ID record off-permit yang dibuat (atau null jika gagal)
     */
    public static String createPendingOffPermit(String dateOff, String reason) {
        ApiSession session = loginAsDefaultUser();
        return createPendingOffPermit(session.token(), dateOff, reason);
    }

    /**
     * Seeding pengajuan Izin Off menggunakan token user tertentu via POST /off-permit.
     *
     * @param userToken JWT Bearer token user target
     * @param dateOff   tanggal izin (yyyy-MM-dd)
     * @param reason    alasan izin
     * @return ID record off-permit yang dibuat (atau null jika gagal)
     */
    public static String createPendingOffPermit(String userToken, String dateOff, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("date_off", dateOff);
        body.put("reason", reason);

        Response res = given()
                .spec(authSpec(userToken))
                .body(body)
        .when()
                .post(Constants.API_OFF_PERMIT_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Seed POST /off-permit -> HTTP {}", res.getStatusCode());
        if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
            return res.jsonPath().getString("id");
        }
        logger.warn("Seed off-permit did not succeed: {}", res.asString());
        return null;
    }

    /**
     * Login sebagai user tertentu via POST /authentication dan mengembalikan ApiSession.
     *
     * @param email           email user
     * @param encodedPassword password Base64 ter-encode
     * @return ApiSession berisi JWT token dan userId
     */
    public static ApiSession loginAsUser(String email, String encodedPassword) {
        Response res = BaseApiTest.authenticate(email, encodedPassword);
        int status = res.getStatusCode();
        if (status != 200 && status != 201) {
            throw new IllegalStateException("User API authentication failed for " + email + " with HTTP " + status);
        }
        return new ApiSession(res.jsonPath().getString("accessToken"), res.jsonPath().getString("user.id"));
    }

    /**
     * Mengaktifkan user uji terisolasi sekaligus menautkan upliner_id ke Admin HRD
     * agar Hybrid Admin approval (PATCH /off-permit, dll.) dapat diuji tanpa bentrok atasan.
     *
     * @param userId ID user uji
     * @return true jika HTTP 200/201
     */
    public static boolean activateUserWithAdminUpliner(String userId) {
        return activateUserWithAdminUplinerAndLeave(userId, -1);
    }

    /**
     * Mengaktifkan user uji terisolasi dengan upliner Admin HRD serta kuota cuti opsional
     * (leave_amount >= 0) untuk skenario Pengajuan-Cuti & Sakit.
     *
     * @param userId      ID user uji
     * @param leaveAmount jumlah kuota cuti awal (negatif jika tidak diset)
     * @return true jika HTTP 200/201
     */
    public static boolean activateUserWithAdminUplinerAndLeave(String userId, int leaveAmount) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        ApiSession admin = loginAsHybridAdmin();
        Map<String, Object> body = new HashMap<>();
        body.put("is_active", true);
        body.put("isVerified", true);
        body.put("upliner_id", admin.userId());
        body.put("employee_type_id", "93ead3fc-6a90-4741-9f89-856a83182225"); // PKWT agar masuk /apps/absent (bukan /mitra)
        if (leaveAmount >= 0) {
            body.put("job_level_id", "d9c079bd-c9b9-44eb-98df-65f96f75534b");
            body.put("job_departement_id", "7e8b9e92-7e8f-47ed-b886-b647bf107c03");
            body.put("is_user_project", true); // wajib true agar GET /project-leave/{id} & /apps/absent/leave-client SSR 200 OK
            body.put("leave_amount", leaveAmount);
        } else {
            body.put("is_user_project", false);
        }

        Response res = given()
                .spec(authSpec(admin.token()))
                .body(body)
        .when()
                .patch(Constants.API_USERS_ENDPOINT + "/" + userId)
        .then()
                .extract()
                .response();
        logger.info("Hybrid Admin PATCH /users/{} (active + upliner=admin, leave_amount={}) -> HTTP {}",
                userId, leaveAmount, res.getStatusCode());
        boolean patchOk = res.getStatusCode() == 200 || res.getStatusCode() == 201;
        if (patchOk && leaveAmount >= 0) {
            assignDefaultScheduleAsAdmin(admin.token(), userId);
        }
        return patchOk;
    }

    private static void assignDefaultScheduleAsAdmin(String adminToken, String userId) {
        try {
            Response schedRes = given()
                    .spec(authSpec(adminToken))
                    .queryParam("$limit", 1)
            .when()
                    .get("/schedule")
            .then()
                    .extract()
                    .response();
            String scheduleId = schedRes.jsonPath().getString("data[0].id");
            if (scheduleId == null || scheduleId.isBlank()) {
                scheduleId = "aaa0fc8a-0860-4792-90b5-dc95eef9043d";
            }
            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId);
            body.put("schedule_id", scheduleId);
            Response assignRes = given()
                    .spec(authSpec(adminToken))
                    .body(body)
            .when()
                    .post("/user-schedule")
            .then()
                    .extract()
                    .response();
            logger.info("Hybrid Admin POST /user-schedule (user={}, schedule={}) -> HTTP {}",
                    userId, scheduleId, assignRes.getStatusCode());
        } catch (Exception e) {
            logger.warn("Could not assign default /user-schedule for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Admin menyetujui atau menolak pengajuan Izin Off via PATCH /off-permit/{id}.
     *
     * @param permitId     ID record off-permit
     * @param approve      true untuk Disetuji, false untuk Ditolak
     * @param rejectReason alasan penolakan (diisi jika approve = false)
     * @return true jika HTTP 200/201
     */
    public static boolean reviewOffPermitAsAdmin(String permitId, boolean approve, String rejectReason) {
        Map<String, Object> body = new HashMap<>();
        body.put("is_approved", approve);
        if (!approve && rejectReason != null && !rejectReason.isBlank()) {
            body.put("reject_reason", rejectReason);
        }
        return patchPermitEndpoint(Constants.API_OFF_PERMIT_ENDPOINT, permitId, body, approve);
    }

    /**
     * Admin menyetujui atau menolak pengajuan Izin Terlambat via PATCH /late-permit/{id}.
     *
     * @param permitId     ID record late-permit
     * @param approve      true untuk Disetuji, false untuk Ditolak
     * @param rejectReason alasan penolakan
     * @return true jika HTTP 200/201
     */
    public static boolean reviewLatePermitAsAdmin(String permitId, boolean approve, String rejectReason) {
        Map<String, Object> body = new HashMap<>();
        body.put("is_approved_late", approve);
        if (!approve && rejectReason != null && !rejectReason.isBlank()) {
            body.put("late_reject_reason", rejectReason);
        }
        return patchPermitEndpoint(Constants.API_LATE_PERMIT_ENDPOINT, permitId, body, approve);
    }

    /**
     * Admin menyetujui atau menolak pengajuan Izin Pulang Cepat via PATCH /early-leave/{id}.
     *
     * @param permitId     ID record early-leave
     * @param approve      true untuk Disetuji, false untuk Ditolak
     * @param rejectReason alasan penolakan
     * @return true jika HTTP 200/201
     */
    public static boolean reviewEarlyLeaveAsAdmin(String permitId, boolean approve, String rejectReason) {
        Map<String, Object> body = new HashMap<>();
        body.put("is_approved_early_leave", approve);
        if (!approve && rejectReason != null && !rejectReason.isBlank()) {
            body.put("early_leave_reject_reason", rejectReason);
        }
        return patchPermitEndpoint(Constants.API_EARLY_LEAVE_ENDPOINT, permitId, body, approve);
    }

    private static boolean patchPermitEndpoint(String endpointPath, String recordId, Map<String, Object> body, boolean approve) {
        if (recordId == null || recordId.isBlank()) {
            logger.warn("Cannot review empty recordId on {}", endpointPath);
            return false;
        }
        ApiSession admin = loginAsHybridAdmin();
        Response res = given()
                .spec(authSpec(admin.token()))
                .body(body)
        .when()
                .patch(endpointPath + "/" + recordId)
        .then()
                .extract()
                .response();
        logger.info("Hybrid Admin PATCH {}/{} (approve={}) -> HTTP {}", endpointPath, recordId, approve, res.getStatusCode());
        return res.getStatusCode() == 200 || res.getStatusCode() == 201;
    }

    /**
     * Mengambil total jumlah record milik user utama pada endpoint FeathersJS tertentu.
     *
     * @param endpointPath path endpoint (misal Constants.API_LATE_PERMIT_ENDPOINT)
     * @return total record
     */
    public static int getUserRecordsCount(String endpointPath) {
        ApiSession session = loginAsDefaultUser();
        Response res = given()
                .spec(authSpec(session.token()))
                .queryParam("user_id", session.userId())
                .queryParam("$limit", 1)
        .when()
                .get(endpointPath)
        .then()
                .extract()
                .response();
        Integer total = res.jsonPath().get("total");
        return total != null ? total : 0;
    }

    /**
     * Mencari ID record terbaru milik user utama yang field alasannya mengandung teks tertentu.
     */
    public static String findLatestUserRecordIdByField(String endpointPath, String reasonField, String reasonSnippet) {
        ApiSession session = loginAsDefaultUser();
        return findLatestUserRecordIdByField(session.token(), session.userId(), endpointPath, reasonField, reasonSnippet);
    }

    /**
     * Mencari ID record terbaru milik user tertentu yang field alasannya mengandung teks tertentu
     * (untuk cleanup data hasil submit UI di @AfterMethod).
     */
    public static String findLatestUserRecordIdByField(
            String userToken, String userId, String endpointPath, String reasonField, String reasonSnippet) {
        if (reasonSnippet == null || reasonSnippet.isBlank()) {
            return null;
        }
        Response res = given()
                .spec(authSpec(userToken))
                .queryParam("user_id", userId)
                .queryParam("$limit", 25)
                .queryParam("$sort[created_at]", -1)
        .when()
                .get(endpointPath)
        .then()
                .extract()
                .response();
        List<Map<String, Object>> items = res.jsonPath().getList("data");
        if (items == null) {
            return null;
        }
        for (Map<String, Object> item : items) {
            Object val = item.get(reasonField);
            if (val != null && String.valueOf(val).contains(reasonSnippet)) {
                Object id = item.get("id");
                return id != null ? String.valueOf(id) : null;
            }
        }
        return null;
    }

    /**
     * Seeding pengajuan Izin Terlambat berstatus Pending sebagai Karyawan via POST /late-permit.
     *
     * @param lateTimeInIso waktu keterlambatan (ISO-8601)
     * @param reason        alasan keterlambatan
     * @return ID record late-permit yang dibuat
     */
    public static String createPendingLatePermit(String lateTimeInIso, String reason) {
        ApiSession session = loginAsDefaultUser();
        return createPendingLatePermit(session.token(), session.userId(), lateTimeInIso, reason);
    }

    /**
     * Seeding pengajuan Izin Terlambat berstatus Pending menggunakan token user tertentu.
     *
     * @param userToken     JWT token user
     * @param userId        ID user
     * @param lateTimeInIso waktu keterlambatan (ISO-8601)
     * @param reason        alasan keterlambatan
     * @return ID record late-permit yang dibuat
     */
    public static String createPendingLatePermit(String userToken, String userId, String lateTimeInIso, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("late_time_in", lateTimeInIso);
        body.put("late_reason", reason);
        body.put("time_zone", "+07:00");

        Response res = given()
                .spec(authSpec(userToken))
                .body(body)
        .when()
                .post(Constants.API_LATE_PERMIT_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Seed POST /late-permit -> HTTP {}", res.getStatusCode());
        if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
            return res.jsonPath().getString("id");
        }
        logger.warn("Seed late-permit did not succeed: {}", res.asString());
        return null;
    }

    /**
     * Seeding pengajuan Izin Pulang Cepat berstatus Pending sebagai Karyawan via POST /early-leave.
     *
     * @param earlyTimeOutIso waktu pulang cepat (ISO-8601)
     * @param reason          alasan pulang cepat
     * @return ID record early-leave yang dibuat
     */
    public static String createPendingEarlyLeave(String earlyTimeOutIso, String reason) {
        ApiSession session = loginAsDefaultUser();
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", session.userId());
        body.put("early_time_out", earlyTimeOutIso);
        body.put("early_leave_reason", reason);
        body.put("time_zone", "+07:00");

        Response res = given()
                .spec(authSpec(session.token()))
                .body(body)
        .when()
                .post(Constants.API_EARLY_LEAVE_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Seed POST /early-leave -> HTTP {}", res.getStatusCode());
        if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
            return res.jsonPath().getString("id");
        }
        logger.warn("Seed early-leave did not succeed: {}", res.asString());
        return null;
    }

    /**
     * Seeding pengajuan Perjalanan Dinas berstatus Pending sebagai Karyawan via POST /business-trips.
     *
     * @param tripDateIso tanggal perjalanan format ISO-8601 UTC
     * @param notes       catatan dinas
     * @return ID record business-trip yang dibuat
     */
    public static String createPendingBusinessTrip(String tripDateIso, String notes) {
        ApiSession session = loginAsDefaultUser();
        Map<String, Object> body = new HashMap<>();
        body.put("trip_date", tripDateIso);
        body.put("type", "Perjalanan Dinas");
        body.put("notes", notes);
        body.put("time_zone", "+07:00");

        Response res = given()
                .spec(authSpec(session.token()))
                .body(body)
        .when()
                .post(Constants.API_BUSINESS_TRIPS_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Seed POST /business-trips -> HTTP {}", res.getStatusCode());
        if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
            return res.jsonPath().getString("id");
        }
        logger.warn("Seed business-trips did not succeed: {}", res.asString());
        return null;
    }

    /**
     * Seeding pengajuan Koreksi Absen sebagai Karyawan via POST /activity-correction.
     *
     * @param dateInIso  waktu masuk (ISO-8601)
     * @param dateOutIso waktu keluar (ISO-8601)
     * @param isWfh      true jika WFH, false jika WFO
     * @return ID record koreksi absen yang dibuat
     */
    public static String createPendingCorrection(String dateInIso, String dateOutIso, boolean isWfh) {
        ApiSession session = loginAsDefaultUser();
        Map<String, Object> body = new HashMap<>();
        body.put("date_in", dateInIso);
        body.put("date_out", dateOutIso);
        body.put("time_zone", "+07:00");
        body.put("is_wfh", isWfh);

        Response res = given()
                .spec(authSpec(session.token()))
                .body(body)
        .when()
                .post(Constants.API_CORRECTIONS_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Seed POST /activity-correction -> HTTP {}", res.getStatusCode());
        if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
            return res.jsonPath().getString("id");
        }
        logger.warn("Seed activity-correction did not succeed: {}", res.asString());
        return null;
    }

    /**
     * Admin menyetujui atau menolak pengajuan Koreksi Absen via PATCH /activity-correction/{id}.
     *
     * @param correctionId ID record koreksi
     * @param approve      true untuk Approved, false untuk Rejected
     * @param rejectReason alasan penolakan (diisi jika approve = false)
     * @return true jika HTTP 200/201
     */
    public static boolean reviewCorrectionAsAdmin(String correctionId, boolean approve, String rejectReason) {
        return patchApprovalStatus(Constants.API_CORRECTIONS_ENDPOINT, correctionId, approve, rejectReason);
    }

    /**
     * Seeding pengajuan Lembur sebagai Karyawan via POST /overtime.
     *
     * @param overtimeInIso  jam mulai lembur (ISO-8601)
     * @param overtimeOutIso jam selesai lembur (ISO-8601)
     * @param notes          catatan lembur
     * @return ID record overtime yang dibuat
     */
    public static String createPendingOvertime(String overtimeInIso, String overtimeOutIso, String notes) {
        ApiSession session = loginAsDefaultUser();
        Map<String, Object> body = new HashMap<>();
        body.put("overtime_in", overtimeInIso);
        body.put("overtime_out", overtimeOutIso);
        body.put("notes", notes);
        body.put("time_zone", "+07:00");

        Response res = given()
                .spec(authSpec(session.token()))
                .body(body)
        .when()
                .post(Constants.API_OVERTIME_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Seed POST /overtime -> HTTP {}", res.getStatusCode());
        if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
            return res.jsonPath().getString("id");
        }
        logger.warn("Seed overtime did not succeed: {}", res.asString());
        return null;
    }

    /**
     * Admin menyetujui atau menolak pengajuan Lembur via PATCH /overtime/{id}.
     *
     * @param overtimeId   ID record overtime
     * @param approve      true untuk Approved, false untuk Rejected
     * @param rejectReason alasan penolakan (diisi jika approve = false)
     * @return true jika HTTP 200/201
     */
    public static boolean reviewOvertimeAsAdmin(String overtimeId, boolean approve, String rejectReason) {
        return patchApprovalStatus(Constants.API_OVERTIME_ENDPOINT, overtimeId, approve, rejectReason);
    }

    /**
     * Helper bersama untuk PATCH status approval/rejection oleh Admin HRD.
     */
    private static boolean patchApprovalStatus(String endpointPath, String recordId, boolean approve, String rejectReason) {
        if (recordId == null || recordId.isBlank()) {
            logger.warn("Cannot review empty recordId on {}", endpointPath);
            return false;
        }
        ApiSession admin = loginAsHybridAdmin();
        Map<String, Object> body = new HashMap<>();
        body.put("status", approve ? "APPROVED" : "REJECTED");
        body.put("is_approve", approve);
        if (!approve && rejectReason != null && !rejectReason.isBlank()) {
            body.put("reason_reject", rejectReason);
            body.put("reject_reason", rejectReason);
            body.put("notes", rejectReason);
        }

        Response res = given()
                .spec(authSpec(admin.token()))
                .body(body)
        .when()
                .patch(endpointPath + "/" + recordId)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Admin PATCH {}/{} (approve={}) -> HTTP {}", endpointPath, recordId, approve, res.getStatusCode());
        return res.getStatusCode() == 200 || res.getStatusCode() == 201;
    }

    /**
     * Mengambil ringkasan sisa cuti karyawan utama via GET /project-leave.
     *
     * @return Response JSON /project-leave
     */
    public static Response queryUserLeaveSummary() {
        ApiSession session = loginAsDefaultUser();
        return given()
                .spec(authSpec(session.token()))
        .when()
                .get(Constants.API_PROJECT_LEAVE_ENDPOINT)
        .then()
                .extract()
                .response();
    }

    /**
     * Mengambil daftar aktivitas presensi dengan filter rentang tanggal menggunakan operator FeathersJS
     * {@code created_at[$gte]} dan {@code created_at[$lte]}.
     * PENTING: Dilarang menggunakan query param {@code startDate} / {@code start_date} karena memicu SQL 500 Unknown column.
     *
     * @param bearerToken JWT token user atau admin
     * @param startIso    batas awal tanggal/waktu (misal "2026-11-01" atau ISO-8601)
     * @param endIso      batas akhir tanggal/waktu (misal "2026-11-30" atau ISO-8601)
     * @return Response GET /activity
     */
    public static Response queryActivitiesByDateRange(String bearerToken, String startIso, String endIso) {
        String effectiveToken = (bearerToken != null && !bearerToken.isBlank())
                ? bearerToken
                : loginAsDefaultUser().token();
        Response res = given()
                .spec(authSpec(effectiveToken))
                .queryParam("created_at[$gte]", startIso)
                .queryParam("created_at[$lte]", endIso)
        .when()
                .get(Constants.API_ACTIVITY_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("GET /activity?created_at[$gte]={}&created_at[$lte]={} -> HTTP {}",
                startIso, endIso, res.getStatusCode());
        return res;
    }

    /**
     * Mencari userId berdasarkan email melalui Admin API GET /users?email=...
     *
     * @param email alamat email user
     * @return userId jika ditemukan, selain itu null
     */
    public static String findUserIdByEmail(String email) {
        return findUserFieldByEmail(email, "data.id");
    }

    /**
     * Mengambil path/ID foto profil user berdasarkan email melalui Admin API GET /users?email=...
     *
     * @param email alamat email user
     * @return nilai field photo jika ditemukan, selain itu null
     */
    public static String findUserPhotoByEmail(String email) {
        return findUserFieldByEmail(email, "data.photo");
    }

    private static String findUserFieldByEmail(String email, String jsonPath) {
        if (email == null || email.isBlank()) {
            return null;
        }
        ApiSession admin = loginAsHybridAdmin();
        Response res = given()
                .spec(authSpec(admin.token()))
                .queryParam("email", email)
        .when()
                .get(Constants.API_USERS_ENDPOINT)
        .then()
                .extract()
                .response();
        List<Object> values = res.jsonPath().getList(jsonPath);
        if (values == null || values.isEmpty() || values.get(0) == null) {
            return null;
        }
        return String.valueOf(values.get(0));
    }

    /**
     * Memeriksa status HTTP dari file upload pada static host /uploads.
     *
     * @param photoPath nilai field photo (ID, path relatif, atau URL penuh)
     * @return HTTP status code atau -1 jika path kosong/gagal
     */
    public static int checkStaticUploadStatus(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) {
            return -1;
        }
        String trimmed = photoPath.trim();
        String targetUrl;
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            targetUrl = trimmed;
        } else if (trimmed.startsWith("/uploads/")) {
            targetUrl = "https://api-dev.dikahadir.com" + trimmed;
        } else {
            String normalized = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
            targetUrl = Constants.API_STATIC_UPLOADS_BASE_URI + "/" + normalized;
        }
        try {
            Response res = given()
                    .relaxedHTTPSValidation()
            .when()
                    .get(targetUrl)
            .then()
                    .extract()
                    .response();
            logger.info("Static upload check GET {} -> HTTP {}", targetUrl, res.getStatusCode());
            return res.getStatusCode();
        } catch (Exception e) {
            logger.warn("Static upload check failed for [{}]: {}", targetUrl, e.getMessage());
            return -1;
        }
    }

    /**
     * Menghapus akun uji berdasarkan email secara aman di tahap @AfterMethod.
     *
     * @param email alamat email akun uji yang akan dihapus
     */
    public static void cleanupUserByEmailQuietly(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        try {
            String userId = findUserIdByEmail(email);
            if (userId != null && !userId.isBlank()) {
                cleanupRecordQuietly(Constants.API_USERS_ENDPOINT, userId);
            }
        } catch (Exception e) {
            logger.warn("Cleanup user by email skipped for [{}]: {}", email, e.getMessage());
        }
    }

    /**
     * Mengambil NIK valid yang belum terpakai dari pool master DIKA (Constants.VALID_NIK_POOL).
     *
     * @return NIK valid tersedia
     */
    public static String findAvailableNikFromPool() {
        try {
            ApiSession admin = loginAsHybridAdmin();
            for (String candidate : Constants.VALID_NIK_POOL) {
                Response res = given()
                        .spec(authSpec(admin.token()))
                        .queryParam("nik", candidate)
                .when()
                        .get(Constants.API_USERS_ENDPOINT)
                .then()
                        .extract()
                        .response();
                List<String> ids = res.jsonPath().getList("data.id");
                if (ids == null || ids.isEmpty()) {
                    logger.info("Found available valid NIK from pool: {}", candidate);
                    return candidate;
                }
            }
        } catch (Exception e) {
            logger.warn("Could not query VALID_NIK_POOL availability: {}", e.getMessage());
        }
        return Constants.VALID_NIK_POOL[0];
    }

    /**
     * Mendaftarkan akun karyawan mandiri via POST /users (is_mobile=true, company_code=DIKA)
     * menggunakan password ter-encode Base64 (kebutuhan E2E SIT_USR_UI_130).
     *
     * @param nik             NIK valid
     * @param fullname        nama lengkap
     * @param email           email pendaftar
     * @param encodedPassword password ter-encode Base64 (didecode via SecurityUtil)
     * @return userId baru atau null jika gagal
     */
    public static String createSelfRegisteredMobileUser(
            String nik,
            String fullname,
            String email,
            String encodedPassword
    ) {
        String photoId = uploadTestPhoto("valid_photo.jpg");
        Map<String, Object> body = new HashMap<>();
        body.put("nik", nik);
        body.put("fullname", fullname);
        body.put("email", email);
        body.put("password", SecurityUtil.decode(encodedPassword));
        body.put("company_code", "DIKA");
        body.put("is_mobile", true);
        if (photoId != null && !photoId.isBlank()) {
            body.put("photo", photoId);
        }

        Response res = given()
                .spec(BaseApiTest.buildDefaultRequestSpec())
                .body(body)
        .when()
                .post(Constants.API_USERS_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Seed self-register POST /users [{}] -> HTTP {}", email, res.getStatusCode());
        if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
            String id = res.jsonPath().getString("id");
            return (id != null && !id.isBlank()) ? id : findUserIdByEmail(email);
        }
        logger.warn("Seed self-register did not succeed: {}", res.asString());
        return null;
    }

    /**
     * Mengubah status aktif/non-aktif akun Karyawan oleh Admin via PATCH /users/{id}
     * disertai flag {@code isVerified: true} (kebutuhan E2E SIT_USR_UI_130–131).
     *
     * @param userId   ID user target
     * @param isActive true untuk mengaktifkan, false untuk menonaktifkan
     * @return true jika HTTP 200/201
     */
    public static boolean updateUserActiveStatusAsAdmin(String userId, boolean isActive) {
        if (userId == null || userId.isBlank()) {
            logger.warn("Cannot update active status for blank userId");
            return false;
        }
        ApiSession admin = loginAsHybridAdmin();
        Map<String, Object> body = new HashMap<>();
        body.put("is_active", isActive);
        body.put("isVerified", true);

        Response res = BaseApiTest.executeWithConnectRetry(() -> given()
                .spec(authSpec(admin.token()))
                .body(body)
        .when()
                .patch(Constants.API_USERS_ENDPOINT + "/" + userId)
        .then()
                .extract()
                .response(), "PATCH /users/" + userId);

        logger.info("Hybrid Admin PATCH /users/{} (is_active={}, isVerified=true) -> HTTP {}",
                userId, isActive, res.getStatusCode());
        return res.getStatusCode() == 200 || res.getStatusCode() == 201;
    }

    /**
     * Mengambil detail satu record transaksi cuti melalui {@code GET /request-leave/{id}}.
     *
     * @param leaveId ID transaksi cuti
     * @return Response GET /request-leave/{id}
     */
    public static Response getRequestLeaveById(String leaveId) {
        ApiSession admin = loginAsHybridAdmin();
        Response res = BaseApiTest.executeWithConnectRetry(() -> given()
                .spec(authSpec(admin.token()))
        .when()
                .get(Constants.API_REQUEST_LEAVE_ENDPOINT + "/" + leaveId)
        .then()
                .extract()
                .response(), "GET /request-leave/" + leaveId);
        logger.info("Hybrid Admin GET /request-leave/{} -> HTTP {}", leaveId, res.getStatusCode());
        return res;
    }

    /**
     * Membatalkan pengajuan cuti Karyawan oleh Admin melalui jalur resmi portal Admin
     * {@code POST /api/cancel/leave} (kebutuhan E2E SIT_USR_UI_154).
     *
     * @param leaveTransId ID transaksi cuti
     * @return true jika HTTP 200/201
     */
    public static boolean cancelLeaveAsAdmin(String leaveTransId) {
        return cancelLeaveAsAdmin(leaveTransId, "E2E Cancel Cuti Admin Automation");
    }

    /**
     * Membatalkan pengajuan cuti Karyawan oleh Admin melalui Next.js BFF {@code POST /api/cancel/leave}
     * (dengan payload terverifikasi {@code leave_id}, {@code lv_trans_id}, {@code lv_approve_notes})
     * serta fallback ke {@code POST /cancel-leave} bila record berasal dari sync HRIS non-null.
     *
     * @param leaveTransId ID transaksi cuti (request-leave id)
     * @param approveNotes alasan pembatalan cuti (lv_approve_notes)
     * @return true jika pembatalan berhasil (HTTP 200/201)
     */
    public static boolean cancelLeaveAsAdmin(String leaveTransId, String approveNotes) {
        if (leaveTransId == null || leaveTransId.isBlank()) {
            logger.warn("Cannot cancel blank leaveTransId");
            return false;
        }
        String notes = (approveNotes != null && !approveNotes.isBlank())
                ? approveNotes
                : "E2E Cancel Cuti Admin";

        Object hrisLvTransId = null;
        try {
            Response leaveDetail = getRequestLeaveById(leaveTransId);
            if (leaveDetail.getStatusCode() == 200) {
                hrisLvTransId = leaveDetail.jsonPath().get("hris_lv_trans_id");
            }
        } catch (Exception e) {
            logger.warn("Could not inspect hris_lv_trans_id for leave [{}]: {}", leaveTransId, e.getMessage());
        }

        Map<String, String> adminCookies = loginBffCookies(
                Constants.HYBRID_ADMIN_EMAIL, Constants.HYBRID_ADMIN_PASSWORD_ENCODED);
        if (!adminCookies.isEmpty()) {
            Map<String, Object> bffBody = new HashMap<>();
            bffBody.put("leave_id", leaveTransId);
            bffBody.put("lv_trans_id", hrisLvTransId);
            bffBody.put("lv_approve_notes", notes);

            Response bffRes = BaseApiTest.executeWithConnectRetry(() -> given()
                    .baseUri(Constants.BASE_URL)
                    .relaxedHTTPSValidation()
                    .cookies(adminCookies)
                    .contentType("application/json")
                    .body(bffBody)
            .when()
                    .post(Constants.BFF_CANCEL_LEAVE_ENDPOINT)
            .then()
                    .extract()
                    .response(), "POST /api/cancel/leave");
            logger.info("Hybrid Admin BFF POST /api/cancel/leave (leave_id={}) -> HTTP {} body={}",
                    leaveTransId, bffRes.getStatusCode(), bffRes.asString());
            if (bffRes.getStatusCode() == 200 || bffRes.getStatusCode() == 201) {
                return true;
            }
        }

        if (hrisLvTransId != null) {
            ApiSession admin = loginAsHybridAdmin();
            Map<String, Object> body = new HashMap<>();
            body.put("leave_trans_id", hrisLvTransId);
            body.put("lv_approve_notes", notes);

            Response res = BaseApiTest.executeWithConnectRetry(() -> given()
                    .spec(authSpec(admin.token()))
                    .body(body)
            .when()
                    .post(Constants.API_CANCEL_LEAVE_ENDPOINT)
            .then()
                    .extract()
                    .response(), "POST /cancel-leave");
            logger.info("Hybrid Admin POST /cancel-leave (leave_trans_id={}) -> HTTP {}",
                    hrisLvTransId, res.getStatusCode());
            return res.getStatusCode() == 200 || res.getStatusCode() == 201;
        }
        return false;
    }

    /**
     * Mengambil daftar titik Absen Point aktif dari Admin via GET /location-point
     * (kebutuhan E2E SIT_USR_UI_135).
     *
     * @return Response JSON /location-point
     */
    public static Response getActiveLocationPointsAsAdmin() {
        ApiSession admin = loginAsHybridAdmin();
        Response res = given()
                .spec(authSpec(admin.token()))
        .when()
                .get(Constants.API_LOCATIONS_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Admin GET /location-point -> HTTP {}", res.getStatusCode());
        return res;
    }

    /**
     * Mengambil pasangan koordinat [latitude, longitude] titik Absen Point aktif pertama
     * dari GET /location-point (fallback ke DEFAULT_OFFICE_LAT/LON jika tidak ditemukan).
     *
     * @return array double[2] berisi {latitude, longitude}
     */
    public static double[] getFirstActiveLocationCoordinates() {
        try {
            Response res = getActiveLocationPointsAsAdmin();
            if (res.getStatusCode() == 200) {
                Double lat = res.jsonPath().getDouble("data[0].latitude");
                Double lon = res.jsonPath().getDouble("data[0].longitude");
                if (lat != null && lon != null) {
                    return new double[]{lat, lon};
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse /location-point coordinates, using default office GPS: {}", e.getMessage());
        }
        return new double[]{Constants.DEFAULT_OFFICE_LAT, Constants.DEFAULT_OFFICE_LON};
    }

    /**
     * Membuat user baru oleh Admin dengan kuota Jumlah Cuti (leave_amount) via POST /users
     * menggunakan password ter-encode Base64 (kebutuhan E2E SIT_USR_UI_140 [Known Defect]).
     *
     * @param nik             NIK karyawan baru
     * @param fullname        nama lengkap
     * @param email           email user baru
     * @param encodedPassword password ter-encode Base64 (akan didecode via SecurityUtil)
     * @param leaveAmount     kuota cuti awal (misal 12)
     * @return Response POST /users
     */
    public static Response createUserWithLeaveAmountAsAdmin(
            String nik,
            String fullname,
            String email,
            String encodedPassword,
            int leaveAmount
    ) {
        ApiSession admin = loginAsHybridAdmin();
        Map<String, Object> body = new HashMap<>();
        body.put("nik", nik);
        body.put("fullname", fullname);
        body.put("email", email);
        body.put("password", SecurityUtil.decode(encodedPassword));
        body.put("leave_amount", leaveAmount);
        body.put("is_active", true);
        body.put("isVerified", true);

        Response res = given()
                .spec(authSpec(admin.token()))
                .body(body)
        .when()
                .post(Constants.API_USERS_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Admin POST /users (leave_amount={}) -> HTTP {}", leaveAmount, res.getStatusCode());
        return res;
    }

    /**
     * Menginspeksi ringkasan kuota cuti user tertentu melalui path parameter terverifikasi
     * {@code GET /project-leave/{userId}} menggunakan token Admin
     * (kebutuhan E2E SIT_USR_UI_140 [Known Defect]).
     *
     * @param userId ID user yang diperiksa
     * @return Response GET /project-leave/{userId}
     */
    public static Response queryProjectLeaveByUserAsAdmin(String userId) {
        return getProjectLeaveSummaryByUserId(null, userId);
    }

    /**
     * Mengambil ringkasan kuota cuti user tertentu melalui {@code GET /project-leave/{userId}}
     * menggunakan Bearer token yang diberikan (atau token Admin jika null/kosong).
     *
     * @param bearerToken JWT token opsional
     * @param userId      ID user target
     * @return Response GET /project-leave/{userId}
     */
    public static Response getProjectLeaveSummaryByUserId(String bearerToken, String userId) {
        String effectiveToken = (bearerToken != null && !bearerToken.isBlank())
                ? bearerToken
                : loginAsHybridAdmin().token();
        Response res = BaseApiTest.executeWithConnectRetry(() -> given()
                .spec(authSpec(effectiveToken))
        .when()
                .get(Constants.API_PROJECT_LEAVE_ENDPOINT + "/" + userId)
        .then()
                .extract()
                .response(), "GET /project-leave/" + userId);

        logger.info("Hybrid GET /project-leave/{} -> HTTP {}", userId, res.getStatusCode());
        return res;
    }

    /**
     * Mengambil ID tipe cuti yang valid untuk submit (memiliki {@code day_limit != null}
     * agar backend tidak melempar NPE pada properti {@code day_limit}) melalui GET /leave-type.
     *
     * @param bearerToken JWT Bearer token user/admin (boleh null -> gunakan default user)
     * @return leave_type_id atau null jika tidak tersedia
     */
    public static String getPreferredLeaveTypeId(String bearerToken) {
        String effectiveToken = (bearerToken != null && !bearerToken.isBlank())
                ? bearerToken
                : loginAsDefaultUser().token();
        Response res = given()
                .spec(authSpec(effectiveToken))
        .when()
                .get(Constants.API_LEAVE_TYPE_ENDPOINT)
        .then()
                .extract()
                .response();
        List<Map<String, Object>> types = res.jsonPath().getList("data");
        if (types == null || types.isEmpty()) {
            return null;
        }
        for (Map<String, Object> item : types) {
            if (item.get("day_limit") != null) {
                return String.valueOf(item.get("id"));
            }
        }
        return String.valueOf(types.get(0).get("id"));
    }

    /**
     * Login ke Next.js BFF {@code POST /api/login} dan mengembalikan seluruh cookies sesi
     * (termasuk {@code hadir_web_magang}).
     */
    private static Map<String, String> loginBffCookies(String userEmail, String encodedPassword) {
        Map<String, Object> loginPayload = new HashMap<>();
        loginPayload.put("strategy", "local");
        loginPayload.put("email", userEmail);
        loginPayload.put("password", SecurityUtil.decode(encodedPassword));

        Response loginRes = BaseApiTest.executeWithConnectRetry(() -> given()
                .baseUri(Constants.BASE_URL)
                .relaxedHTTPSValidation()
                .contentType("application/json")
                .body(loginPayload)
        .when()
                .post(Constants.BFF_LOGIN_ENDPOINT)
        .then()
                .extract()
                .response(), "POST /api/login");

        Map<String, String> cookies = loginRes.getCookies();
        if (cookies == null || cookies.isEmpty()) {
            logger.warn("BFF login returned empty cookies for [{}]: HTTP {}", userEmail, loginRes.getStatusCode());
            return Map.of();
        }
        return cookies;
    }

    /**
     * Seeding pengajuan Cuti Karyawan via Next.js BFF {@code POST /api/login} + {@code POST /api/leave}
     * agar sesi cookie {@code hadir_web_magang} terautentikasi persis seperti pengajuan UI.
     */
    public static String createPendingLeave(
            String userEmail,
            String encodedPassword,
            String leaveTypeId,
            String dateFromYmd,
            String dateToYmd,
            String notes
    ) {
        Map<String, String> cookies = loginBffCookies(userEmail, encodedPassword);
        if (cookies.isEmpty()) {
            return null;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("leave_type_id", leaveTypeId);
        body.put("leave_date_from", dateFromYmd);
        body.put("leave_date_to", dateToYmd);
        body.put("time_zone", "+07:00");
        body.put("notes", notes);

        Response res = given()
                .baseUri(Constants.BASE_URL)
                .relaxedHTTPSValidation()
                .cookies(cookies)
                .contentType("application/json")
                .body(body)
        .when()
                .post(Constants.BFF_LEAVE_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Seed BFF POST /api/leave -> HTTP {}", res.getStatusCode());
        if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
            return res.jsonPath().getString("id");
        }
        logger.warn("Seed BFF /api/leave did not succeed: {}", res.asString());
        return null;
    }

    /**
     * Admin/Upliner menyetujui atau menolak pengajuan Cuti via PATCH /request-leave/{id}.
     *
     * @param leaveId     ID transaksi cuti
     * @param approve     true untuk Approved, false untuk Rejected
     * @param rejectNotes alasan penolakan (diisi jika approve = false)
     * @return true jika HTTP 200/201
     */
    public static boolean reviewLeaveAsAdmin(String leaveId, boolean approve, String rejectNotes) {
        if (leaveId == null || leaveId.isBlank()) {
            return false;
        }
        ApiSession admin = loginAsHybridAdmin();
        Map<String, Object> body = new HashMap<>();
        body.put("is_approved", approve);
        if (!approve && rejectNotes != null && !rejectNotes.isBlank()) {
            body.put("reject_notes", rejectNotes);
        }
        Response res = given()
                .spec(authSpec(admin.token()))
                .body(body)
        .when()
                .patch(Constants.API_REQUEST_LEAVE_ENDPOINT + "/" + leaveId)
        .then()
                .extract()
                .response();
        logger.info("Hybrid Admin PATCH /request-leave/{} (approve={}) -> HTTP {}", leaveId, approve, res.getStatusCode());
        return res.getStatusCode() == 200 || res.getStatusCode() == 201;
    }

    /**
     * Membatalkan pengajuan cuti Pending via {@code PATCH /request-leave/{id}} dengan {@code is_canceled=true}
     * (menghasilkan badge status 'Dibatalkan' pada kartu List Request Cuti).
     */
    public static boolean cancelPendingLeaveAsAdmin(String leaveId) {
        if (leaveId == null || leaveId.isBlank()) {
            return false;
        }
        ApiSession admin = loginAsHybridAdmin();
        Map<String, Object> body = new HashMap<>();
        body.put("is_canceled", true);
        Response res = given()
                .spec(authSpec(admin.token()))
                .body(body)
        .when()
                .patch(Constants.API_REQUEST_LEAVE_ENDPOINT + "/" + leaveId)
        .then()
                .extract()
                .response();
        logger.info("Hybrid Admin PATCH /request-leave/{} (is_canceled=true) -> HTTP {}", leaveId, res.getStatusCode());
        return res.getStatusCode() == 200 || res.getStatusCode() == 201;
    }

    /**
     * Mengambil jumlah total transaksi cuti user tertentu melalui GET /request-leave.
     */
    public static int getUserRequestLeaveCount(String bearerToken, String userId) {
        String effectiveToken = (bearerToken != null && !bearerToken.isBlank())
                ? bearerToken
                : loginAsDefaultUser().token();
        Response res = given()
                .spec(authSpec(effectiveToken))
                .queryParam("user_id", userId)
                .queryParam("$limit", 1)
        .when()
                .get(Constants.API_REQUEST_LEAVE_ENDPOINT)
        .then()
                .extract()
                .response();
        Integer total = res.jsonPath().get("total");
        return total != null ? total : 0;
    }

    /**
     * Mencari ID transaksi cuti terbaru milik user tertentu berdasarkan snippet notes.
     */
    public static String findLatestRequestLeaveIdByNotes(String bearerToken, String userId, String notesSnippet) {
        return findLatestUserRecordIdByField(
                bearerToken, userId, Constants.API_REQUEST_LEAVE_ENDPOINT, "notes", notesSnippet);
    }

    /**
     * Mencari ID transaksi cuti terbaru milik user tertentu (tanpa filter snippet notes).
     */
    public static String findLatestRequestLeaveId(String bearerToken, String userId) {
        String effectiveToken = (bearerToken != null && !bearerToken.isBlank())
                ? bearerToken
                : loginAsDefaultUser().token();
        Response res = given()
                .spec(authSpec(effectiveToken))
                .queryParam("user_id", userId)
                .queryParam("$limit", 1)
                .queryParam("$sort[created_at]", -1)
        .when()
                .get(Constants.API_REQUEST_LEAVE_ENDPOINT)
        .then()
                .extract()
                .response();
        List<String> ids = res.jsonPath().getList("data.id");
        return (ids != null && !ids.isEmpty()) ? ids.get(0) : null;
    }

    /**
     * Seeding pengajuan Sakit (SAKIT) melalui Next.js BFF {@code POST /api/sick}
     * lalu mengembalikan ID record dari {@code GET /activity?status=SAKIT}.
     */
    public static String createPendingSick(
            String userEmail,
            String encodedPassword,
            String userToken,
            String userId,
            String dateFromYmd,
            String dateToYmd,
            String documentId
    ) {
        Map<String, String> cookies = loginBffCookies(userEmail, encodedPassword);
        if (cookies.isEmpty()) {
            return null;
        }
        Map<String, Object> docEntry = new HashMap<>();
        docEntry.put("document", documentId);

        Map<String, Object> body = new HashMap<>();
        body.put("request_type", "SAKIT");
        body.put("sick_request_date_from", dateFromYmd);
        body.put("sick_request_date_to", dateToYmd);
        body.put("time_zone", "+07:00");
        body.put("user_sick_document", List.of(docEntry));

        Response res = given()
                .baseUri(Constants.BASE_URL)
                .relaxedHTTPSValidation()
                .cookies(cookies)
                .contentType("application/json")
                .body(body)
        .when()
                .post(Constants.BFF_SICK_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Seed BFF POST /api/sick -> HTTP {}", res.getStatusCode());
        if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
            return findLatestSickActivityId(userToken, userId);
        }
        logger.warn("Seed BFF /api/sick did not succeed: {}", res.asString());
        return null;
    }

    /**
     * Mengambil jumlah pengajuan Sakit (status=SAKIT) milik user tertentu via GET /activity.
     */
    public static int getUserSickCount(String bearerToken, String userId) {
        String effectiveToken = (bearerToken != null && !bearerToken.isBlank())
                ? bearerToken
                : loginAsDefaultUser().token();
        Response res = given()
                .spec(authSpec(effectiveToken))
                .queryParam("user_id", userId)
                .queryParam("status", "SAKIT")
                .queryParam("$limit", 1)
        .when()
                .get(Constants.API_ACTIVITY_ENDPOINT)
        .then()
                .extract()
                .response();
        Integer total = res.jsonPath().get("total");
        return total != null ? total : 0;
    }

    /**
     * Mencari ID pengajuan Sakit terbaru milik user tertentu via GET /activity?status=SAKIT.
     */
    public static String findLatestSickActivityId(String bearerToken, String userId) {
        String effectiveToken = (bearerToken != null && !bearerToken.isBlank())
                ? bearerToken
                : loginAsDefaultUser().token();
        Response res = given()
                .spec(authSpec(effectiveToken))
                .queryParam("user_id", userId)
                .queryParam("status", "SAKIT")
                .queryParam("$limit", 1)
                .queryParam("$sort[created_at]", -1)
        .when()
                .get(Constants.API_ACTIVITY_ENDPOINT)
        .then()
                .extract()
                .response();
        List<String> ids = res.jsonPath().getList("data.id");
        return (ids != null && !ids.isEmpty()) ? ids.get(0) : null;
    }

    /**
     * Mengirim request POST /share-report dengan token eksplisit (termasuk uji token invalid/otorisasi).
     */
    public static Response postShareReport(
            String bearerToken,
            String companyId,
            String uplinerId,
            String dateFromYmd,
            String dateToYmd
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("date_from", dateFromYmd);
        body.put("date_to", dateToYmd);
        body.put("company_id", companyId);
        body.put("user_id", null);
        body.put("nik", null);
        body.put("job_department_id", null);
        body.put("division_name", null);
        body.put("upliner_id", uplinerId);

        Response res = BaseApiTest.executeWithConnectRetry(() -> given()
                .spec(authSpec(bearerToken))
                .body(body)
        .when()
                .post(Constants.API_SHARE_REPORT_ENDPOINT)
        .then()
                .extract()
                .response(), "POST /share-report");
        logger.info("POST /share-report -> HTTP {}", res.getStatusCode());
        return res;
    }

    /**
     * Menghapus record uji secara aman di tahap @AfterMethod agar data tetap bersih.
     *
     * @param endpointPath path endpoint resource (misal Constants.API_OFF_PERMIT_ENDPOINT)
     * @param recordId     ID record yang akan dihapus
     */
    public static void cleanupRecordQuietly(String endpointPath, String recordId) {
        if (recordId == null || recordId.isBlank()) {
            return;
        }
        try {
            ApiSession admin = loginAsHybridAdmin();
            Response res = given()
                    .spec(authSpec(admin.token()))
            .when()
                    .delete(endpointPath + "/" + recordId)
            .then()
                    .extract()
                    .response();
            logger.info("Cleanup DELETE {}/{} -> HTTP {}", endpointPath, recordId, res.getStatusCode());
        } catch (Exception e) {
            logger.warn("Cleanup skipped for {}/{}: {}", endpointPath, recordId, e.getMessage());
        }
    }

    public static String futureDateYmd(int plusDays) {
        return LocalDate.now().plusDays(plusDays).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String pastDateYmd(int minusDays) {
        return LocalDate.now().minusDays(minusDays).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String isoUtcTimestamp(int plusDays, int hourUtc) {
        Instant instant = LocalDate.now().plusDays(plusDays)
                .atTime(hourUtc, 0)
                .toInstant(ZoneOffset.UTC);
        return instant.toString();
    }
}
