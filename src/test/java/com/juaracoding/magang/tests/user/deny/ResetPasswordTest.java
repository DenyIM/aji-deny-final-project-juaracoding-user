package com.juaracoding.magang.tests.user.deny;

import com.juaracoding.magang.BaseTest;
import com.juaracoding.magang.pages.user.deny.ResetPasswordPage;
import com.juaracoding.magang.pages.user.deny.UserLoginPage;
import com.juaracoding.magang.reports.ExtentTestManager;
import com.juaracoding.magang.utils.Constants;
import com.juaracoding.magang.utils.SecurityUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Pengujian Web UI Selenium untuk fitur Autentikasi-Reset Password User Magang
 * (/absen/reset-password-request & /absen/reset-password).
 * Cakupan skenario: SIT_USR_UI_026 s/d SIT_USR_UI_035 (10 Test Cases).
 * Penanggung jawab: Tester Deny | Branch: feature/deny-user-reset-password.
 */
public class ResetPasswordTest extends BaseTest {

        // Password uji disimpan dalam bentuk Base64 sesuai kebijakan zero plaintext
        private static final String TEST_NEW_PASSWORD_ENCODED = "TmV3UGFzc18yMDI2IQ=="; // 13 karakter (> 8)
        private static final String TEST_MISMATCH_PASSWORD_ENCODED = "T3RoZXJQYXNzXzIwMjYh"; // 15 karakter (> 8)
        private static final String TEST_SHORT_PASSWORD_ENCODED = "UGFzczEyMzQ="; // tepat 8 karakter (<= 8)

        private String decodedNewPassword() {
                return SecurityUtil.decode(TEST_NEW_PASSWORD_ENCODED);
        }

        private String decodedMismatchPassword() {
                return SecurityUtil.decode(TEST_MISMATCH_PASSWORD_ENCODED);
        }

        private String decodedShortPassword() {
                return SecurityUtil.decode(TEST_SHORT_PASSWORD_ENCODED);
        }

        @Test(priority = 26, description = "SIT_USR_UI_026: Navigasi tombol 'Lupa password ?' dari halaman Login ke /absen/reset-password-request", groups = {
                        "smoke", "regression", "positive" })
        public void testSIT_USR_UI_026_NavigateFromLoginToResetRequest() {
                UserLoginPage loginPage = new UserLoginPage(getDriver());
                loginPage.navigateToLoginPage();
                ExtentTestManager.logInfo("SIT_USR_UI_026 - Klik tombol 'Lupa password ?' pada halaman /absen/login.");
                loginPage.clickForgotPasswordLink();

                ResetPasswordPage resetPage = new ResetPasswordPage(getDriver());
                boolean redirected = resetPage.waitForRedirectToRequestPage();
                String currentUrl = resetPage.getCurrentUrl();
                ExtentTestManager.logInfo("Current URL setelah klik Lupa password: " + currentUrl);

                Assert.assertTrue(redirected && currentUrl.contains("/absen/reset-password-request"),
                                "Harus berpindah ke halaman /absen/reset-password-request. URL aktual: " + currentUrl);
                Assert.assertTrue(resetPage.isEmailFieldDisplayed() && resetPage.isSubmitButtonDisplayed(),
                                "Input #email dan tombol Submit harus tampil di /absen/reset-password-request.");
        }

        @Test(priority = 27, description = "SIT_USR_UI_027: Request reset password email terdaftar -> alert sukses & redirect ke /absen/reset-password", groups = {
                        "regression", "positive" })
        public void testSIT_USR_UI_027_RequestResetRegisteredEmailAndRedirect() {
                ResetPasswordPage resetPage = new ResetPasswordPage(getDriver());
                resetPage.navigateToRequestPage();
                ExtentTestManager.logInfo("SIT_USR_UI_027 - Mengirim request reset password untuk email terdaftar: "
                                + Constants.USER_EMAIL);

                resetPage.submitEmailRequest(Constants.USER_EMAIL);

                boolean alertDisplayed = resetPage.isAlertMessageDisplayed();
                String alertText = resetPage.getAlertText();
                ExtentTestManager.logInfo("Alert respon request reset email: " + alertText);

                Assert.assertTrue(alertDisplayed && alertText.toLowerCase().contains("link reset password terkirim"),
                                "Alert sukses 'Link reset password terkirim, silahkan cek inbox email' harus tampil. Aktual: "
                                                + alertText);

                boolean redirected = resetPage.waitForRedirectToResetPage();
                String currentUrl = resetPage.getCurrentUrl();
                ExtentTestManager.logInfo("Current URL setelah timeout redirect 5 detik: " + currentUrl);

                Assert.assertTrue(redirected && currentUrl.contains("/absen/reset-password")
                                && !currentUrl.contains("/absen/reset-password-request"),
                                "Setelah 5 detik, halaman harus redirect ke /absen/reset-password. URL aktual: "
                                                + currentUrl);
        }

