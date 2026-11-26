package com.juaracoding.magang.tests.user.deny;

import com.juaracoding.magang.BaseTest;
import com.juaracoding.magang.api.helper.AdminApiHelper;
import com.juaracoding.magang.pages.user.deny.RegisterPage;
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
 * Pengujian Web UI Selenium untuk fitur Autentikasi-Register User Magang
 * (https://magang.dikahadir.com/absen/register).
 * Cakupan skenario: SIT_USR_UI_013 s/d SIT_USR_UI_025 (13 Test Cases).
 * Penanggung jawab: Tester Deny | Branch: feature/deny-user-register.
 */
public class RegisterTest extends BaseTest {

    // Password uji registrasi disimpan dalam bentuk Base64 (aturan zero plaintext)
    private static final String TEST_REGISTER_PASSWORD_ENCODED = "UmVnVGVzdF8yMDI2IQ==";
    private static final String VALID_SELFIE_FILE = "valid_selfie.jpg";
    private static final String INVALID_PDF_FILE = "invalid_upload.pdf";

    private static String cachedPoolNik;
    private final ThreadLocal<List<String>> createdEmails = ThreadLocal.withInitial(ArrayList::new);

    @BeforeClass(alwaysRun = true)
    public void resolveAvailablePoolNikOnce() {
        cachedPoolNik = AdminApiHelper.findAvailableNikFromPool();
        logger.info("Cached pool NIK for RegisterTest suite: {}", cachedPoolNik);
    }

    private String getPoolNik() {
        if (cachedPoolNik == null || cachedPoolNik.isBlank()) {
            cachedPoolNik = AdminApiHelper.findAvailableNikFromPool();
        }
        return cachedPoolNik;
    }

    private RegisterPage openRegisterPage() {
        RegisterPage registerPage = new RegisterPage(getDriver());
        registerPage.navigateToRegisterPage();
        return registerPage;
    }

    private String uniqueEmail(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "@mail.com";
    }

    private String registerPassword() {
        return SecurityUtil.decode(TEST_REGISTER_PASSWORD_ENCODED);
    }

    private void trackForCleanup(String email) {
        createdEmails.get().add(email);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupRegisteredTestAccounts() {
        List<String> emails = createdEmails.get();
        try {
            for (String email : emails) {
                AdminApiHelper.cleanupUserByEmailQuietly(email);
            }
        } finally {
            emails.clear();
            createdEmails.remove();
        }
    }

    @Test(priority = 13, description = "SIT_USR_UI_013: Navigasi dari halaman Login ke halaman Daftar Akun Baru (/absen/register)",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_013_NavigateFromLoginToRegister() {
        UserLoginPage loginPage = new UserLoginPage(getDriver());
        loginPage.navigateToLoginPage();
        ExtentTestManager.logInfo("SIT_USR_UI_013 - Klik tombol 'disini' pada teks 'Belum punya akun ? buat disini' di /absen/login.");
        loginPage.clickRegisterLink();

        RegisterPage registerPage = new RegisterPage(getDriver());
        boolean redirected = registerPage.waitForRedirectToRegister();
        String currentUrl = registerPage.getCurrentUrl();
        ExtentTestManager.logInfo("Current URL setelah klik link Register: " + currentUrl);

        Assert.assertTrue(redirected && currentUrl.contains("/absen/register"),
                "Harus berpindah ke halaman /absen/register. URL aktual: " + currentUrl);
        Assert.assertTrue(registerPage.areAllRegisterFormComponentsDisplayed(),
                "Ke-5 field pendaftaran (NIK, Nama Lengkap, Email, Password, Upload Selfie) dan tombol Daftar harus tampil.");
    }

    @Test(priority = 14, description = "SIT_USR_UI_014: Verifikasi seluruh komponen form /absen/register & input NIK valid dari pool",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_014_VerifyRegisterFormAndValidPoolNikInput() {
        RegisterPage registerPage = openRegisterPage();
        String availableNik = getPoolNik();
        ExtentTestManager.logInfo("SIT_USR_UI_014 - Memverifikasi komponen form Register & menginput NIK valid dari pool: " + availableNik);

        Assert.assertTrue(registerPage.areAllRegisterFormComponentsDisplayed(),
                "Seluruh komponen form /absen/register (#nik, #fullname, #email, #password, #selfie, #submit) harus tampil aktif.");

        registerPage.enterNik(availableNik);
        Assert.assertEquals(registerPage.getNikValue(), availableNik,
                "Input NIK valid dari pool harus diterima pada field #nik.");
        String nikValidation = registerPage.getNikValidationMessage();
        Assert.assertTrue(nikValidation == null || nikValidation.isEmpty(),
                "Input NIK valid tidak boleh memicu error validasi form: " + nikValidation);
    }

    @Test(priority = 15, description = "SIT_USR_UI_015: Fungsi tombol ikon mata (Show/Hide Password) pada form Register (/absen/register)",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_015_ShowHidePasswordToggle() {
        RegisterPage registerPage = openRegisterPage();
        ExtentTestManager.logInfo("SIT_USR_UI_015 - Menguji tombol ikon mata Show/Hide pada field #password halaman Register.");
        registerPage.enterPassword(registerPassword());

        Assert.assertEquals(registerPage.getPasswordFieldType(), "password",
                "Default tipe field password pada form Register harus 'password'.");

        registerPage.clickShowHidePassword();
        String shownType = registerPage.getPasswordFieldType();
        ExtentTestManager.logInfo("Tipe field #password pasca klik ikon mata ke-1: " + shownType);
        Assert.assertEquals(shownType, "text",
                "Setelah ikon mata diklik pertama kali, tipe #password harus berubah menjadi 'text'.");

        registerPage.clickShowHidePassword();
        String hiddenType = registerPage.getPasswordFieldType();
        ExtentTestManager.logInfo("Tipe field #password pasca klik ikon mata ke-2: " + hiddenType);
        Assert.assertEquals(hiddenType, "password",
                "Setelah ikon mata diklik ulang, tipe #password harus kembali menjadi 'password'.");
    }

    @Test(priority = 16, description = "SIT_USR_UI_016: Submit pendaftaran dengan NIK yang tidak ada di master HRD (INVALID999)",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_016_SubmitWithInvalidUnregisteredMasterNik() {
        RegisterPage registerPage = openRegisterPage();
        String email = uniqueEmail("reg_invalidnik");
        ExtentTestManager.logInfo("SIT_USR_UI_016 - Menguji penolakan submit registrasi memakai NIK tidak terdaftar di master HRD: INVALID999.");

        registerPage.submitRegistration("INVALID999", "SQA Invalid NIK", email, registerPassword(), VALID_SELFIE_FILE);

        boolean alertDisplayed = registerPage.isAlertMessageDisplayed();
        String alertText = registerPage.getAlertText();
        ExtentTestManager.logInfo("Alert penolakan NIK tidak ditemukan: " + alertText);

        Assert.assertTrue(alertDisplayed && !alertText.toLowerCase().contains("berhasil register"),
                "Server harus menolak NIK INVALID999 dengan alert error. Alert aktual: " + alertText);
        Assert.assertTrue(registerPage.getCurrentUrl().contains("/absen/register"),
                "Halaman harus tetap berada di /absen/register setelah registrasi gagal.");
    }

    @Test(priority = 17, description = "SIT_USR_UI_017: Submit pendaftaran dengan NIK yang sudah terdaftar aktif (D8240010)",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_017_SubmitWithAlreadyRegisteredNik() {
        RegisterPage registerPage = openRegisterPage();
        String email = uniqueEmail("reg_dupnik");
        trackForCleanup(email);
        ExtentTestManager.logInfo("SIT_USR_UI_017 [Severity: Medium | Priority: P2] - Menguji penolakan registrasi memakai NIK aktif: " + Constants.USER_NIK);

        registerPage.submitRegistration(Constants.USER_NIK, "SQA Duplicate NIK", email, registerPassword(), VALID_SELFIE_FILE);

        boolean alertDisplayed = registerPage.isAlertMessageDisplayed();
        String alertText = registerPage.getAlertText();
        ExtentTestManager.logInfo("Alert respon NIK sudah terdaftar: " + alertText);

        Assert.assertTrue(alertDisplayed && !alertText.toLowerCase().contains("berhasil register"),
                "[KNOWN DEFECT] Sistem seharusnya menolak registrasi dengan NIK yang sudah aktif (" + Constants.USER_NIK
                        + "), namun live server mengizinkan duplikasi NIK dengan alert: " + alertText);
        Assert.assertTrue(registerPage.getCurrentUrl().contains("/absen/register"),
                "Halaman harus tetap berada di /absen/register.");
    }

    @Test(priority = 18, description = "SIT_USR_UI_018: Submit pendaftaran lengkap dengan NIK valid, Nama, Email, Password & Foto Selfie",
            groups = {"smoke", "regression", "positive"})
    public void testSIT_USR_UI_018_SubmitCompleteValidRegistration() {
        RegisterPage registerPage = openRegisterPage();
        String availableNik = getPoolNik();
        String email = uniqueEmail("reg_valid");
        trackForCleanup(email);

        ExtentTestManager.logInfo("SIT_USR_UI_018 - Mengirim pendaftaran lengkap dengan NIK pool=" + availableNik + ", email=" + email);
        registerPage.submitRegistration(availableNik, "SQA Automation Register", email, registerPassword(), VALID_SELFIE_FILE);

        boolean alertDisplayed = registerPage.isAlertMessageDisplayed();
        String alertText = registerPage.getAlertText();
        ExtentTestManager.logInfo("Alert hasil submit registrasi valid: " + alertText);

        Assert.assertTrue(alertDisplayed, "Alert sukses registrasi harus tampil.");
        Assert.assertTrue(alertText.toLowerCase().contains("berhasil register"),
                "Alert harus mengandung pesan sukses 'berhasil register, silahkan menunggu di approve oleh admin'. Aktual: " + alertText);
    }

    @Test(priority = 19, description = "SIT_USR_UI_019: Submit pendaftaran dengan format email tidak valid",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_019_SubmitWithInvalidEmailFormat() {
        RegisterPage registerPage = openRegisterPage();
        String availableNik = getPoolNik();
        ExtentTestManager.logInfo("SIT_USR_UI_019 - Menguji validasi format email invalid 'user_tanpa_at.com' pada form Register.");

        registerPage.submitRegistration(availableNik, "SQA Invalid Email", "user_tanpa_at.com", registerPassword(), VALID_SELFIE_FILE);

        String validationMsg = registerPage.getEmailValidationMessage();
        ExtentTestManager.logInfo("HTML5 email validationMessage: " + validationMsg);

        Assert.assertTrue((validationMsg != null && !validationMsg.isEmpty())
                        || (registerPage.isAlertMessageDisplayed() && !registerPage.getAlertText().toLowerCase().contains("berhasil")),
                "Form wajib menolak format email tanpa '@'. ValidationMsg: " + validationMsg);
        Assert.assertTrue(registerPage.getCurrentUrl().contains("/absen/register"),
                "Form dengan format email salah tidak boleh lolos registrasi.");
    }

    @Test(priority = 20, description = "SIT_USR_UI_020: Submit pendaftaran dengan email yang sudah dipakai akun lain (hadirsqa1@gmail.com)",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_020_SubmitWithDuplicateExistingEmail() {
        RegisterPage registerPage = openRegisterPage();
        String availableNik = getPoolNik();
        ExtentTestManager.logInfo("SIT_USR_UI_020 - Menguji registrasi menggunakan email yang sudah terdaftar: " + Constants.USER_EMAIL);

        registerPage.submitRegistration(availableNik, "SQA Duplicate Email", Constants.USER_EMAIL, registerPassword(), VALID_SELFIE_FILE);

        boolean alertDisplayed = registerPage.isAlertMessageDisplayed();
        String alertText = registerPage.getAlertText();
        ExtentTestManager.logInfo("Alert penolakan duplicate email: " + alertText);

        Assert.assertTrue(alertDisplayed && !alertText.toLowerCase().contains("berhasil register"),
                "Sistem harus menolak registrasi dengan email yang sudah digunakan. Alert aktual: " + alertText);
    }

    @Test(priority = 21, description = "SIT_USR_UI_021: Submit pendaftaran dengan password kurang dari batas minimum karakter ('123')",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_021_SubmitWithShortPasswordBelowMinimum() {
        RegisterPage registerPage = openRegisterPage();
        String availableNik = getPoolNik();
        String email = uniqueEmail("reg_shortpass");
        trackForCleanup(email);
        ExtentTestManager.logInfo("SIT_USR_UI_021 [Severity: Medium | Priority: P2] - Menguji submit pendaftaran dengan password < 6 karakter ('123').");

        registerPage.submitRegistration(availableNik, "SQA Short Password", email, "123", VALID_SELFIE_FILE);

        String passValidation = registerPage.getPasswordValidationMessage();
        boolean alertDisplayed = registerPage.isAlertMessageDisplayed();
        String alertText = alertDisplayed ? registerPage.getAlertText() : "";
        ExtentTestManager.logInfo("Respon password '123' -> validationMessage=[" + passValidation + "], alert=[" + alertText + "]");

        Assert.assertTrue((passValidation != null && !passValidation.isEmpty())
                        || (alertDisplayed && !alertText.toLowerCase().contains("berhasil register")),
                "[KNOWN DEFECT] Sistem tidak boleh meloloskan pendaftaran dengan password kurang dari 6 karakter ('123'), namun live meloloskan dengan alert: " + alertText);
    }

    @Test(priority = 22, description = "SIT_USR_UI_022: Submit pendaftaran tanpa mengunggah foto selfie wajib",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_022_SubmitWithoutMandatorySelfie() {
        RegisterPage registerPage = openRegisterPage();
        String availableNik = getPoolNik();
        String email = uniqueEmail("reg_nofile");
        ExtentTestManager.logInfo("SIT_USR_UI_022 - Menguji validasi wajib unggah foto selfie saat #selfie dikosongkan.");

        registerPage.submitRegistration(availableNik, "SQA Missing Selfie", email, registerPassword(), null);

        String selfieValidation = registerPage.getSelfieValidationMessage();
        String inlineError = registerPage.getInlineFileErrorText();
        ExtentTestManager.logInfo("Validasi #selfie kosong -> validationMessage=[" + selfieValidation + "], inlineError=[" + inlineError + "]");

        Assert.assertTrue((selfieValidation != null && !selfieValidation.isEmpty())
                        || !inlineError.isEmpty(),
                "Form wajib meminta pengguna mengunggah foto selfie terlebih dahulu (HTML5 required / pesan error).");
        Assert.assertTrue(registerPage.getCurrentUrl().contains("/absen/register"),
                "Registrasi tanpa foto selfie harus tertahan di /absen/register.");
    }

    @Test(priority = 23, description = "SIT_USR_UI_023: Upload file dokumen (.pdf/.txt) pada kolom foto selfie registrasi",
            groups = {"regression", "negative"})
    public void testSIT_USR_UI_023_UploadNonImageDocumentOnSelfieField() {
        RegisterPage registerPage = openRegisterPage();
        String availableNik = getPoolNik();
        String email = uniqueEmail("reg_pdf");
        ExtentTestManager.logInfo("SIT_USR_UI_023 - Menguji upload file non-gambar (" + INVALID_PDF_FILE + ") pada kolom #selfie.");

        registerPage.submitRegistration(availableNik, "SQA Invalid File", email, registerPassword(), INVALID_PDF_FILE);

        String inlineError = registerPage.getInlineFileErrorText();
        boolean alertDisplayed = registerPage.isAlertMessageDisplayed();
        String alertText = alertDisplayed ? registerPage.getAlertText() : "";
        ExtentTestManager.logInfo("Pesan penolakan file non-gambar -> inline=[" + inlineError + "], alert=[" + alertText + "]");

        Assert.assertTrue(inlineError.contains("*File harus berupa gambar")
                        || alertText.contains("*File harus berupa gambar"),
                "Sistem harus menolak file .pdf dengan pesan '*File harus berupa gambar'. Inline=["
                        + inlineError + "], Alert=[" + alertText + "]");
    }

    @Test(priority = 24, description = "SIT_USR_UI_024: Verifikasi preview foto selfie tampil saat dipilih & gambar tetap utuh pasca submit",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_024_SelfiePreviewAndUploadedImageIntegrity() {
        RegisterPage registerPage = openRegisterPage();
        String availableNik = getPoolNik();
        String email = uniqueEmail("reg_kd_photo");
        trackForCleanup(email);

        ExtentTestManager.logInfo("SIT_USR_UI_024 [Known Defect | Severity: High | Priority: P1] - Memilih "
                + VALID_SELFIE_FILE + " di #selfie, memeriksa elemen <img> preview, submit registrasi & mengecek /uploads/<id>.");

        registerPage.fillRegistrationForm(availableNik, "SQA Preview Check", email, registerPassword(), VALID_SELFIE_FILE);
        boolean previewDisplayed = registerPage.isSelfiePreviewDisplayedAndLoaded();
        ExtentTestManager.logInfo("Status preview <img> di form /absen/register: " + previewDisplayed);

        registerPage.clickDaftar();
        registerPage.isAlertMessageDisplayed();
        String alertText = registerPage.getAlertText();
        String photoId = AdminApiHelper.findUserPhotoByEmail(email);
        int uploadHttpStatus = AdminApiHelper.checkStaticUploadStatus(photoId);
        ExtentTestManager.logInfo("Hasil submit: alert=[" + alertText + "], photo=[" + photoId
                + "], static GET HTTP=" + uploadHttpStatus);

        boolean uploadedImageReadable = (photoId != null && !photoId.isBlank() && uploadHttpStatus == 200);
        Assert.assertTrue(previewDisplayed && uploadedImageReadable,
                "[KNOWN DEFECT] Preview foto selfie di /absen/register tidak dirender ke elemen <img> (previewDisplayed="
                        + previewDisplayed + ") atau file terunggah di /uploads/<id> mengembalikan broken/HTTP "
                        + uploadHttpStatus + " (photo=" + photoId + ").");
    }

    @Test(priority = 25, description = "SIT_USR_UI_025: Klik tombol tautan 'Sudah punya akun ? masuk disini' dari halaman Registrasi",
            groups = {"regression", "positive"})
    public void testSIT_USR_UI_025_ClickAlreadyHaveAccountLinkToLogin() {
        RegisterPage registerPage = openRegisterPage();
        ExtentTestManager.logInfo("SIT_USR_UI_025 - Klik tombol 'disini' pada teks 'Sudah punya akun ? masuk disini'.");
        registerPage.clickBackToLoginLink();

        boolean backToLogin = registerPage.waitForRedirectToLogin();
        String currentUrl = registerPage.getCurrentUrl();
        ExtentTestManager.logInfo("Current URL setelah klik kembali ke Login: " + currentUrl);

        Assert.assertTrue(backToLogin && currentUrl.contains("/absen/login"),
                "Harus berpindah kembali ke halaman /absen/login. URL aktual: " + currentUrl);
    }
}
