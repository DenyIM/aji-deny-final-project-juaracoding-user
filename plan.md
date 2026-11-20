# 📋 Implementation Plan: Enterprise Karyawan Automation Framework (`magang.dikahadir.com`)

## Goal Description
Membangun **Enterprise Web UI & API Test Automation Framework** khusus untuk **portal Karyawan / User Magang (Mobile Mirroring)** pada platform **[HADIR Mobile Mirroring](https://magang.dikahadir.com/absen/login)** yang **100% selaras** secara arsitektur, versi dependensi, standar keamanan kredensial, pola pelaporan, dan pembagian kerja tim dengan repositori **Web Admin (`final-project-admin`)** serta acuan standar industri pada [reference.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/reference.md).

Kerangka kerja ini menggunakan **Java 21 LTS**, **Apache Maven**, **Selenium WebDriver `4.52.0`** (default `headless = true`), **REST Assured `6.3.0`**, **TestNG `7.12.0`**, **ExtentReports `5.1.2`** (Dark Theme + Base64 failure screenshot), **Apache POI `5.2.5`**, dan **Jackson Databind `2.17.2`**, mencakup **175 Test Cases User** (**120 Web UI** + **55 REST Assured API**) dari [SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/SIT_Scenario_Magang_Hadir.xlsx).

---

## Ringkasan Keputusan Teknis & Spesifikasi Ketat

> [!IMPORTANT]
> **1. Tech Stack, Versi Tools & Kebijakan Headless**
> - **Runtime & Build**: Java 21 LTS, Apache Maven `3.9+`, `maven-compiler-plugin` `3.13.0`, `maven-surefire-plugin` `3.5.0`.
> - **Libraries**: Selenium WebDriver `4.52.0`, REST Assured `6.3.0` (`rest-assured`, `json-path`, `json-schema-validator`), TestNG `7.12.0`, ExtentReports `5.1.2`, Apache POI `5.2.5` (`poi`, `poi-ooxml`), Jackson Databind `2.17.2`, Commons IO `2.16.1`, SLF4J `2.0.16`.
> - **Default Headless**: Chrome **WAJIB** default `headless = true` pada `DriverFactory.java`, `BaseTest.java`, `pom.xml`, `testng.xml`, `testng-deny.xml`, dan `testng-aji.xml`, namun dapat dioverride kapan saja melalui parameter CLI `-Dheadless=false`.
> - **Simulasi Kamera & GPS (Mobile Mirroring)**: `DriverFactory.java` mengaktifkan `--use-fake-ui-for-media-stream`, `--use-fake-device-for-media-stream`, serta Chrome `prefs` untuk `geolocation = 1` (Allow) dan `media_stream_camera = 1` (Allow). Untuk skenario negatif GPS di luar radius atau permission diblokir (`SIT_USR_UI_043`, `044`, `045`), digunakan **Chrome DevTools Protocol (CDP)** (`Emulation.setGeolocationOverride` & `Browser.setPermission`).

> [!IMPORTANT]
> **2. Keamanan Kredensial Base64 (`SecurityUtil.java`)**
> - **Zero Plaintext Policy**: Dilarang keras menuliskan password plaintext di file `.java`, komentar kode, `pom.xml`, maupun `testng*.xml`.
> - `SecurityUtil.java` memanfaatkan `java.util.Base64` untuk melakukan `decode(...)` di memori saat runtime.
> - Konstanta di `Constants.java`:
>   - `USER_EMAIL = "hadirsqa1@gmail.com"` (NIK: `D6201031`, Nama: `Hadir SQA Testing 1`)
>   - `USER_PASSWORD_ENCODED = "U1FBQEhhZGlyMTIzNDU="`
>   - `HYBRID_ADMIN_EMAIL = "admin@hadir.com"`
>   - `HYBRID_ADMIN_PASSWORD_ENCODED = "TWFnYW5nU1FBX0pDQDEyMw=="`
>   - `VALID_NIK_POOL = {"F1240001", "D6240005", "D7240031", "D8240010", "D7240029", "D7240028", "D7240027", "D6240004"}`

> [!IMPORTANT]
> **3. Backend API Riil & Pola Hybrid API + UI (`AdminApiHelper.java`)**
> - **Backend API Base URI**: `API_BASE_URI = "https://api-dev.dikahadir.com/hadir-api-magang"` (FeathersJS).
> - **SSL/TLS Staging**: `BaseApiTest` dan `AdminApiHelper` wajib mengaktifkan **`RestAssured.useRelaxedHTTPSValidation()`** serta `.relaxedHTTPSValidation()` pada `RequestSpecBuilder`.
> - **Endpoint Autentikasi**: **`POST /authentication`** dengan payload `{"email": "...", "password": "...", "strategy": "local"}` (mengembalikan HTTP `201` dengan `accessToken` JWT dan `user.id`).
> - **Pola Hybrid API + UI (`AdminApiHelper`)**: Pada tahap `@BeforeMethod` / `@BeforeClass` untuk skenario UI Karyawan yang membutuhkan status `Approved` / `Rejected` dari Admin (seperti verifikasi status kartu dan alasan penolakan HRD pada **Pengajuan Izin** `SIT_USR_UI_083`, **Koreksi Absen**, atau **Lembur**):
>   1. REST Assured login sebagai Karyawan (`POST /authentication`) → *seeding* pengajuan baru (`POST /off-permit`, `POST /activity-correction`, atau `POST /overtime`) → ambil `requestId`.
>   2. REST Assured login sebagai Admin (`HYBRID_ADMIN_PASSWORD_ENCODED`) → jalankan `PATCH` approval/rejection beserta alasan penolakan.
>   3. Selenium Web UI login sebagai Karyawan di `/absen/login` → verifikasi status badge dan alasan penolakan tampil akurat di halaman `/apps/absent/permit`, `/apps/absent/correction`, atau `/apps/absent/overtime`.

---

## Arsitektur Sistem

```mermaid
flowchart TD
    subgraph Execution & Runners
        TX1[testng.xml - Master 175 TCs]
        TX2[testng-deny.xml - 87 TCs]
        TX3[testng-aji.xml - 88 TCs]
        TL[TestListener - Auto Tag Deny/Aji & UI/API]
    end

    subgraph Core Thread-Safe & Security Engine
        SEC[SecurityUtil - Base64 Decode]
        CONST[Constants - URLs, Endpoints, Base64, NIK Pool]
        DF[DriverFactory - ThreadLocal, headless=true, Fake Camera & GPS]
        BP[BasePage - Explicit Wait 15s & Safe Actions]
        BT[BaseTest - @BeforeMethod / @AfterMethod]
        BAPI[BaseApiTest - useRelaxedHTTPSValidation & JWT]
        HYB[AdminApiHelper - Hybrid @Before Seeding & Admin Approval]
    end

    subgraph Tester Deny Scope - 87 TCs
        D_POM[pages.user.deny: UserLoginPage, RegisterPage, ResetPasswordPage, PermitPage, LeaveAndSickPage]
        D_UI[tests.user.deny: 60 Web UI TCs]
        D_API[UserGroupAApiTest: 27 API TCs]
    end

    subgraph Tester Aji Scope - 88 TCs
        A_POM[pages.user.aji: AttendancePage, ActivityHistoryPage, CorrectionPage, OvertimePage, UserProfilePage]
        A_UI[tests.user.aji: 60 Web UI TCs]
        A_API[UserGroupBApiTest: 28 API TCs]
    end

    subgraph Reporting & Evidence
        SU[ScreenshotUtil - Base64 & PNG]
        EM[ExtentManager - SparkReporter Dark Theme]
        ETM[ExtentTestManager - ThreadLocal Logs & Test ID]
        REP[reports/ExtentReport.html]
    end

    TX1 & TX2 & TX3 --> TL
    TX1 & TX2 & TX3 --> BT & BAPI
    BT --> DF
    D_UI --> D_POM --> BP
    A_UI --> A_POM --> BP
    HYB -->|@Before Seed + Admin PATCH| D_UI & A_UI
    TL -->|On Failure| SU --> EM
    D_UI & D_API & A_UI & A_API --> ETM --> EM --> REP
```

---

## Pembagian Tugas Adil 2 Tester (Total 175 TCs — Tanpa Mengubah Isi SIT)

Mengacu langsung pada sheet `Tester Deny (Admin & User)`, `Tester Aji (Admin & User)`, dan `Summary & Collaboration Guide` di [SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/SIT_Scenario_Magang_Hadir.xlsx):

| Tester | Cakupan Web UI (`120 TCs`) | Cakupan REST Assured API (`55 TCs`) | Total & Bobot |
| :--- | :--- | :--- | :---: |
| **👤 Tester Deny** | **60 Web UI TCs** (`pages.user.deny` & `tests.user.deny`):<br>1. `UserLoginTest` — `Autentikasi-Login` (`SIT_USR_UI_001–012`: 12 TCs)<br>2. `RegisterTest` — `Autentikasi-Register` (`SIT_USR_UI_013–025`: 13 TCs)<br>3. `ResetPasswordTest` — `Autentikasi-Reset Password` (`SIT_USR_UI_026–033`: 8 TCs)<br>4. `PermitTest` — `Pengajuan-Izin` (`SIT_USR_UI_075–084`: 10 TCs — *Hybrid*)<br>5. `LeaveAndSickTest` — `Pengajuan-Cuti & Sakit` (`SIT_USR_UI_096–112`: 17 TCs) | **27 API TCs** (`UserGroupAApiTest`):<br>1. Auth & Reset Password API (`SIT_USR_API_001–010`: 10 TCs)<br>2. Registration & NIK Check API (`SIT_USR_API_011–018`: 8 TCs)<br>3. Leaves / Izin / Cuti / Sakit API (`SIT_USR_API_044–049`: 6 TCs)<br>4. Profile & Change Password API (`SIT_USR_API_053–055`: 3 TCs) | **87 TCs** (~49.7%) |
| **👤 Tester Aji** | **60 Web UI TCs** (`pages.user.aji` & `tests.user.aji`):<br>1. `AttendanceTest` — `Presensi-Beranda` (`SIT_USR_UI_034–053`: 20 TCs)<br>2. `ActivityHistoryTest` — `Presensi-Log Absen` (`SIT_USR_UI_054–064`: 11 TCs)<br>3. `CorrectionTest` — `Pengajuan-Koreksi Absen` (`SIT_USR_UI_065–074`: 10 TCs — *Hybrid*)<br>4. `OvertimeTest` — `Pengajuan-Lembur` (`SIT_USR_UI_085–095`: 11 TCs — *Hybrid*)<br>5. `UserProfileTest` — `Profil-Menu Utama & Ubah Password` (`SIT_USR_UI_113–120`: 8 TCs) | **28 API TCs** (`UserGroupBApiTest`):<br>1. Attendance Clock-In/Out & SLA API (`SIT_USR_API_019–030`: 12 TCs)<br>2. Activity Log & IDOR Security API (`SIT_USR_API_031–038`: 8 TCs)<br>3. Corrections API (`SIT_USR_API_039–043`: 5 TCs)<br>4. Overtimes API (`SIT_USR_API_050–052`: 3 TCs) | **88 TCs** (~50.3%) |
| **TOTAL** | **120 Web UI TCs** | **55 API TCs** | **175 TCs** |

### Pemetaan Endpoint SIT ke Endpoint Backend FeathersJS Riil
Dalam [SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/SIT_Scenario_Magang_Hadir.xlsx), kolom Endpoint ditulis secara logis (`/api/...`). Pada implementasi `Constants.java`, `UserGroupAApiTest`, dan `UserGroupBApiTest`, request diarahkan ke path FeathersJS terverifikasi pada `https://api-dev.dikahadir.com/hadir-api-magang`:
- Login & JWT → `POST /authentication`
- Profil, Cek NIK, Register, Ubah Password → `/users` (`GET /users`, `PATCH /users/{id}`, endpoint registrasi/reset password sesuai inspeksi bundle live)
- Presensi & Log Aktivitas → `/activity` (dan endpoint absen clock-in/clock-out hasil inspeksi live)
- Koreksi Absen → `/activity-correction` (`GET`, `POST`, `PATCH`, `DELETE`)
- Izin Off / Terlambat / Pulang Cepat & Cuti / Sakit → `/off-permit`, `/late-permit`, `/early-leave`, `/project-leave`
- Lembur → `/overtime` (`GET`, `POST`, `PATCH`)

---

## Penanganan Skenario Khusus (Defect Verification, CDP & Testdata)

1. **Verifikasi Defect Aktual (`[Defect/Negatif]`)**:
   - **Broken Image setelah Submit/Upload**: `SIT_USR_UI_024` (Register), `042` (Absen Masuk), `049` (Absen Pulang), `056` (Log Absen), `091` (Lampiran Lembur), `106` & `107` (Surat Sakit). Assertion memverifikasi perilaku aktual sesuai kolom *Hasil yang Diharapkan* di SIT (misal preview muncul sebelum submit, lalu `naturalWidth == 0` / HTTP 404 setelah submit atau download gagal) serta dicatat di `ExtentTestManager.logInfo(...)`.
   - **Tanpa Batas Maksimal Cuti**: `SIT_USR_UI_101` (> 30 hari / 60 hari) dan `SIT_USR_API_047` memverifikasi perilaku defect validasi kuota cuti.
2. **Simulasi Browser Permission & Geolocation via CDP**:
   - `SIT_USR_UI_043`: `Emulation.setGeolocationOverride` dengan koordinat di luar radius geofence kantor (> 500m).
   - `SIT_USR_UI_044`: `Browser.setPermission` untuk memblokir `videoCapture` (kamera).
   - `SIT_USR_UI_045`: `Browser.setPermission` untuk memblokir `geolocation` (lokasi).
3. **File Testdata (`src/test/resources/testdata/`)**:
   - Dummy gambar valid (`.jpg` / `.png`) untuk foto wajah registrasi, surat dokter, dan SPK lembur.
   - File non-gambar (`.pdf` / `.txt` / `.mp3` / `.zip`) untuk negative upload (`023`, `109`).
   - File berukuran `> 5MB` untuk validasi batas ukuran maksimal (`110`).
4. **Standar Pelaporan & Penandaan Test Case**:
   - Setiap `@Test` wajib mencantumkan ID SIT pada `description` (contoh: `@Test(priority = 1, description = "SIT_USR_UI_001 - Login User Magang dengan kredensial valid", groups = {"smoke", "auth"})`) dan memanggil `ExtentTestManager.logInfo("SIT_USR_UI_001: ...")`.
   - `TestListener.java` otomatis men-tag kategori berdasarkan nama package: `.deny.` → `"Tester Deny"`, `.aji.` → `"Tester Aji"`, `.api.` → `"REST Assured API"`, `.tests.` → `"Selenium Web UI"`.

---

## Struktur Direktori Target (`final-project-karyawan/`)

```text
final-project-karyawan/
├── pom.xml                                  # Java 21, Selenium 4.52.0, REST Assured 6.3.0, TestNG 7.12.0, ExtentReports 5.1.2
├── .gitignore
├── .gitattributes
├── .editorconfig
├── README.md
├── reference.md
├── plan.md
├── tasklist.md
├── WebKaryawanPrompt.md
├── SIT_Scenario_Magang_Hadir.xlsx
├── docs/
│   └── SIT_Scenario_Magang_Hadir.xlsx       # Salinan rujukan matriks SIT di folder docs
├── reports/
│   ├── ExtentReport.html                    # Dark Theme HTML + Base64 screenshots
│   └── screenshots/
└── src/
    ├── main/java/com/juaracoding/magang/
    │   ├── drivers/
    │   │   └── DriverFactory.java           # ThreadLocal WebDriver, headless=true default, fake camera & GPS prefs
    │   ├── pages/
    │   │   ├── BasePage.java                # Explicit wait 15s, click fallback JS, type, getText, isDisplayed, scroll
    │   │   └── user/
    │   │       ├── deny/                    # UserLoginPage, RegisterPage, ResetPasswordPage, PermitPage, LeaveAndSickPage
    │   │       └── aji/                     # AttendancePage, ActivityHistoryPage, CorrectionPage, OvertimePage, UserProfilePage
    │   ├── reports/
    │   │   ├── ExtentManager.java           # Singleton ExtentSparkReporter (Dark Theme)
    │   │   └── ExtentTestManager.java       # ThreadLocal logging + logInfo/logPass/logFail
    │   └── utils/
    │       ├── Constants.java               # Base64 credentials, URLs, API_BASE_URI, endpoints, VALID_NIK_POOL
    │       ├── SecurityUtil.java            # Base64 decoder/encoder
    │       └── ScreenshotUtil.java          # Base64 & PNG screenshot capture
    └── test/
        ├── java/com/juaracoding/magang/
        │   ├── BaseTest.java                # @BeforeMethod / @AfterMethod WebDriver lifecycle (default headless=true)
        │   ├── listeners/
        │   │   └── TestListener.java        # Auto-tag Deny/Aji, UI/API, Hybrid, Base64 screenshot on failure
        │   ├── api/
        │   │   ├── BaseApiTest.java         # REST Assured + useRelaxedHTTPSValidation() + /authentication
        │   │   ├── helper/
        │   │   │   └── AdminApiHelper.java  # Hybrid @Before seeding + Admin PATCH approval/rejection helper
        │   │   └── user/
        │   │       ├── UserGroupAApiTest.java # Tester Deny (27 API TCs)
        │   │       └── UserGroupBApiTest.java # Tester Aji (28 API TCs)
        │   └── tests/user/
        │       ├── deny/                    # UserLoginTest, RegisterTest, ResetPasswordTest, PermitTest, LeaveAndSickTest
        │       └── aji/                     # AttendanceTest, ActivityHistoryTest, CorrectionTest, OvertimeTest, UserProfileTest
        └── resources/
            ├── testng.xml                   # Master Suite Karyawan (175 TCs, headless=true)
            ├── testng-deny.xml              # Suite khusus Tester Deny (87 TCs, headless=true)
            ├── testng-aji.xml               # Suite khusus Tester Aji (88 TCs, headless=true)
            └── testdata/                    # Dummy selfie .jpg/.png, file >5MB, file non-image (.pdf/.txt/.mp3)
```

---

## Git Workflow (Opsi B — Zero Merge Conflict)
1. Kerjakan **Fase 2–3** (Maven setup & Core Foundation) pada branch `main` terlebih dahulu.
2. Untuk **Fase 4–5** (POM + Test Class), buat feature branch terpisah dari `main`:
   - **Tester Deny**: `feature/deny-user-login`, `feature/deny-user-register`, `feature/deny-user-reset-password`, `feature/deny-user-permit`, `feature/deny-user-leave-sick`, `feature/deny-user-api-a`
   - **Tester Aji**: `feature/aji-user-attendance`, `feature/aji-user-activity`, `feature/aji-user-correction`, `feature/aji-user-overtime`, `feature/aji-user-profile`, `feature/aji-user-api-b`
3. Dilarang mengubah file pondasi bersama (`BasePage`, `BaseTest`, `BaseApiTest`, `Constants`, `DriverFactory`, `testng*.xml`) di dalam branch fitur agar merge ke `main` bebas konflik.

---

## Verification Plan
- **Fase 1 (Review)**: Verifikasi keselarasan `plan.md` dan `tasklist.md` terhadap [WebKaryawanPrompt.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/WebKaryawanPrompt.md), [SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/SIT_Scenario_Magang_Hadir.xlsx), dan [reference.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/reference.md).
- **Fase 2–3**: `mvn clean test-compile` memastikan tidak ada error kompilasi Java 21 dan tidak ada kredensial plaintext.
- **Fase 4–5**: Verifikasi live locator & API payload pada `magang.dikahadir.com/absen/login` dan `api-dev.dikahadir.com/hadir-api-magang`, lalu uji API suites `mvn test -Dtest=UserGroupAApiTest,UserGroupBApiTest`.
- **Fase 6**: Jalankan suite runner headless:
  - `mvn test -DsuiteXmlFile=src/test/resources/testng-deny.xml` (87 TCs)
  - `mvn test -DsuiteXmlFile=src/test/resources/testng-aji.xml` (88 TCs)
  - `mvn clean test` (`testng.xml` — 175 TCs, `headless=true`)
- **Fase 7**: Validasi `reports/ExtentReport.html` (Dark Theme, filter `Tester Deny` / `Tester Aji` / `Selenium Web UI` / `REST Assured API`, Base64 screenshots), sinkronisasi status eksekusi pada `SIT_Scenario_Magang_Hadir.xlsx`, dan lengkapi `README.md` + `walkthrough.md`.

