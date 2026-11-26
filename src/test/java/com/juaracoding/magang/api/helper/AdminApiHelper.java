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

        RequestSpecification req = given()
                .baseUri(Constants.API_BASE_URI)
                .relaxedHTTPSValidation()
                .multiPart("uri", filePath.toFile(), mimeType);
        if (bearerToken != null && !bearerToken.isBlank()) {
            req.header("Authorization", "Bearer " + bearerToken.trim());
        }

        Response res = req
                .when()
                .post(Constants.API_UPLOAD_ENDPOINT)
                .then()
                .extract()
                .response();

        logger.info("Hybrid POST /upload [{}] (mime={}) -> HTTP {}", fileName, mimeType, res.getStatusCode());
        if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
            return res.jsonPath().getString("id");
        }
        logger.warn("Upload failed: {}", res.asString());
        return null;
    }

    /**
     * Seeding pengajuan Izin Off sebagai Karyawan via POST /off-permit.
     *
     * @param dateOff tanggal izin (yyyy-MM-dd)
     * @param reason  alasan izin
     * @return ID record off-permit yang dibuat (atau null jika gagal)
     */
    public static String createPendingOffPermit(String dateOff, String reason) {
        ApiSession session = loginAsDefaultUser();
        Map<String, Object> body = new HashMap<>();
        body.put("date_off", dateOff);
        body.put("reason", reason);

        Response res = given()
                .spec(authSpec(session.token()))
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
     * Admin menyetujui atau menolak pengajuan Izin Off via PATCH /off-permit/{id}.
     *
     * @param permitId     ID record off-permit
     * @param approve      true untuk Approved, false untuk Rejected
     * @param rejectReason alasan penolakan (diisi jika approve = false)
     * @return true jika HTTP 200/201
     */
    public static boolean reviewOffPermitAsAdmin(String permitId, boolean approve, String rejectReason) {
        return patchApprovalStatus(Constants.API_OFF_PERMIT_ENDPOINT, permitId, approve, rejectReason);
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
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", session.userId());
        body.put("late_time_in", lateTimeInIso);
        body.put("late_reason", reason);
        body.put("time_zone", "+07:00");

        Response res = given()
                .spec(authSpec(session.token()))
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
        List<String> ids = res.jsonPath().getList("data.id");
        return (ids != null && !ids.isEmpty()) ? ids.get(0) : null;
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

        Response res = given()
                .spec(authSpec(admin.token()))
                .body(body)
        .when()
                .patch(Constants.API_USERS_ENDPOINT + "/" + userId)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Admin PATCH /users/{} (is_active={}, isVerified=true) -> HTTP {}",
                userId, isActive, res.getStatusCode());
        return res.getStatusCode() == 200 || res.getStatusCode() == 201;
    }

    /**
     * Membatalkan pengajuan cuti Karyawan oleh Admin melalui endpoint terverifikasi
     * {@code POST /cancel-leave} (kebutuhan E2E SIT_USR_UI_134).
     *
     * @param leaveTransId ID transaksi cuti
     * @return true jika HTTP 200/201
     */
    public static boolean cancelLeaveAsAdmin(String leaveTransId) {
        return cancelLeaveAsAdmin(leaveTransId, "E2E Cancel Cuti Admin Automation");
    }

    /**
     * Membatalkan pengajuan cuti Karyawan oleh Admin melalui endpoint terverifikasi
     * {@code POST /cancel-leave} dengan payload {@code leave_trans_id} dan {@code lv_approve_notes}
     * (kebutuhan E2E SIT_USR_UI_134).
     *
     * @param leaveTransId ID transaksi cuti (leave_trans_id)
     * @param approveNotes alasan pembatalan cuti (lv_approve_notes)
     * @return true jika pembatalan berhasil (HTTP 200/201)
     */
    public static boolean cancelLeaveAsAdmin(String leaveTransId, String approveNotes) {
        if (leaveTransId == null || leaveTransId.isBlank()) {
            logger.warn("Cannot cancel blank leaveTransId");
            return false;
        }
        ApiSession admin = loginAsHybridAdmin();
        Map<String, Object> body = new HashMap<>();
        body.put("leave_trans_id", leaveTransId);
        body.put("lv_approve_notes",
                (approveNotes != null && !approveNotes.isBlank()) ? approveNotes : "E2E Cancel Cuti Admin");

        Response res = given()
                .spec(authSpec(admin.token()))
                .body(body)
        .when()
                .post(Constants.API_CANCEL_LEAVE_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Admin POST /cancel-leave (leave_trans_id={}) -> HTTP {}", leaveTransId, res.getStatusCode());
        return res.getStatusCode() == 200 || res.getStatusCode() == 201;
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
        Response res = given()
                .spec(authSpec(effectiveToken))
        .when()
                .get(Constants.API_PROJECT_LEAVE_ENDPOINT + "/" + userId)
        .then()
                .extract()
                .response();

        logger.info("Hybrid GET /project-leave/{} -> HTTP {}", userId, res.getStatusCode());
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
