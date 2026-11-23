package com.juaracoding.magang.reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.juaracoding.magang.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Singleton konfigurasi ExtentReports 5 dengan ExtentSparkReporter (Dark Theme).
 */
public final class ExtentManager {

    private static final Logger logger = LoggerFactory.getLogger(ExtentManager.class);
    private static ExtentReports extentReports;

    private ExtentManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Mengambil instance tunggal ExtentReports secara thread-safe.
     *
     * @return ExtentReports instance
     */
    public static synchronized ExtentReports getInstance() {
        if (extentReports == null) {
            createInstance();
        }
        return extentReports;
    }

    private static void createInstance() {
        File reportDir = new File(Constants.EXTENT_REPORT_DIR);
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(Constants.EXTENT_REPORT_FILE);
        sparkReporter.config().setTheme(Theme.DARK);
        sparkReporter.config().setDocumentTitle("Karyawan SIT Report - HADIR Magang Automation");
        sparkReporter.config().setReportName("Karyawan Web UI (Selenium) & REST Assured API Execution");
        sparkReporter.config().setTimeStampFormat("dd-MM-yyyy HH:mm:ss");
        sparkReporter.config().setEncoding("UTF-8");

        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);

        extentReports.setSystemInfo("Application", "HADIR Mobile Mirroring (magang.dikahadir.com/absen/login)");
        extentReports.setSystemInfo("API Base URI", Constants.API_BASE_URI);
        extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
        extentReports.setSystemInfo("Operating System", System.getProperty("os.name"));
        extentReports.setSystemInfo("SDET Tester Deny", "Karyawan Features Group A (99 TCs: 72 UI + 27 API)");
        extentReports.setSystemInfo("SDET Tester Aji", "Karyawan Features Group B (96 TCs: 68 UI + 28 API)");
        extentReports.setSystemInfo("Total Karyawan Scenarios", "195 Test Cases (140 UI + 55 API)");

        logger.info("ExtentReports 5 initialized at: {}", Constants.EXTENT_REPORT_FILE);
    }

    /**
     * Menyimpan seluruh hasil pengujian ke file HTML ExtentReport.
     */
    public static synchronized void flushReports() {
        if (extentReports != null) {
            extentReports.flush();
            logger.info("ExtentReport flushed to: {}", Constants.EXTENT_REPORT_FILE);
        }
    }
}

