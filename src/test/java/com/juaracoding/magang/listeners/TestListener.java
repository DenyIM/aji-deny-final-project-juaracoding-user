package com.juaracoding.magang.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.juaracoding.magang.drivers.DriverFactory;
import com.juaracoding.magang.reports.ExtentManager;
import com.juaracoding.magang.reports.ExtentTestManager;
import com.juaracoding.magang.utils.ScreenshotUtil;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG ITestListener yang mengintegrasikan eksekusi UI/API ke ExtentReports 5,
 * melakukan kategorisasi otomatis (Tester Deny / Tester Aji, UI / API / Hybrid),
 * serta melampirkan screenshot Base64 saat test gagal.
 */
public class TestListener implements ITestListener {

    private static final Logger logger = LoggerFactory.getLogger(TestListener.class);

    @Override
    public void onStart(ITestContext context) {
        logger.info("Test Suite started: [{}]", context.getName());
        ExtentManager.getInstance();
    }

    @Override
    public void onFinish(ITestContext context) {
        logger.info("Test Suite finished: [{}]", context.getName());
        ExtentManager.flushReports();
    }

    @Override
    public void onTestStart(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "Execution of " + methodName;
        }

        logger.info("--- Starting Test: [{}] | {} ---", methodName, description);
        ExtentTest test = ExtentTestManager.startTest(methodName, description);

        String className = result.getTestClass().getName();
        if (className.contains(".deny.") || className.contains("GroupA")) {
            test.assignCategory("Tester Deny");
        } else if (className.contains(".aji.") || className.contains("GroupB")) {
            test.assignCategory("Tester Aji");
        }

        if (className.contains(".api.")) {
            test.assignCategory("REST Assured API");
        } else if (className.contains(".tests.")) {
            test.assignCategory("Selenium Web UI");
        }

        String[] groups = result.getMethod().getGroups();
        if (groups != null && groups.length > 0) {
            test.assignCategory(groups);
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        logger.info("Test PASSED: [{}]", methodName);
        ExtentTestManager.logPass("Test case passed: " + methodName);
        ExtentTestManager.unload();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        Throwable throwable = result.getThrowable();
        logger.error("Test FAILED: [{}] - Reason: {}", methodName,
                throwable != null ? throwable.getMessage() : "Unknown");

        ExtentTestManager.logFail("Test case failed: " + (throwable != null ? throwable.getMessage() : ""));
        if (throwable != null && ExtentTestManager.getTest() != null) {
            ExtentTestManager.getTest().fail(throwable);
        }

        WebDriver driver = DriverFactory.getDriver();
        if (driver != null) {
            String base64Screenshot = ScreenshotUtil.captureBase64(driver);
            if (!base64Screenshot.isEmpty() && ExtentTestManager.getTest() != null) {
                ExtentTestManager.getTest().fail(
                        "Failure Evidence Screenshot:",
                        MediaEntityBuilder.createScreenCaptureFromBase64String(
                                "data:image/png;base64," + base64Screenshot).build()
                );
            }
        }

        ExtentTestManager.unload();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        logger.warn("Test SKIPPED: [{}]", methodName);
        ExtentTestManager.logSkip("Test case skipped: " + methodName);
        ExtentTestManager.unload();
    }
}

