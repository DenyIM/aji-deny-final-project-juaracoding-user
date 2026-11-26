package com.juaracoding.magang.pages.user.deny;

import com.juaracoding.magang.pages.BasePage;
import com.juaracoding.magang.utils.Constants;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page Object Model untuk halaman Reset Password Karyawan:
 * 1. Request Reset (/absen/reset-password-request)
 * 2. Form Reset Password + OTP (/absen/reset-password)
 * Cakupan skenario: SIT_USR_UI_026 s/d SIT_USR_UI_035 (Tester Deny).
 */
public class ResetPasswordPage extends BasePage {

    @FindBy(css = "input#email")
    private WebElement emailInput;

    @FindBy(css = "input#password")
    private WebElement newPasswordInput;

    @FindBy(css = "input#passwordConfirm")
    private WebElement confirmPasswordInput;

    @FindBy(css = "input#otp")
    private WebElement otpInput;

    @FindBy(xpath = "//input[@id='password']/following-sibling::div[contains(@class,'MuiInputAdornment-root')]//button")
    private WebElement newPasswordEyeButton;

    @FindBy(xpath = "//input[@id='passwordConfirm']/following-sibling::div[contains(@class,'MuiInputAdornment-root')]//button")
    private WebElement confirmPasswordEyeButton;

    @FindBy(css = "button[type='submit']")
    private WebElement submitButton;

    @FindBy(css = "div.MuiAlert-message")
    private WebElement alertMessage;

    @FindBy(xpath = "//p[contains(normalize-space(),'Kembali ke halaman')]/following-sibling::button[normalize-space()='Login']")
    private WebElement backToLoginButton;

    public ResetPasswordPage(WebDriver driver) {
        super(driver);
    }

    public void navigateToRequestPage() {
        safeNavigate(Constants.USER_RESET_PASSWORD_REQUEST_URL);
        waitForVisibility(emailInput);
        logger.info("Navigated to Reset Password Request page: {}", Constants.USER_RESET_PASSWORD_REQUEST_URL);
    }

    public void navigateToResetPage() {
        safeNavigate(Constants.USER_RESET_PASSWORD_URL);
        waitForVisibility(newPasswordInput);
        logger.info("Navigated to Reset Password page: {}", Constants.USER_RESET_PASSWORD_URL);
    }

    public void navigateToResetPageWithEmail(String email) {
        String url = Constants.USER_RESET_PASSWORD_URL + "?email=" + email;
        safeNavigate(url);
        waitForVisibility(newPasswordInput);
        logger.info("Navigated to Reset Password page with email query: {}", url);
    }

    public boolean waitForRedirectToRequestPage() {
        boolean matched = waitForUrlContains("/absen/reset-password-request");
        if (matched) {
            waitForVisibility(emailInput);
        }
        return matched;
    }

    public boolean waitForRedirectToResetPage() {
        boolean matched = waitForUrlContains("/absen/reset-password")
                && !getCurrentUrl().contains("/absen/reset-password-request");
        if (!matched) {
            try {
                matched = wait.until(d -> d.getCurrentUrl().contains("/absen/reset-password")
                        && !d.getCurrentUrl().contains("/absen/reset-password-request"));
            } catch (Exception ignored) {
                matched = false;
            }
        }
        return matched;
    }

    public boolean waitForRedirectToLogin() {
        return waitForUrlContains("/absen/login");
    }

    public boolean isEmailFieldDisplayed() {
        return isDisplayed(emailInput);
    }

    public boolean isSubmitButtonDisplayed() {
        return isDisplayed(submitButton);
    }

    public boolean areAllResetFormFieldsDisplayed() {
        return isDisplayed(newPasswordInput)
                && isDisplayed(confirmPasswordInput)
                && isDisplayed(otpInput)
                && isDisplayed(submitButton);
    }

    public void enterEmail(String email) {
        type(emailInput, email);
    }

    public String getEmailValidationMessage() {
        return getAttribute(emailInput, "validationMessage");
    }

    public void enterNewPassword(String plainPassword) {
        type(newPasswordInput, plainPassword);
    }

    public void enterConfirmPassword(String plainConfirmPassword) {
        type(confirmPasswordInput, plainConfirmPassword);
    }

    public void enterOtp(String otp) {
        type(otpInput, otp);
    }

    public void clickSubmit() {
        click(submitButton);
    }

    public void submitEmailRequest(String email) {
        logger.info("Submitting reset password request for email=[{}]", email);
        enterEmail(email);
        clickSubmit();
    }

    public void submitNewPassword(String plainNewPassword, String plainConfirmPassword, String otp) {
        logger.info("Submitting reset password form with OTP len={}", otp == null ? 0 : otp.length());
        enterNewPassword(plainNewPassword);
        enterConfirmPassword(plainConfirmPassword);
        enterOtp(otp);
        clickSubmit();
    }

    public void clickShowHideNewPassword() {
        click(newPasswordEyeButton);
    }

    public void clickShowHideConfirmPassword() {
        click(confirmPasswordEyeButton);
    }

    public String getNewPasswordFieldType() {
        return getAttribute(newPasswordInput, "type");
    }

    public String getConfirmPasswordFieldType() {
        return getAttribute(confirmPasswordInput, "type");
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

    public void clickBackToLoginLink() {
        click(backToLoginButton);
        logger.info("Clicked 'Login' link to return to /absen/login.");
    }
}

