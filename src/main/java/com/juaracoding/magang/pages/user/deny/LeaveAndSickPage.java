package com.juaracoding.magang.pages.user.deny;

import com.juaracoding.magang.pages.BasePage;
import com.juaracoding.magang.utils.Constants;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Page Object Model untuk fitur Pengajuan-Cuti (/apps/absent/leave-client)
 * dan Pengajuan-Sakit (/apps/absent/sick) pada Mobile Mirroring Karyawan.
 * Cakupan skenario: SIT_USR_UI_103 s/d SIT_USR_UI_126 (Tester Deny).
 */
public class LeaveAndSickPage extends BasePage {

    private static final By MUI_DRAWER_PAPER = By.cssSelector("div.MuiDrawer-paper");
    private static final By AJUKAN_CUTI_BTN = By.xpath("//button[contains(normalize-space(),'Ajukan Cuti')]");
    private static final By AJUKAN_SAKIT_BTN = By.xpath("//button[contains(normalize-space(),'Ajukan Sakit')]");
    private static final By TOTAL_TEXT = By.xpath("//p[starts-with(normalize-space(),'Total')]");
    private static final By HISTORY_CARDS = By.cssSelector("div.infinite-scroll-component div.MuiCard-root");
    private static final By CALENDAR_WRAPPER = By.cssSelector("div[role='dialog'] div.rdrCalendarWrapper");
    private static final By SICK_FILE_INPUT = By.cssSelector("div.MuiDrawer-paper input[type='file']");
    private static final By SICK_FORM_PREVIEW_IMG = By.cssSelector("div.MuiDrawer-paper img[alt='Gambar surat sakit']");
    private static final By SICK_CARD_DOC_IMG = By.cssSelector("div.infinite-scroll-component img[alt='Bukti surat sakit']");

    private static final By CUTI_MENU_TILE = By.xpath("//a[contains(@class,'user__menu__item')][.//p[normalize-space()='Cuti']]");
    private static final By SAKIT_MENU_TILE = By.xpath("//a[contains(@class,'user__menu__item')][.//p[normalize-space()='Sakit']]");
    private static final By BACK_TO_HOME_LINK = By.xpath("//a[.//p[normalize-space()='Kembali']]/button | //a[.//p[normalize-space()='Kembali']]");

    public LeaveAndSickPage(WebDriver driver) {
        super(driver);
    }

    // =========================================================================
    // NAVIGASI & COMMON HEADER
    // =========================================================================

