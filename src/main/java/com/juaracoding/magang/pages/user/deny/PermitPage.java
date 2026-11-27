package com.juaracoding.magang.pages.user.deny;

import com.juaracoding.magang.pages.BasePage;
import com.juaracoding.magang.utils.Constants;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

/**
 * Page Object Model untuk halaman Pengajuan Izin Karyawan
 * (https://magang.dikahadir.com/apps/absent/permit).
 * Cakupan skenario: SIT_USR_UI_078 s/d SIT_USR_UI_091 (Tester Deny).
 */
public class PermitPage extends BasePage {

    private static final By ACTIVE_AJUKAN_BUTTON =
            By.xpath("//div[not(@hidden) and @role='tabpanel']//button[contains(normalize-space(),'Ajukan')]");
    private static final By MUI_DRAWER_PAPER = By.cssSelector("div.MuiDrawer-paper");
    private static final By MUI_PICKERS_POPPER = By.cssSelector("div.MuiPickersPopper-root, div[role='dialog']");

    @FindBy(xpath = "//a[contains(@class,'user__menu__item')][.//p[normalize-space()='Izin']]")
    private WebElement izinMenuTile;

    @FindBy(xpath = "//a[.//p[normalize-space()='Kembali']]/button | //a[.//p[normalize-space()='Kembali']]")
    private WebElement backToHomeButton;

    @FindBy(xpath = "//*[contains(normalize-space(),'Halaman Izin')]")
    private WebElement headerTitle;

    @FindBy(xpath = "//button[@role='tab' and normalize-space()='Terlambat']")
    private WebElement tabTerlambat;

    @FindBy(xpath = "//button[@role='tab' and normalize-space()='Pulang Cepat']")
    private WebElement tabPulangCepat;

    @FindBy(xpath = "//button[@role='tab' and normalize-space()='Izin Off']")
    private WebElement tabIzinOff;

    public PermitPage(WebDriver driver) {
        super(driver);
    }

    public void navigateFromAbsentHome() {
        waitForUrlContains("/apps/absent");
        WebElement tile = waitForVisibility(izinMenuTile);
        scrollToElement(tile);
        clickWithJS(tile);
        waitForUrlContains("/apps/absent/permit");
        waitForPermitPageReady();
        logger.info("Navigated to Permit page from /apps/absent menu tile.");
    }

    public void navigateDirectToPermitPage() {
        safeNavigate(Constants.USER_PERMIT_URL);
        waitForUrlContains("/apps/absent/permit");
        waitForPermitPageReady();
    }

    public void navigateDirectToTab(String permitQueryValue) {
        safeNavigate(Constants.USER_PERMIT_URL + "?permit=" + permitQueryValue);
        waitForUrlContains("/apps/absent/permit");
        waitForPermitPageReady();
    }

    public void waitForPermitPageReady() {
        wait.until(d -> !d.findElements(ACTIVE_AJUKAN_BUTTON).isEmpty());
        waitForDataLoadComplete();
    }

    private void waitForDataLoadComplete() {
        try {
            wait.until(d -> d.findElements(By.cssSelector("div[role='tabpanel']:not([hidden]) span.MuiSkeleton-root")).isEmpty());
        } catch (Exception ignored) {
            // Skeleton may have already disappeared before check
        }
        sleepQuietly(350);
    }

    public boolean isHeaderDisplayed() {
        return isDisplayed(headerTitle);
    }

    public boolean isTerlambatTabDisplayed() {
        return isDisplayed(tabTerlambat);
    }

    public boolean isPulangCepatTabDisplayed() {
        return isDisplayed(tabPulangCepat);
    }

    public boolean isIzinOffTabDisplayed() {
        return !driver.findElements(By.xpath("//button[@role='tab' and normalize-space()='Izin Off']")).isEmpty()
                && tabIzinOff.isDisplayed();
    }

    public void clickTabTerlambat() {
        WebElement tab = waitForVisibility(tabTerlambat);
        clickWithJS(tab);
        waitForDataLoadComplete();
    }

    public void clickTabPulangCepat() {
        WebElement tab = waitForVisibility(tabPulangCepat);
        clickWithJS(tab);
        waitForUrlContains("permit=early");
        waitForDataLoadComplete();
    }

    public void clickTabIzinOffOrNavigate() {
        if (isIzinOffTabDisplayed()) {
            clickWithJS(tabIzinOff);
            waitForUrlContains("permit=off-permit");
            waitForDataLoadComplete();
        } else {
            logger.info("Izin Off tab hidden by user project config; opening ?permit=off-permit directly.");
            navigateDirectToTab("off-permit");
        }
    }

    public boolean isActiveTabListTitleDisplayed(String expectedTitle) {
        By titleLocator = By.xpath("//div[not(@hidden) and @role='tabpanel']//p[normalize-space()='" + expectedTitle + "']");
        return isDisplayed(titleLocator);
    }

    public boolean isActiveAjukanButtonDisplayed(String expectedButtonText) {
        By btnLocator = By.xpath("//div[not(@hidden) and @role='tabpanel']//button[normalize-space()='" + expectedButtonText + "']");
        return isDisplayed(btnLocator);
    }

