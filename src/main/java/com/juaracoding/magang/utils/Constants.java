package com.juaracoding.magang.utils;

/**
 * Sentralisasi konfigurasi URL Mobile Mirroring, endpoint backend FeathersJS & Next.js BFF terverifikasi,
 * kredensial ter-encode Base64 (zero plaintext), timeout, pool NIK, dan path laporan pengujian Karyawan.
 */
public final class Constants {

    private Constants() {
        // Private constructor to prevent instantiation
    }

    // ==========================================
    // WEB UI URLS (MOBILE MIRRORING KARYAWAN)
    // ==========================================
    public static final String BASE_URL = "https://magang.dikahadir.com";
    public static final String USER_LOGIN_URL = BASE_URL + "/absen/login";
    public static final String USER_REGISTER_URL = BASE_URL + "/absen/register";
    public static final String USER_FORGOT_PASSWORD_URL = BASE_URL + "/absen/forgot-password";
    public static final String USER_RESET_PASSWORD_REQUEST_URL = BASE_URL + "/absen/reset-password-request";
    public static final String USER_RESET_PASSWORD_URL = BASE_URL + "/absen/reset-password";
    public static final String USER_HOME_URL = BASE_URL + "/apps/absent";
    public static final String USER_ACTIVITY_URL = BASE_URL + "/apps/absent/activity";
    public static final String USER_CORRECTION_URL = BASE_URL + "/apps/absent/correction";
    public static final String USER_CORRECTION_DETAIL_URL_PREFIX = BASE_URL + "/apps/absent/correction/";
    public static final String USER_PERMIT_URL = BASE_URL + "/apps/absent/permit";
    public static final String USER_OVERTIME_URL = BASE_URL + "/apps/absent/overtime";
    public static final String USER_OVERTIME_CLIENT_URL = BASE_URL + "/apps/absent/overtime-client";
    public static final String USER_OVERTIME_DETAIL_URL_PREFIX = BASE_URL + "/apps/absent/overtime/";
    public static final String USER_LEAVE_URL = BASE_URL + "/apps/absent/leave";
    public static final String USER_LEAVE_CLIENT_URL = BASE_URL + "/apps/absent/leave-client";
    public static final String USER_LEAVE_DETAIL_URL_PREFIX = BASE_URL + "/apps/absent/leave/";
    public static final String USER_SICK_URL = BASE_URL + "/apps/absent/sick";
    public static final String USER_STAFF_URL = BASE_URL + "/apps/absent/staff";
    public static final String USER_CHANGE_PASSWORD_URL = BASE_URL + "/apps/absent/change-password";
    public static final String USER_DOWNLOAD_REPORT_URL = BASE_URL + "/apps/absent/reports/share_report";

    // ==========================================
    // REAL BACKEND API URI & ENDPOINTS (FEATHERSJS)
    // ==========================================
    public static final String API_BASE_URI = "https://api-dev.dikahadir.com/hadir-api-magang";
    public static final String API_STATIC_UPLOADS_BASE_URI = "https://api-dev.dikahadir.com/uploads";
    public static final String API_AUTH_ENDPOINT = "/authentication";
    public static final String API_AUTH_MANAGEMENT_ENDPOINT = "/auth-management";
    public static final String API_USERS_ENDPOINT = "/users";
    public static final String API_USER_SUMMARY_ENDPOINT = "/user-summary";
    public static final String API_ACTIVITY_ENDPOINT = "/activity";
    public static final String API_CLOCK_OUT_ENDPOINT = "/activity/request-activity";
    public static final String API_REQUEST_ACTIVITY_ENDPOINT = "/activity/request-activity";
    public static final String API_CORRECTIONS_ENDPOINT = "/activity-correction";
    public static final String API_OVERTIME_ENDPOINT = "/overtime";
    public static final String API_OFF_PERMIT_ENDPOINT = "/off-permit";
    public static final String API_LATE_PERMIT_ENDPOINT = "/late-permit";
    public static final String API_EARLY_LEAVE_ENDPOINT = "/early-leave";
    public static final String API_BUSINESS_TRIPS_ENDPOINT = "/business-trips";
    public static final String API_PROJECT_LEAVE_ENDPOINT = "/project-leave";
    public static final String API_CANCEL_LEAVE_ENDPOINT = "/cancel-leave";
    public static final String API_SICK_ENDPOINT = "/sick";
    public static final String API_SHARE_REPORT_ENDPOINT = "/share-report";
    public static final String API_UPLOAD_ENDPOINT = "/upload";
    public static final String API_LOCATIONS_ENDPOINT = "/location-point";

