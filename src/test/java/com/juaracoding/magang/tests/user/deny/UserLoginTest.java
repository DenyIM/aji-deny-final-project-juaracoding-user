package com.juaracoding.magang.tests.user.deny;

import com.juaracoding.magang.BaseTest;
import com.juaracoding.magang.pages.user.deny.UserLoginPage;
import com.juaracoding.magang.reports.ExtentTestManager;
import com.juaracoding.magang.utils.Constants;
import com.juaracoding.magang.utils.SecurityUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Pengujian Web UI Selenium untuk fitur Autentikasi-Login User Magang (Mobile Mirroring).
 * Cakupan skenario: SIT_USR_UI_001 s/d SIT_USR_UI_012 (12 Test Cases).
 * Penanggung jawab: Tester Deny | Branch: feature/deny-user-login.
 */
public class UserLoginTest extends BaseTest {

    private UserLoginPage openLoginPage() {
        UserLoginPage loginPage = new UserLoginPage(getDriver());
        loginPage.navigateToLoginPage();
        return loginPage;
    }

    @Test(priority = 1, description = "SIT_USR_UI_001: Login User Magang dengan kredensial valid (hadirsqa1@gmail.com)",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_001_ValidUserLogin() {
        UserLoginPage loginPage = openLoginPage();
        ExtentTestManager.logInfo("SIT_USR_UI_001 - Login menggunakan email Karyawan valid & password hasil decode Base64.");
        loginPage.loginWithDefaultUser();

        boolean redirected = loginPage.waitForRedirectToAbsent();
        String currentUrl = loginPage.getCurrentUrl();
        ExtentTestManager.logInfo("Current URL setelah submit login: " + currentUrl);

        Assert.assertTrue(redirected || currentUrl.contains("/apps/absent"),
                "User harus berhasil login dan diarahkan ke /apps/absent. URL aktual: " + currentUrl
                        + (loginPage.isAlertMessageDisplayed() ? " | Alert: " + loginPage.getAlertText() : ""));
    }

    @Test(priority = 2, description = "SIT_USR_UI_002: Login User Magang dengan password salah",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_002_InvalidPassword() {
        UserLoginPage loginPage = openLoginPage();
        ExtentTestManager.logInfo("SIT_USR_UI_002 - Menguji penolakan login User dengan password salah.");
        loginPage.login(Constants.USER_EMAIL, "Salah123");

        boolean alertShown = loginPage.isAlertMessageDisplayed();
        String alertText = loginPage.getAlertText();
        ExtentTestManager.logInfo("Alert error tampil: " + alertText);

        Assert.assertTrue(alertShown, "Alert error harus tampil saat password salah.");
        Assert.assertTrue(loginPage.getCurrentUrl().contains("/absen/login"),
                "Halaman harus tetap berada di /absen/login.");
    }

    @Test(priority = 3, description = "SIT_USR_UI_003: Login User Magang dengan email tidak terdaftar",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_003_UnregisteredEmail() {
        UserLoginPage loginPage = openLoginPage();
        ExtentTestManager.logInfo("SIT_USR_UI_003 - Menguji penolakan login User dengan email tidak terdaftar.");
        loginPage.login("unreg_user@gmail.com", "AnyPass123!");

        boolean alertShown = loginPage.isAlertMessageDisplayed();
        String alertText = loginPage.getAlertText();
        ExtentTestManager.logInfo("Alert error tampil: " + alertText);

        Assert.assertTrue(alertShown, "Alert error harus tampil untuk email tidak terdaftar.");
        Assert.assertFalse(loginPage.getCurrentUrl().contains("/apps/absent"),
                "Akun tidak terdaftar tidak boleh redirect ke /apps/absent.");
    }

    @Test(priority = 4, description = "SIT_USR_UI_004: Login User Magang dengan format email tanpa domain (@)",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_004_InvalidEmailFormatWithoutAt() {
        UserLoginPage loginPage = openLoginPage();
        ExtentTestManager.logInfo("SIT_USR_UI_004 - Menguji validasi HTML5/form format email tanpa '@'.");
        loginPage.login("hadirsqa1gmail.com", "AnyPass123!");

        String validationMsg = loginPage.getEmailValidationMessage();
        ExtentTestManager.logInfo("HTML5 validationMessage: " + validationMsg);

        Assert.assertTrue((validationMsg != null && !validationMsg.isEmpty()) || loginPage.isAlertMessageDisplayed(),
                "Form harus menolak format email tanpa '@'.");
        Assert.assertTrue(loginPage.getCurrentUrl().contains("/absen/login"),
                "Form tidak boleh terkirim dan tetap di /absen/login.");
    }

    @Test(priority = 5, description = "SIT_USR_UI_005: Login User Magang dengan field email kosong",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_005_EmptyEmailField() {
        UserLoginPage loginPage = openLoginPage();
        ExtentTestManager.logInfo("SIT_USR_UI_005 - Menguji login dengan field email dikosongkan.");
        loginPage.login("", SecurityUtil.decode(Constants.USER_PASSWORD_ENCODED));

        String currentUrl = loginPage.getCurrentUrl();
        ExtentTestManager.logInfo("URL setelah submit email kosong: " + currentUrl);

        Assert.assertTrue(currentUrl.contains("/absen/login"),
                "User harus tetap tertahan di halaman /absen/login ketika email kosong.");
        Assert.assertFalse(currentUrl.contains("/apps/absent"),
                "Login tanpa email tidak boleh masuk ke /apps/absent.");
    }

    @Test(priority = 6, description = "SIT_USR_UI_006: Login User Magang dengan field password kosong",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_006_EmptyPasswordField() {
        UserLoginPage loginPage = openLoginPage();
        ExtentTestManager.logInfo("SIT_USR_UI_006 - Menguji login dengan field password dikosongkan.");
        loginPage.login(Constants.USER_EMAIL, "");

        String currentUrl = loginPage.getCurrentUrl();
        ExtentTestManager.logInfo("URL setelah submit password kosong: " + currentUrl);

        Assert.assertTrue(currentUrl.contains("/absen/login"),
                "User harus tetap tertahan di halaman /absen/login ketika password kosong.");
        Assert.assertFalse(currentUrl.contains("/apps/absent"),
                "Login tanpa password tidak boleh masuk ke /apps/absent.");
    }

    @Test(priority = 7, description = "SIT_USR_UI_007: Login User Magang dengan kedua field kosong",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_007_BothFieldsEmpty() {
        UserLoginPage loginPage = openLoginPage();
        ExtentTestManager.logInfo("SIT_USR_UI_007 - Menguji submit form dengan email & password kosong.");
        loginPage.login("", "");

        String currentUrl = loginPage.getCurrentUrl();
        ExtentTestManager.logInfo("URL setelah submit kedua field kosong: " + currentUrl);

        Assert.assertTrue(currentUrl.contains("/absen/login"),
                "Form kosong tidak boleh mengizinkan akses ke /apps/absent.");
    }

    @Test(priority = 8, description = "SIT_USR_UI_008: Fungsi tombol Show/Hide Password (ikon mata) pada Login User",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_008_ShowHidePasswordToggle() {
        UserLoginPage loginPage = openLoginPage();
        ExtentTestManager.logInfo("SIT_USR_UI_008 - Menguji toggle ikon mata pada field password Login User.");
        loginPage.enterPassword("SecretCheck123!");

        Assert.assertEquals(loginPage.getPasswordFieldType(), "password",
                "Default tipe field password harus 'password'.");

        loginPage.clickShowHidePassword();
        String shownType = loginPage.getPasswordFieldType();
        ExtentTestManager.logInfo("Tipe field setelah klik ikon mata pertama: " + shownType);
        Assert.assertEquals(shownType, "text",
                "Setelah ikon mata diklik, tipe field harus berubah menjadi 'text'.");

        loginPage.clickShowHidePassword();
        String hiddenType = loginPage.getPasswordFieldType();
        ExtentTestManager.logInfo("Tipe field setelah klik ikon mata kedua: " + hiddenType);
        Assert.assertEquals(hiddenType, "password",
                "Setelah ikon mata diklik ulang, tipe field harus kembali ke 'password'.");
    }

    @Test(priority = 9, description = "SIT_USR_UI_009: Proteksi SQL Injection pada form Login User",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_009_SqlInjectionOnEmail() {
        UserLoginPage loginPage = openLoginPage();
        ExtentTestManager.logInfo("SIT_USR_UI_009 - Menguji ketahanan form login terhadap SQL Injection payload.");
        loginPage.login("' OR 1=1 --", "Anything123!");

        String currentUrl = loginPage.getCurrentUrl();
        ExtentTestManager.logInfo("URL setelah percobaan SQL Injection: " + currentUrl);

        Assert.assertFalse(currentUrl.contains("/apps/absent"),
                "Payload SQL Injection tidak boleh berhasil masuk ke /apps/absent.");
        Assert.assertTrue(currentUrl.contains("/absen/login"),
                "Halaman harus tetap tertahan di /absen/login.");
    }

    @Test(priority = 10, description = "SIT_USR_UI_010: Proteksi XSS Script Injection pada form Login User",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_010_XssInjectionOnEmail() {
        UserLoginPage loginPage = openLoginPage();
        ExtentTestManager.logInfo("SIT_USR_UI_010 - Menguji ketahanan form login terhadap XSS script payload.");
        loginPage.login("<script>alert(1)</script>", "Anything123!");

        Assert.assertFalse(loginPage.isBrowserJsAlertPresent(),
                "Script XSS tidak boleh mengeksekusi JS alert() di browser.");
        String currentUrl = loginPage.getCurrentUrl();
        ExtentTestManager.logInfo("URL setelah percobaan XSS Injection: " + currentUrl);

        Assert.assertFalse(currentUrl.contains("/apps/absent"),
                "Payload XSS tidak boleh mengizinkan login ke /apps/absent.");
        Assert.assertTrue(currentUrl.contains("/absen/login"),
                "Halaman harus tetap aman di /absen/login.");
    }

    @Test(priority = 11, description = "SIT_USR_UI_011: Akses langsung halaman /apps/absent tanpa login",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_011_UnauthenticatedDirectAccess() {
        UserLoginPage loginPage = new UserLoginPage(getDriver());
        ExtentTestManager.logInfo("SIT_USR_UI_011 - Mengakses langsung /apps/absent tanpa sesi login.");
        loginPage.navigateToAbsentDirectly();

        boolean redirectedToLogin = loginPage.waitForRedirectToLogin();
        String currentUrl = loginPage.getCurrentUrl();
        ExtentTestManager.logInfo("URL hasil proteksi route tanpa sesi: " + currentUrl);

        Assert.assertTrue(redirectedToLogin || loginPage.isEmailFieldDisplayed(),
                "Akses tanpa sesi wajib diarahkan kembali ke /absen/login. URL aktual: " + currentUrl);
    }

    @Test(priority = 12, description = "SIT_USR_UI_012: Logout dari aplikasi mobile user dan cek tombol Back browser",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_012_UserLogoutAndBackProtection() {
        UserLoginPage loginPage = openLoginPage();
        ExtentTestManager.logInfo("SIT_USR_UI_012 - Login sebagai User lalu melakukan Logout dan uji tombol Back browser.");
        loginPage.loginWithDefaultUser();

        boolean loggedIn = loginPage.waitForRedirectToAbsent();
        Assert.assertTrue(loggedIn, "Prasyarat login User harus berhasil masuk ke /apps/absent. URL aktual: "
                + loginPage.getCurrentUrl()
                + (loginPage.isAlertMessageDisplayed() ? " | Alert: " + loginPage.getAlertText() : ""));

        loginPage.performLogout();
        boolean backToLogin = loginPage.waitForRedirectToLogin();
        String urlAfterLogout = loginPage.getCurrentUrl();
        ExtentTestManager.logInfo("URL setelah Logout: " + urlAfterLogout);

        Assert.assertTrue(backToLogin || urlAfterLogout.contains("/absen/login"),
                "Setelah logout, User harus kembali ke /absen/login. URL aktual: " + urlAfterLogout);

        loginPage.navigateBack();
        boolean stillProtected = loginPage.waitForRedirectToLogin() || loginPage.isEmailFieldDisplayed();
        String urlAfterBack = loginPage.getCurrentUrl();
        ExtentTestManager.logInfo("URL setelah tekan tombol Back browser: " + urlAfterBack);

        Assert.assertTrue(stillProtected && !urlAfterBack.endsWith("/apps/absent"),
                "Setelah Logout dan tekan tombol Back, user tidak boleh mengakses /apps/absent. URL aktual: " + urlAfterBack);
    }
}
