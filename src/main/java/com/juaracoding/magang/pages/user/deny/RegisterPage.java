package com.juaracoding.magang.pages.user.deny;

import com.juaracoding.magang.pages.BasePage;
import com.juaracoding.magang.utils.Constants;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Page Object Model untuk halaman Pendaftaran Mandiri Karyawan
 * (https://magang.dikahadir.com/absen/register).
 * Cakupan skenario: SIT_USR_UI_013 s/d SIT_USR_UI_025 (Tester Deny).
 */
public class RegisterPage extends BasePage {

    private static final By SELFIE_PREVIEW_IMAGES = By.xpath(
            "//form//img | //img[starts-with(@src,'blob:') or contains(translate(@alt,'PREVIEWSELFIE','previewselfie'),'preview') or contains(translate(@alt,'PREVIEWSELFIE','previewselfie'),'selfie')]"
    );

    @FindBy(css = "input#nik")
    private WebElement nikInput;

    @FindBy(css = "input#fullname")
    private WebElement fullnameInput;

    @FindBy(css = "input#email")
    private WebElement emailInput;

    @FindBy(css = "input#password")
    private WebElement passwordInput;

    @FindBy(css = "div.MuiInputAdornment-root button")
    private WebElement showHidePasswordButton;

    @FindBy(css = "input#selfie")
    private WebElement selfieFileInput;

    @FindBy(xpath = "//input[@id='selfie']/ancestor::div[contains(@class,'MuiFormControl-root')]/following-sibling::p[1]")
    private WebElement inlineFileErrorText;

    @FindBy(css = "button#submit, button[type='submit']")
    private WebElement daftarButton;

    @FindBy(css = "div.MuiAlert-message")
    private WebElement alertMessage;

    @FindBy(xpath = "//p[contains(normalize-space(),'Sudah punya akun')]/following-sibling::button[normalize-space()='disini']")
    private WebElement alreadyHaveAccountLoginButton;

    public RegisterPage(WebDriver driver) {
        super(driver);
    }

    public void navigateToRegisterPage() {
        safeNavigate(Constants.USER_REGISTER_URL);
        waitForVisibility(nikInput);
        logger.info("Navigated to User Register page: {}", Constants.USER_REGISTER_URL);
    }

    public boolean waitForRedirectToRegister() {
        boolean urlMatched = waitForUrlContains("/absen/register");
        if (urlMatched) {
            waitForVisibility(nikInput);
        }
        return urlMatched;
    }

    public boolean waitForRedirectToLogin() {
        return waitForUrlContains("/absen/login");
    }

    public boolean areAllRegisterFormComponentsDisplayed() {
        return isDisplayed(nikInput)
                && isDisplayed(fullnameInput)
                && isDisplayed(emailInput)
                && isDisplayed(passwordInput)
                && isDisplayed(selfieFileInput)
                && isDisplayed(daftarButton);
    }

    public void enterNik(String nik) {
        type(nikInput, nik);
    }

    public String getNikValue() {
        return getAttribute(nikInput, "value");
    }

    public void enterFullname(String fullname) {
        type(fullnameInput, fullname);
    }

    public void enterEmail(String email) {
        type(emailInput, email);
    }

    public void enterPassword(String plainPassword) {
        type(passwordInput, plainPassword);
    }

    public void clickShowHidePassword() {
        click(showHidePasswordButton);
    }

    public String getPasswordFieldType() {
        return getAttribute(passwordInput, "type");
    }

    /**
     * Memilih file uji pada input #selfie tanpa melakukan klik agar dialog OS tidak muncul.
     *
     * @param testdataFileName nama file di dalam src/test/resources/testdata/
     */
    public void uploadSelfieFile(String testdataFileName) {
        Path filePath = Paths.get(Constants.TESTDATA_DIR, testdataFileName).toAbsolutePath().normalize();
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("Testdata file not found: " + filePath);
        }
        waitForVisibility(selfieFileInput);
        selfieFileInput.sendKeys(filePath.toString());
        sleepQuietly(400);
        logger.info("Selected file on #selfie: {}", filePath.getFileName());
    }

    public void fillRegistrationForm(String nik, String fullname, String email, String plainPassword, String selfieFileName) {
        enterNik(nik);
        enterFullname(fullname);
        enterEmail(email);
        enterPassword(plainPassword);
        if (selfieFileName != null && !selfieFileName.isBlank()) {
            uploadSelfieFile(selfieFileName);
        }
    }

    public void clickDaftar() {
        click(daftarButton);
    }

    public void submitRegistration(String nik, String fullname, String email, String plainPassword, String selfieFileName) {
        logger.info("Submitting registration form -> nik=[{}], email=[{}], file=[{}]", nik, email, selfieFileName);
        fillRegistrationForm(nik, fullname, email, plainPassword, selfieFileName);
        clickDaftar();
    }

    public boolean isAlertMessageDisplayed() {
        try {
            WebElement el = waitForVisibility(alertMessage);
            wait.until(d -> !el.getText().trim().isEmpty());
            return el.isDisplayed() && !el.getText().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public String getAlertText() {
        try {
            WebElement el = waitForVisibility(alertMessage);
            wait.until(d -> !el.getText().trim().isEmpty());
            return el.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public String getInlineFileErrorText() {
        try {
            List<WebElement> els = driver.findElements(
                    By.xpath("//input[@id='selfie']/ancestor::div[contains(@class,'MuiFormControl-root')]/following-sibling::p[1]")
            );
            return els.isEmpty() ? "" : els.get(0).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public String getNikValidationMessage() {
        return getAttribute(nikInput, "validationMessage");
    }

    public String getEmailValidationMessage() {
        return getAttribute(emailInput, "validationMessage");
    }

    public String getPasswordValidationMessage() {
        return getAttribute(passwordInput, "validationMessage");
    }

    public String getSelfieValidationMessage() {
        return getAttribute(selfieFileInput, "validationMessage");
    }

    /**
     * Memeriksa apakah preview foto selfie dirender ke elemen &lt;img&gt; di form
     * dan tidak mengalami broken image (naturalWidth &gt; 0).
     *
     * @return true jika elemen preview tampil utuh
     */
    public boolean isSelfiePreviewDisplayedAndLoaded() {
        List<WebElement> previewImgs = driver.findElements(SELFIE_PREVIEW_IMAGES);
        for (WebElement img : previewImgs) {
            String alt = img.getAttribute("alt");
            if ("bg".equalsIgnoreCase(alt)) {
                continue;
            }
            if (img.isDisplayed() && !isImageBroken(img)) {
                return true;
            }
        }
        return false;
    }

    public void clickBackToLoginLink() {
        click(alreadyHaveAccountLoginButton);
        logger.info("Clicked 'disini' button to return to Login page.");
    }
}