    public String getActiveTotalText() {
        By totalLocator = By.xpath("//div[not(@hidden) and @role='tabpanel']//p[starts-with(normalize-space(),'Total')]");
        return getText(totalLocator);
    }

    public int getActiveTotalCount() {
        String text = getActiveTotalText();
        String digits = text.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    public void openPermitDrawer() {
        openSwipeableDrawerWithRetry(ACTIVE_AJUKAN_BUTTON);
        waitForVisibility(MUI_DRAWER_PAPER);
        sleepQuietly(400);
        logger.info("Opened SwipeableDrawer for permit submission.");
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
        logger.info("Closed SwipeableDrawer without submit.");
    }

    /**
     * Memilih tanggal aktif hari ini pada komponen MUI DatePicker di dalam drawer
     * (minDate & maxDate dibatasi hari ini oleh aplikasi).
     */
    public void selectTodayOnDrawerDatePicker() {
        By calendarIconBtn = By.xpath(
                "//div[contains(@class,'MuiDrawer-paper')]//label[normalize-space()='Tanggal' or normalize-space()='Tanggal Off']/following::div[contains(@class,'MuiFormControl-root')][1]//button"
        );
        List<WebElement> btns = driver.findElements(calendarIconBtn);
        if (!btns.isEmpty()) {
            clickWithJS(btns.get(0));
            waitForVisibility(MUI_PICKERS_POPPER);
            List<WebElement> todayBtns = driver.findElements(
                    By.cssSelector("div.MuiPickersPopper-root button.MuiPickersDay-today:not([disabled]), div[role='dialog'] button.MuiPickersDay-today:not([disabled])")
            );
            if (todayBtns.isEmpty()) {
                todayBtns = driver.findElements(
                        By.cssSelector("div.MuiPickersPopper-root button.MuiPickersDay-root:not(.Mui-disabled), div[role='dialog'] button.MuiPickersDay-root:not(.Mui-disabled)")
                );
            }
            if (!todayBtns.isEmpty()) {
                clickWithJS(todayBtns.get(0));
                sleepQuietly(350);
                logger.info("Selected today's date from MUI DatePicker dialog.");
                return;
            }
        }
        throw new IllegalStateException("Could not select today's date in MUI DatePicker inside drawer.");
    }

    /**
     * Mengisi komponen MUI TimePicker di dalam drawer (format HH:mm, misal "09:15" atau "15:30").
     */
    public void setDrawerTime(String hhMm) {
        String[] parts = hhMm.split(":");
        int targetHour = Integer.parseInt(parts[0]);
        int targetMinute = Integer.parseInt(parts[1]);

        By timeInputLoc = By.xpath(
                "//div[contains(@class,'MuiDrawer-paper')]//label[normalize-space()='Jam']/following::div[contains(@class,'MuiFormControl-root')][1]//input"
        );
        WebElement timeInput = waitForVisibility(timeInputLoc);
        String digitsOnly = hhMm.replace(":", "");
        clickWithJS(timeInput);
        timeInput.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        timeInput.sendKeys(digitsOnly);
        sleepQuietly(250);

        String valAfterKeys = timeInput.getAttribute("value");
        if (valAfterKeys != null && !valAfterKeys.isBlank() && valAfterKeys.contains(":")) {
            logger.info("Set TimePicker via keyboard input: {}", valAfterKeys);
            return;
        }

        // Fallback via Clock Popper icon & Clock dial selection
        By clockBtnLoc = By.xpath(
                "//div[contains(@class,'MuiDrawer-paper')]//label[normalize-space()='Jam']/following::div[contains(@class,'MuiFormControl-root')][1]//button"
        );
        List<WebElement> clockBtns = driver.findElements(clockBtnLoc);
        if (!clockBtns.isEmpty()) {
            clickWithJS(clockBtns.get(0));
            waitForVisibility(MUI_PICKERS_POPPER);
            sleepQuietly(300);
            selectClockNumber(String.valueOf(targetHour));
            sleepQuietly(350);
            selectClockNumber(String.format("%02d", targetMinute));
            sleepQuietly(300);
            logger.info("Set TimePicker via MUI Clock dial -> input value=[{}]", timeInput.getAttribute("value"));
        }
    }

    private void selectClockNumber(String label) {
        String ariaMatch = label + " hours";
        String minMatch = label + " minutes";
        List<WebElement> candidates = driver.findElements(By.xpath(
                "//div[contains(@class,'MuiPickersPopper-root') or @role='dialog']//span[contains(@class,'MuiClockNumber-root') and (normalize-space()='"
                        + label + "' or @aria-label='" + ariaMatch + "' or @aria-label='" + minMatch + "')]"
        ));
        if (candidates.isEmpty()) {
            candidates = driver.findElements(By.xpath(
                    "//div[contains(@class,'MuiPickersPopper-root') or @role='dialog']//span[contains(@class,'MuiClockNumber-root')]"
            ));
        }
        if (!candidates.isEmpty()) {
            WebElement numEl = candidates.get(0);
            ((JavascriptExecutor) driver).executeScript(
                    "const r = arguments[0].getBoundingClientRect();"
                            + "const x = r.left + r.width/2, y = r.top + r.height/2;"
                            + "const mask = document.querySelector('.MuiClock-squareMask') || arguments[0];"
                            + "['mousedown','mouseup','click'].forEach(t => mask.dispatchEvent(new MouseEvent(t, {bubbles:true, cancelable:true, clientX:x, clientY:y})));",
                    numEl
            );
        }
    }

    /**
     * Mengisi controlled React &lt;textarea&gt; di dalam MuiDrawer-paper dengan aman
     * memakai HTMLTextAreaElement.prototype value setter + event input agar state react-hook-form terupdate.
     */
    private void setReactTextareaValue(By locator, String text) {
        WebElement el = waitForVisibility(locator);
        ((JavascriptExecutor) driver).executeScript(
                "const setter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value').set;"
                        + "setter.call(arguments[0], arguments[1]);"
                        + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                        + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                el, text == null ? "" : text
        );
        sleepQuietly(200);
    }

    public void enterNotes(String notes) {
        setReactTextareaValue(By.cssSelector("div.MuiDrawer-paper textarea#notes"), notes);
    }

    public String getNotesValue() {
        WebElement el = waitForVisibility(By.cssSelector("div.MuiDrawer-paper textarea#notes"));
        Object val = ((JavascriptExecutor) driver).executeScript("return arguments[0].value;", el);
        return val != null ? val.toString() : "";
    }

    public void enterOffReason(String reason) {
        setReactTextareaValue(By.cssSelector("div.MuiDrawer-paper textarea#reason"), reason);
    }

    public String getOffReasonValue() {
        WebElement el = waitForVisibility(By.cssSelector("div.MuiDrawer-paper textarea#reason"));
        Object val = ((JavascriptExecutor) driver).executeScript("return arguments[0].value;", el);
        return val != null ? val.toString() : "";
    }

    public void clickDrawerResetButton() {
        WebElement resetBtn = waitForVisibility(
                By.xpath("//div[contains(@class,'MuiDrawer-paper')]//button[normalize-space()='Reset']")
        );
        clickWithJS(resetBtn);
        sleepQuietly(300);
        logger.info("Clicked 'Reset' button inside Permit drawer.");
    }

    public void clickDrawerSubmitButton() {
        WebElement submitBtn = waitForVisibility(
                By.xpath("//div[contains(@class,'MuiDrawer-paper')]//button[@type='submit']")
        );
        clickWithJS(submitBtn);
        sleepQuietly(300);
        logger.info("Clicked 'Ajukan' submit button inside Permit drawer.");
    }

    public List<String> getAllDrawerHelperErrorTexts() {
        sleepQuietly(400);
        List<WebElement> els = driver.findElements(By.cssSelector("div.MuiDrawer-paper p.MuiFormHelperText-root.Mui-error"));
        List<String> texts = new ArrayList<>();
        for (WebElement el : els) {
            String t = el.getText().trim();
            if (!t.isEmpty()) {
                texts.add(t);
            }
        }
        return texts;
    }

    public String getSnackbarMessage() {
        return waitForSnackbarText(10);
    }

    public int getPermitCardCount() {
        waitForDataLoadComplete();
        List<WebElement> cards = driver.findElements(
                By.cssSelector("div[role='tabpanel']:not([hidden]) div.MuiPaper-root.MuiCard-root")
        );
        return cards.size();
    }

    public String findPermitCardTextContaining(String keyword) {
        waitForDataLoadComplete();
        List<WebElement> cards = driver.findElements(
                By.cssSelector("div[role='tabpanel']:not([hidden]) div.MuiPaper-root.MuiCard-root")
        );
        for (WebElement card : cards) {
            String txt = card.getText();
            if (txt != null && txt.contains(keyword)) {
                return txt;
            }
        }
        return "";
    }

    public boolean cardHasFiveAttributes(String cardText, String typeLabel) {
        if (cardText == null || cardText.isBlank()) {
            return false;
        }
        return cardText.contains(typeLabel)
                && cardText.contains("Status")
                && cardText.contains("Catatan")
                && cardText.contains("Alasan Ditolak")
                && cardText.contains("Tanggal Dibuat");
    }

    public void clickBackToHome() {
        WebElement backBtn = waitForVisibility(backToHomeButton);
        clickWithJS(backBtn);
        logger.info("Clicked '< Kembali' link on Halaman Izin.");
    }

    public boolean waitForReturnToHome() {
        try {
            return wait.until(d -> d.getCurrentUrl().endsWith("/apps/absent")
                    || (d.getCurrentUrl().contains("/apps/absent") && !d.getCurrentUrl().contains("/permit")));
        } catch (Exception e) {
            return getCurrentUrl().contains("/apps/absent") && !getCurrentUrl().contains("/permit");
        }
    }
}
