package com.juaracoding.magang.tests.user.deny;

import com.juaracoding.magang.BaseTest;
import com.juaracoding.magang.api.helper.AdminApiHelper;
import com.juaracoding.magang.pages.user.deny.LeaveAndSickPage;
import com.juaracoding.magang.pages.user.deny.UserLoginPage;
import com.juaracoding.magang.reports.ExtentTestManager;
import com.juaracoding.magang.utils.Constants;
import com.juaracoding.magang.utils.SecurityUtil;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pengujian Web UI Selenium untuk fitur Pengajuan-Cuti (/apps/absent/leave-client)
 * dan Pengajuan-Sakit (/apps/absent/sick) pada Mobile Mirroring Karyawan.
 * Cakupan skenario: SIT_USR_UI_103 s/d SIT_USR_UI_126 (24 Test Cases).
 * Penanggung jawab: Tester Deny | Branch: feature/deny-user-leave-sick.
 */
public class LeaveAndSickTest extends BaseTest {

    private record CleanupRecord(String endpoint, String id) {}
    private record TempUserSession(String email, String userId, String token) {}

    private static String cachedPoolNik;
    private static String cachedLeaveTypeId;
    private static String cachedSickDocId;

    private final ThreadLocal<List<CleanupRecord>> createdRecords = ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<List<String>> createdTempUserEmails = ThreadLocal.withInitial(ArrayList::new);

    @BeforeClass(alwaysRun = true)
    public void resolveSharedFixtureOnce() {
        cachedPoolNik = AdminApiHelper.findAvailableNikFromPool();
        AdminApiHelper.ApiSession defaultSession = AdminApiHelper.loginAsDefaultUser();
        cachedLeaveTypeId = AdminApiHelper.getPreferredLeaveTypeId(defaultSession.token());
        cachedSickDocId = AdminApiHelper.uploadTestPhoto(defaultSession.token(), "valid_photo.jpg");
        logger.info("Initialized LeaveAndSickTest fixture: poolNik={}, leaveTypeId={}, sickDocId={}",
                cachedPoolNik, cachedLeaveTypeId, cachedSickDocId);
    }

    private void trackCleanup(String endpoint, String id) {
        if (id != null && !id.isBlank()) {
            createdRecords.get().add(new CleanupRecord(endpoint, id));
        }
    }

