package com.juaracoding.magang.tests.user.deny;

import com.juaracoding.magang.BaseTest;
import com.juaracoding.magang.api.helper.AdminApiHelper;
import com.juaracoding.magang.pages.user.deny.LeaveAndSickPage;
import com.juaracoding.magang.pages.user.deny.PermitPage;
import com.juaracoding.magang.pages.user.deny.UserLoginPage;
import com.juaracoding.magang.reports.ExtentTestManager;
import com.juaracoding.magang.utils.Constants;
import com.juaracoding.magang.utils.SecurityUtil;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pengujian Integrasi Lintas Modul E2E (Hybrid API + UI) untuk Tester Deny.
 * Cakupan SIT v5 (docs/SIT_Scenario_Magang_Hadir.xlsx):
 * SIT_USR_UI_150, SIT_USR_UI_151, SIT_USR_UI_152, SIT_USR_UI_153, dan SIT_USR_UI_154 (5 Test Cases).
 * Penanggung jawab: Tester Deny | Branch: feature/deny-user-e2e.
 */
public class UserCrossModuleDenyE2ETest extends BaseTest {

    private record CleanupRecord(String endpoint, String id) {}
    private record TempUserSession(String email, String userId, String token) {}

    private static String cachedPoolNik;
    private static String cachedLeaveTypeId;

    private final ThreadLocal<List<CleanupRecord>> createdRecords = ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<List<String>> reactivateUserIds = ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<List<String>> createdTempUserEmails = ThreadLocal.withInitial(ArrayList::new);

    @BeforeClass(alwaysRun = true)
    public void resolveSharedFixtureOnce() {
        cachedPoolNik = AdminApiHelper.findAvailableNikFromPool();
        AdminApiHelper.ApiSession defaultSession = AdminApiHelper.loginAsDefaultUser();
        cachedLeaveTypeId = AdminApiHelper.getPreferredLeaveTypeId(defaultSession.token());
        logger.info("Initialized UserCrossModuleDenyE2ETest fixture: poolNik={}, leaveTypeId={}",
                cachedPoolNik, cachedLeaveTypeId);
    }

    private String resolvePoolNik() {
        return (cachedPoolNik != null && !cachedPoolNik.isBlank()) ? cachedPoolNik : Constants.VALID_NIK_POOL[0];
    }

    private void trackCleanup(String endpoint, String id) {
        if (id != null && !id.isBlank()) {
            createdRecords.get().add(new CleanupRecord(endpoint, id));
        }
    }

    private void trackTempUser(String email, String userIdToRestore) {
        if (email != null && !email.isBlank()) {
            createdTempUserEmails.get().add(email);
        }
        if (userIdToRestore != null && !userIdToRestore.isBlank()) {
            reactivateUserIds.get().add(userIdToRestore);
        }
    }