        @Test(priority = 28, description = "SIT_USR_UI_028: Request reset password dengan email yang tidak terdaftar", groups = {
                        "regression", "negative" })
        public void testSIT_USR_UI_028_RequestResetUnregisteredEmail() {
                ResetPasswordPage resetPage = new ResetPasswordPage(getDriver());
                resetPage.navigateToRequestPage();
                ExtentTestManager.logInfo(
                                "SIT_USR_UI_028 - Menguji request reset password dengan email tidak terdaftar (unknown_reset@gmail.com).");

                resetPage.submitEmailRequest("unknown_reset@gmail.com");

                boolean alertDisplayed = resetPage.isAlertMessageDisplayed();
                String alertText = resetPage.getAlertText();
                ExtentTestManager.logInfo("Alert respon email tidak terdaftar: " + alertText);

                Assert.assertTrue(alertDisplayed && !alertText.toLowerCase().contains("link reset password terkirim"),
                                "Server harus menolak email tidak terdaftar dengan alert error. Alert aktual: "
                                                + alertText);
                Assert.assertTrue(resetPage.getCurrentUrl().contains("/absen/reset-password-request"),
                                "Halaman harus tetap berada di /absen/reset-password-request.");
        }

        @Test(priority = 29, description = "SIT_USR_UI_029: Request reset password dengan field email dikosongkan", groups = {
                        "regression", "negative" })
        public void testSIT_USR_UI_029_RequestResetWithEmptyEmail() {
                ResetPasswordPage resetPage = new ResetPasswordPage(getDriver());
                resetPage.navigateToRequestPage();
                ExtentTestManager.logInfo(
                                "SIT_USR_UI_029 - Menguji submit /absen/reset-password-request saat field #email dikosongkan.");

                resetPage.submitEmailRequest("");

                String validationMsg = resetPage.getEmailValidationMessage();
                boolean alertDisplayed = resetPage.isAlertMessageDisplayed();
                String alertText = alertDisplayed ? resetPage.getAlertText() : "";
                ExtentTestManager.logInfo("Respon email kosong -> validationMessage=[" + validationMsg + "], alert=["
                                + alertText + "]");

                Assert.assertTrue((validationMsg != null && !validationMsg.isEmpty())
                                || (alertDisplayed
                                                && !alertText.toLowerCase().contains("link reset password terkirim")),
                                "Form wajib menahan atau menolak pengiriman jika email kosong. Alert aktual: "
                                                + alertText);
                Assert.assertTrue(resetPage.getCurrentUrl().contains("/absen/reset-password-request"),
                                "Halaman harus tetap berada di /absen/reset-password-request.");
        }

        @Test(priority = 30, description = "SIT_USR_UI_030: Request reset password dengan format email tidak valid", groups = {
                        "regression", "negative" })
        public void testSIT_USR_UI_030_RequestResetWithInvalidEmailFormat() {
                ResetPasswordPage resetPage = new ResetPasswordPage(getDriver());
                resetPage.navigateToRequestPage();
                ExtentTestManager.logInfo(
                                "SIT_USR_UI_030 - Menguji validasi HTML5 type='email' dengan input 'email_invalid_tanpa_at'.");

                resetPage.submitEmailRequest("email_invalid_tanpa_at");

                String validationMsg = resetPage.getEmailValidationMessage();
                ExtentTestManager.logInfo("HTML5 email validationMessage: " + validationMsg);

                Assert.assertTrue(validationMsg != null && !validationMsg.isEmpty(),
                                "Browser HTML5 validation type='email' harus menolak format 'email_invalid_tanpa_at'.");
                Assert.assertTrue(resetPage.getCurrentUrl().contains("/absen/reset-password-request"),
                                "Form format email tidak valid tidak boleh ter-submit.");
        }