    private TempUserSession provisionTempUserWithLeave(String prefix, int leaveAmount) {
        String email = prefix + "_" + System.currentTimeMillis() + "@mail.com";
        String nik = (cachedPoolNik != null && !cachedPoolNik.isBlank()) ? cachedPoolNik : Constants.VALID_NIK_POOL[0];
        String userId = AdminApiHelper.createSelfRegisteredMobileUser(
                nik, "SQA LeaveSick Temp", email, Constants.USER_PASSWORD_ENCODED);
        Assert.assertNotNull(userId, "Provisioning user uji terisolasi harus mengembalikan userId.");
        createdTempUserEmails.get().add(email);
        boolean activated = AdminApiHelper.activateUserWithAdminUplinerAndLeave(userId, leaveAmount);
        Assert.assertTrue(activated, "Aktivasi user uji & penyetelan upliner Admin + leave_amount harus berhasil.");
        AdminApiHelper.ApiSession session = AdminApiHelper.loginAsUser(email, Constants.USER_PASSWORD_ENCODED);
        return new TempUserSession(email, session.userId(), session.token());
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupSeededAndSubmittedData() {
        List<CleanupRecord> records = createdRecords.get();
        try {
            for (CleanupRecord r : records) {
                AdminApiHelper.cleanupRecordQuietly(r.endpoint(), r.id());
            }
        } finally {
            records.clear();
            createdRecords.remove();
        }
        List<String> tempEmails = createdTempUserEmails.get();
        try {
            for (String email : tempEmails) {
                AdminApiHelper.cleanupUserByEmailQuietly(email);
            }
        } finally {
            tempEmails.clear();
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

    private LeaveAndSickPage loginAndOpenLeave(String email) {
        loginInBrowser(email);
        LeaveAndSickPage page = new LeaveAndSickPage(getDriver());
        page.navigateToLeaveFromHome();
        return page;
    }

    private LeaveAndSickPage loginAndOpenSick(String email) {
        loginInBrowser(email);
        LeaveAndSickPage page = new LeaveAndSickPage(getDriver());
        page.navigateToSickFromHome();
        return page;
    }

    private String uniqueTag(String prefix) {
        return prefix + "_" + System.currentTimeMillis();
    }

    // =========================================================================
    // PENGAJUAN-CUTI (SIT_USR_UI_103 s/d SIT_USR_UI_115)
    // =========================================================================

    @Test(priority = 103, description = "SIT_USR_UI_103: Navigasi dari Menu 'Cuti' ke /apps/absent/leave-client & verifikasi header + 'Total : N'",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_103_NavigateToLeavePageAndVerifyHeader() {
        LeaveAndSickPage page = loginAndOpenLeave(Constants.USER_EMAIL);
        String currentUrl = page.getCurrentUrl();
        String totalText = page.getTotalText();
        ExtentTestManager.logInfo("SIT_USR_UI_103 - URL=[" + currentUrl + "], Total=[" + totalText + "]");

        Assert.assertTrue(currentUrl.contains("/apps/absent/leave-client"),
                "URL harus mengarah ke /apps/absent/leave-client. Aktual: " + currentUrl);
        Assert.assertTrue(page.isLeaveHeaderDisplayed(),
                "Header 'Halaman Cuti' dan subtitle 'List Request Cuti' harus tampil.");
        Assert.assertTrue(totalText.startsWith("Total"),
                "Label 'Total : N' harus tampil pada halaman Cuti. Aktual: " + totalText);
    }

    @Test(priority = 104, description = "SIT_USR_UI_104: Buka drawer 'Ajukan Cuti' & klik tab 'Info Cuti' untuk verifikasi 6 metrik kuota cuti user",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_104_VerifySixInfoCutiMetricsMatchApi() {
        AdminApiHelper.ApiSession session = AdminApiHelper.loginAsDefaultUser();
        Response apiRes = AdminApiHelper.getProjectLeaveSummaryByUserId(session.token(), session.userId());
        Assert.assertEquals(apiRes.getStatusCode(), 200, "GET /project-leave/{userId} harus mengembalikan HTTP 200.");

        int apiTotalCuti = apiRes.jsonPath().getInt("total_cuti");
        int apiCutiTersedia = apiRes.jsonPath().getInt("cuti_available");
        int apiCutiBersama = apiRes.jsonPath().getInt("mass_leave");
        int apiCutiDisetujui = apiRes.jsonPath().getInt("cuti_approve");
        int apiCutiDiajukan = apiRes.jsonPath().getInt("cuti_request");
        int apiSisaCuti = apiRes.jsonPath().getInt("sisa_cuti");

        LeaveAndSickPage page = loginAndOpenLeave(Constants.USER_EMAIL);
        page.openLeaveDrawer();
        page.clickInfoCutiTab();
        Map<String, Integer> uiMetrics = page.getInfoCutiMetricsMap();
        ExtentTestManager.logInfo("SIT_USR_UI_104 - UI Info Cuti metrics: " + uiMetrics);

        Assert.assertEquals(uiMetrics.get("Total Cuti"), Integer.valueOf(apiTotalCuti));
        Assert.assertEquals(uiMetrics.get("Total cuti tersedia"), Integer.valueOf(apiCutiTersedia));
        Assert.assertEquals(uiMetrics.get("Total cuti bersama"), Integer.valueOf(apiCutiBersama));
        Assert.assertEquals(uiMetrics.get("Total cuti disetujui"), Integer.valueOf(apiCutiDisetujui));
        Assert.assertEquals(uiMetrics.get("Total cuti yang sedang diajukan"), Integer.valueOf(apiCutiDiajukan));
        Assert.assertEquals(uiMetrics.get("Total sisa cuti (Annual)"), Integer.valueOf(apiSisaCuti));
    }

    @Test(priority = 105, description = "SIT_USR_UI_105: Submit pengajuan Cuti pada tab 'Form Cuti' dengan Tipe Cuti, rentang tanggal, & Catatan valid",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_105_SubmitValidLeaveRequest() {
        TempUserSession tempUser = provisionTempUserWithLeave("cuti_valid", 12);
        LeaveAndSickPage page = loginAndOpenLeave(tempUser.email());
        String notes = uniqueTag("SQA_Cuti_Valid");
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);
        ExtentTestManager.logInfo("SIT_USR_UI_105 - Mengajukan cuti valid [" + start + " s/d " + end + "], notes=" + notes);

        page.openLeaveDrawer();
        page.selectPreferredLeaveType();
        page.openDrawerCalendarModal();
        page.selectCalendarDateRange(start, end);
        page.clickCalendarSimpan();
        page.enterLeaveNotes(notes);
        String snackbar = page.submitAndCaptureSnackbar();
        page.waitForLeavePageReady();
        String createdId = AdminApiHelper.findLatestRequestLeaveIdByNotes(tempUser.token(), tempUser.userId(), notes);
        trackCleanup(Constants.API_REQUEST_LEAVE_ENDPOINT, createdId);
        int afterTotal = page.getTotalCount();
        String savedCard = page.findCardTextContaining(notes);
        ExtentTestManager.logInfo("Snackbar Cuti: [" + snackbar + "], createdId=[" + createdId + "], Total=[" + afterTotal + "]");

        Assert.assertNotNull(createdId, "Record pengajuan cuti harus tersimpan di backend /request-leave.");
        Assert.assertTrue(afterTotal >= 1 && !savedCard.isEmpty(),
                "Kartu pengajuan cuti baru harus tampil di List Request Cuti pasca submit. Total=" + afterTotal);
        Assert.assertTrue(snackbar.isEmpty() || snackbar.contains("Berhasil melakukan request cuti"),
                "Jika snackbar tertangkap sebelum router.reload(), harus berisi 'Berhasil melakukan request cuti'. Aktual: " + snackbar);
    }

    @Test(priority = 106, description = "SIT_USR_UI_106: Submit pengajuan Cuti tanpa memilih Tipe Cuti",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_106_SubmitLeaveWithoutLeaveTypeRejected() {
        TempUserSession tempUser = provisionTempUserWithLeave("cuti_notype", 12);
        LeaveAndSickPage page = loginAndOpenLeave(tempUser.email());
        String notes = uniqueTag("SQA_Cuti_NoType");
        LocalDate start = LocalDate.now().plusDays(1);
        ExtentTestManager.logInfo("SIT_USR_UI_106 - Submit form Cuti tanpa Tipe Cuti (#leave_type_id kosong).");

        page.openLeaveDrawer();
        page.openDrawerCalendarModal();
        page.selectCalendarDateRange(start, start);
        page.clickCalendarSimpan();
        page.enterLeaveNotes(notes);
        String snackbar = page.submitAndCaptureSnackbar();
        String createdId = AdminApiHelper.findLatestRequestLeaveIdByNotes(tempUser.token(), tempUser.userId(), notes);
        trackCleanup(Constants.API_REQUEST_LEAVE_ENDPOINT, createdId);
        ExtentTestManager.logInfo("Hasil submit tanpa Tipe Cuti: snackbar=[" + snackbar + "], createdId=" + createdId);

        Assert.assertNull(createdId, "Pengajuan tanpa Tipe Cuti tidak boleh tersimpan ke database.");
        Assert.assertFalse(snackbar.toLowerCase().contains("berhasil"),
                "Tidak boleh muncul snackbar sukses saat Tipe Cuti kosong. Aktual: " + snackbar);
        Assert.assertTrue(!snackbar.isEmpty() || page.isDrawerOpen(),
                "Sistem harus menolak pengajuan dengan pesan validasi/error saat Tipe Cuti kosong.");
    }

    @Test(priority = 107, description = "SIT_USR_UI_107: Submit pengajuan Cuti tanpa memilih tanggal cuti pada 'Pilih Tanggal'",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_107_SubmitLeaveWithoutDateRangeRejected() {
        TempUserSession tempUser = provisionTempUserWithLeave("cuti_nodate", 12);
        LeaveAndSickPage page = loginAndOpenLeave(tempUser.email());
        String notes = uniqueTag("SQA_Cuti_NoDate");
        ExtentTestManager.logInfo("SIT_USR_UI_107 - Submit form Cuti tanpa memilih tanggal.");

        page.openLeaveDrawer();
        page.selectPreferredLeaveType();
        page.enterLeaveNotes(notes);
        String snackbar = page.submitAndCaptureSnackbar();
        String createdId = AdminApiHelper.findLatestRequestLeaveIdByNotes(tempUser.token(), tempUser.userId(), notes);
        trackCleanup(Constants.API_REQUEST_LEAVE_ENDPOINT, createdId);
        ExtentTestManager.logInfo("Hasil submit tanpa tanggal: snackbar=[" + snackbar + "], createdId=" + createdId);

        Assert.assertNull(createdId, "Pengajuan tanpa tanggal tidak boleh tersimpan ke database.");
        Assert.assertFalse(snackbar.toLowerCase().contains("berhasil"),
                "Tidak boleh muncul snackbar sukses saat tanggal cuti kosong. Aktual: " + snackbar);
        Assert.assertTrue(!snackbar.isEmpty() || page.isDrawerOpen(),
                "Sistem harus menolak pengajuan dengan pesan validasi tanggal cuti wajib dipilih.");
    }

    @Test(priority = 108, description = "SIT_USR_UI_108: Submit pengajuan Cuti tanpa mengisi kolom Catatan (#notes) [KNOWN DEFECT]",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_108_SubmitLeaveWithoutNotesRejected() {
        TempUserSession tempUser = provisionTempUserWithLeave("cuti_nonote", 12);
        int beforeCount = AdminApiHelper.getUserRequestLeaveCount(tempUser.token(), tempUser.userId());
        LeaveAndSickPage page = loginAndOpenLeave(tempUser.email());
        LocalDate start = LocalDate.now().plusDays(1);
        ExtentTestManager.logInfo("SIT_USR_UI_108 [Strict QA Check] - Submit form Cuti tanpa Catatan (#notes).");

        page.openLeaveDrawer();
        page.selectPreferredLeaveType();
        page.openDrawerCalendarModal();
        page.selectCalendarDateRange(start, start);
        page.clickCalendarSimpan();
        String snackbar = page.submitAndCaptureSnackbar();
        int afterCount = AdminApiHelper.getUserRequestLeaveCount(tempUser.token(), tempUser.userId());
        if (afterCount > beforeCount) {
            trackCleanup(Constants.API_REQUEST_LEAVE_ENDPOINT,
                    AdminApiHelper.findLatestRequestLeaveId(tempUser.token(), tempUser.userId()));
        }
        ExtentTestManager.logInfo("Hasil submit tanpa Catatan: snackbar=[" + snackbar + "], before=" + beforeCount + ", after=" + afterCount);

        Assert.assertEquals(afterCount, beforeCount,
                "[KNOWN DEFECT] Pengajuan cuti tanpa kolom Catatan (#notes) wajib ditolak validasi, "
                        + "namun aplikasi live meloloskan submit tanpa required field (before=" + beforeCount + ", after=" + afterCount + ").");
        Assert.assertFalse(snackbar.toLowerCase().contains("berhasil"),
                "[KNOWN DEFECT] Snackbar tidak boleh menampilkan pesan berhasil saat Catatan kosong.");
    }

    @Test(priority = 109, description = "SIT_USR_UI_109: Submit pengajuan Cuti melebihi sisa kuota (> 30 hari / 60 hari) harus ditolak validasi batas kuota [KNOWN DEFECT]",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_109_SubmitLeaveExceedingQuotaMustBeRejected_KnownDefect() {
        TempUserSession tempUser = provisionTempUserWithLeave("cuti_quota60", 12);
        LeaveAndSickPage page = loginAndOpenLeave(tempUser.email());
        String notes = uniqueTag("SQA_Quota_60Hari");
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(59); // rentang 60 hari vs sisa_cuti 12 hari
        ExtentTestManager.logInfo("SIT_USR_UI_109 [Known Defect Check] - Submit cuti 60 hari (" + start + " s/d " + end + ") pada kuota 12 hari.");

        page.openLeaveDrawer();
        page.selectLeaveTypeContaining("persalinan"); // day_limit=90 agar lolos pengecekan tipe cuti dan menguji validasi kuota 60 hari vs 12 hari
        page.openDrawerCalendarModal();
        page.selectCalendarDateRange(start, end);
        page.clickCalendarSimpan();
        page.enterLeaveNotes(notes);
        String snackbar = page.submitAndCaptureSnackbar();
        String createdId = AdminApiHelper.findLatestRequestLeaveIdByNotes(tempUser.token(), tempUser.userId(), notes);
        trackCleanup(Constants.API_REQUEST_LEAVE_ENDPOINT, createdId);
        ExtentTestManager.logInfo("SIT_USR_UI_109 Actual Result -> snackbar=[" + snackbar + "], createdId=[" + createdId + "]");

        boolean rejectedByQuotaValidation = (createdId == null) && !snackbar.contains("Berhasil");
        Assert.assertTrue(rejectedByQuotaValidation,
                "[KNOWN DEFECT] Sistem wajib menolak pengajuan cuti 60 hari yang melebihi sisa kuota tahunan (12 hari), "
                        + "namun aplikasi live meloloskan submit dengan snackbar='" + snackbar + "' dan createdId=" + createdId);
    }

    @Test(priority = 110, description = "SIT_USR_UI_110: Verifikasi tombol 'Batal' & 'Simpan' pada modal kalender react-date-range 'Pilih Tanggal' [KNOWN DEFECT]",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_110_VerifyLeaveCalendarBatalAndSimpan() {
        LeaveAndSickPage page = loginAndOpenLeave(Constants.USER_EMAIL);
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);
        ExtentTestManager.logInfo("SIT_USR_UI_110 [Strict QA Check] - Menguji tombol Batal lalu Simpan pada modal kalender Form Cuti.");

        page.openLeaveDrawer();
        String beforeDateBox = page.getDrawerDateBoxText();
        page.openDrawerCalendarModal();
        page.selectCalendarDateRange(start, end);
        page.clickCalendarBatal();
        String afterBatalBox = page.getDrawerDateBoxText();

        Assert.assertEquals(afterBatalBox, beforeDateBox,
                "[KNOWN DEFECT] Klik 'Batal' di kalender Form Cuti tidak mengembalikan nilai tanggal form (hanya menutup modal, tanggal tetap tersimpan: "
                        + afterBatalBox + ").");

        page.openDrawerCalendarModal();
        page.selectCalendarDateRange(start, end);
        page.clickCalendarSimpan();
        String afterSimpanBox = page.getDrawerDateBoxText();
        ExtentTestManager.logInfo("Tanggal pasca Batal=[" + afterBatalBox + "], pasca Simpan=[" + afterSimpanBox + "]");

        Assert.assertTrue(afterSimpanBox.matches(".*\\d{2}/\\d{2}/\\d{4}.*-.*\\d{2}/\\d{2}/\\d{4}.*"),
                "Setelah klik 'Simpan', field tanggal harus terisi format DD/MM/YYYY - DD/MM/YYYY. Aktual: " + afterSimpanBox);
    }

    @Test(priority = 111, description = "SIT_USR_UI_111: Klik tombol 'Reset' pada tab Form Cuti mengosongkan Tipe Cuti, Tanggal, & Catatan",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_111_VerifyLeaveFormResetButton() {
        LeaveAndSickPage page = loginAndOpenLeave(Constants.USER_EMAIL);
        LocalDate start = LocalDate.now().plusDays(1);
        ExtentTestManager.logInfo("SIT_USR_UI_111 - Mengisi seluruh field Form Cuti lalu menekan tombol Reset.");

        page.openLeaveDrawer();
        page.selectPreferredLeaveType();
        page.openDrawerCalendarModal();
        page.selectCalendarDateRange(start, start.plusDays(1));
        page.clickCalendarSimpan();
        page.enterLeaveNotes("Catatan uji reset form cuti");
        page.clickDrawerResetButton();

        String notesAfterReset = page.getLeaveNotesValue();
        String dateAfterReset = page.getDrawerDateBoxText();
        ExtentTestManager.logInfo("Pasca Reset Form Cuti -> notes=[" + notesAfterReset + "], date=[" + dateAfterReset + "]");

        Assert.assertTrue(notesAfterReset.isEmpty(), "Kolom Catatan (#notes) harus kosong pasca Reset. Aktual: " + notesAfterReset);
        Assert.assertEquals(dateAfterReset, "-", "Field tanggal harus kembali kosong '-' pasca Reset. Aktual: " + dateAfterReset);
    }

    @Test(priority = 112, description = "SIT_USR_UI_112: Verifikasi kelengkapan field & status kartu 'List Request Cuti' (Approved, Rejected, Dibatalkan, Reject Notes)",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_112_VerifyLeaveCardsFieldsAndAllStatuses() {
        TempUserSession tempUser = provisionTempUserWithLeave("cuti_cards", 12);
        String pendingNote = uniqueTag("SQA_Cuti_Pending");
        String approvedNote = uniqueTag("SQA_Cuti_Approved");
        String rejectedNote = uniqueTag("SQA_Cuti_Rejected");
        String canceledNote = uniqueTag("SQA_Cuti_Canceled");
        String rejectReason = "Kuota tim penuh pada tanggal tsb";

        String idPending = AdminApiHelper.createPendingLeave(
                tempUser.email(), Constants.USER_PASSWORD_ENCODED, cachedLeaveTypeId,
                AdminApiHelper.futureDateYmd(1), AdminApiHelper.futureDateYmd(1), pendingNote);
        trackCleanup(Constants.API_REQUEST_LEAVE_ENDPOINT, idPending);

        String idApproved = AdminApiHelper.createPendingLeave(
                tempUser.email(), Constants.USER_PASSWORD_ENCODED, cachedLeaveTypeId,
                AdminApiHelper.futureDateYmd(3), AdminApiHelper.futureDateYmd(3), approvedNote);
        trackCleanup(Constants.API_REQUEST_LEAVE_ENDPOINT, idApproved);
        Assert.assertTrue(AdminApiHelper.reviewLeaveAsAdmin(idApproved, true, null), "Approve cuti seeding harus berhasil.");

        String idRejected = AdminApiHelper.createPendingLeave(
                tempUser.email(), Constants.USER_PASSWORD_ENCODED, cachedLeaveTypeId,
                AdminApiHelper.futureDateYmd(5), AdminApiHelper.futureDateYmd(5), rejectedNote);
        trackCleanup(Constants.API_REQUEST_LEAVE_ENDPOINT, idRejected);
        Assert.assertTrue(AdminApiHelper.reviewLeaveAsAdmin(idRejected, false, rejectReason), "Reject cuti seeding harus berhasil.");

        String idCanceled = AdminApiHelper.createPendingLeave(
                tempUser.email(), Constants.USER_PASSWORD_ENCODED, cachedLeaveTypeId,
                AdminApiHelper.futureDateYmd(7), AdminApiHelper.futureDateYmd(7), canceledNote);
        trackCleanup(Constants.API_REQUEST_LEAVE_ENDPOINT, idCanceled);
        Assert.assertTrue(AdminApiHelper.cancelPendingLeaveAsAdmin(idCanceled),
                "Cancel cuti via PATCH /request-leave (is_canceled=true) harus berhasil.");

        LeaveAndSickPage page = loginAndOpenLeave(tempUser.email());
        String pendingCard = page.findCardTextContaining(pendingNote);
        String approvedCard = page.findCardTextContaining(approvedNote);
        String rejectedCard = page.findCardTextContaining(rejectedNote);
        String canceledCard = page.findCardTextContaining(canceledNote);
        ExtentTestManager.logInfo("SIT_USR_UI_112 - Cards verified: Pending=[" + pendingCard + "], Rejected=[" + rejectedCard + "]");

        for (String card : List.of(pendingCard, approvedCard, rejectedCard, canceledCard)) {
            Assert.assertFalse(card.isEmpty(), "Semua 4 kartu cuti harus tampil di List Request Cuti.");
            Assert.assertTrue(card.contains("Dari Tanggal :")
                            && card.contains("Sampai Tanggal :")
                            && card.contains("Status:")
                            && card.contains("Tipe Cuti :")
                            && card.contains("Notes :")
                            && card.contains("Reject Notes :")
                            && card.contains("Tanggal Pengajuan :"),
                    "Atribut kartu cuti harus lengkap. Aktual kartu:\n" + card);
        }
        Assert.assertTrue(pendingCard.contains("Menunggu Approval"), "Kartu pending harus berstatus 'Menunggu Approval'.");
        Assert.assertTrue(approvedCard.contains("Approved"), "Kartu disetujui harus berstatus 'Approved'.");
        Assert.assertTrue(rejectedCard.contains("Rejected") && rejectedCard.contains(rejectReason),
                "Kartu ditolak harus berstatus 'Rejected' dan menampilkan Reject Notes: " + rejectReason);
        Assert.assertTrue(canceledCard.contains("Dibatalkan"),
                "Kartu is_canceled=true harus berstatus 'Dibatalkan'. Aktual:\n" + canceledCard);
    }

    @Test(priority = 113, description = "SIT_USR_UI_113: Scroll daftar 'List Request Cuti' untuk memuat lebih banyak data (InfiniteScroll $skip=10*page)",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_113_LeaveInfiniteScrollLoadsMoreCards() {
        TempUserSession tempUser = provisionTempUserWithLeave("cuti_scroll", 20);
        for (int i = 0; i < 12; i++) {
            String day = AdminApiHelper.futureDateYmd(i + 1);
            String id = AdminApiHelper.createPendingLeave(
                    tempUser.email(), Constants.USER_PASSWORD_ENCODED, cachedLeaveTypeId, day, day, "SQA_LeaveScroll_" + i);
            trackCleanup(Constants.API_REQUEST_LEAVE_ENDPOINT, id);
        }

        LeaveAndSickPage page = loginAndOpenLeave(tempUser.email());
        int beforeCards = page.getHistoryCardCount();
        ExtentTestManager.logInfo("SIT_USR_UI_113 - Kartu Cuti awal: " + beforeCards + ", Total server: " + page.getTotalText());

        page.scrollUntilCardCountIncreases(beforeCards);
        int afterCards = page.getHistoryCardCount();
        ExtentTestManager.logInfo("Kartu Cuti pasca InfiniteScroll: " + afterCards);

        Assert.assertTrue(afterCards > beforeCards,
                "InfiniteScroll Cuti ($skip=10*page) harus menambah jumlah kartu setelah scroll ke bawah. Before="
                        + beforeCards + ", After=" + afterCards);
    }

    @Test(priority = 114, description = "SIT_USR_UI_114: Tutup SwipeableDrawer 'Ajukan Cuti' tanpa submit mereset tab & input form",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_114_CloseLeaveDrawerResetsInput() {
        LeaveAndSickPage page = loginAndOpenLeave(Constants.USER_EMAIL);
        int initialTotal = page.getTotalCount();
        ExtentTestManager.logInfo("SIT_USR_UI_114 - Total awal=" + initialTotal + ", menguji tutup drawer tanpa submit.");

        page.openLeaveDrawer();
        page.enterLeaveNotes("Draft notes sebelum drawer ditutup");
        page.closeDrawerWithoutSubmit();
        Assert.assertFalse(page.isDrawerOpen(), "Drawer 'Ajukan Cuti' harus tertutup.");
        Assert.assertEquals(page.getTotalCount(), initialTotal, "Total request cuti tidak boleh berubah.");

        page.openLeaveDrawer();
        String notesOnReopen = page.getLeaveNotesValue();
        ExtentTestManager.logInfo("Notes saat drawer dibuka ulang: [" + notesOnReopen + "]");
        Assert.assertTrue(notesOnReopen.isEmpty(),
                "State input Catatan harus ter-reset kosong setelah drawer ditutup tanpa submit. Aktual: " + notesOnReopen);
    }

    @Test(priority = 115, description = "SIT_USR_UI_115: Klik tombol '< Kembali' pada header 'Halaman Cuti' kembali ke /apps/absent",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_115_ClickBackFromLeaveHeader() {
        LeaveAndSickPage page = loginAndOpenLeave(Constants.USER_EMAIL);
        page.clickBackToHome();
        boolean returned = page.waitForReturnToHome();
        String url = page.getCurrentUrl();
        ExtentTestManager.logInfo("SIT_USR_UI_115 - URL pasca '< Kembali': " + url);

        Assert.assertTrue(returned, "Harus kembali ke /apps/absent dari Halaman Cuti. Aktual: " + url);
    }

    // =========================================================================
    // PENGAJUAN-SAKIT (SIT_USR_UI_116 s/d SIT_USR_UI_126)
    // =========================================================================

    @Test(priority = 116, description = "SIT_USR_UI_116: Navigasi dari Menu 'Sakit' ke /apps/absent/sick & verifikasi header + 'Total : N'",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_116_NavigateToSickPageAndVerifyHeader() {
        LeaveAndSickPage page = loginAndOpenSick(Constants.USER_EMAIL);
        String url = page.getCurrentUrl();
        String totalText = page.getTotalText();
        ExtentTestManager.logInfo("SIT_USR_UI_116 - URL=[" + url + "], Total=[" + totalText + "]");

        Assert.assertTrue(url.contains("/apps/absent/sick"),
                "URL harus mengarah ke /apps/absent/sick. Aktual: " + url);
        Assert.assertTrue(page.isSickHeaderDisplayed(),
                "Header 'Halaman Sakit' & subtitle 'List Request Sakit' harus tampil.");
        Assert.assertTrue(totalText.startsWith("Total"),
                "Counter 'Total : N' harus tampil di Halaman Sakit. Aktual: " + totalText);
    }

    @Test(priority = 117, description = "SIT_USR_UI_117: Pilih tanggal & foto SKD valid di drawer 'Ajukan Request Sakit' -> verifikasi preview & tombol 'Upload ulang'",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_117_UploadValidSkdShowsPreviewAndUploadUlang() {
        LeaveAndSickPage page = loginAndOpenSick(Constants.USER_EMAIL);
        LocalDate today = LocalDate.now();
        ExtentTestManager.logInfo("SIT_USR_UI_117 - Memilih tanggal sakit & mengunggah valid_photo.jpg.");

        page.openSickDrawer();
        page.openDrawerCalendarModal();
        page.selectCalendarDateRange(today, today);
        page.clickCalendarSimpan();
        page.uploadSickDocument("valid_photo.jpg");

        Assert.assertTrue(page.isSickFormPreviewDisplayed() && page.isSickFormPreviewLoaded(),
                "Preview <img alt='Gambar surat sakit'> harus tampil utuh (naturalWidth > 0).");
        Assert.assertEquals(page.getSickUploadButtonText(), "Upload ulang",
                "Teks tombol upload harus berubah menjadi 'Upload ulang' setelah foto dipilih.");
    }

    @Test(priority = 118, description = "SIT_USR_UI_118: Klik tombol 'Upload ulang' untuk mengganti file foto Surat Dokter sebelum submit",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_118_ReuploadSkdUpdatesBlobPreview() {
        LeaveAndSickPage page = loginAndOpenSick(Constants.USER_EMAIL);
        page.openSickDrawer();
        page.uploadSickDocument("valid_photo.jpg");
        String firstBlobSrc = page.getSickFormPreviewSrc();

        page.uploadSickDocument("valid_selfie.png");
        String secondBlobSrc = page.getSickFormPreviewSrc();
        ExtentTestManager.logInfo("SIT_USR_UI_118 - Blob preview: [" + firstBlobSrc + "] -> [" + secondBlobSrc + "]");

        Assert.assertTrue(secondBlobSrc.startsWith("blob:") && !secondBlobSrc.equals(firstBlobSrc),
                "URL blob preview harus diperbarui ke file foto kedua.");
        Assert.assertTrue(page.isSickFormPreviewLoaded(),
                "Preview gambar kedua (valid_selfie.png) harus termuat utuh (naturalWidth > 0).");
    }

    @Test(priority = 119, description = "SIT_USR_UI_119: Submit pengajuan Sakit lengkap dengan tanggal & foto SKD valid",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_119_SubmitValidSickRequest() {
        TempUserSession tempUser = provisionTempUserWithLeave("sakit_valid", 12);
        LeaveAndSickPage page = loginAndOpenSick(tempUser.email());
        LocalDate today = LocalDate.now();
        ExtentTestManager.logInfo("SIT_USR_UI_119 - Submit pengajuan Sakit tanggal=" + today + " dengan valid_photo.jpg.");

        page.openSickDrawer();
        page.openDrawerCalendarModal();
        page.selectCalendarDateRange(today, today);
        page.clickCalendarSimpan();
        page.uploadSickDocument("valid_photo.jpg");
        String snackbar = page.submitAndCaptureSnackbar();
        page.waitForSickPageReady();
        String createdId = AdminApiHelper.findLatestSickActivityId(tempUser.token(), tempUser.userId());
        trackCleanup(Constants.API_ACTIVITY_ENDPOINT, createdId);
        int afterTotal = page.getTotalCount();
        int cardCount = page.getHistoryCardCount();
        ExtentTestManager.logInfo("Snackbar Sakit: [" + snackbar + "], createdId=[" + createdId + "], Total=" + afterTotal + ", cards=" + cardCount);

        Assert.assertNotNull(createdId, "Record pengajuan Sakit harus tercatat pada backend /activity.");
        Assert.assertTrue(afterTotal >= 1 && cardCount >= 1,
                "Kartu baru harus terbentuk di List Request Sakit pasca submit. Total=" + afterTotal + ", cards=" + cardCount);
        Assert.assertTrue(snackbar.isEmpty() || snackbar.contains("Sukses request sakit"),
                "Jika snackbar tertangkap sebelum router.reload(), harus berisi 'Sukses request sakit'. Aktual: " + snackbar);
    }

    @Test(priority = 120, description = "SIT_USR_UI_120: Verifikasi foto 'Bukti surat sakit' pada kartu riwayat /apps/absent/sick tampil utuh pasca submit [KNOWN DEFECT]",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_120_VerifySickCardImageLoadsWithout404_KnownDefect() {
        TempUserSession tempUser = provisionTempUserWithLeave("sakit_img404", 12);
        LeaveAndSickPage page = loginAndOpenSick(tempUser.email());
        LocalDate today = LocalDate.now();

        page.openSickDrawer();
        page.openDrawerCalendarModal();
        page.selectCalendarDateRange(today, today);
        page.clickCalendarSimpan();
        page.uploadSickDocument("valid_photo.jpg");
        String snackbar = page.submitAndCaptureSnackbar();
        page.waitForSickPageReady();
        String createdId = AdminApiHelper.findLatestSickActivityId(tempUser.token(), tempUser.userId());
        trackCleanup(Constants.API_ACTIVITY_ENDPOINT, createdId);
        Assert.assertNotNull(createdId,
                "Prasyarat submit Sakit harus berhasil terlebih dahulu. Snackbar: " + snackbar);

        page.navigateDirectToSick();
        boolean imgLoaded = page.isFirstSickCardImageLoaded();
        String imgSrc = page.getFirstSickCardImageSrc();
        int httpStatus = AdminApiHelper.checkStaticUploadStatus(imgSrc);
        ExtentTestManager.logInfo("SIT_USR_UI_120 [Known Defect Check] - cardImgSrc=[" + imgSrc + "], loaded=" + imgLoaded + ", HTTP=" + httpStatus);

        Assert.assertTrue(imgLoaded && httpStatus == 200,
                "[KNOWN DEFECT] Foto 'Bukti surat sakit' pada kartu riwayat /apps/absent/sick wajib tampil utuh "
                        + "(naturalWidth > 0 & HTTP 200), namun aktual loaded=" + imgLoaded + ", HTTP=" + httpStatus + ", src=" + imgSrc);
    }

    @Test(priority = 121, description = "SIT_USR_UI_121: Uji tombol 'Batal' modal kalender & tombol 'Reset' pada form Ajukan Request Sakit [KNOWN DEFECT]",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_121_VerifySickCalendarBatalAndFormReset() {
        LeaveAndSickPage page = loginAndOpenSick(Constants.USER_EMAIL);
        LocalDate today = LocalDate.now();
        ExtentTestManager.logInfo("SIT_USR_UI_121 [Strict QA Check] - Uji modal kalender Batal lalu Reset pada form Ajukan Request Sakit.");

        page.openSickDrawer();
        String initialBox = page.getDrawerDateBoxText();
        page.openDrawerCalendarModal();
        page.selectCalendarDateRange(today, today);
        page.clickCalendarBatal();
        String afterBatalBox = page.getDrawerDateBoxText();
        Assert.assertEquals(afterBatalBox, initialBox,
                "[KNOWN DEFECT] Klik 'Batal' di kalender Sakit tidak membatalkan perubahan pilihan tanggal pada state form (aktual tetap: "
                        + afterBatalBox + ").");

        page.openDrawerCalendarModal();
        page.selectCalendarDateRange(today, today);
        page.clickCalendarSimpan();
        page.uploadSickDocument("valid_photo.jpg");
        Assert.assertTrue(page.isSickFormPreviewDisplayed(), "Preview SKD harus tampil sebelum Reset.");

        page.clickDrawerResetButton();
        String boxAfterReset = page.getDrawerDateBoxText();
        boolean previewAfterReset = page.isSickFormPreviewDisplayed();
        String btnAfterReset = page.getSickUploadButtonText();
        ExtentTestManager.logInfo("Pasca Reset Sakit -> date=[" + boxAfterReset + "], preview=" + previewAfterReset + ", btn=" + btnAfterReset);

        Assert.assertEquals(boxAfterReset, "/", "Tanggal harus kembali kosong '/' pasca Reset. Aktual: " + boxAfterReset);
        Assert.assertFalse(previewAfterReset, "Preview foto SKD harus terhapus pasca Reset.");
        Assert.assertEquals(btnAfterReset, "Upload", "Tombol harus kembali bertuliskan 'Upload' pasca Reset.");
    }

    @Test(priority = 122, description = "SIT_USR_UI_122: Submit pengajuan Sakit tanpa melampirkan foto Surat Keterangan Dokter [KNOWN DEFECT]",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_122_SubmitSickWithoutDocumentRejected() {
        TempUserSession tempUser = provisionTempUserWithLeave("sakit_nodoc", 12);
        int beforeCount = AdminApiHelper.getUserSickCount(tempUser.token(), tempUser.userId());
        LeaveAndSickPage page = loginAndOpenSick(tempUser.email());
        LocalDate today = LocalDate.now();
        ExtentTestManager.logInfo("SIT_USR_UI_122 [Strict QA Check] - Submit pengajuan Sakit tanpa mengunggah SKD.");

        page.openSickDrawer();
        page.openDrawerCalendarModal();
        page.selectCalendarDateRange(today, today);
        page.clickCalendarSimpan();
        String snackbar = page.submitAndCaptureSnackbar();
        int afterCount = AdminApiHelper.getUserSickCount(tempUser.token(), tempUser.userId());
        if (afterCount > beforeCount) {
            trackCleanup(Constants.API_ACTIVITY_ENDPOINT,
                    AdminApiHelper.findLatestSickActivityId(tempUser.token(), tempUser.userId()));
        }
        ExtentTestManager.logInfo("Hasil submit tanpa SKD: snackbar=[" + snackbar + "], before=" + beforeCount + ", after=" + afterCount);

        Assert.assertEquals(afterCount, beforeCount,
                "[KNOWN DEFECT] Pengajuan Sakit tanpa foto Surat Keterangan Dokter wajib ditolak validasi, "
                        + "namun aplikasi live tetap membuat record sakit di server (before=" + beforeCount + ", after=" + afterCount + ").");
        Assert.assertFalse(snackbar.contains("Sukses"),
                "[KNOWN DEFECT] Snackbar tidak boleh sukses saat SKD kosong. Aktual: " + snackbar);
    }

    @Test(priority = 123, description = "SIT_USR_UI_123: Upload file non-gambar (.pdf/.mp3) pada input lampiran Surat Dokter",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_123_UploadNonImageShowsAlertAndRejectsFile() {
        LeaveAndSickPage page = loginAndOpenSick(Constants.USER_EMAIL);
        ExtentTestManager.logInfo("SIT_USR_UI_123 - Mengunggah invalid_upload.pdf ke input file Sakit.");

        page.openSickDrawer();
        String alertText = page.uploadSickDocumentAndCaptureAlert("invalid_upload.pdf");
        ExtentTestManager.logInfo("Alert non-gambar pada Sakit: [" + alertText + "]");

        Assert.assertTrue(alertText.contains("Pastikan gambar/foto yang diupload"),
                "Alert 'Pastikan gambar/foto yang diupload' harus tampil untuk file non-gambar. Aktual: " + alertText);
        Assert.assertFalse(page.isSickFormPreviewDisplayed(),
                "Preview gambar tidak boleh muncul setelah file non-gambar ditolak.");
    }

    @Test(priority = 124, description = "SIT_USR_UI_124: Submit pengajuan Sakit tanpa memilih rentang tanggal sakit",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_124_SubmitSickWithoutDateRejected() {
        TempUserSession tempUser = provisionTempUserWithLeave("sakit_nodate", 12);
        int beforeCount = AdminApiHelper.getUserSickCount(tempUser.token(), tempUser.userId());
        LeaveAndSickPage page = loginAndOpenSick(tempUser.email());
        ExtentTestManager.logInfo("SIT_USR_UI_124 - Submit pengajuan Sakit tanpa memilih rentang tanggal.");

        page.openSickDrawer();
        page.uploadSickDocument("valid_photo.jpg");
        page.clickDrawerSubmitButton();

        String snackbar = page.getSnackbarMessage();
        int afterCount = AdminApiHelper.getUserSickCount(tempUser.token(), tempUser.userId());
        if (afterCount > beforeCount) {
            trackCleanup(Constants.API_ACTIVITY_ENDPOINT,
                    AdminApiHelper.findLatestSickActivityId(tempUser.token(), tempUser.userId()));
        }
        ExtentTestManager.logInfo("Hasil submit Sakit tanpa tanggal: snackbar=[" + snackbar + "], before=" + beforeCount + ", after=" + afterCount);

        Assert.assertEquals(afterCount, beforeCount,
                "Pengajuan Sakit tanpa tanggal wajib ditolak server.");
        Assert.assertFalse(snackbar.contains("Sukses"),
                "Snackbar tidak boleh sukses saat tanggal sakit kosong. Aktual: " + snackbar);
        Assert.assertFalse(snackbar.isEmpty(), "Harus muncul snackbar error saat tanggal sakit belum dipilih.");
    }

    @Test(priority = 125, description = "SIT_USR_UI_125: Scroll daftar 'List Request Sakit' untuk memuat lebih banyak data (InfiniteScroll $skip=10*page)",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_125_SickInfiniteScrollLoadsMoreCards() {
        TempUserSession tempUser = provisionTempUserWithLeave("sakit_scroll", 12);
        String docId = (cachedSickDocId != null && !cachedSickDocId.isBlank())
                ? cachedSickDocId
                : AdminApiHelper.uploadTestPhoto(tempUser.token(), "valid_photo.jpg");
        for (int i = 0; i < 12; i++) {
            String day = AdminApiHelper.futureDateYmd(i);
            String id = AdminApiHelper.createPendingSick(
                    tempUser.email(), Constants.USER_PASSWORD_ENCODED, tempUser.token(), tempUser.userId(), day, day, docId);
            trackCleanup(Constants.API_ACTIVITY_ENDPOINT, id);
        }

        LeaveAndSickPage page = loginAndOpenSick(tempUser.email());
        int beforeCards = page.getHistoryCardCount();
        ExtentTestManager.logInfo("SIT_USR_UI_125 - Kartu Sakit awal: " + beforeCards + ", Total server: " + page.getTotalText());

        page.scrollUntilCardCountIncreases(beforeCards);
        int afterCards = page.getHistoryCardCount();
        ExtentTestManager.logInfo("Kartu Sakit pasca InfiniteScroll: " + afterCards);

        Assert.assertTrue(afterCards > beforeCards,
                "InfiniteScroll Sakit ($skip=10*page) harus memuat batch kartu berikutnya saat scroll ke bawah. Before="
                        + beforeCards + ", After=" + afterCards);
    }

    @Test(priority = 126, description = "SIT_USR_UI_126: Klik tombol '< Kembali' pada header 'Halaman Sakit' kembali ke /apps/absent",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_126_ClickBackFromSickHeader() {
        LeaveAndSickPage page = loginAndOpenSick(Constants.USER_EMAIL);
        page.clickBackToHome();
        boolean returned = page.waitForReturnToHome();
        String url = page.getCurrentUrl();
        ExtentTestManager.logInfo("SIT_USR_UI_126 - URL pasca '< Kembali': " + url);

        Assert.assertTrue(returned, "Harus kembali ke /apps/absent dari Halaman Sakit. Aktual: " + url);
    }
}
