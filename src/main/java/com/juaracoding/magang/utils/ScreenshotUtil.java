package com.juaracoding.magang.utils;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utilitas untuk menangkap tangkapan layar (screenshot) sebagai bukti kegagalan uji.
 */
public final class ScreenshotUtil {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotUtil.class);

    private ScreenshotUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Menangkap screenshot dalam format Base64 string untuk disematkan di ExtentReports.
     *
     * @param driver WebDriver instance
     * @return Base64 string screenshot, atau string kosong jika gagal
     */
    public static String captureBase64(WebDriver driver) {
        if (driver == null) {
            return "";
        }
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            logger.error("Failed to capture Base64 screenshot: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Menangkap screenshot dan menyimpannya sebagai file PNG pada reports/screenshots/.
     *
     * @param driver         WebDriver instance
     * @param screenshotName Nama dasar file screenshot
     * @return Path absolut file PNG yang tersimpan
     */
    public static String captureToFile(WebDriver driver, String screenshotName) {
        if (driver == null) {
            return "";
        }
        try {
            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String sanitizedName = screenshotName.replaceAll("[^a-zA-Z0-9_-]", "_");
            String fileName = sanitizedName + "_" + timestamp + ".png";

            File destinationDir = new File(Constants.SCREENSHOTS_DIR);
            if (!destinationDir.exists()) {
                destinationDir.mkdirs();
            }

            File destinationFile = new File(destinationDir, fileName);
            FileUtils.copyFile(source, destinationFile);
            return destinationFile.getAbsolutePath();
        } catch (IOException e) {
            logger.error("Failed to save screenshot file: {}", e.getMessage());
            return "";
        }
    }
}