    private TempUserSession provisionTempUserWithAdminUpliner(String prefix, int leaveAmount) {
        String email = prefix + "_" + System.currentTimeMillis() + "@mail.com";
        String userId = AdminApiHelper.createSelfRegisteredMobileUser(
                resolvePoolNik(), "SQA E2E Temp", email, Constants.USER_PASSWORD_ENCODED);
        Assert.assertNotNull(userId, "Provisioning user uji terisolasi harus mengembalikan userId.");
        trackTempUser(email, null);

        boolean activated = AdminApiHelper.activateUserWithAdminUplinerAndLeave(userId, leaveAmount);
        Assert.assertTrue(activated, "Aktivasi user uji & penyetelan upliner Admin harus berhasil.");

        AdminApiHelper.ApiSession session = AdminApiHelper.loginAsUser(email, Constants.USER_PASSWORD_ENCODED);
        return new TempUserSession(email, session.userId(), session.token());
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupCrossModuleE2EData() {
        List<CleanupRecord> records = createdRecords.get();
        try {
            for (CleanupRecord r : records) {
                AdminApiHelper.cleanupRecordQuietly(r.endpoint(), r.id());
            }
        } finally {
            records.clear();
            createdRecords.remove();
        }

        List<String> restoreIds = reactivateUserIds.get();
        try {
            for (String uid : restoreIds) {
                AdminApiHelper.updateUserActiveStatusAsAdmin(uid, true);
            }
        } catch (Exception e) {
            logger.warn("Best-effort reactivation skipped: {}", e.getMessage());
        } finally {
            restoreIds.clear();
            reactivateUserIds.remove();
        }

        List<String> emails = createdTempUserEmails.get();
        try {
            for (String email : emails) {
                AdminApiHelper.cleanupUserByEmailQuietly(email);
            }
        } finally {
            emails.clear();
            createdTempUserEmails.remove();
        }
    }

    private void loginInBrowser(String email) {
        UserLoginPage loginPage = new UserLoginPage(getDriver());
        loginPage.navigateToLoginPage();
        loginPage.login(email, SecurityUtil.decode(Constants.USER_PASSWORD_ENCODED));
        Assert.assertTrue(loginPage.waitForRedirectToAbsent(),
                "Prasyarat login User gagal menuju /apps/absent. URL aktual: " + loginPage.getCurrentUrl());
    }

    @Test(priority = 150,
            description = "SIT_USR_UI_150: [E2E] Registrasi mandiri disetujui Admin via PATCH /users/{id} (isVerified=true) -> Karyawan berhasil login di /absen/login",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_150_AdminApprovesSelfRegistrationThenUserLogsIn() {
        String email = "e2e_selfreg_" + System.currentTimeMillis() + "@mail.com";
        String nik = resolvePoolNik();
        ExtentTestManager.logInfo("SIT_USR_UI_150 [Hybrid API+UI] - Seed registrasi mandiri POST /users (is_mobile=true) NIK=" + nik);

        String userId = AdminApiHelper.createSelfRegisteredMobileUser(
                nik, "SQA E2E SelfReg", email, Constants.USER_PASSWORD_ENCODED);
        trackTempUser(email, null);
        Assert.assertNotNull(userId, "Seed registrasi mandiri POST /users harus mengembalikan userId.");

        ExtentTestManager.logInfo("Step 2 (Admin API): Setujui akun via PATCH /users/" + userId + " (is_active=true, isVerified=true).");
        boolean approved = AdminApiHelper.updateUserActiveStatusAsAdmin(userId, true);
        Assert.assertTrue(approved, "Approval akun oleh Admin via PATCH /users/{id} harus berhasil.");

        ExtentTestManager.logInfo("Step 3 (User UI): Login sebagai user hasil approve di /absen/login.");
        UserLoginPage loginPage = new UserLoginPage(getDriver());
        loginPage.navigateToLoginPage();
        loginPage.login(email, SecurityUtil.decode(Constants.USER_PASSWORD_ENCODED));
        boolean redirected = loginPage.waitForRedirectToAbsent();
        String currentUrl = loginPage.getCurrentUrl();
        ExtentTestManager.logInfo("URL pasca login user yang disetujui Admin: " + currentUrl);

        Assert.assertTrue(redirected && currentUrl.contains("/apps/absent"),
                "User hasil registrasi mandiri yang disetujui Admin harus berhasil masuk ke /apps/absent. Aktual: " + currentUrl);
    }

    @Test(priority = 151,
            description = "SIT_USR_UI_151: [E2E] Admin mengubah status akun Karyawan Non-aktif (is_active=false) -> percobaan login di /absen/login ditolak",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_151_AdminDeactivatesUserThenLoginRejected() {
        TempUserSession tempUser = provisionTempUserWithAdminUpliner("e2e_deact", -1);
        trackTempUser(null, tempUser.userId());
        ExtentTestManager.logInfo("SIT_USR_UI_151 [Hybrid API+UI] - Menonaktifkan akun uji sementara via PATCH /users/" + tempUser.userId() + " (is_active=false).");

        boolean deactivated = AdminApiHelper.updateUserActiveStatusAsAdmin(tempUser.userId(), false);
        Assert.assertTrue(deactivated, "Penonaktifan akun uji via PATCH /users/{id} (is_active=false) harus berhasil.");

        UserLoginPage loginPage = new UserLoginPage(getDriver());
        loginPage.navigateToLoginPage();
        loginPage.login(tempUser.email(), SecurityUtil.decode(Constants.USER_PASSWORD_ENCODED));

        boolean alertDisplayed = loginPage.isAlertMessageDisplayed();
        String alertText = loginPage.getAlertText();
        String currentUrl = loginPage.getCurrentUrl();
        ExtentTestManager.logInfo("Hasil percobaan login akun non-aktif -> URL=[" + currentUrl + "], Alert=[" + alertText + "]");

        Assert.assertTrue(alertDisplayed,
                "Alert penolakan login harus tampil untuk akun berstatus non-aktif (is_active=false).");
        Assert.assertTrue(currentUrl.contains("/absen/login") && !currentUrl.contains("/apps/absent"),
                "Akun non-aktif wajib ditolak login dan tetap berada di /absen/login. Aktual: " + currentUrl);
    }

    @Test(priority = 152,
            description = "SIT_USR_UI_152: [E2E] Admin menyetujui Izin Off via API -> status pada kartu /apps/absent/permit?permit=off-permit Karyawan berubah menjadi Disetuji",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_152_AdminApprovesOffPermitSyncedToUserUi() {
        TempUserSession tempUser = provisionTempUserWithAdminUpliner("e2e_off_app", -1);
        String reason = "E2E_Approve_Off_" + System.currentTimeMillis();
        String dateOff = AdminApiHelper.futureDateYmd(14);
        ExtentTestManager.logInfo("SIT_USR_UI_152 [Hybrid API+UI] - Seed Izin Off + Approve Admin via PATCH /off-permit/{id}.");

        String permitId = AdminApiHelper.createPendingOffPermit(tempUser.token(), dateOff, reason);
        trackCleanup(Constants.API_OFF_PERMIT_ENDPOINT, permitId);
        Assert.assertNotNull(permitId, "Seed POST /off-permit harus mengembalikan permitId.");

        boolean approved = AdminApiHelper.reviewOffPermitAsAdmin(permitId, true, null);
        Assert.assertTrue(approved, "Approval Admin PATCH /off-permit/{id} harus berhasil.");

        loginInBrowser(tempUser.email());
        PermitPage permitPage = new PermitPage(getDriver());
        permitPage.navigateDirectToTab("off-permit");

        String cardText = permitPage.findPermitCardTextContaining(reason);
        ExtentTestManager.logInfo("Kartu Izin Off terverifikasi di UI:\n" + cardText);

        Assert.assertTrue(permitPage.cardHasFiveAttributes(cardText, "Izin Off") && cardText.contains("Disetuji"),
                "Kartu Izin Off di UI Karyawan wajib berstatus 'Disetuji' dengan 5 atribut lengkap. Aktual:\n" + cardText);
    }

    @Test(priority = 153,
            description = "SIT_USR_UI_153: [E2E] Admin menolak Izin Off dengan alasan -> status Ditolak & teks Alasan Ditolak tampil di /apps/absent/permit",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_153_AdminRejectsOffPermitWithReasonSyncedToUserUi() {
        TempUserSession tempUser = provisionTempUserWithAdminUpliner("e2e_off_rej", -1);
        String reason = "E2E_Reject_Off_" + System.currentTimeMillis();
        String rejectNotes = "Kuota tim operasional penuh E2E " + System.currentTimeMillis();
        String dateOff = AdminApiHelper.futureDateYmd(15);
        ExtentTestManager.logInfo("SIT_USR_UI_153 [Hybrid API+UI] - Seed Izin Off + Reject Admin dengan alasan spesifik.");

        String permitId = AdminApiHelper.createPendingOffPermit(tempUser.token(), dateOff, reason);
        trackCleanup(Constants.API_OFF_PERMIT_ENDPOINT, permitId);
        Assert.assertNotNull(permitId, "Seed POST /off-permit harus mengembalikan permitId.");

        boolean rejected = AdminApiHelper.reviewOffPermitAsAdmin(permitId, false, rejectNotes);
        Assert.assertTrue(rejected, "Rejection Admin PATCH /off-permit/{id} harus berhasil.");

        loginInBrowser(tempUser.email());
        PermitPage permitPage = new PermitPage(getDriver());
        permitPage.navigateDirectToTab("off-permit");

        String cardText = permitPage.findPermitCardTextContaining(reason);
        ExtentTestManager.logInfo("Kartu Izin Off Rejected terverifikasi di UI:\n" + cardText);

        Assert.assertTrue(permitPage.cardHasFiveAttributes(cardText, "Izin Off")
                        && cardText.contains("Ditolak")
                        && cardText.contains(rejectNotes),
                "Kartu Izin Off di UI wajib berstatus 'Ditolak' & menampilkan Alasan Ditolak [" + rejectNotes + "]. Aktual:\n" + cardText);
    }

    @Test(priority = 154,
            description = "SIT_USR_UI_154: [E2E] Admin membatalkan Cuti Karyawan via POST /cancel-leave -> status 'Dibatalkan' & kuota 'Info Cuti' di /apps/absent/leave-client sinkron",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_154_AdminCancelsLeaveSyncedToCardAndInfoCutiQuota() {
        TempUserSession tempUser = provisionTempUserWithAdminUpliner("e2e_cancel_leave", 12);
        String leaveNotes = "E2E_Cancel_Leave_" + System.currentTimeMillis();
        String cancelNotes = "Pembatalan cuti E2E HRD " + System.currentTimeMillis();
        String d1 = AdminApiHelper.futureDateYmd(2);
        String d2 = AdminApiHelper.futureDateYmd(3);
        ExtentTestManager.logInfo("SIT_USR_UI_154 [Hybrid API+UI] - Buat pengajuan cuti aktif, catat GET /project-leave/{userId}, lalu Admin batalkan cuti.");

        String leaveTypeId = (cachedLeaveTypeId != null && !cachedLeaveTypeId.isBlank())
                ? cachedLeaveTypeId
                : AdminApiHelper.getPreferredLeaveTypeId(tempUser.token());
        String leaveId = AdminApiHelper.createPendingLeave(
                tempUser.email(), Constants.USER_PASSWORD_ENCODED, leaveTypeId, d1, d2, leaveNotes);
        trackCleanup(Constants.API_REQUEST_LEAVE_ENDPOINT, leaveId);
        Assert.assertNotNull(leaveId, "Prasyarat pembuatan pengajuan cuti via /api/leave harus berhasil.");

        Response quotaBeforeRes = AdminApiHelper.getProjectLeaveSummaryByUserId(tempUser.token(), tempUser.userId());
        Assert.assertEquals(quotaBeforeRes.getStatusCode(), 200, "GET /project-leave/{userId} awal harus HTTP 200.");
        int sisaCutiBefore = quotaBeforeRes.jsonPath().getInt("sisa_cuti");
        ExtentTestManager.logInfo("Kuota sebelum cancel -> sisa_cuti=" + sisaCutiBefore + " | body=" + quotaBeforeRes.asString());

        boolean canceled = AdminApiHelper.cancelLeaveAsAdmin(leaveId, cancelNotes);
        Assert.assertTrue(canceled, "Pembatalan cuti oleh Admin via POST /api/cancel/leave harus berhasil.");

        Response detailRes = AdminApiHelper.getRequestLeaveById(leaveId);
        Assert.assertEquals(detailRes.getStatusCode(), 200, "GET /request-leave/{id} harus HTTP 200.");
        Boolean isCanceledApi = detailRes.jsonPath().get("is_canceled");
        String rejectNotesApi = detailRes.jsonPath().getString("reject_notes");
        Assert.assertEquals(isCanceledApi, Boolean.TRUE, "Backend request-leave wajib menandai is_canceled=true pasca pembatalan.");
        Assert.assertTrue(rejectNotesApi != null && rejectNotesApi.contains(cancelNotes),
                "Backend reject_notes wajib menyimpan alasan cancel Admin. Aktual: " + rejectNotesApi);

        Response quotaAfterRes = AdminApiHelper.getProjectLeaveSummaryByUserId(tempUser.token(), tempUser.userId());
        Assert.assertEquals(quotaAfterRes.getStatusCode(), 200, "GET /project-leave/{userId} pasca cancel harus HTTP 200.");
        int apiTotalCuti = quotaAfterRes.jsonPath().getInt("total_cuti");
        int apiCutiTersedia = quotaAfterRes.jsonPath().getInt("cuti_available");
        int apiCutiBersama = quotaAfterRes.jsonPath().getInt("mass_leave");
        int apiCutiDisetujui = quotaAfterRes.jsonPath().getInt("cuti_approve");
        int apiCutiDiajukan = quotaAfterRes.jsonPath().getInt("cuti_request");
        int apiSisaCuti = quotaAfterRes.jsonPath().getInt("sisa_cuti");
        ExtentTestManager.logInfo("Kuota pasca cancel -> sisa_cuti=" + apiSisaCuti + " | body=" + quotaAfterRes.asString());
        Assert.assertTrue(apiSisaCuti >= sisaCutiBefore,
                "Kuota sisa_cuti pasca pembatalan harus tetap/kembali utuh (>= " + sisaCutiBefore + "). Aktual: " + apiSisaCuti);

        loginInBrowser(tempUser.email());
        LeaveAndSickPage leavePage = new LeaveAndSickPage(getDriver());
        leavePage.navigateToLeaveFromHome();

        String cardText = leavePage.findCardTextContaining(leaveNotes);
        ExtentTestManager.logInfo("Kartu Cuti Dibatalkan pada UI:\n" + cardText);
        Assert.assertFalse(cardText.isEmpty(), "Kartu cuti [" + leaveNotes + "] harus ditemukan pada List Request Cuti.");
        Assert.assertTrue(cardText.contains("Dibatalkan") && cardText.contains(cancelNotes),
                "Kartu cuti wajib menampilkan status 'Dibatalkan' dan Reject Notes [" + cancelNotes + "]. Aktual:\n" + cardText);

        leavePage.openLeaveDrawer();
        leavePage.clickInfoCutiTab();
        Map<String, Integer> uiMetrics = leavePage.getInfoCutiMetricsMap();
        ExtentTestManager.logInfo("Info Cuti UI pasca cancel: " + uiMetrics);

        Assert.assertEquals(uiMetrics.get("Total Cuti"), Integer.valueOf(apiTotalCuti));
        Assert.assertEquals(uiMetrics.get("Total cuti tersedia"), Integer.valueOf(apiCutiTersedia));
        Assert.assertEquals(uiMetrics.get("Total cuti bersama"), Integer.valueOf(apiCutiBersama));
        Assert.assertEquals(uiMetrics.get("Total cuti disetujui"), Integer.valueOf(apiCutiDisetujui));
        Assert.assertEquals(uiMetrics.get("Total cuti yang sedang diajukan"), Integer.valueOf(apiCutiDiajukan));
        Assert.assertEquals(uiMetrics.get("Total sisa cuti (Annual)"), Integer.valueOf(apiSisaCuti));
    }
}

