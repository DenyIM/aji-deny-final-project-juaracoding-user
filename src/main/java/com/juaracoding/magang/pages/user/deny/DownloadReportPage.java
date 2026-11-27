package com.juaracoding.magang.pages.user.deny;

import com.juaracoding.magang.pages.BasePage;
import com.juaracoding.magang.utils.Constants;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chromium.HasCdp;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Page Object Model untuk halaman Download Report Karyawan
 * (https://magang.dikahadir.com/apps/absent/reports/share_report).
 * Cakupan skenario: SIT_USR_UI_136 s/d SIT_USR_UI_141 (Tester Deny).
 */
public class DownloadReportPage extends BasePage {

    private static final By DOWNLOAD_REPORTS_MENU_TILE =
            By.xpath("//a[contains(@class,'user__menu__item')][.//p[normalize-space()='Download Reports']]");
    private static final By BACK_TO_HOME_LINK =
            By.xpath("//a[.//p[normalize-space()='Kembali']]/button | //a[.//p[normalize-space()='Kembali']]");
    private static final By HEADER_TITLE =
            By.xpath("//*[contains(normalize-space(),'Halaman Download Report')]");
    private static final By REPORT_TYPE_LABEL =
            By.id("type_report");
    private static final By REPORT_TYPE_SELECT_TRIGGER =
            By.xpath("//label[@id='type_report']/following::div[contains(@class,'MuiSelect-select')][1]");
    private static final By SUBMIT_DOWNLOAD_BTN =
            By.xpath("//button[@type='submit' and (contains(normalize-space(),'Download Report') or contains(normalize-space(),'Loading'))]");
    private static final By YUP_HELPER_ERROR =
            By.cssSelector("p.MuiFormHelperText-root.Mui-error");

    public DownloadReportPage(WebDriver driver) {
        super(driver);
    }

    public void navigateFromAbsentHome() {
        waitForUrlContains("/apps/absent");
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement tile = waitForVisibility(DOWNLOAD_REPORTS_MENU_TILE);
                scrollToElement(tile);
                clickWithJS(tile);
                waitForUrlContains("/apps/absent/reports/share_report");
                waitForDownloadReportPageReady();
                return;
            } catch (Exception e) {
                logger.warn("Download Reports tile click attempt {} encountered {}, retrying...",
                        attempt, e.getClass().getSimpleName());
                sleepQuietly(500);
            }
        }
        WebElement tile = waitForVisibility(DOWNLOAD_REPORTS_MENU_TILE);
        clickWithJS(tile);
        waitForUrlContains("/apps/absent/reports/share_report");
        waitForDownloadReportPageReady();
    }

    public void navigateDirectToDownloadReport() {
        safeNavigate(Constants.USER_DOWNLOAD_REPORT_URL);
        waitForUrlContains("/apps/absent/reports/share_report");
        waitForDownloadReportPageReady();
    }

    public void waitForDownloadReportPageReady() {
        waitForVisibility(HEADER_TITLE);
        waitForVisibility(REPORT_TYPE_SELECT_TRIGGER);
        waitForVisibility(SUBMIT_DOWNLOAD_BTN);
        sleepQuietly(300);
    }

    public boolean isHeaderDisplayed() {
        return isDisplayed(HEADER_TITLE);
    }

    public boolean isReportTypeDropdownDisplayed() {
        return isDisplayed(REPORT_TYPE_LABEL) && isDisplayed(REPORT_TYPE_SELECT_TRIGGER);
    }

    public boolean isDownloadButtonDisplayed() {
        return isDisplayed(SUBMIT_DOWNLOAD_BTN);
    }

    public String getSelectedReportTypeText() {
        return getText(REPORT_TYPE_SELECT_TRIGGER);
    }

    public void selectReportType(String label) {
        WebElement trigger = waitForVisibility(REPORT_TYPE_SELECT_TRIGGER);
        openMuiSelectWithMouseDown(trigger);
        By optionLoc = By.xpath("//ul[@role='listbox']//li[@role='option' and normalize-space()='" + label + "']");
        WebElement option = waitForVisibility(optionLoc);
        clickWithJS(option);
        waitForInvisibility(By.cssSelector("ul[role='listbox']"));
        sleepQuietly(200);
        logger.info("Selected Download Report type: [{}]", label);
    }

    public void clickDownloadReportButton() {
        WebElement btn = waitForVisibility(SUBMIT_DOWNLOAD_BTN);
        clickWithJS(btn);
        sleepQuietly(200);
        logger.info("Clicked 'Download Report' submit button.");
    }

    public void waitUntilLoadingFinishes() {
        try {
            wait.until(d -> {
                List<WebElement> btns = d.findElements(SUBMIT_DOWNLOAD_BTN);
                return btns.isEmpty() || !btns.get(0).getText().contains("Loading...");
            });
        } catch (Exception ignored) {
            // Best effort wait for button loading state to clear
        }
    }

    public String getSnackbarMessage() {
        return waitForSnackbarText(10);
    }

    public void dismissSnackbarIfPresent() {
        List<WebElement> closeBtns = driver.findElements(By.cssSelector("div.MuiSnackbar-root button[aria-label='close']"));
        for (WebElement btn : closeBtns) {
            if (btn.isDisplayed()) {
                clickWithJS(btn);
                sleepQuietly(250);
                return;
            }
        }
        waitForInvisibility(By.cssSelector("div.MuiSnackbar-root"));
    }

    public String getValidationErrorText() {
        sleepQuietly(300);
        List<WebElement> errors = driver.findElements(YUP_HELPER_ERROR);
        for (WebElement err : errors) {
            if (err.isDisplayed() && !err.getText().isBlank()) {
                return err.getText().trim();
            }
        }
        return "";
    }

    public void enableCdpDownloadDirectory(Path downloadDir) throws IOException {
        Files.createDirectories(downloadDir);
        if (driver instanceof HasCdp cdpDriver) {
            Map<String, Object> params = new HashMap<>();
            params.put("behavior", "allow");
            params.put("downloadPath", downloadDir.toAbsolutePath().normalize().toString());
            cdpDriver.executeCdpCommand("Browser.setDownloadBehavior", params);
            logger.info("CDP Browser.setDownloadBehavior enabled -> {}", downloadDir.toAbsolutePath());
        }
    }

    public Path waitForDownloadedXlsx(Path downloadDir, String nameKeyword, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        while (System.currentTimeMillis() < deadline) {
            try (Stream<Path> files = Files.list(downloadDir)) {
                Path match = files
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String fname = p.getFileName().toString();
                            return fname.endsWith(".xlsx") && !fname.endsWith(".crdownload")
                                    && (nameKeyword == null || nameKeyword.isBlank() || fname.contains(nameKeyword));
                        })
                        .findFirst()
                        .orElse(null);
                if (match != null && Files.size(match) > 0) {
                    logger.info("Verified downloaded XLSX file: {} ({} bytes)", match.getFileName(), Files.size(match));
                    return match;
                }
            } catch (IOException ignored) {
                // Retry until download completes
            }
            sleepQuietly(300);
        }
        return null;
    }

    public void blockShareReportEndpointViaCdp(boolean block) {
        if (driver instanceof HasCdp cdpDriver) {
            cdpDriver.executeCdpCommand("Network.enable", Map.of());
            Map<String, Object> params = new HashMap<>();
            params.put("urls", block ? List.of("*share-report*") : List.of());
            cdpDriver.executeCdpCommand("Network.setBlockedURLs", params);
            logger.info("CDP Network.setBlockedURLs (*share-report*) -> blocked={}", block);
        }
    }

    /**
     * Klik Download Report saat endpoint gagal, tangkap/terima alert JS terlebih dahulu
     * (karena blok catch memanggil alert(err) sebelum openSnackBar(...)), lalu kembalikan teks Snackbar.
     */
    public String submitExpectingServerErrorFeedback() {
        ((JavascriptExecutor) driver).executeScript(
                "window.__sqaDownloadAlert = '';"
                        + "if (!window.__sqaOrigAlertDownload) { window.__sqaOrigAlertDownload = window.alert; }"
                        + "window.alert = function(msg) { window.__sqaDownloadAlert = String(msg || ''); };"
        );
        clickDownloadReportButton();
        try {
            WebDriverWait alertWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            Alert alert = alertWait.until(ExpectedConditions.alertIsPresent());
            alert.accept();
        } catch (Exception ignored) {
            // Captured cleanly by non-blocking spy so openSnackBar executes immediately
        }
        waitUntilLoadingFinishes();
        return getSnackbarMessage();
    }

    public void clickBackToHome() {
        WebElement back = waitForVisibility(BACK_TO_HOME_LINK);
        clickWithJS(back);
        logger.info("Clicked '< Kembali' link on Halaman Download Report.");
    }

    public boolean waitForReturnToHome() {
        try {
            return wait.until(d -> {
                String url = d.getCurrentUrl();
                return url.contains("/apps/absent") && !url.contains("/reports/share_report");
            });
        } catch (Exception e) {
            String url = getCurrentUrl();
            return url.contains("/apps/absent") && !url.contains("/reports/share_report");
        }
    }
}