    // ==========================================
    // NEXT.JS BFF ROUTES (HOSTED ON BASE_URL)
    // ==========================================
    public static final String BFF_SESSION_COOKIE_NAME = "hadir_app_magang";
    public static final String BFF_LOGIN_ENDPOINT = "/api/login";
    public static final String BFF_USER_SUMMARY_ENDPOINT = "/api/users/summary";
    public static final String BFF_USER_LAST_ACTIVITY_ENDPOINT = "/api/activity/user-last-activity";
    public static final String BFF_USER_CHECK_CORRECTION_ENDPOINT = "/api/activity/user-check-correction";
    public static final String BFF_USERS_ENDPOINT = "/api/users";
    public static final String BFF_CORRECTIONS_ENDPOINT = "/api/activity-correction";
    public static final String BFF_OFF_PERMIT_ENDPOINT = "/api/off-permit";
    public static final String BFF_LATE_PERMIT_ENDPOINT = "/api/permit/late-permit";
    public static final String BFF_EARLY_LEAVE_ENDPOINT = "/api/permit/early-permit";
    public static final String BFF_LEAVE_ENDPOINT = "/api/leave";
    public static final String BFF_OVERTIME_ENDPOINT = "/api/overtime";
    public static final String BFF_SICK_ENDPOINT = "/api/sick";
    public static final String BFF_ACTIVITY_SICK_ENDPOINT = "/api/activity/sick";
    public static final String BFF_PROJECT_LEAVE_ENDPOINT = "/api/leave/project-leave";
    public static final String BFF_CLIENT_GENERATE_REPORT_ENDPOINT = "/api/client/generate-report";
    public static final String BFF_CLIENT_LEAVE_REPORT_ENDPOINT = "/api/client/leave-report";
    public static final String BFF_CLIENT_OVERTIME_REPORT_ENDPOINT = "/api/client/overtime-report";

    // ==========================================
    // CREDENTIALS (BASE64 ENCODED - NO PLAINTEXT)
    // ==========================================
    public static final String USER_EMAIL = "hadirsqa1@gmail.com";
    public static final String USER_NAME = "Hadir SQA Testing 1";
    public static final String USER_NIK = "D8240010";
    public static final String USER_PASSWORD_ENCODED = "U1FBQEhhZGlyMTIzNDU=";

    // Kredensial Admin khusus untuk Hybrid @Before/@After API Approval & Seeding
    public static final String HYBRID_ADMIN_EMAIL = "admin@hadir.com";
    public static final String HYBRID_ADMIN_PASSWORD_ENCODED = "TWFnYW5nU1FBX0pDQDEyMw==";

    // Pool 34 NIK master DIKA terverifikasi yang belum terpakai akun aktif hadirsqa1@gmail.com (D8240010)
    public static final String[] VALID_NIK_POOL = {
        "D7231978", "D7231983", "D7231984", "D7231985", "D7232001",
        "D7232029", "D6230561", "D8230009", "D8230013", "D8230014",
        "D7240028", "D7240027", "D6240004", "D8240008", "D7240023",
        "D7240022", "D7240021", "D7240020", "D7240019", "D8240007",
        "D6240003", "D7240018", "D9240001", "D7240017", "D7240016",
        "D7240015", "D7240014", "D6240002", "D7240013", "D7240012",
        "F1240001", "D6240005", "D7240031", "D7240029"
    };

    // Koordinat GPS default area kantor & luar radius (untuk simulasi CDP)
    public static final double DEFAULT_OFFICE_LAT = -6.2088;
    public static final double DEFAULT_OFFICE_LON = 106.8456;
    public static final double OUT_OF_RADIUS_LAT = -6.9175;
    public static final double OUT_OF_RADIUS_LON = 107.6191;

    // ==========================================
    // TIMEOUTS (SECONDS)
    // ==========================================
    public static final int EXPLICIT_WAIT_TIMEOUT = 15;
    public static final int PAGE_LOAD_TIMEOUT = 30;
    public static final int SCRIPT_TIMEOUT = 30;

    // ==========================================
    // REPORTS & TEST DATA PATHS
    // ==========================================
    public static final String EXTENT_REPORT_DIR = "reports/";
    public static final String EXTENT_REPORT_FILE = EXTENT_REPORT_DIR + "ExtentReport.html";
    public static final String SCREENSHOTS_DIR = "reports/screenshots/";
    public static final String SIT_EXCEL_PATH = "docs/SIT_Scenario_Magang_Hadir.xlsx";
    public static final String TESTDATA_DIR = "src/test/resources/testdata/";
}
