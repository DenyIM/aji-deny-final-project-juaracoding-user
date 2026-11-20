package com.juaracoding.magang.drivers;

import com.juaracoding.magang.utils.Constants;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Pengelola instance WebDriver berbasis ThreadLocal untuk eksekusi paralel yang aman.
 * Chrome dikonfigurasi default headless = true serta mendukung fake camera & geolocation allow
 * untuk kebutuhan Mobile Mirroring Karyawan.
 */
public final class DriverFactory {

    private static final Logger logger = LoggerFactory.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Menginisialisasi WebDriver sesuai browser dan mode headless.
     *
     * @param browser  Nama browser ("chrome", "firefox", "edge"), default "chrome"
     * @param headless true untuk mode headless (default), false untuk mode visual
     * @return WebDriver yang aktif pada thread saat ini
     */
    public static WebDriver initDriver(String browser, boolean headless) {
        String browserName = (browser != null && !browser.trim().isEmpty())
                ? browser.toLowerCase().trim()
                : "chrome";

        logger.info("Initializing WebDriver -> browser=[{}], headless=[{}]", browserName, headless);

        WebDriver driver;
        switch (browserName) {
            case "firefox":
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (headless) {
                    firefoxOptions.addArguments("-headless");
                }
                firefoxOptions.setAcceptInsecureCerts(true);
                firefoxOptions.addPreference("permissions.default.geo", 1);
                firefoxOptions.addPreference("media.navigator.streams.fake", true);
                firefoxOptions.addPreference("media.navigator.permission.disabled", true);
                driver = new FirefoxDriver(firefoxOptions);
                break;

            case "edge":
                EdgeOptions edgeOptions = new EdgeOptions();
                if (headless) {
                    edgeOptions.addArguments("--headless=new");
                }
                edgeOptions.addArguments(
                        "--window-size=1920,1080",
                        "--no-sandbox",
                        "--disable-dev-shm-usage",
                        "--remote-allow-origins=*",
                        "--use-fake-ui-for-media-stream",
                        "--use-fake-device-for-media-stream"
                );
                edgeOptions.setAcceptInsecureCerts(true);
                driver = new EdgeDriver(edgeOptions);
                break;

            case "chrome":
            default:
                ChromeOptions chromeOptions = new ChromeOptions();
                if (headless) {
                    chromeOptions.addArguments("--headless=new");
                }
                chromeOptions.addArguments(
                        "--window-size=1920,1080",
                        "--no-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-gpu",
                        "--remote-allow-origins=*",
                        "--disable-notifications",
                        "--disable-popup-blocking",
                        "--ignore-certificate-errors",
                        "--disable-blink-features=AutomationControlled",
                        "--use-fake-ui-for-media-stream",
                        "--use-fake-device-for-media-stream"
                );
                chromeOptions.setAcceptInsecureCerts(true);
                chromeOptions.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
                chromeOptions.setExperimentalOption("useAutomationExtension", false);

                Map<String, Object> prefs = new HashMap<>();
                prefs.put("credentials_enable_service", false);
                prefs.put("profile.password_manager_enabled", false);
                prefs.put("profile.default_content_setting_values.notifications", 2);
                prefs.put("profile.default_content_setting_values.geolocation", 1);
                prefs.put("profile.default_content_setting_values.media_stream_camera", 1);
                chromeOptions.setExperimentalOption("prefs", prefs);

                driver = new ChromeDriver(chromeOptions);
                break;
        }

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(Constants.PAGE_LOAD_TIMEOUT));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(Constants.SCRIPT_TIMEOUT));
        if (!headless) {
            driver.manage().window().maximize();
        }

        setDriver(driver);
        return driver;
    }

    /**
     * Mengambil WebDriver milik thread aktif.
     *
     * @return WebDriver instance
     */
    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    /**
     * Menyimpan WebDriver ke ThreadLocal.
     *
     * @param driver WebDriver instance
     */
    public static void setDriver(WebDriver driver) {
        driverThreadLocal.set(driver);
    }

    /**
     * Menutup sesi WebDriver dan membersihkan ThreadLocal.
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
                logger.info("WebDriver terminated for current thread.");
            } catch (Exception e) {
                logger.warn("Error while quitting WebDriver: {}", e.getMessage());
            } finally {
                driverThreadLocal.remove();
            }
        }
    }
}

