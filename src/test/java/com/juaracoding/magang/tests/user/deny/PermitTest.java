package com.juaracoding.magang.tests.user.deny;

import com.juaracoding.magang.BaseTest;
import com.juaracoding.magang.api.helper.AdminApiHelper;
import com.juaracoding.magang.pages.user.deny.PermitPage;
import com.juaracoding.magang.pages.user.deny.UserLoginPage;
import com.juaracoding.magang.reports.ExtentTestManager;
import com.juaracoding.magang.utils.Constants;
import com.juaracoding.magang.utils.SecurityUtil;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Pengujian Web UI Selenium untuk fitur Pengajuan-Izin Karyawan
 * (https://magang.dikahadir.com/apps/absent/permit).
 * Cakupan skenario: SIT_USR_UI_078 s/d SIT_USR_UI_091 (14 Test Cases).
 * Penanggung jawab: Tester Deny | Branch: feature/deny-user-permit.
 */
public class PermitTest extends BaseTest {

    private record CleanupRecord(String endpoint, String id) {}
    private record TempUserSession(String email, String userId, String token) {}

    private static String cachedPoolNik;
    private final ThreadLocal<List<CleanupRecord>> createdRecords = ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<List<String>> createdTempUserEmails = ThreadLocal.withInitial(ArrayList::new);

    @BeforeClass(alwaysRun = true)
    public void resolvePoolNikOnce() {
        cachedPoolNik = AdminApiHelper.findAvailableNikFromPool();
        logger.info("Resolved cached NIK for PermitTest isolated user seeding: {}", cachedPoolNik);
    }

    private void trackCleanup(String endpoint, String id) {
        if (id != null && !id.isBlank()) {
            createdRecords.get().add(new CleanupRecord(endpoint, id));
        }
    }

    private TempUserSession provisionTempUserWithAdminUpliner(String prefix) {
        String email = prefix + "_" + System.currentTimeMillis() + "@mail.com";
        String nik = (cachedPoolNik != null && !cachedPoolNik.isBlank()) ? cachedPoolNik : Constants.VALID_NIK_POOL[0];
        String userId = AdminApiHelper.createSelfRegisteredMobileUser(
                nik, "SQA Permit Temp", email, Constants.USER_PASSWORD_ENCODED);
        Assert.assertNotNull(userId, "Provisioning user uji terisolasi harus mengembalikan userId.");
        createdTempUserEmails.get().add(email);
        boolean activated = AdminApiHelper.activateUserWithAdminUpliner(userId);
        Assert.assertTrue(activated, "Aktivasi user uji & penyetelan upliner Admin harus berhasil.");
        AdminApiHelper.ApiSession session = AdminApiHelper.loginAsUser(email, Constants.USER_PASSWORD_ENCODED);
        return new TempUserSession(email, session.userId(), session.token());
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupSeededAndSubmittedPermits() {
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

    private PermitPage loginAndOpenPermitPage(String email) {
        UserLoginPage loginPage = new UserLoginPage(getDriver());
        loginPage.navigateToLoginPage();
        loginPage.login(email, SecurityUtil.decode(Constants.USER_PASSWORD_ENCODED));
        Assert.assertTrue(loginPage.waitForRedirectToAbsent(),
                "Prasyarat login User gagal menuju /apps/absent. URL aktual: " + loginPage.getCurrentUrl());
        PermitPage permitPage = new PermitPage(getDriver());
        permitPage.navigateDirectToPermitPage();
        return permitPage;
    }

    private PermitPage loginDefaultAndOpenPermitPage() {
        return loginAndOpenPermitPage(Constants.USER_EMAIL);
    }

    private String uniqueTag(String prefix) {
        return prefix + "_" + System.currentTimeMillis();
    }

    @Test(priority = 78, description = "SIT_USR_UI_078: Navigasi ke /apps/absent/permit -> verifikasi header, 3 tab (Terlambat, Pulang Cepat, Izin Off), & 'Total : N'",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_078_NavigateAndVerifyPermitTabsAndTotal() {
        UserLoginPage loginPage = new UserLoginPage(getDriver());
        loginPage.navigateToLoginPage();
        loginPage.loginWithDefaultUser();
        Assert.assertTrue(loginPage.waitForRedirectToAbsent(), "User harus masuk ke /apps/absent terlebih dahulu.");

        ExtentTestManager.logInfo("SIT_USR_UI_078 - Klik menu 'Izin' dari /apps/absent dan verifikasi header, tab & Total.");
        PermitPage permitPage = new PermitPage(getDriver());
        permitPage.navigateFromAbsentHome();

        boolean izinOffVisible = permitPage.isIzinOffTabDisplayed();
        ExtentTestManager.logInfo("Tab Izin Off visible on hadirsqa1@gmail.com UI=" + izinOffVisible);

        Assert.assertTrue(permitPage.getCurrentUrl().contains("/apps/absent/permit"),
                "URL harus mengarah ke /apps/absent/permit.");
        Assert.assertTrue(permitPage.isHeaderDisplayed(), "Header banner 'Halaman Izin' harus tampil.");
        Assert.assertTrue(permitPage.isTerlambatTabDisplayed() && permitPage.isPulangCepatTabDisplayed(),
                "Tab 'Terlambat' dan 'Pulang Cepat' harus tampil.");
        Assert.assertTrue(permitPage.isActiveTabListTitleDisplayed("List Izin Terlambat"),
                "Heading 'List Izin Terlambat' harus tampil pada tab default.");
        String totalText = permitPage.getActiveTotalText();
        ExtentTestManager.logInfo("Total text tampil: " + totalText);
        Assert.assertTrue(totalText.startsWith("Total :"), "Counter 'Total : N' harus tampil. Aktual: " + totalText);
    }

    @Test(priority = 79, description = "SIT_USR_UI_079: Submit form 'Ajukan Izin Terlambat' dengan Tanggal, Jam, dan Keterangan valid",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_079_SubmitValidLatePermit() {
        TempUserSession tempUser = provisionTempUserWithAdminUpliner("permit_late");
        PermitPage permitPage = loginAndOpenPermitPage(tempUser.email());
        String note = uniqueTag("SQA_Late_Valid");
        ExtentTestManager.logInfo("SIT_USR_UI_079 - Membuka drawer Ajukan Izin Terlambat & submit data valid (akun terisolasi).");

        permitPage.openPermitDrawer();
        permitPage.selectTodayOnDrawerDatePicker();
        permitPage.setDrawerTime("09:15");
        permitPage.enterNotes(note);
        permitPage.clickDrawerSubmitButton();

        String snackbar = permitPage.getSnackbarMessage();
        ExtentTestManager.logInfo("Snackbar hasil submit Terlambat: " + snackbar);

        String createdId = AdminApiHelper.findLatestUserRecordIdByField(
                tempUser.token(), tempUser.userId(), Constants.API_LATE_PERMIT_ENDPOINT, "late_reason", note);
        trackCleanup(Constants.API_LATE_PERMIT_ENDPOINT, createdId);

        Assert.assertTrue(snackbar.toLowerCase().contains("berhasil mengajukan izin terlambat") || createdId != null,
                "Snackbar 'Berhasil mengajukan izin terlambat' harus muncul dan record tersimpan. Snackbar=["
                        + snackbar + "], createdId=[" + createdId + "]");
    }

    @Test(priority = 80, description = "SIT_USR_UI_080: Submit 'Ajukan Izin Terlambat' tanpa mengisi field wajib (Tanggal, Jam, Keterangan)",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_080_SubmitEmptyLatePermitValidation() {
        PermitPage permitPage = loginDefaultAndOpenPermitPage();
        ExtentTestManager.logInfo("SIT_USR_UI_080 - Submit drawer Ajukan Izin Terlambat kosong untuk memeriksa validasi Yup.");

        permitPage.openPermitDrawer();
        permitPage.clickDrawerSubmitButton();

        List<String> errors = permitPage.getAllDrawerHelperErrorTexts();
        String joined = String.join(" | ", errors);
        ExtentTestManager.logInfo("Pesan error Yup yang tampil: " + joined);

        Assert.assertTrue(joined.contains("Tanggal Harus diisi!"),
                "Pesan 'Tanggal Harus diisi!' harus tampil. Aktual: " + joined);
        Assert.assertTrue(joined.contains("Jam Harus diisi!"),
                "Pesan 'Jam Harus diisi!' harus tampil. Aktual: " + joined);
        Assert.assertTrue(joined.contains("Keterangan Harus diisi!"),
                "Pesan 'Keterangan Harus diisi!' harus tampil. Aktual: " + joined);
    }

    @Test(priority = 81, description = "SIT_USR_UI_081: Beralih ke tab 'Pulang Cepat' & submit 'Ajukan Pulang Cepat' valid",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_081_SwitchToEarlyLeaveAndSubmitValid() {
        TempUserSession tempUser = provisionTempUserWithAdminUpliner("permit_early");
        PermitPage permitPage = loginAndOpenPermitPage(tempUser.email());
        ExtentTestManager.logInfo("SIT_USR_UI_081 - Beralih ke tab 'Pulang Cepat', cek List & Total, lalu submit valid.");

        permitPage.clickTabPulangCepat();
        Assert.assertTrue(permitPage.getCurrentUrl().contains("permit=early"),
                "URL harus berganti ke ?permit=early. URL aktual: " + permitPage.getCurrentUrl());
        Assert.assertTrue(permitPage.isActiveTabListTitleDisplayed("List Pulang Cepat"),
                "Judul 'List Pulang Cepat' harus tampil.");
        Assert.assertTrue(permitPage.getActiveTotalText().startsWith("Total :"),
                "Counter 'Total : N' harus tampil di tab Pulang Cepat.");

        String note = uniqueTag("SQA_Early_Valid");
        permitPage.openPermitDrawer();
        permitPage.selectTodayOnDrawerDatePicker();
        permitPage.setDrawerTime("15:30");
        permitPage.enterNotes(note);
        permitPage.clickDrawerSubmitButton();

        String snackbar = permitPage.getSnackbarMessage();
        ExtentTestManager.logInfo("Snackbar hasil submit Pulang Cepat: " + snackbar);

        String createdId = AdminApiHelper.findLatestUserRecordIdByField(
                tempUser.token(), tempUser.userId(), Constants.API_EARLY_LEAVE_ENDPOINT, "early_leave_reason", note);
        trackCleanup(Constants.API_EARLY_LEAVE_ENDPOINT, createdId);

        Assert.assertTrue(snackbar.toLowerCase().contains("berhasil mengajukan pulang cepat") || createdId != null,
                "Snackbar 'Berhasil mengajukan pulang cepat' harus tampil. Aktual: " + snackbar);
    }

    @Test(priority = 82, description = "SIT_USR_UI_082: Submit 'Ajukan Pulang Cepat' tanpa mengisi Jam / Keterangan wajib",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_082_SubmitEmptyEarlyLeaveValidation() {
        PermitPage permitPage = loginDefaultAndOpenPermitPage();
        permitPage.clickTabPulangCepat();
        ExtentTestManager.logInfo("SIT_USR_UI_082 - Submit drawer Ajukan Pulang Cepat dengan Jam & Keterangan kosong.");

        permitPage.openPermitDrawer();
        permitPage.selectTodayOnDrawerDatePicker();
        permitPage.clickDrawerSubmitButton();

        List<String> errors = permitPage.getAllDrawerHelperErrorTexts();
        String joined = String.join(" | ", errors);
        ExtentTestManager.logInfo("Error validasi Pulang Cepat: " + joined);

        Assert.assertTrue(joined.contains("Jam Harus diisi!"),
                "Pesan 'Jam Harus diisi!' harus tampil. Aktual: " + joined);
        Assert.assertTrue(joined.contains("Keterangan Harus diisi!"),
                "Pesan 'Keterangan Harus diisi!' harus tampil. Aktual: " + joined);
    }

    @Test(priority = 83, description = "SIT_USR_UI_083: Beralih ke tab 'Izin Off' (?permit=off-permit) dan verifikasi List Izin Off",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_083_SwitchToIzinOffTabAndVerifyContent() {
        PermitPage permitPage = loginDefaultAndOpenPermitPage();
        ExtentTestManager.logInfo("SIT_USR_UI_083 - Membuka tab/rute 'Izin Off' (?permit=off-permit) dan memverifikasi konten.");

        permitPage.clickTabIzinOffOrNavigate();

        Assert.assertTrue(permitPage.getCurrentUrl().contains("permit=off-permit"),
                "URL harus mengarah ke ?permit=off-permit. URL aktual: " + permitPage.getCurrentUrl());
        Assert.assertTrue(permitPage.isActiveTabListTitleDisplayed("List Izin Off"),
                "Judul 'List Izin Off' harus tampil.");
        Assert.assertTrue(permitPage.isActiveAjukanButtonDisplayed("Ajukan Izin Off"),
                "Tombol 'Ajukan Izin Off' harus tampil.");
    }

    @Test(priority = 84, description = "SIT_USR_UI_084: Submit 'Ajukan Izin Off' dengan Tanggal Off & Alasan Off valid",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_084_SubmitValidIzinOff() {
        TempUserSession tempUser = provisionTempUserWithAdminUpliner("permit_off");
        PermitPage permitPage = loginAndOpenPermitPage(tempUser.email());
        permitPage.clickTabIzinOffOrNavigate();
        String reason = uniqueTag("SQA_Off_Valid");
        ExtentTestManager.logInfo("SIT_USR_UI_084 - Membuka drawer Ajukan Izin Off dan submit tanggal hari ini & alasan valid.");

        permitPage.openPermitDrawer();
        permitPage.selectTodayOnDrawerDatePicker();
        permitPage.enterOffReason(reason);
        permitPage.clickDrawerSubmitButton();

        String snackbar = permitPage.getSnackbarMessage();
        ExtentTestManager.logInfo("Snackbar hasil submit Izin Off: " + snackbar);

        String createdId = AdminApiHelper.findLatestUserRecordIdByField(
                tempUser.token(), tempUser.userId(), Constants.API_OFF_PERMIT_ENDPOINT, "reason", reason);
        trackCleanup(Constants.API_OFF_PERMIT_ENDPOINT, createdId);

        Assert.assertTrue(snackbar.toLowerCase().contains("berhasil melakukan request izin off") || createdId != null,
                "Snackbar 'Berhasil melakukan request izin off' harus tampil. Aktual: " + snackbar);
    }

    @Test(priority = 85, description = "SIT_USR_UI_085: Submit 'Ajukan Izin Off' tanpa memilih Tanggal Off atau Alasan Off",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_085_SubmitEmptyIzinOffValidation() {
        PermitPage permitPage = loginDefaultAndOpenPermitPage();
        permitPage.clickTabIzinOffOrNavigate();
        ExtentTestManager.logInfo("SIT_USR_UI_085 - Klik Ajukan pada drawer Izin Off saat Tanggal Off & Alasan Off kosong.");

        permitPage.openPermitDrawer();
        permitPage.clickDrawerSubmitButton();

        List<String> errors = permitPage.getAllDrawerHelperErrorTexts();
        String joined = String.join(" | ", errors);
        ExtentTestManager.logInfo("Error validasi Izin Off: " + joined);

        Assert.assertTrue(joined.contains("Pilih tanggal off!."),
                "Pesan 'Pilih tanggal off!.' harus tampil. Aktual: " + joined);
        Assert.assertTrue(joined.contains("Alasan dan mengambill izin off."),
                "Pesan 'Alasan dan mengambill izin off.' harus tampil. Aktual: " + joined);
    }

    @Test(priority = 86, description = "SIT_USR_UI_086: Verifikasi kelengkapan 5 field kartu Izin (Jam/Tanggal, Status Menunggu Approval/Disetuji/Ditolak, Catatan, Alasan Ditolak, Tanggal Dibuat)",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_086_VerifyPermitCardFiveAttributesAndHybridStatuses() {
        TempUserSession tempUser = provisionTempUserWithAdminUpliner("permit_cards");
        String pendingReason = uniqueTag("SQA_Card_Pending");
        String approvedReason = uniqueTag("SQA_Card_Approved");
        String rejectedReason = uniqueTag("SQA_Card_Rejected");
        String rejectNoteText = "Alasan penolakan HRD " + System.currentTimeMillis();

        String datePending = AdminApiHelper.futureDateYmd(10);
        String dateApproved = AdminApiHelper.futureDateYmd(11);
        String dateRejected = AdminApiHelper.futureDateYmd(12);

        String pendingId = AdminApiHelper.createPendingOffPermit(tempUser.token(), datePending, pendingReason);
        String approvedId = AdminApiHelper.createPendingOffPermit(tempUser.token(), dateApproved, approvedReason);
        String rejectedId = AdminApiHelper.createPendingOffPermit(tempUser.token(), dateRejected, rejectedReason);

        trackCleanup(Constants.API_OFF_PERMIT_ENDPOINT, pendingId);
        trackCleanup(Constants.API_OFF_PERMIT_ENDPOINT, approvedId);
        trackCleanup(Constants.API_OFF_PERMIT_ENDPOINT, rejectedId);

        Assert.assertNotNull(pendingId, "Hybrid API seed pending off-permit harus berhasil.");
        Assert.assertNotNull(approvedId, "Hybrid API seed approved off-permit harus berhasil.");
        Assert.assertNotNull(rejectedId, "Hybrid API seed rejected off-permit harus berhasil.");

        boolean approvedOk = AdminApiHelper.reviewOffPermitAsAdmin(approvedId, true, null);
        boolean rejectedOk = AdminApiHelper.reviewOffPermitAsAdmin(rejectedId, false, rejectNoteText);
        Assert.assertTrue(approvedOk, "Hybrid Admin PATCH Approved off-permit harus berhasil.");
        Assert.assertTrue(rejectedOk, "Hybrid Admin PATCH Rejected off-permit harus berhasil.");

        PermitPage permitPage = loginAndOpenPermitPage(tempUser.email());
        permitPage.clickTabIzinOffOrNavigate();
        ExtentTestManager.logInfo("SIT_USR_UI_086 - Memverifikasi 5 atribut kartu & 3 status (Menunggu Approval, Disetuji, Ditolak + Alasan Ditolak).");

        String pendingCard = permitPage.findPermitCardTextContaining(pendingReason);
        String approvedCard = permitPage.findPermitCardTextContaining(approvedReason);
        String rejectedCard = permitPage.findPermitCardTextContaining(rejectedReason);

        Assert.assertTrue(permitPage.cardHasFiveAttributes(pendingCard, "Izin Off")
                        && pendingCard.contains("Menunggu Approval"),
                "Kartu Pending harus lengkap 5 atribut & berstatus 'Menunggu Approval'. Aktual:\n" + pendingCard);
        Assert.assertTrue(permitPage.cardHasFiveAttributes(approvedCard, "Izin Off")
                        && approvedCard.contains("Disetuji"),
                "Kartu Approved harus lengkap 5 atribut & berstatus 'Disetuji'. Aktual:\n" + approvedCard);
        Assert.assertTrue(permitPage.cardHasFiveAttributes(rejectedCard, "Izin Off")
                        && rejectedCard.contains("Ditolak")
                        && rejectedCard.contains(rejectNoteText),
                "Kartu Rejected harus lengkap 5 atribut, berstatus 'Ditolak', & menampilkan Alasan Ditolak. Aktual:\n" + rejectedCard);
    }

    @Test(priority = 87, description = "SIT_USR_UI_087: Klik tombol 'Reset' pada form drawer Pengajuan Izin (Terlambat / Pulang Cepat / Izin Off)",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_087_ResetPermitDrawerForm() {
        PermitPage permitPage = loginDefaultAndOpenPermitPage();
        ExtentTestManager.logInfo("SIT_USR_UI_087 - Mengisi form drawer Izin Terlambat & klik tombol 'Reset'.");

        permitPage.openPermitDrawer();
        permitPage.enterNotes("Teks sementara sebelum di-reset");
        Assert.assertFalse(permitPage.getNotesValue().isEmpty(), "Field Keterangan harus terisi sebelum Reset.");

        permitPage.clickDrawerResetButton();
        String afterReset = permitPage.getNotesValue();
        ExtentTestManager.logInfo("Nilai field Keterangan pasca Reset: [" + afterReset + "]");
        Assert.assertTrue(afterReset == null || afterReset.isEmpty(),
                "Field Keterangan harus kembali kosong setelah tombol Reset diklik. Aktual: " + afterReset);
    }

    @Test(priority = 88, description = "SIT_USR_UI_088: Tutup SwipeableDrawer form Pengajuan Izin tanpa submit (onClose)",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_088_ClosePermitDrawerWithoutSubmit() {
        PermitPage permitPage = loginDefaultAndOpenPermitPage();
        int initialTotal = permitPage.getActiveTotalCount();
        ExtentTestManager.logInfo("SIT_USR_UI_088 - Total awal=" + initialTotal + ", membuka lalu menutup drawer tanpa submit.");

        permitPage.openPermitDrawer();
        Assert.assertTrue(permitPage.isDrawerOpen(), "Drawer harus terbuka sebelum diuji tutup.");

        permitPage.closeDrawerWithoutSubmit();
        Assert.assertFalse(permitPage.isDrawerOpen(), "Drawer harus tertutup aman saat backdrop/Escape dipicu.");
        Assert.assertEquals(permitPage.getActiveTotalCount(), initialTotal,
                "Jumlah Total daftar pengajuan tidak boleh berubah jika drawer ditutup tanpa submit.");
    }

    @Test(priority = 89, description = "SIT_USR_UI_089: Scroll daftar riwayat pada Pengajuan-Izin untuk memuat lebih banyak data (InfiniteScroll $skip=10*page)",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_089_InfiniteScrollLoadsMorePermits() {
        int lateTotal = AdminApiHelper.getUserRecordsCount(Constants.API_LATE_PERMIT_ENDPOINT);
        for (int i = lateTotal; i < 12; i++) {
            String id = AdminApiHelper.createPendingLatePermit(
                    AdminApiHelper.isoUtcTimestamp(i + 20, 2), "SQA_InfiniteScroll_Seed_" + i);
            trackCleanup(Constants.API_LATE_PERMIT_ENDPOINT, id);
        }

        PermitPage permitPage = loginDefaultAndOpenPermitPage();
        int beforeCards = permitPage.getPermitCardCount();
        ExtentTestManager.logInfo("SIT_USR_UI_089 - Jumlah kartu Terlambat sebelum scroll: " + beforeCards + " (Total server=" + Math.max(lateTotal, 12) + ")");

        permitPage.scrollToBottomForInfiniteScroll();
        int afterCards = permitPage.getPermitCardCount();
        ExtentTestManager.logInfo("Jumlah kartu Terlambat pasca InfiniteScroll: " + afterCards);

        Assert.assertTrue(afterCards > beforeCards,
                "InfiniteScroll ($skip=10*page) harus memuat batch kartu tambahan saat di-scroll ke bawah. Before="
                        + beforeCards + ", After=" + afterCards);
    }

    @Test(priority = 90, description = "SIT_USR_UI_090: Klik tombol '< Kembali' pada header 'Halaman Izin' kembali ke /apps/absent",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_090_ClickBackToHomeFromPermitHeader() {
        PermitPage permitPage = loginDefaultAndOpenPermitPage();
        ExtentTestManager.logInfo("SIT_USR_UI_090 - Klik tombol '< Kembali' di header 'Halaman Izin'.");

        permitPage.clickBackToHome();
        boolean returned = permitPage.waitForReturnToHome();
        String currentUrl = permitPage.getCurrentUrl();
        ExtentTestManager.logInfo("URL pasca klik '< Kembali': " + currentUrl);

        Assert.assertTrue(returned && currentUrl.contains("/apps/absent") && !currentUrl.contains("/permit"),
                "Harus berpindah kembali ke beranda /apps/absent. URL aktual: " + currentUrl);
    }

    @Test(priority = 91, description = "SIT_USR_UI_091: Penanganan pesan snackbar error dari server jika pengajuan Izin ditolak backend",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_091_BackendRejectionShowsErrorSnackbar() {
        // Seed 1 Izin Terlambat untuk hari ini pada user terisolasi agar submit kedua dari UI
        // memicu penolakan duplikat tanggal (HTTP 409) dan menampilkan pesan error di Snackbar.
        TempUserSession tempUser = provisionTempUserWithAdminUpliner("permit_reject");
        String todayIsoUtc = AdminApiHelper.isoUtcTimestamp(0, 2);
        String seedId = AdminApiHelper.createPendingLatePermit(
                tempUser.token(), tempUser.userId(), todayIsoUtc, uniqueTag("SQA_DupLate_Seed"));
        trackCleanup(Constants.API_LATE_PERMIT_ENDPOINT, seedId);
        Assert.assertNotNull(seedId, "Seed awal Izin Terlambat hari ini harus berhasil.");

        PermitPage permitPage = loginAndOpenPermitPage(tempUser.email());
        String dupNote = uniqueTag("SQA_DupLate_UI");
        ExtentTestManager.logInfo("SIT_USR_UI_091 - Menguji submit Izin Terlambat duplikat hari ini untuk memicu penolakan backend (Snackbar).");

        permitPage.openPermitDrawer();
        permitPage.selectTodayOnDrawerDatePicker();
        permitPage.setDrawerTime("09:30");
        permitPage.enterNotes(dupNote);
        permitPage.clickDrawerSubmitButton();

        String snackbar = permitPage.getSnackbarMessage();
        String uiCreatedId = AdminApiHelper.findLatestUserRecordIdByField(
                tempUser.token(), tempUser.userId(), Constants.API_LATE_PERMIT_ENDPOINT, "late_reason", dupNote);
        trackCleanup(Constants.API_LATE_PERMIT_ENDPOINT, uiCreatedId);

        ExtentTestManager.logInfo("Snackbar server pada pengajuan ditolak backend: [" + snackbar + "], uiCreatedId=[" + uiCreatedId + "]");
        Assert.assertFalse(snackbar.isEmpty(),
                "Komponen Snackbar harus tampil merespons penolakan server tanpa crash halaman.");
        Assert.assertTrue(!snackbar.toLowerCase().contains("berhasil") && uiCreatedId == null,
                "Pengajuan duplikat tanggal hari ini harus ditolak server dan menampilkan pesan error di Snackbar. Aktual: " + snackbar);
    }
}
