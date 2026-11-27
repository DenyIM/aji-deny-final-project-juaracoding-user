package com.juaracoding.magang.tests.user.deny;

import com.juaracoding.magang.BaseTest;
import com.juaracoding.magang.api.helper.AdminApiHelper;
import com.juaracoding.magang.pages.user.deny.DownloadReportPage;
import com.juaracoding.magang.pages.user.deny.UserLoginPage;
import com.juaracoding.magang.reports.ExtentTestManager;
import com.juaracoding.magang.utils.Constants;
import com.juaracoding.magang.utils.SecurityUtil;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Pengujian Web UI Selenium untuk fitur Profil-Download Report Karyawan
 * (https://magang.dikahadir.com/apps/absent/reports/share_report).
 * Cakupan skenario: SIT_USR_UI_136 s/d SIT_USR_UI_141 (6 Test Cases).
 * Penanggung jawab: Tester Deny | Branch: feature/deny-user-download-report.
 */
public class DownloadReportTest extends BaseTest {

    private static final Path TEST_DOWNLOAD_DIR = Paths.get("target", "test-downloads");

    @AfterMethod(alwaysRun = true)
    public void cleanupDownloadsAndCdpRules() {
        try {
            if (getDriver() != null) {
                new DownloadReportPage(getDriver()).blockShareReportEndpointViaCdp(false);
            }
        } catch (Exception ignored) {
            // Driver may already be closing
        }
        if (Files.exists(TEST_DOWNLOAD_DIR)) {
            try (Stream<Path> walk = Files.walk(TEST_DOWNLOAD_DIR)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Best effort download file cleanup
                    }
                });
            } catch (IOException ignored) {
                // Best effort directory cleanup
            }
        }
    }

    private DownloadReportPage loginDefaultAndOpenDownloadReport() {
        UserLoginPage loginPage = new UserLoginPage(getDriver());
        loginPage.navigateToLoginPage();
        loginPage.login(Constants.USER_EMAIL, SecurityUtil.decode(Constants.USER_PASSWORD_ENCODED));
        Assert.assertTrue(loginPage.waitForRedirectToAbsent(),
                "Prasyarat login User gagal menuju /apps/absent. URL aktual: " + loginPage.getCurrentUrl());
        DownloadReportPage page = new DownloadReportPage(getDriver());
        page.navigateFromAbsentHome();
        return page;
    }

    @Test(priority = 136, description = "SIT_USR_UI_136: Navigasi dari ikon Menu 'Download Reports' di beranda /apps/absent ke /apps/absent/reports/share_report",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_136_NavigateFromDownloadReportsMenu() {
        DownloadReportPage page = loginDefaultAndOpenDownloadReport();
        String currentUrl = page.getCurrentUrl();
        ExtentTestManager.logInfo("SIT_USR_UI_136 - Berhasil navigasi via ikon 'Download Reports' ke URL: " + currentUrl);

        Assert.assertTrue(currentUrl.contains("/apps/absent/reports/share_report"),
                "URL harus mengarah ke /apps/absent/reports/share_report. Aktual: " + currentUrl);
        Assert.assertTrue(page.isHeaderDisplayed(), "Header 'Halaman Download Report' harus tampil.");
        Assert.assertTrue(page.isReportTypeDropdownDisplayed(), "Dropdown 'Pilih tipe report' harus tampil.");
        Assert.assertTrue(page.isDownloadButtonDisplayed(), "Tombol 'Download Report' harus tampil.");
    }

    @Test(priority = 137, description = "SIT_USR_UI_137: Pilih Tipe Report 'Harian' dan klik 'Download Report' berhasil mengekspor file .xlsx",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_137_DownloadDailyReportXlsx() throws IOException {
        DownloadReportPage page = loginDefaultAndOpenDownloadReport();
        page.enableCdpDownloadDirectory(TEST_DOWNLOAD_DIR);
        ExtentTestManager.logInfo("SIT_USR_UI_137 - Memilih tipe report 'Harian' (value='day') lalu klik 'Download Report'.");

        page.selectReportType("Harian");
        page.clickDownloadReportButton();
        page.waitUntilLoadingFinishes();

        String snackbar = page.getSnackbarMessage();
        Path downloaded = page.waitForDownloadedXlsx(TEST_DOWNLOAD_DIR, "Harian", 10);
        ExtentTestManager.logInfo("Snackbar=['" + snackbar + "'], fileXlsx=[" + (downloaded != null ? downloaded.getFileName() : "none") + "]");

        Assert.assertTrue(snackbar.contains("Berhasil download report"),
                "Snackbar 'Berhasil download report' harus tampil. Aktual: " + snackbar);
        Assert.assertNotNull(downloaded, "File .xlsx laporan Harian harus terunduh ke disk dengan ukuran > 0 bytes.");
    }

    @Test(priority = 138, description = "SIT_USR_UI_138: Pilih Tipe Report 'Mingguan' & 'Bulanan' dan klik 'Download Report' berhasil mengekspor file .xlsx",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_138_DownloadWeeklyAndMonthlyReportsXlsx() throws IOException {
        DownloadReportPage page = loginDefaultAndOpenDownloadReport();
        page.enableCdpDownloadDirectory(TEST_DOWNLOAD_DIR);

        ExtentTestManager.logInfo("SIT_USR_UI_138 [1/2] - Memilih tipe report 'Mingguan' (value='week') dan mengekspor .xlsx.");
        page.selectReportType("Mingguan");
        page.clickDownloadReportButton();
        page.waitUntilLoadingFinishes();
        String weeklySnackbar = page.getSnackbarMessage();
        Path weeklyFile = page.waitForDownloadedXlsx(TEST_DOWNLOAD_DIR, "Mingguan", 10);
        ExtentTestManager.logInfo("Mingguan -> snackbar=[" + weeklySnackbar + "], file=[" + (weeklyFile != null ? weeklyFile.getFileName() : "none") + "]");

        Assert.assertTrue(weeklySnackbar.contains("Berhasil download report"),
                "Snackbar sukses laporan Mingguan harus tampil. Aktual: " + weeklySnackbar);
        Assert.assertNotNull(weeklyFile, "File .xlsx laporan Mingguan harus terunduh.");

        page.dismissSnackbarIfPresent();

        ExtentTestManager.logInfo("SIT_USR_UI_138 [2/2] - Memilih tipe report 'Bulanan' (value='month') dan mengekspor .xlsx.");
        page.selectReportType("Bulanan");
        page.clickDownloadReportButton();
        page.waitUntilLoadingFinishes();
        String monthlySnackbar = page.getSnackbarMessage();
        Path monthlyFile = page.waitForDownloadedXlsx(TEST_DOWNLOAD_DIR, "Bulanan", 10);
        ExtentTestManager.logInfo("Bulanan -> snackbar=[" + monthlySnackbar + "], file=[" + (monthlyFile != null ? monthlyFile.getFileName() : "none") + "]");

        Assert.assertTrue(monthlySnackbar.contains("Berhasil download report"),
                "Snackbar sukses laporan Bulanan harus tampil. Aktual: " + monthlySnackbar);
        Assert.assertNotNull(monthlyFile, "File .xlsx laporan Bulanan harus terunduh.");
    }

    @Test(priority = 139, description = "SIT_USR_UI_139: Klik tombol 'Download Report' tanpa memilih Tipe Report",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_139_SubmitWithoutReportTypeShowsYupValidation() {
        DownloadReportPage page = loginDefaultAndOpenDownloadReport();
        ExtentTestManager.logInfo("SIT_USR_UI_139 - Klik 'Download Report' tanpa memilih opsi Tipe Report.");

        page.clickDownloadReportButton();
        String validationErr = page.getValidationErrorText();
        ExtentTestManager.logInfo("Validasi Yup aktual: [" + validationErr + "]");

        Assert.assertTrue(validationErr.contains("Tipe report harus dipilih!"),
                "Validasi Yup harus menampilkan 'Tipe report harus dipilih!'. Aktual: " + validationErr);
    }

    @Test(priority = 140, description = "SIT_USR_UI_140: Penanganan error/penolakan saat POST /share-report gagal atau tidak memiliki otorisasi",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_140_HandleShareReportRejectionSafely() {
        AdminApiHelper.ApiSession session = AdminApiHelper.loginAsDefaultUser();
        Response invalidTokenRes = AdminApiHelper.postShareReport(
                "invalid_jwt_token_for_sqa_check",
                "72aafd87-9909-49bc-9e7c-5e6d5c41f437",
                session.userId(),
                AdminApiHelper.futureDateYmd(0),
                AdminApiHelper.futureDateYmd(0)
        );
        ExtentTestManager.logInfo("SIT_USR_UI_140 API Check (invalid token POST /share-report) -> HTTP "
                + invalidTokenRes.getStatusCode() + ": " + invalidTokenRes.asString());
        Assert.assertEquals(invalidTokenRes.getStatusCode(), 401,
                "Endpoint POST /share-report harus menolak token tidak valid dengan HTTP 401.");

        DownloadReportPage page = loginDefaultAndOpenDownloadReport();
        page.blockShareReportEndpointViaCdp(true);
        page.selectReportType("Harian");
        String errorSnackbar = page.submitExpectingServerErrorFeedback();
        ExtentTestManager.logInfo("SIT_USR_UI_140 UI Error Snackbar: [" + errorSnackbar + "], URL=[" + page.getCurrentUrl() + "]");

        Assert.assertFalse(errorSnackbar.isEmpty(),
                "Aplikasi harus menampilkan Snackbar error secara aman saat POST /share-report gagal.");
        Assert.assertFalse(errorSnackbar.toLowerCase().contains("berhasil"),
                "Snackbar tidak boleh menampilkan pesan sukses saat request gagal. Aktual: " + errorSnackbar);
        Assert.assertTrue(page.getCurrentUrl().contains("/apps/absent/reports/share_report"),
                "UI tidak boleh crash dan tetap berada di halaman Download Report.");
    }

    @Test(priority = 141, description = "SIT_USR_UI_141: Klik tombol '< Kembali' pada header 'Halaman Download Report' kembali ke /apps/absent",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_141_ClickBackToHomeFromDownloadReportHeader() {
        DownloadReportPage page = loginDefaultAndOpenDownloadReport();
        page.clickBackToHome();
        boolean returned = page.waitForReturnToHome();
        String url = page.getCurrentUrl();
        ExtentTestManager.logInfo("SIT_USR_UI_141 - URL setelah klik '< Kembali': " + url);

        Assert.assertTrue(returned, "Harus kembali ke /apps/absent dari Halaman Download Report. Aktual: " + url);
    }
}