    private void clickHomeMenuTileWithRetry(By tileLocator, String expectedPath) {
        waitForUrlContains("/apps/absent");
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement tile = waitForVisibility(tileLocator);
                scrollToElement(tile);
                clickWithJS(tile);
                waitForUrlContains(expectedPath);
                return;
            } catch (Exception e) {
                logger.warn("Menu tile click attempt {} for [{}] encountered {}, retrying...",
                        attempt, expectedPath, e.getClass().getSimpleName());
                sleepQuietly(500);
            }
        }
        WebElement tile = waitForVisibility(tileLocator);
        clickWithJS(tile);
        waitForUrlContains(expectedPath);
    }

    public void navigateToLeaveFromHome() {
        clickHomeMenuTileWithRetry(CUTI_MENU_TILE, "/apps/absent/leave-client");
        waitForLeavePageReady();
    }

    public void navigateToSickFromHome() {
        clickHomeMenuTileWithRetry(SAKIT_MENU_TILE, "/apps/absent/sick");
        waitForSickPageReady();
    }

    public void navigateDirectToLeave() {
        safeNavigate(Constants.USER_LEAVE_CLIENT_URL);
        waitForUrlContains("/apps/absent/leave-client");
        waitForLeavePageReady();
    }

    public void navigateDirectToSick() {
        safeNavigate(Constants.USER_SICK_URL);
        waitForUrlContains("/apps/absent/sick");
        waitForSickPageReady();
    }

    public void waitForLeavePageReady() {
        waitForVisibility(AJUKAN_CUTI_BTN);
        waitForVisibility(By.xpath("//*[contains(normalize-space(),'List Request Cuti')]"));
        sleepQuietly(350);
    }

    public void waitForSickPageReady() {
        waitForVisibility(AJUKAN_SAKIT_BTN);
        waitForVisibility(By.xpath("//*[contains(normalize-space(),'List Request Sakit')]"));
        sleepQuietly(350);
    }

    public boolean isLeaveHeaderDisplayed() {
        return isDisplayed(By.xpath("//*[contains(normalize-space(),'Halaman Cuti')]"))
                && isDisplayed(By.xpath("//*[contains(normalize-space(),'List Request Cuti')]"));
    }

    public boolean isSickHeaderDisplayed() {
        return isDisplayed(By.xpath("//*[contains(normalize-space(),'Halaman Sakit')]"))
                && isDisplayed(By.xpath("//*[contains(normalize-space(),'List Request Sakit')]"));
    }

    public String getTotalText() {
        return getText(TOTAL_TEXT);
    }

    public int getTotalCount() {
        String text = getTotalText();
        String digits = text.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    public void clickBackToHome() {
        WebElement back = waitForVisibility(BACK_TO_HOME_LINK);
        clickWithJS(back);
        logger.info("Clicked '< Kembali' link on Leave/Sick header.");
    }

    public boolean waitForReturnToHome() {
        try {
            return wait.until(d -> {
                String url = d.getCurrentUrl();
                return url.contains("/apps/absent") && !url.contains("/leave") && !url.contains("/sick");
            });
        } catch (Exception e) {
            String url = getCurrentUrl();
            return url.contains("/apps/absent") && !url.contains("/leave") && !url.contains("/sick");
        }
    }

    public int getHistoryCardCount() {
        List<WebElement> cards = driver.findElements(HISTORY_CARDS);
        if (cards.isEmpty()) {
            cards = driver.findElements(By.cssSelector("div.MuiCard-root"));
        }
        return cards.size();
    }

    public List<String> getAllCardTexts() {
        List<WebElement> cards = driver.findElements(HISTORY_CARDS);
        if (cards.isEmpty()) {
            cards = driver.findElements(By.cssSelector("div.MuiCard-root"));
        }
        List<String> texts = new ArrayList<>();
        for (WebElement card : cards) {
            String t = card.getText();
            if (t != null && !t.isBlank()) {
                texts.add(t);
            }
        }
        return texts;
    }

    public String findCardTextContaining(String keyword) {
        for (String cardText : getAllCardTexts()) {
            if (cardText.contains(keyword)) {
                return cardText;
            }
        }
        return "";
    }

    public void scrollUntilCardCountIncreases(int beforeCount) {
        Dimension originalSize = driver.manage().window().getSize();
        try {
            driver.manage().window().setSize(new Dimension(430, 760));
            sleepQuietly(300);
            for (int attempt = 0; attempt < 5; attempt++) {
                ((JavascriptExecutor) driver).executeScript(
                        "window.scrollTo(0, 0);"
                );
                sleepQuietly(150);
                ((JavascriptExecutor) driver).executeScript(
                        "const maxH = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);"
                                + "window.scrollTo(0, Math.floor(maxH * 0.6));"
                );
                sleepQuietly(250);
                scrollToBottomForInfiniteScroll();
                ((JavascriptExecutor) driver).executeScript("window.dispatchEvent(new Event('scroll'));");
                sleepQuietly(1000);
                if (getHistoryCardCount() > beforeCount) {
                    return;
                }
            }
        } finally {
            try {
                driver.manage().window().setSize(originalSize);
            } catch (Exception ignored) {
                // Best effort restore window size
            }
        }
    }

    public String getSnackbarMessage() {
        return waitForSnackbarText(10);
    }

    /**
     * Memasang MutationObserver read-only sebelum submit form, klik tombol Ajukan,
     * dan menangkap pesan Snackbar MUI sesaat pun sebelum router.reload() merender ulang DOM.
     */
    public String submitAndCaptureSnackbar() {
        ((JavascriptExecutor) driver).executeScript(
                "sessionStorage.removeItem('__sqaCapturedSnackbar');"
                        + "window.__sqaCapturedSnackbar = '';"
                        + "const capture = () => {"
                        + "  const els = document.querySelectorAll('.MuiSnackbar-root .MuiSnackbarContent-message, .MuiSnackbarContent-message, div.MuiAlert-message');"
                        + "  for (const el of els) {"
                        + "    if (el.closest('#__next-route-announcer__')) continue;"
                        + "    const t = (el.innerText || el.textContent || '').trim();"
                        + "    if (t && t !== 'HADIR') { window.__sqaCapturedSnackbar = t; sessionStorage.setItem('__sqaCapturedSnackbar', t); }"
                        + "  }"
                        + "};"
                        + "if (window.__sqaObs) { try { window.__sqaObs.disconnect(); } catch (e) {} }"
                        + "window.__sqaObs = new MutationObserver(capture);"
                        + "window.__sqaObs.observe(document.body, { childList: true, subtree: true, characterData: true });"
                        + "capture();"
        );
        clickDrawerSubmitButton();
        long deadline = System.currentTimeMillis() + 10_000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                Object captured = ((JavascriptExecutor) driver).executeScript(
                        "return sessionStorage.getItem('__sqaCapturedSnackbar') || window.__sqaCapturedSnackbar || '';"
                );
                if (captured != null && !captured.toString().isBlank()) {
                    return captured.toString().trim();
                }
                List<WebElement> els = driver.findElements(By.cssSelector("div.MuiSnackbar-root div.MuiSnackbarContent-message, div.MuiAlert-message"));
                for (WebElement el : els) {
                    if (el.isDisplayed() && !el.getText().isBlank()) {
                        return el.getText().trim();
                    }
                }
                // Jika router.reload() sudah menyelesaikan reload dan menutup drawer, hentikan tunggu snackbar
                if (!isDrawerOpen()) {
                    sleepQuietly(350);
                    Object afterReloadCap = ((JavascriptExecutor) driver).executeScript(
                            "return sessionStorage.getItem('__sqaCapturedSnackbar') || '';"
                    );
                    return afterReloadCap != null ? afterReloadCap.toString().trim() : "";
                }
            } catch (Exception ignored) {
                // DOM/context sedang transition selama window.location.reload()
            }
            sleepQuietly(100);
        }
        return "";
    }

    public boolean isDrawerOpen() {
        List<WebElement> drawers = driver.findElements(MUI_DRAWER_PAPER);
        for (WebElement drawer : drawers) {
            if (drawer.isDisplayed()) {
                return true;
            }
        }
        return false;
    }

    public void closeDrawerWithoutSubmit() {
        List<WebElement> backdrops = driver.findElements(By.cssSelector("div.MuiBackdrop-root"));
        boolean closed = false;
        for (WebElement backdrop : backdrops) {
            if (backdrop.isDisplayed()) {
                clickWithJS(backdrop);
                closed = waitForInvisibility(MUI_DRAWER_PAPER);
                if (closed) {
                    break;
                }
            }
        }
        if (!closed && isDrawerOpen()) {
            driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
            waitForInvisibility(MUI_DRAWER_PAPER);
        }
        sleepQuietly(300);
    }

    public void clickDrawerResetButton() {
        WebElement resetBtn = waitForVisibility(
                By.xpath("//div[contains(@class,'MuiDrawer-paper')]//button[normalize-space()='Reset']")
        );
        clickWithJS(resetBtn);
        sleepQuietly(300);
    }

    public void clickDrawerSubmitButton() {
        WebElement submitBtn = waitForVisibility(
                By.xpath("//div[contains(@class,'MuiDrawer-paper')]//button[@type='submit']")
        );
        clickWithJS(submitBtn);
        sleepQuietly(400);
    }

    // =========================================================================
    // PENGAJUAN-CUTI (/apps/absent/leave-client)
    // =========================================================================

    public void openLeaveDrawer() {
        openSwipeableDrawerWithRetry(AJUKAN_CUTI_BTN);
        waitForVisibility(MUI_DRAWER_PAPER);
        sleepQuietly(350);
    }

    public void clickFormCutiTab() {
        WebElement tab = waitForVisibility(
                By.xpath("//div[contains(@class,'MuiDrawer-paper')]//button[@role='tab' and normalize-space()='Form Cuti']")
        );
        clickWithJS(tab);
        sleepQuietly(250);
    }

    public void clickInfoCutiTab() {
        WebElement tab = waitForVisibility(
                By.xpath("//div[contains(@class,'MuiDrawer-paper')]//button[@role='tab' and normalize-space()='Info Cuti']")
        );
        clickWithJS(tab);
        sleepQuietly(350);
    }

    public boolean isFormCutiTabSelected() {
        WebElement tab = waitForVisibility(
                By.xpath("//div[contains(@class,'MuiDrawer-paper')]//button[@role='tab' and normalize-space()='Form Cuti']")
        );
        return "true".equalsIgnoreCase(tab.getAttribute("aria-selected"));
    }

    /**
     * Membaca seluruh pasangan label dan angka pada tab 'Info Cuti' di dalam Drawer
     * tanpa bergantung pada tag pembungkus (div/p/span).
     */
    public Map<String, Integer> getInfoCutiMetricsMap() {
        WebElement infoPanel = waitForVisibility(
                By.xpath("//div[contains(@class,'MuiDrawer-paper')]//div[@role='tabpanel' and not(@hidden)]")
        );
        wait.until(d -> infoPanel.getText().contains("Total Cuti"));
        String[] rawLines = infoPanel.getText().split("\\R+");
        List<String> lines = new ArrayList<>();
        for (String line : rawLines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        Map<String, Integer> metrics = new LinkedHashMap<>();
        List<String> expectedLabels = List.of(
                "Total Cuti",
                "Total cuti tersedia",
                "Total cuti bersama",
                "Total cuti disetujui",
                "Total cuti yang sedang diajukan",
                "Total sisa cuti (Annual)"
        );
        for (int i = 0; i < lines.size(); i++) {
            String current = lines.get(i);
            for (String label : expectedLabels) {
                if (current.equals(label) && i + 1 < lines.size()) {
                    String digits = lines.get(i + 1).replaceAll("[^0-9-]", "");
                    if (!digits.isEmpty()) {
                        metrics.put(label, Integer.parseInt(digits));
                    }
                } else if (current.startsWith(label) && current.contains(":")) {
                    String afterColon = current.substring(current.indexOf(':') + 1).replaceAll("[^0-9-]", "");
                    if (!afterColon.isEmpty()) {
                        metrics.put(label, Integer.parseInt(afterColon));
                    }
                }
            }
        }
        logger.info("Info Cuti metrics parsed from UI: {} (raw lines={})", metrics, lines);
        return metrics;
    }

    public void selectPreferredLeaveType() {
        selectLeaveTypeContaining("khitanan");
    }

    public void selectLeaveTypeContaining(String preferredKeyword) {
        WebElement trigger = waitForVisibility(By.cssSelector("div.MuiDrawer-paper #leave_type_id"));
        openMuiSelectWithMouseDown(trigger);
        By listboxOptions = By.cssSelector("ul[role='listbox'] li[role='option']");
        waitForVisibility(listboxOptions);
        List<WebElement> options = driver.findElements(listboxOptions);
        WebElement chosen = null;
        String kw = preferredKeyword != null ? preferredKeyword.toLowerCase() : "";
        for (WebElement opt : options) {
            String ariaDisabled = opt.getAttribute("aria-disabled");
            String text = opt.getText() != null ? opt.getText().trim() : "";
            if (!"true".equalsIgnoreCase(ariaDisabled) && !text.isEmpty()) {
                if (!kw.isEmpty() && text.toLowerCase().contains(kw)) {
                    chosen = opt;
                    break;
                }
                if (chosen == null && !text.toLowerCase().contains("cuti tahunan")) {
                    chosen = opt;
                }
            }
        }
        if (chosen == null && !options.isEmpty()) {
            chosen = options.get(0);
        }
        if (chosen == null) {
            throw new IllegalStateException("No selectable leave type option found inside #leave_type_id dropdown.");
        }
        logger.info("Selecting leave type option: [{}]", chosen.getText().trim());
        clickWithJS(chosen);
        waitForInvisibility(By.cssSelector("ul[role='listbox']"));
        sleepQuietly(200);
    }

    public String getSelectedLeaveTypeText() {
        WebElement trigger = waitForVisibility(By.cssSelector("div.MuiDrawer-paper #leave_type_id"));
        String txt = trigger.getText();
        return txt != null ? txt.trim() : "";
    }

    public void openDrawerCalendarModal() {
        WebElement box = waitForVisibility(By.xpath(
                "//div[contains(@class,'MuiDrawer-paper')]//*[normalize-space()='Pilih Tanggal']/following-sibling::div[1]"
        ));
        clickWithJS(box);
        waitForVisibility(CALENDAR_WRAPPER);
        sleepQuietly(300);
    }

    public String getDrawerDateBoxText() {
        WebElement box = waitForVisibility(By.xpath(
                "//div[contains(@class,'MuiDrawer-paper')]//*[normalize-space()='Pilih Tanggal']/following-sibling::div[1]//p"
        ));
        String txt = box.getText();
        return txt != null ? txt.replace('\u00a0', ' ').trim() : "";
    }

    /**
     * Memilih rentang tanggal dari {@code startDate} sampai {@code endDate} di modal react-date-range
     * (mendukung lintas bulan seperti 60 hari untuk SIT_USR_UI_109).
     */
    public void selectCalendarDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate now = LocalDate.now();
        int startMonthsAhead = (startDate.getYear() - now.getYear()) * 12 + (startDate.getMonthValue() - now.getMonthValue());
        int currentMonthOffset = 0;
        currentMonthOffset = moveCalendarMonths(currentMonthOffset, startMonthsAhead);
        clickCalendarDayOfMonth(startDate.getDayOfMonth());
        sleepQuietly(250);

        int endMonthsAhead = (endDate.getYear() - now.getYear()) * 12 + (endDate.getMonthValue() - now.getMonthValue());
        moveCalendarMonths(currentMonthOffset, endMonthsAhead);
        clickCalendarDayOfMonth(endDate.getDayOfMonth());
        sleepQuietly(250);
    }

    private int moveCalendarMonths(int currentOffset, int targetOffset) {
        while (currentOffset < targetOffset) {
            WebElement nextBtn = waitForVisibility(By.cssSelector("div[role='dialog'] button.rdrNextPrevButton.rdrNextButton"));
            clickWithJS(nextBtn);
            sleepQuietly(200);
            currentOffset++;
        }
        while (currentOffset > targetOffset) {
            WebElement prevBtn = waitForVisibility(
                    By.cssSelector("div[role='dialog'] button.rdrNextPrevButton.rdrPprevButton, div[role='dialog'] button.rdrNextPrevButton.rdrPrevButton")
            );
            clickWithJS(prevBtn);
            sleepQuietly(200);
            currentOffset--;
        }
        return currentOffset;
    }

    private void clickCalendarDayOfMonth(int dayOfMonth) {
        List<WebElement> days = driver.findElements(By.cssSelector(
                "div[role='dialog'] button.rdrDay:not(.rdrDayPassive):not(.rdrDayDisabled)"
        ));
        String expected = String.valueOf(dayOfMonth);
        for (WebElement dayBtn : days) {
            String txt = dayBtn.getText() != null ? dayBtn.getText().trim() : "";
            if (txt.equals(expected)) {
                dispatchCalendarDayMouseEvents(dayBtn);
                return;
            }
        }
        throw new IllegalStateException("Day [" + dayOfMonth + "] not found/enabled in react-date-range calendar.");
    }

    public void clickCalendarBatal() {
        WebElement btn = waitForVisibility(By.xpath("//div[@role='dialog']//button[normalize-space()='Batal']"));
        clickWithJS(btn);
        waitForInvisibility(CALENDAR_WRAPPER);
        sleepQuietly(250);
    }

    public void clickCalendarSimpan() {
        WebElement btn = waitForVisibility(By.xpath("//div[@role='dialog']//button[normalize-space()='Simpan']"));
        clickWithJS(btn);
        waitForInvisibility(CALENDAR_WRAPPER);
        sleepQuietly(250);
    }

    public void enterLeaveNotes(String notes) {
        WebElement el = waitForVisibility(By.cssSelector("div.MuiDrawer-paper textarea#notes"));
        ((JavascriptExecutor) driver).executeScript(
                "const setter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value').set;"
                        + "setter.call(arguments[0], arguments[1]);"
                        + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                        + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                el, notes == null ? "" : notes
        );
        sleepQuietly(200);
    }

    public String getLeaveNotesValue() {
        WebElement el = waitForVisibility(By.cssSelector("div.MuiDrawer-paper textarea#notes"));
        Object val = ((JavascriptExecutor) driver).executeScript("return arguments[0].value;", el);
        return val != null ? val.toString() : "";
    }

    // =========================================================================
    // PENGAJUAN-SAKIT (/apps/absent/sick)
    // =========================================================================

    public void openSickDrawer() {
        openSwipeableDrawerWithRetry(AJUKAN_SAKIT_BTN);
        waitForVisibility(MUI_DRAWER_PAPER);
        sleepQuietly(350);
    }

    public void uploadSickDocument(String fileName) {
        Path filePath = Paths.get(Constants.TESTDATA_DIR, fileName).toAbsolutePath().normalize();
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("Sick upload testdata file not found: " + filePath);
        }
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(SICK_FILE_INPUT));
        fileInput.sendKeys(filePath.toString());
        sleepQuietly(450);
        logger.info("Sent file [{}] to hidden SKD file input.", filePath.getFileName());
    }

    /**
     * Mengunggah file non-gambar sambil menangkap alert validasi browser
     * baik melalui native {@code switchTo().alert()} maupun pass-through spy
     * apabila ChromeDriver headless auto-dismiss prompt saat {@code sendKeys}.
     */
    public String uploadSickDocumentAndCaptureAlert(String fileName) {
        Path filePath = Paths.get(Constants.TESTDATA_DIR, fileName).toAbsolutePath().normalize();
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("Sick upload testdata file not found: " + filePath);
        }
        ((JavascriptExecutor) driver).executeScript(
                "window.__sqaAlertMsg = '';"
                        + "if (!window.__sqaOrigAlert) { window.__sqaOrigAlert = window.alert; }"
                        + "window.alert = function(msg) { window.__sqaAlertMsg = String(msg || ''); return window.__sqaOrigAlert.call(window, msg); };"
        );
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(SICK_FILE_INPUT));
        fileInput.sendKeys(filePath.toString());
        String nativeAlert = waitAndAcceptUploadAlertIfPresent(2);
        if (!nativeAlert.isEmpty()) {
            return nativeAlert;
        }
        try {
            Object captured = ((JavascriptExecutor) driver).executeScript("return window.__sqaAlertMsg || '';");
            return captured != null ? captured.toString().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public String waitAndAcceptUploadAlertIfPresent(int timeoutSeconds) {
        try {
            WebDriverWait alertWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            Alert alert = alertWait.until(ExpectedConditions.alertIsPresent());
            String text = alert.getText();
            alert.accept();
            logger.info("Accepted browser alert on Sick upload: [{}]", text);
            return text != null ? text.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public String getSickUploadButtonText() {
        WebElement btn = waitForVisibility(By.xpath(
                "//div[contains(@class,'MuiDrawer-paper')]//button[normalize-space()='Upload' or normalize-space()='Upload ulang']"
        ));
        return btn.getText().trim();
    }

    public boolean isSickFormPreviewDisplayed() {
        List<WebElement> imgs = driver.findElements(SICK_FORM_PREVIEW_IMG);
        return !imgs.isEmpty() && imgs.get(0).isDisplayed();
    }

    public boolean isSickFormPreviewLoaded() {
        WebElement img = waitForVisibility(SICK_FORM_PREVIEW_IMG);
        sleepQuietly(350);
        return !isImageBroken(img);
    }

    public String getSickFormPreviewSrc() {
        WebElement img = waitForVisibility(SICK_FORM_PREVIEW_IMG);
        String src = img.getAttribute("src");
        return src != null ? src : "";
    }

    public boolean isFirstSickCardImageLoaded() {
        List<WebElement> imgs = driver.findElements(SICK_CARD_DOC_IMG);
        if (imgs.isEmpty()) {
            return false;
        }
        WebElement firstImg = imgs.get(0);
        sleepQuietly(500);
        return !isImageBroken(firstImg);
    }

    public String getFirstSickCardImageSrc() {
        List<WebElement> imgs = driver.findElements(SICK_CARD_DOC_IMG);
        if (imgs.isEmpty()) {
            return "";
        }
        String src = imgs.get(0).getAttribute("src");
        return src != null ? src : "";
    }
}
