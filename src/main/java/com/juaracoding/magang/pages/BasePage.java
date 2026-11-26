package com.juaracoding.magang.pages;

import com.juaracoding.magang.utils.Constants;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chromium.HasCdp;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kelas induk abstrak untuk seluruh Page Object Model (POM) Karyawan.
 * Menyediakan explicit wait wrappers, interaksi aman komponen Next.js/MUI (SwipeableDrawer,
 * Select mousedown, react-date-range, InfiniteScroll), pembersihan sesi storage/cookies,
 * pengecekan broken image, serta utilitas CDP untuk simulasi GPS & permission kamera/lokasi.
 */
public abstract class BasePage {

    private static final By MUI_DRAWER_PAPER = By.cssSelector("div.MuiDrawer-paper");
    private static final By MUI_SNACKBAR_OR_ALERT =
            By.cssSelector("div.MuiSnackbarContent-message, div.MuiAlert-message");

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(Constants.EXPLICIT_WAIT_TIMEOUT));
        PageFactory.initElements(driver, this);
    }

    protected WebElement waitForVisibility(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    protected WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickability(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    protected WebElement waitForClickability(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected boolean waitForInvisibility(By locator) {
        try {
            return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean waitForUrlContains(String urlFraction) {
        try {
            return wait.until(ExpectedConditions.urlContains(urlFraction));
        } catch (Exception e) {
            return getCurrentUrl().contains(urlFraction);
        }
    }

    /**
     * Navigasi URL dengan proteksi terhadap stall script pihak ketiga (Cloudflare Insights/analytics).
     *
     * @param url target URL
     */
    protected void safeNavigate(String url) {
        logger.info("Navigating to: {}", url);
        try {
            driver.get(url);
        } catch (TimeoutException e) {
            logger.warn("Page load timeout caught for [{}], stopping external scripts.", url);
            try {
                ((JavascriptExecutor) driver).executeScript("window.stop();");
            } catch (Exception ignored) {
                // Ignore if renderer busy
            }
        }
        sleepQuietly(400);
    }

    protected void click(WebElement element) {
        try {
            waitForClickability(element).click();
        } catch (Exception e) {
            logger.warn("Standard click failed, falling back to JS click: {}", e.getMessage());
            scrollToElement(element);
            clickWithJS(element);
        }
    }

    protected void click(By locator) {
        WebElement element = waitForClickability(locator);
        click(element);
    }

    protected void clickWithJS(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    protected void type(WebElement element, String text) {
        WebElement el = waitForVisibility(element);
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        el.clear();
        if (text != null && !text.isEmpty()) {
            el.sendKeys(text);
        }
    }

    protected void type(By locator, String text) {
        type(waitForVisibility(locator), text);
    }

    protected String getText(WebElement element) {
        return waitForVisibility(element).getText().trim();
    }

    protected String getText(By locator) {
        return waitForVisibility(locator).getText().trim();
    }

    protected String getAttribute(WebElement element, String attribute) {
        return waitForVisibility(element).getAttribute(attribute);
    }

    protected boolean isDisplayed(WebElement element) {
        try {
            return waitForVisibility(element).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isDisplayed(By locator) {
        try {
            return waitForVisibility(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", element);
    }

    /**
     * Membuka MUI SwipeableDrawer di portal Mobile Mirroring dengan JS click & retry hidrasi Next.js
     * (selaras dengan pola teruji AdminCrossModuleE2EPage).
     *
     * @param triggerButtonLocator locator tombol pembuka drawer (misal Ajukan Sakit / Ajukan Cuti)
     */
    protected void openSwipeableDrawerWithRetry(By triggerButtonLocator) {
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        WebElement btn = waitForVisibility(triggerButtonLocator);
        sleepQuietly(800);
        for (int attempt = 1; attempt <= 3; attempt++) {
            clickWithJS(btn);
            sleepQuietly(600);
            List<WebElement> drawers = driver.findElements(MUI_DRAWER_PAPER);
            for (WebElement drawer : drawers) {
                if (drawer.isDisplayed()) {
                    return;
                }
            }
            logger.warn("SwipeableDrawer not yet visible after attempt {}, retrying...", attempt);
            sleepQuietly(800);
        }
        waitForVisibility(MUI_DRAWER_PAPER);
    }

    /**
     * Membuka dropdown MUI Select di dalam Drawer menggunakan event mousedown.
     *
     * @param selectTrigger elemen trigger MUI Select
     */
    protected void openMuiSelectWithMouseDown(WebElement selectTrigger) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].dispatchEvent(new MouseEvent('mousedown', {bubbles:true, cancelable:true}));",
                selectTrigger
        );
    }

    /**
     * Memilih tanggal pada komponen react-date-range menggunakan rangkaian MouseEvent penuh.
     *
     * @param dayButton elemen tombol tanggal rdrDay
     */
    protected void dispatchCalendarDayMouseEvents(WebElement dayButton) {
        ((JavascriptExecutor) driver).executeScript(
                "['mouseenter','mousedown','mouseup','click'].forEach(ev => "
                        + "arguments[0].dispatchEvent(new MouseEvent(ev, {bubbles:true, cancelable:true})));",
                dayButton
        );
    }

    /**
     * Melakukan scroll ke bagian paling bawah viewport/container untuk memicu InfiniteScroll
     * ($skip = 10 * page) pada daftar riwayat Mobile Mirroring.
     */
    public void scrollToBottomForInfiniteScroll() {
        ((JavascriptExecutor) driver).executeScript(
                "window.scrollTo(0, Math.max(document.body.scrollHeight, document.documentElement.scrollHeight));"
        );
        sleepQuietly(900);
    }

    /**
     * Menunggu dan mengambil teks notifikasi MUI Snackbar / Alert.
     *
     * @param timeoutSeconds batas waktu tunggu dalam detik
     * @return teks snackbar/alert atau string kosong jika tidak muncul
     */
    public String waitForSnackbarText(int timeoutSeconds) {
        try {
            WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            WebElement msg = customWait.until(ExpectedConditions.visibilityOfElementLocated(MUI_SNACKBAR_OR_ALERT));
            return msg.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Membersihkan cookie dan Web Storage agar transisi antar sesi user terisolasi penuh.
     */
    public void clearBrowserStorageAndCookies() {
        try {
            driver.manage().deleteAllCookies();
        } catch (Exception ignored) {
            // Best-effort cookie cleanup
        }
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "window.localStorage.clear(); window.sessionStorage.clear();"
            );
        } catch (Exception ignored) {
            // Storage may not be accessible on about:blank
        }
    }

    protected void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Memeriksa apakah elemen &lt;img&gt; gagal dimuat (broken image: naturalWidth == 0).
     *
     * @param imgElement WebElement gambar
     * @return true jika gambar broken atau belum selesai dimuat
     */
    protected boolean isImageBroken(WebElement imgElement) {
        Object result = ((JavascriptExecutor) driver).executeScript(
                "return arguments[0] && arguments[0].complete && arguments[0].naturalWidth > 0;",
                imgElement
        );
        return !(result instanceof Boolean && (Boolean) result);
    }

    /**
     * Simulasi override koordinat GPS browser via Chrome DevTools Protocol (CDP).
     *
     * @param latitude  garis lintang
     * @param longitude garis bujur
     * @param accuracy  akurasi dalam meter
     */
    public void overrideGeolocation(double latitude, double longitude, double accuracy) {
        if (driver instanceof HasCdp cdpDriver) {
            Map<String, Object> params = new HashMap<>();
            params.put("latitude", latitude);
            params.put("longitude", longitude);
            params.put("accuracy", accuracy);
            cdpDriver.executeCdpCommand("Emulation.setGeolocationOverride", params);
            logger.info("CDP Geolocation overridden to lat={}, lon={}", latitude, longitude);
        }
    }

    /**
     * Mengubah izin browser (misal "geolocation" atau "videoCapture") via Chrome DevTools Protocol.
     *
     * @param permissionName nama permission CDP ("geolocation", "videoCapture")
     * @param setting        nilai izin ("granted", "denied", "prompt")
     */
    public void setBrowserPermission(String permissionName, String setting) {
        if (driver instanceof HasCdp cdpDriver) {
            Map<String, Object> descriptor = new HashMap<>();
            descriptor.put("name", permissionName);

            Map<String, Object> params = new HashMap<>();
            params.put("permission", descriptor);
            params.put("setting", setting);
            params.put("origin", Constants.BASE_URL);

            cdpDriver.executeCdpCommand("Browser.setPermission", params);
            logger.info("CDP Permission [{}] set to [{}]", permissionName, setting);
        }
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getTitle() {
        return driver.getTitle();
    }
}
