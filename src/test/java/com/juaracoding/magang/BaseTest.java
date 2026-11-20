package com.juaracoding.magang;

import com.juaracoding.magang.drivers.DriverFactory;
import com.juaracoding.magang.listeners.TestListener;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * Kelas induk untuk seluruh pengujian Web UI Selenium Karyawan (Mobile Mirroring).
 * Mengelola siklus hidup WebDriver per-metode uji dengan default headless = true.
 */
@Listeners(TestListener.class)
public abstract class BaseTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeMethod(alwaysRun = true)
    @Parameters({"browser", "headless"})
    public void setUp(
            @Optional("chrome") String browser,
            @Optional("true") String headless
    ) {
        String sysBrowser = System.getProperty("browser");
        String effectiveBrowser = (sysBrowser != null && !sysBrowser.trim().isEmpty()) ? sysBrowser : browser;

        String sysHeadless = System.getProperty("headless");
        boolean isHeadless = (sysHeadless != null && !sysHeadless.trim().isEmpty())
                ? Boolean.parseBoolean(sysHeadless)
                : Boolean.parseBoolean(headless);

        logger.info("Setting up Web UI Test -> browser=[{}], headless=[{}]", effectiveBrowser, isHeadless);
        DriverFactory.initDriver(effectiveBrowser, isHeadless);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        logger.info("Tearing down Web UI Test: quitting WebDriver.");
        DriverFactory.quitDriver();
    }

    protected WebDriver getDriver() {
        return DriverFactory.getDriver();
    }
}

