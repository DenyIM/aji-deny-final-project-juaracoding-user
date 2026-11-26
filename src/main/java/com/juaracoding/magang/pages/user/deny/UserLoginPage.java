package com.juaracoding.magang.pages.user.deny;

import com.juaracoding.magang.pages.BasePage;
import com.juaracoding.magang.utils.Constants;
import com.juaracoding.magang.utils.SecurityUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page Object Model untuk halaman Login Karyawan Mobile Mirroring
 * (https://magang.dikahadir.com/absen/login).
 * Cakupan skenario: SIT_USR_UI_001 s/d SIT_USR_UI_012 (Tester Deny).
 */
public class UserLoginPage extends BasePage {

    @FindBy(css = "input#email, input[name='email'], input[type='email']")
    private WebElement emailInput;

    @FindBy(css = "input#password")
    private WebElement passwordInput;

    @FindBy(css = "div.MuiInputAdornment-root button")
    private WebElement showHidePasswordButton;

    @FindBy(css = "button[type='submit']")
    private WebElement loginButton;

    @FindBy(css = "div.MuiAlert-message, div[role='alert'].MuiAlert-root")
    private WebElement alertMessage;

    @FindBy(xpath = "//button[.//img[@alt='menu']]")
    private WebElement burgerMenuButton;

    @FindBy(xpath = "//button[normalize-space()='Logout']")
    private WebElement logoutButton;

    @FindBy(xpath = "//p[contains(normalize-space(),'Belum punya akun')]/following-sibling::button[normalize-space()='disini']")
    private WebElement registerHereButton;

    @FindBy(xpath = "//button[normalize-space()='Lupa password ?']")
    private WebElement forgotPasswordButton;

    public UserLoginPage(WebDriver driver) {
        super(driver);
    }

    public void navigateToLoginPage() {
        safeGetUrl(Constants.USER_LOGIN_URL);
        waitForVisibility(emailInput);
        logger.info("Navigated to User Login page: {}", Constants.USER_LOGIN_URL);
    }

    public void navigateToAbsentDirectly() {
        safeGetUrl(Constants.USER_HOME_URL);
        logger.info("Directly navigated to protected User URL: {}", Constants.USER_HOME_URL);
    }

    private void safeGetUrl(String url) {
        try {
            driver.get(url);
        } catch (TimeoutException e) {
            logger.warn("Page load timeout while fetching [{}], stopping slow sub-resources and verifying DOM.", url);
            try {
                ((JavascriptExecutor) driver).executeScript("window.stop();");
            } catch (Exception ignored) {
                // ignore if renderer unavailable
            }
            if (driver.findElements(By.cssSelector("input#email")).isEmpty() && !driver.getCurrentUrl().contains("/absen/login")) {
                driver.get(url);
            }
        }
    }

    public void enterEmail(String email) {
        type(emailInput, email);
    }

    public void enterPassword(String plainPassword) {
        type(passwordInput, plainPassword);
    }

    public void clickLoginButton() {
        click(loginButton);
    }

    public void clickRegisterLink() {
        click(registerHereButton);
        logger.info("Clicked 'disini' button to navigate to Register page.");
    }

    public void clickForgotPasswordLink() {
        click(forgotPasswordButton);
        logger.info("Clicked 'Lupa password ?' button to navigate to Reset Password Request page.");
    }

    public void login(String email, String plainPassword) {
        logger.info("Attempting User Login for email: [{}]", email);
        enterEmail(email);
        enterPassword(plainPassword);
        clickLoginButton();
    }

    public void loginWithDefaultUser() {
        String decodedPassword = SecurityUtil.decode(Constants.USER_PASSWORD_ENCODED);
        login(Constants.USER_EMAIL, decodedPassword);
    }

    public void clickShowHidePassword() {
        click(showHidePasswordButton);
    }

    public String getPasswordFieldType() {
        return getAttribute(passwordInput, "type");
    }

    public String getEmailValidationMessage() {
        return getAttribute(emailInput, "validationMessage");
    }

    public String getPasswordValidationMessage() {
        return getAttribute(passwordInput, "validationMessage");
    }

    public boolean isEmailFieldDisplayed() {
        return isDisplayed(emailInput);
    }

    public boolean isAlertMessageDisplayed() {
        try {
            WebElement el = waitForVisibility(alertMessage);
            return el.isDisplayed() && !el.getText().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public String getAlertText() {
        try {
            wait.until(d -> !alertMessage.getText().trim().isEmpty());
            return alertMessage.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean waitForRedirectToAbsent() {
        return waitForUrlContains("/apps/absent");
    }

    public boolean waitForRedirectToLogin() {
        return waitForUrlContains("/absen/login");
    }

    public boolean isBrowserJsAlertPresent() {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }

    public void performLogout() {
        logger.info("Performing User Logout from /apps/absent burger menu.");
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        WebElement menuBtn = waitForClickability(burgerMenuButton);
        clickWithJS(menuBtn);

        WebElement logoutEl = waitForClickability(logoutButton);
        clickWithJS(logoutEl);
    }

    public void navigateBack() {
        driver.navigate().back();
        logger.info("Navigated back using browser Back button.");
    }
}