        @Test(priority = 31, description = "SIT_USR_UI_031: Verifikasi struktur form /absen/reset-password & validasi submit Password Baru >= 8 karakter", groups = {
                        "smoke", "regression", "positive" })
        public void testSIT_USR_UI_031_VerifyResetFormAndSubmitValidationFlow() {
                ResetPasswordPage resetPage = new ResetPasswordPage(getDriver());
                resetPage.navigateToResetPageWithEmail(Constants.USER_EMAIL);
                ExtentTestManager.logInfo(
                                "SIT_USR_UI_031 - Memeriksa komponen form /absen/reset-password (#password, #passwordConfirm, #otp) & lolos validasi client sebelum /auth-management.");

                Assert.assertTrue(resetPage.areAllResetFormFieldsDisplayed(),
                                "Ketiga input (#password, #passwordConfirm, #otp) dan tombol Submit harus tampil pada /absen/reset-password.");

                String validPwd = decodedNewPassword();
                resetPage.submitNewPassword(validPwd, validPwd, "000000");

                boolean alertDisplayed = resetPage.isAlertMessageDisplayed();
                String alertText = resetPage.getAlertText();
                ExtentTestManager.logInfo(
                                "Alert respon /auth-management (OTP dummy '000000' demi keamanan akun): " + alertText);

                Assert.assertTrue(alertDisplayed,
                                "Harus menerima respon alert pasca melewati validasi client dan memanggil /auth-management.");
                Assert.assertFalse(alertText.toLowerCase().contains("lebih dari 8 karakter")
                                || alertText.toLowerCase().contains("tidak sama"),
                                "Password >= 9 karakter yang cocok tidak boleh ditolak oleh validasi client. Alert aktual: "
                                                + alertText);
        }

        @Test(priority = 32, description = "SIT_USR_UI_032: Submit /absen/reset-password dengan Password konfirmasi tidak cocok", groups = {
                        "regression", "negative" })
        public void testSIT_USR_UI_032_SubmitWithMismatchConfirmPassword() {
                ResetPasswordPage resetPage = new ResetPasswordPage(getDriver());
                resetPage.navigateToResetPageWithEmail(Constants.USER_EMAIL);
                ExtentTestManager.logInfo(
                                "SIT_USR_UI_032 - Menguji validasi client ketika #password != #passwordConfirm.");

                resetPage.submitNewPassword(decodedNewPassword(), decodedMismatchPassword(), "123456");

                boolean alertDisplayed = resetPage.isAlertMessageDisplayed();
                String alertText = resetPage.getAlertText();
                ExtentTestManager.logInfo("Alert penolakan mismatch password: " + alertText);

                Assert.assertTrue(
                                alertDisplayed && alertText.toLowerCase()
                                                .contains("tidak sama dengan password konfirmasi"),
                                "Harus muncul alert 'password tidak sama dengan password konfirmasi'. Aktual: "
                                                + alertText);
        }

        @Test(priority = 33, description = "SIT_USR_UI_033: Submit reset password dengan panjang password <= 8 karakter atau OTP invalid", groups = {
                        "regression", "negative" })
        public void testSIT_USR_UI_033_SubmitShortPasswordOrInvalidOtp() {
                ResetPasswordPage resetPage = new ResetPasswordPage(getDriver());
                resetPage.navigateToResetPageWithEmail(Constants.USER_EMAIL);
                ExtentTestManager.logInfo(
                                "SIT_USR_UI_033 (Tahap 1) - Uji input password <= 8 karakter harus ditolak validasi client.");

                String shortPwd = decodedShortPassword();
                resetPage.submitNewPassword(shortPwd, shortPwd, "000000");

                String shortAlert = resetPage.getAlertText();
                ExtentTestManager.logInfo("Alert validasi panjang password <= 8: " + shortAlert);
                Assert.assertTrue(shortAlert.toLowerCase().contains("password harus lebih dari 8 karakter"),
                                "Validasi client harus menampilkan 'password harus lebih dari 8 karakter'. Aktual: "
                                                + shortAlert);

                ExtentTestManager.logInfo(
                                "SIT_USR_UI_033 (Tahap 2) - Uji password >= 9 karakter cocok dengan OTP invalid '000000'.");
                String validPwd = decodedNewPassword();
                resetPage.submitNewPassword(validPwd, validPwd, "000000");

                boolean otpAlertShown = resetPage.isAlertMessageDisplayed();
                String otpAlert = resetPage.getAlertText();
                ExtentTestManager.logInfo("Alert penolakan OTP invalid dari server: " + otpAlert);

                Assert.assertTrue(otpAlertShown && !otpAlert.toLowerCase().contains("berhasil diperbarui"),
                                "OTP invalid '000000' harus ditolak server dan tidak boleh memperbarui password. Alert aktual: "
                                                + otpAlert);
        }

