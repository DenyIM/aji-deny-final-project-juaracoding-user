package com.juaracoding.magang.api.helper;

import com.juaracoding.magang.api.BaseApiTest;
import com.juaracoding.magang.utils.Constants;
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

