package com.juaracoding.magang.api.helper;

import com.juaracoding.magang.api.BaseApiTest;
import com.juaracoding.magang.utils.Constants;
import com.juaracoding.magang.utils.SecurityUtil;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Helper untuk pola Hybrid API + UI di proyek Karyawan.
 * Menggunakan REST Assured pada tahap @BeforeMethod / @BeforeClass untuk:
 * 1. Seeding pengajuan Karyawan (Izin Off, Koreksi Absen, Lembur) menggunakan akun user.
 * 2. Menyetujui (Approve) atau Menolak (Reject) pengajuan beserta alasan penolakan menggunakan
 *    kredensial Admin HRD (HYBRID_ADMIN_PASSWORD_ENCODED) agar UI Karyawan dapat langsung
 *    memverifikasi badge status dan teks alasan penolakan secara deterministik.
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
     * Login sebagai Admin HRD via POST /authentication untuk melakukan approval/rejection
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
     * Mengambil ringkasan sisa cuti karyawan via GET /project-leave.
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
     * Mengubah status aktif/non-aktif akun Karyawan oleh Admin via PATCH /users/{id}
     * (kebutuhan E2E SIT_USR_UI_130–131).
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
        body.put("status", isActive ? "ACTIVE" : "INACTIVE");

        Response res = given()
                .spec(authSpec(admin.token()))
                .body(body)
        .when()
                .patch(Constants.API_USERS_ENDPOINT + "/" + userId)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Admin PATCH /users/{} (is_active={}) -> HTTP {}", userId, isActive, res.getStatusCode());
        return res.getStatusCode() == 200 || res.getStatusCode() == 201;
    }

    /**
     * Membatalkan pengajuan cuti Karyawan oleh Admin pada /project-leave/{id}
     * (kebutuhan E2E SIT_USR_UI_134). Mencoba PATCH status CANCELLED/REJECTED terlebih dahulu,
     * lalu fallback DELETE jika endpoint cancel menggunakan penghapusan record.
     *
     * @param leaveId ID pengajuan cuti pada /project-leave
     * @return true jika pembatalan berhasil (HTTP 200/201/204)
     */
    public static boolean cancelLeaveAsAdmin(String leaveId) {
        if (leaveId == null || leaveId.isBlank()) {
            logger.warn("Cannot cancel blank leaveId");
            return false;
        }
        ApiSession admin = loginAsHybridAdmin();
        Map<String, Object> body = new HashMap<>();
        body.put("status", "CANCELLED");
        body.put("is_cancel", true);
        body.put("is_approve", false);

        Response patchRes = given()
                .spec(authSpec(admin.token()))
                .body(body)
        .when()
                .patch(Constants.API_PROJECT_LEAVE_ENDPOINT + "/" + leaveId)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Admin PATCH /project-leave/{} (cancel) -> HTTP {}", leaveId, patchRes.getStatusCode());
        if (patchRes.getStatusCode() == 200 || patchRes.getStatusCode() == 201) {
            return true;
        }

        Response deleteRes = given()
                .spec(authSpec(admin.token()))
        .when()
                .delete(Constants.API_PROJECT_LEAVE_ENDPOINT + "/" + leaveId)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Admin DELETE /project-leave/{} (cancel fallback) -> HTTP {}", leaveId, deleteRes.getStatusCode());
        return deleteRes.getStatusCode() == 200 || deleteRes.getStatusCode() == 204;
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
     * Menginspeksi data kuota/riwayat project-leave milik user tertentu oleh Admin
     * via GET /project-leave?user_id={userId} (kebutuhan E2E SIT_USR_UI_140 [Known Defect]).
     *
     * @param userId ID user yang diperiksa
     * @return Response GET /project-leave
     */
    public static Response queryProjectLeaveByUserAsAdmin(String userId) {
        ApiSession admin = loginAsHybridAdmin();
        Response res = given()
                .spec(authSpec(admin.token()))
                .queryParam("user_id", userId)
        .when()
                .get(Constants.API_PROJECT_LEAVE_ENDPOINT)
        .then()
                .extract()
                .response();

        logger.info("Hybrid Admin GET /project-leave?user_id={} -> HTTP {}", userId, res.getStatusCode());
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