        @Test(priority = 34, description = "SIT_USR_UI_034: Fungsi kedua tombol ikon mata (Show/Hide) pada Password Baru & Password konfirmasi", groups = {
                        "regression", "positive" })
        public void testSIT_USR_UI_034_ShowHideBothPasswordFields() {
                ResetPasswordPage resetPage = new ResetPasswordPage(getDriver());
                resetPage.navigateToResetPage();
                ExtentTestManager.logInfo(
                                "SIT_USR_UI_034 - Menguji toggle kedua ikon mata pada #password & #passwordConfirm.");

                String sample = decodedNewPassword();
                resetPage.enterNewPassword(sample);
                resetPage.enterConfirmPassword(sample);

                Assert.assertEquals(resetPage.getNewPasswordFieldType(), "password",
                                "Default tipe #password harus 'password'.");
                Assert.assertEquals(resetPage.getConfirmPasswordFieldType(), "password",
                                "Default tipe #passwordConfirm harus 'password'.");

                resetPage.clickShowHideNewPassword();
                Assert.assertEquals(resetPage.getNewPasswordFieldType(), "text",
                                "Klik ikon mata ke-1 pada #password harus mengubah tipe menjadi 'text'.");
                resetPage.clickShowHideNewPassword();
                Assert.assertEquals(resetPage.getNewPasswordFieldType(), "password",
                                "Klik ikon mata ke-2 pada #password harus mengembalikan tipe menjadi 'password'.");

                resetPage.clickShowHideConfirmPassword();
                Assert.assertEquals(resetPage.getConfirmPasswordFieldType(), "text",
                                "Klik ikon mata ke-1 pada #passwordConfirm harus mengubah tipe menjadi 'text'.");
                resetPage.clickShowHideConfirmPassword();
                Assert.assertEquals(resetPage.getConfirmPasswordFieldType(), "password",
                                "Klik ikon mata ke-2 pada #passwordConfirm harus mengembalikan tipe menjadi 'password'.");
        }

        @Test(priority = 35, description = "SIT_USR_UI_035: Klik tautan 'Kembali ke halaman Login' pada halaman Reset Password", groups = {
                        "regression", "positive" })
        public void testSIT_USR_UI_035_ClickBackToLoginLink() {
                ResetPasswordPage resetPage = new ResetPasswordPage(getDriver());

                ExtentTestManager.logInfo(
                                "SIT_USR_UI_035 (1/2) - Uji tombol 'Login' pada /absen/reset-password-request.");
                resetPage.navigateToRequestPage();
                resetPage.clickBackToLoginLink();
                Assert.assertTrue(
                                resetPage.waitForRedirectToLogin()
                                                && resetPage.getCurrentUrl().contains("/absen/login"),
                                "Harus kembali ke /absen/login dari /absen/reset-password-request.");

                ExtentTestManager.logInfo("SIT_USR_UI_035 (2/2) - Uji tombol 'Login' pada /absen/reset-password.");
                resetPage.navigateToResetPage();
                resetPage.clickBackToLoginLink();
                Assert.assertTrue(
                                resetPage.waitForRedirectToLogin()
                                                && resetPage.getCurrentUrl().contains("/absen/login"),
                                "Harus kembali ke /absen/login dari /absen/reset-password.");
        }
}
