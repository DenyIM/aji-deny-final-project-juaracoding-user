# 📋 Implementation Plan: Enterprise Karyawan Automation Framework (`magang.dikahadir.com`) — SIT v3

## Goal Description
Membangun **Enterprise Web UI & API Test Automation Framework** khusus untuk **portal Karyawan / User Magang (Mobile Mirroring)** pada platform **[HADIR Mobile Mirroring](https://magang.dikahadir.com/absen/login)** yang **100% selaras** secara arsitektur, versi dependensi, standar keamanan kredensial, pola pelaporan, dan pembagian kerja tim dengan repositori **Web Admin (`final-project-admin`)** serta acuan standar industri pada [reference.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/reference.md).

Kerangka kerja ini menggunakan **Java 21 LTS**, **Apache Maven**, **Selenium WebDriver `4.52.0`** (default `headless = true`), **REST Assured `6.3.0`**, **TestNG `7.12.0`**, **ExtentReports `5.1.2`** (Dark Theme + Base64 failure screenshot), **Apache POI `5.2.5`**, dan **Jackson Databind `2.17.2`**, mencakup **195 Test Cases User (SIT v3)** (**140 Web UI** + **55 REST Assured API**) dengan *Single Source of Truth* pada [docs/SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/docs/SIT_Scenario_Magang_Hadir.xlsx) dan [WebKaryawanPrompt.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/WebKaryawanPrompt.md).

---

## Ringkasan Keputusan Teknis & Pembaruan SIT v3

> [!IMPORTANT]
> **1. Tech Stack, Versi Tools & Kebijakan Headless**
> - **Runtime & Build**: Java 21 LTS, Apache Maven `3.9+`, `maven-compiler-plugin` `3.13.0`, `maven-surefire-plugin` `3.5.0`.
> - **Libraries**: Selenium WebDriver `4.52.0`, REST Assured `6.3.0` (`rest-assured`, `json-path`, `json-schema-validator`), TestNG `7.12.0`, ExtentReports `5.1.2`, Apache POI `5.2.5` (`poi`, `poi-ooxml`), Jackson Databind `2.17.2`, Commons IO `2.16.1`, SLF4J `2.0.16`.
> - **Default Headless**: Chrome **WAJIB** default `headless = true` pada `DriverFactory.java`, `BaseTest.java`, `pom.xml`, `testng.xml`, `testng-deny.xml`, dan `testng-aji.xml`, namun dapat dioverride kapan saja melalui parameter CLI `-Dheadless=false`.
> - **Simulasi Kamera & GPS (Mobile Mirroring)**: `DriverFactory.java` mengaktifkan `--use-fake-ui-for-media-stream`, `--use-fake-device-for-media-stream`, serta Chrome `prefs` untuk `geolocation = 1` (Allow) dan `media_stream_camera = 1` (Allow). Untuk skenario negatif GPS di luar radius atau permission diblokir (`SIT_USR_UI_043`, `044`, `045`) serta E2E geofence (`SIT_USR_UI_135`), digunakan **Chrome DevTools Protocol (CDP)** via `HasCdp#executeCdpCommand` (`Emulation.setGeolocationOverride` & `Browser.setPermission`).

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
> **3. Pembaruan Akurasi Rute, Form, Infinite Scroll & Backend API FeathersJS (SIT v3)**
> - **Backend API Base URI**: `API_BASE_URI = "https://api-dev.dikahadir.com/hadir-api-magang"` (FeathersJS) dengan `RestAssured.useRelaxedHTTPSValidation()` dan `.relaxedHTTPSValidation()`.
> - **Reset Password (`/absen/forgot-password` / `/absen/reset-password-request` & `/absen/reset-password`, `SIT_USR_UI_026–033`)**: Alur menyertakan input **kode OTP**, password baru, dan konfirmasi password (API: `POST /auth-management` dengan action `sendResetPassword` / `resetPwd`).
> - **Ubah Password (`/apps/absent/change-password`, `SIT_USR_UI_119–125`)**: Form **HANYA** memiliki field **Password Baru** (`password`) dan **Confirm Password** (`passwordConfirmation`) beserta ikon mata Show/Hide — **tidak ada** field `Password Lama` (API: `PATCH /users/{id}`).
> - **Download Report (`/apps/absent/reports/share_report`, `SIT_USR_UI_126–129`)**: Modul untuk **Tester Deny** (`DownloadReportPage` & `DownloadReportTest`) yang terhubung ke `POST /share-report`.
> - **Rute Alias Client**: Portal juga mendukung rute `/apps/absent/leave-client` (Cuti) dan `/apps/absent/overtime-client` (Lembur) di samping `/apps/absent/leave` dan `/apps/absent/overtime`.
> - **Infinite Scroll Pagination Mobile (`$skip = 10 * page`)**: Berbeda dengan Admin yang memakai MUI `TablePagination` desktop, seluruh **6 halaman daftar riwayat User** menggunakan mekanisme *Infinite Scroll* (`onScroll` / `hasMore`):
>   - `Presensi-Log Absen` (`SIT_USR_UI_063`)
>   - `Pengajuan-Koreksi Absen` (`SIT_USR_UI_075`)
>   - `Pengajuan-Izin` (`SIT_USR_UI_086`)
>   - `Pengajuan-Lembur` (`SIT_USR_UI_098`)
>   - `Pengajuan-Cuti` (`SIT_USR_UI_107`)
>   - `Pengajuan-Sakit` (`SIT_USR_UI_117`)
> - **11 Skenario Integrasi Lintas Modul E2E (`SIT_USR_UI_130–140`)**: 5 TCs untuk Tester Deny (`UserCrossModuleDenyE2ETest`: `130–134`) dan 6 TCs untuk Tester Aji (`UserCrossModuleAjiE2ETest`: `135–140`, termasuk `SIT_USR_UI_140` verifikasi kuota `Jumlah Cuti=12` pendaftaran Admin pada `/apps/absent/leave`) memakai `AdminApiHelper` + Selenium.

---

## Arsitektur Sistem

```mermaid
flowchart TD
    subgraph Execution & Runners
        TX1[testng.xml - Master 195 TCs]
        TX2[testng-deny.xml - 99 TCs]
        TX3[testng-aji.xml - 96 TCs]
        TL[TestListener - Auto Tag Deny/Aji & UI/API]
    end

    subgraph Core Thread-Safe & Security Engine
        SEC[SecurityUtil - Base64 Decode]
        CONST[Constants - URLs, Alias Routes, FeathersJS Endpoints, Base64]
        DF[DriverFactory - ThreadLocal, headless=true, Fake Camera & GPS]
        BP[BasePage - Explicit Wait 15s, HasCdp, isImageBroken]
        BT[BaseTest - @BeforeMethod / @AfterMethod]
        BAPI[BaseApiTest - useRelaxedHTTPSValidation & JWT]
        HYB[AdminApiHelper - Hybrid @Before Seeding & Admin Approval]
    end

    subgraph Tester Deny Scope - 99 TCs
        D_POM[pages.user.deny: UserLoginPage, RegisterPage, ResetPasswordPage, PermitPage, LeaveAndSickPage, DownloadReportPage]
        D_UI[tests.user.deny: 72 Web UI TCs incl. InfiniteScroll 086/107/117 & E2E 130-134]
        D_API[UserGroupAApiTest: 27 API TCs]
    end

    subgraph Tester Aji Scope - 96 TCs
        A_POM[pages.user.aji: AttendancePage, ActivityHistoryPage, CorrectionPage, OvertimePage, UserProfilePage]
        A_UI[tests.user.aji: 68 Web UI TCs incl. InfiniteScroll 063/075/098 & E2E 135-140]
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
    HYB -->|@Before Seed + Admin PATCH / E2E| D_UI & A_UI
    TL -->|On Failure| SU --> EM
    D_UI & D_API & A_UI & A_API --> ETM --> EM --> REP
```

---

## Pembagian Tugas Adil 2 Tester (SIT v3 — Total 195 TCs)

Mengacu langsung pada sheet `Tester Deny (Admin & User)`, `Tester Aji (Admin & User)`, dan `Summary & Collaboration Guide` di [docs/SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/docs/SIT_Scenario_Magang_Hadir.xlsx):

| Tester | Cakupan Web UI (`140 TCs`) | Cakupan REST Assured API (`55 TCs`) | Total & Bobot |
| :--- | :--- | :--- | :---: |
| **👤 Tester Deny** | **72 Web UI TCs** (`pages.user.deny` & `tests.user.deny`):<br>1. `UserLoginTest` — `Autentikasi-Login` (`SIT_USR_UI_001–012`: 12 TCs — *utuh & lulus di branch `feature/deny-user-login`*)<br>2. `RegisterTest` — `Autentikasi-Register` (`SIT_USR_UI_013–025`: 13 TCs)<br>3. `ResetPasswordTest` — `Autentikasi-Reset Password` (`SIT_USR_UI_026–033`: 8 TCs, termasuk OTP)<br>4. `PermitTest` — `Pengajuan-Izin` (`SIT_USR_UI_076–086`: 11 TCs — *Hybrid* + *InfiniteScroll* `086`)<br>5. `LeaveAndSickTest` — `Pengajuan-Cuti & Sakit` (`SIT_USR_UI_099–117`: Cuti `099–107` [9 TCs, `/apps/absent/leave-client` + *InfiniteScroll* `107`] + Sakit `108–117` [10 TCs, `/apps/absent/sick` + *InfiniteScroll* `117`] = 19 TCs)<br>6. `DownloadReportTest` — `Profil-Download Report` (`SIT_USR_UI_126–129`: 4 TCs `/apps/absent/reports/share_report`)<br>7. `UserCrossModuleDenyE2ETest` — `Integrasi Lintas Modul (Hybrid API+UI)` (`SIT_USR_UI_130–134`: 5 TCs) | **27 API TCs** (`UserGroupAApiTest`):<br>1. Auth & `/auth-management` Reset Password (`SIT_USR_API_001–010`: 10 TCs)<br>2. Registration & NIK Check (`SIT_USR_API_011–018`: 8 TCs)<br>3. Leaves / Izin / Cuti / Sakit (`SIT_USR_API_044–049`: 6 TCs)<br>4. Profile & `PATCH /users/{id}` Change Password (`SIT_USR_API_053–055`: 3 TCs) | **99 TCs** (50.8%) |
| **👤 Tester Aji** | **68 Web UI TCs** (`pages.user.aji` & `tests.user.aji`):<br>1. `AttendanceTest` — `Presensi-Beranda` (`SIT_USR_UI_034–053`: 20 TCs — *Fake Camera/GPS & CDP*)<br>2. `ActivityHistoryTest` — `Presensi-Log Absen` (`SIT_USR_UI_054–064`: 11 TCs + *InfiniteScroll* `063`)<br>3. `CorrectionTest` — `Pengajuan-Koreksi Absen` (`SIT_USR_UI_065–075`: 11 TCs — *Hybrid* + *InfiniteScroll* `075`)<br>4. `OvertimeTest` — `Pengajuan-Lembur` (`SIT_USR_UI_087–098`: 12 TCs `/apps/absent/overtime-client` — *Hybrid* + *InfiniteScroll* `098`)<br>5. `UserProfileTest` — `Profil-Menu Utama & Ubah Password` (`SIT_USR_UI_118–125`: 8 TCs — tanpa Password Lama)<br>6. `UserCrossModuleAjiE2ETest` — `Integrasi Lintas Modul (Hybrid API+UI)` (`SIT_USR_UI_135–140`: 6 TCs, termasuk E2E Kuota Cuti `140`) | **28 API TCs** (`UserGroupBApiTest`):<br>1. Attendance Clock-In/Out & SLA (`SIT_USR_API_019–030`: 12 TCs)<br>2. Activity Log & IDOR Security (`SIT_USR_API_031–038`: 8 TCs)<br>3. Corrections (`SIT_USR_API_039–043`: 5 TCs)<br>4. Overtimes (`SIT_USR_API_050–052`: 3 TCs) | **96 TCs** (49.2%) |
| **TOTAL** | **140 Web UI TCs** | **55 API TCs** | **195 TCs** |

### Sinkronisasi 55 Endpoint API User ke Backend FeathersJS Riil (`https://api-dev.dikahadir.com/hadir-api-magang`)
Seluruh 55 skenario API pada SIT v3 disinkronkan langsung ke path FeathersJS terverifikasi dengan `SecurityUtil.decode(...)` tanpa password plaintext:
- `SIT_USR_API_001–006`, `010` → `POST /authentication`
- `SIT_USR_API_007–009` → `POST /auth-management` (`action: "sendResetPassword"` / `"resetPwd"`)
- `SIT_USR_API_011–013` → `GET /users` (cek ketersediaan NIK)
- `SIT_USR_API_014–018` → `POST /users` (registrasi mandiri & validasi input/XSS)
- `SIT_USR_API_019–025`, `028–038` → `POST / GET / PATCH / DELETE /activity` (Clock-In, filter tanggal, log aktivitas, SLA `<3000ms`, IDOR check `038`)
- `SIT_USR_API_026–027` → `POST /activity/request-activity` (Clock-Out)
- `SIT_USR_API_039–043` → `POST / GET / DELETE /activity-correction`
- `SIT_USR_API_044` → `POST /off-permit` (serta dukungan `/late-permit`, `/early-leave`)
- `SIT_USR_API_045–047` → `POST /project-leave` (termasuk Known Defect `047`)
- `SIT_USR_API_048–049` → `POST / GET /sick`
- `SIT_USR_API_050–052` → `POST / GET /overtime`
- `SIT_USR_API_053–055` → `GET /users`, `PATCH /users/{id}` (profil & ubah password)
- Pendukung Hybrid / Download Report / E2E → `GET/POST /share-report`, `POST /upload`, `GET /location-point`, `GET/POST /business-trips`.

### Rincian 11 Skenario Integrasi Lintas Modul E2E (`SIT_USR_UI_130–140`)
- **Tester Deny (`UserCrossModuleDenyE2ETest` — `SIT_USR_UI_130–134`)**:
  - `SIT_USR_UI_130`: Registrasi mandiri disetujui Admin (`PATCH /users/{id}` active) → Karyawan berhasil login di `/absen/login`.
  - `SIT_USR_UI_131`: Admin menonaktifkan akun Karyawan (`@Before`) → percobaan login di `/absen/login` ditolak (`@After` aktifkan kembali).
  - `SIT_USR_UI_132`: Admin menyetujui Izin Off (`POST /off-permit` + `PATCH /off-permit/{id}` Approved) → badge `Approved` tampil di `/apps/absent/permit`.
  - `SIT_USR_UI_133`: Admin menolak Izin Off dengan alasan → status `Rejected` & `Alasan Ditolak` tampil di `/apps/absent/permit`.
  - `SIT_USR_UI_134`: Admin membatalkan Cuti Karyawan → status & Sisa Cuti di `/apps/absent/leave` tersinkronisasi.
- **Tester Aji (`UserCrossModuleAjiE2ETest` — `SIT_USR_UI_135–140`)**:
  - `SIT_USR_UI_135`: Admin mengatur titik Absen Point geofence (`GET /location-point`) → CDP Geolocation mengikuti koordinat Admin untuk Clock-In WFO di `/apps/absent`.
  - `SIT_USR_UI_136`: Karyawan Clock-In di `/apps/absent` → data presensi langsung tampil di `/apps/absent/activity` & terverifikasi via `GET /activity`.
  - `SIT_USR_UI_137`: Admin menyetujui Koreksi Absen (`POST /activity-correction` + `PATCH` Approved) → badge `Approved` tampil di `/apps/absent/correction`.
  - `SIT_USR_UI_138`: Admin menolak Koreksi Absen dengan alasan → status `Rejected` & catatan penolakan tampil di `/apps/absent/correction`.
  - `SIT_USR_UI_139`: Admin menyetujui/menolak Lembur (`POST /overtime` + `PATCH`) → status di `/apps/absent/overtime` berubah sesuai keputusan Admin.
  - `SIT_USR_UI_140` **`[Known Defect]`**: Karyawan baru yang didaftarkan Admin dengan `Jumlah Cuti=12` (`leave_amount=12`) → cek kuota & ajukan Cuti di `/apps/absent/leave` (`Actual Result / Defect Notes`: kuota tidak masuk ke `project-leave` sehingga gagal mengajukan cuti).

---

## Kebijakan Penanganan Known Defect, CDP & Standar Pelaporan (SIT v3)

1. **Kebijakan Known Defect (`Expected Result` Standar + `Actual Result / Defect Notes` + `Severity`/`Priority`)**:
   - Pada SIT v3, kolom `Expected Result` selalu berisi **perilaku standar/seharusnya** dari aplikasi, sedangkan perilaku bug live dicatat pada kolom **`Actual Result / Defect Notes`** dengan **`Status = Ready (Known Defect)`** beserta pemisahan **`Severity`** (`Critical`/`High`/`Medium`/`Low`) dan **`Priority`** (`P1`/`P2`/`P3`).
   - Kasus Known Defect pada domain Karyawan:
     - **Broken Image setelah Submit/Upload**: `SIT_USR_UI_024` (Register), `042` (Absen Masuk), `049` (Absen Pulang), `056` (Log Absen), `093` (Lampiran Lembur), `110` & `111` (Surat Sakit).
     - **Tidak Ada Batas Maksimal Cuti**: `SIT_USR_UI_104` (> 30 hari / 60 hari) dan `SIT_USR_API_047`.
     - **Kuota `Jumlah Cuti` Pendaftaran User Tidak Sinkron (`SIT_USR_UI_140`)**: `leave_amount=12` dari Pendaftaran User Admin tidak tersinkronisasi ke `project-leave` user baru.
   - Assertion test memverifikasi **Expected Result standar** dan secara eksplisit mencatat temuan defect live beserta `Severity`/`Priority` melalui `ExtentTestManager.logInfo("[Known Defect] ...")`.
2. **Simulasi Browser Permission & Geolocation via CDP (`HasCdp`)**:
   - `SIT_USR_UI_043`: `overrideGeolocation(...)` dengan koordinat di luar radius geofence kantor (> 500m).
   - `SIT_USR_UI_044`: `setBrowserPermission("videoCapture", "denied")`.
   - `SIT_USR_UI_045`: `setBrowserPermission("geolocation", "denied")`.
3. **File Testdata (`src/test/resources/testdata/`)**:
   - Dummy gambar valid (`valid_selfie.jpg`, `valid_selfie.png`), file non-gambar (`invalid_upload.pdf`, `invalid_upload.txt`, `invalid_upload.mp3`), dan file berukuran `> 5MB` (`oversized_image.jpg`).
4. **Standar Pelaporan & Penandaan Test Case**:
   - Setiap `@Test` wajib mencantumkan ID SIT pada `description` dan memanggil `ExtentTestManager.logInfo("SIT_USR_...: ...")`.
   - `TestListener.java` otomatis men-tag kategori: `.deny.` / `GroupA` → `"Tester Deny"`, `.aji.` / `GroupB` → `"Tester Aji"`, `.api.` → `"REST Assured API"`, `.tests.` → `"Selenium Web UI"`.

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
├── docs/
│   └── SIT_Scenario_Magang_Hadir.xlsx       # Single Source of Truth Matriks SIT v3 (195 User TCs: 140 UI + 55 API)
├── reports/
│   ├── ExtentReport.html                    # Dark Theme HTML + Base64 screenshots
│   └── screenshots/
└── src/
    ├── main/java/com/juaracoding/magang/
    │   ├── drivers/
    │   │   └── DriverFactory.java           # ThreadLocal WebDriver, headless=true default, fake camera & GPS prefs
    │   ├── pages/
    │   │   ├── BasePage.java                # Explicit wait 15s, JS fallback click, HasCdp, isImageBroken
    │   │   └── user/
    │   │       ├── deny/                    # UserLoginPage, RegisterPage, ResetPasswordPage, PermitPage, LeaveAndSickPage, DownloadReportPage
    │   │       └── aji/                     # AttendancePage, ActivityHistoryPage, CorrectionPage, OvertimePage, UserProfilePage
    │   ├── reports/
    │   │   ├── ExtentManager.java           # Singleton ExtentSparkReporter (Dark Theme)
    │   │   └── ExtentTestManager.java       # ThreadLocal logging + logInfo/logPass/logFail
    │   └── utils/
    │       ├── Constants.java               # Base64 credentials, URLs + alias routes, FeathersJS endpoints, VALID_NIK_POOL
    │       ├── SecurityUtil.java            # Base64 decoder/encoder
    │       └── ScreenshotUtil.java          # Base64 & PNG screenshot capture
    └── test/
        ├── java/com/juaracoding/magang/
        │   ├── BaseTest.java                # @BeforeMethod / @AfterMethod WebDriver lifecycle (default headless=true)
        │   ├── listeners/
        │   │   └── TestListener.java        # Auto-tag Deny/Aji, UI/API, Base64 screenshot on failure
        │   ├── api/
        │   │   ├── BaseApiTest.java         # REST Assured + useRelaxedHTTPSValidation() + /authentication
        │   │   ├── helper/
        │   │   │   └── AdminApiHelper.java  # Hybrid @Before seeding + Admin PATCH approval/rejection + E2E helper
        │   │   └── user/
        │   │       ├── UserGroupAApiTest.java # Tester Deny (27 API TCs)
        │   │       └── UserGroupBApiTest.java # Tester Aji (28 API TCs)
        │   └── tests/user/
        │       ├── deny/                    # UserLoginTest, RegisterTest, ResetPasswordTest, PermitTest, LeaveAndSickTest, DownloadReportTest, UserCrossModuleDenyE2ETest
        │       └── aji/                     # AttendanceTest, ActivityHistoryTest, CorrectionTest, OvertimeTest, UserProfileTest, UserCrossModuleAjiE2ETest
        └── resources/
            ├── testng.xml                   # Master Suite Karyawan (195 TCs, headless=true)
            ├── testng-deny.xml              # Suite khusus Tester Deny (99 TCs, headless=true)
            ├── testng-aji.xml               # Suite khusus Tester Aji (96 TCs, headless=true)
            └── testdata/                    # Dummy selfie .jpg/.png, file >5MB, file non-image (.pdf/.txt/.mp3)
```

---

## Git Workflow (Opsi B — Zero Merge Conflict)
1. Kerjakan **Fase 2–3** (Core Foundation) dan sinkronisasi SIT v3 pada branch `main` (dengan `docs/SIT_Scenario_Magang_Hadir.xlsx` sebagai satu-satunya file SIT).
2. Untuk **Fase 4–5** (POM + Test Class), buat feature branch terpisah dari `main`:
   - **Tester Deny**: `feature/deny-user-login` *(sudah selesai & lulus 12/12 test, menunggu review/merge)*, `feature/deny-user-register`, `feature/deny-user-reset-password`, `feature/deny-user-permit`, `feature/deny-user-leave-sick`, `feature/deny-user-download-report`, `feature/deny-user-e2e`, `feature/deny-user-api-a`
   - **Tester Aji**: `feature/aji-user-attendance`, `feature/aji-user-activity`, `feature/aji-user-correction`, `feature/aji-user-overtime`, `feature/aji-user-profile`, `feature/aji-user-e2e`, `feature/aji-user-api-b`
3. Dilarang mengubah file pondasi bersama (`BasePage`, `BaseTest`, `BaseApiTest`, `Constants`, `DriverFactory`, `testng*.xml`) di dalam branch fitur agar Pull Request ke `main` bebas konflik.

---

## Verification Plan
- **Sinkronisasi `main` (SIT v3)**: `mvn clean test-compile` memastikan tidak ada error kompilasi Java 21 dan tidak ada kredensial plaintext.
- **Fase 4–5**: Verifikasi live locator & API payload pada `magang.dikahadir.com/absen/login` dan `api-dev.dikahadir.com/hadir-api-magang`, lalu uji API suites `mvn test -Dtest=UserGroupAApiTest,UserGroupBApiTest`.
- **Fase 6**: Jalankan suite runner headless setelah seluruh branch fitur di-merge ke `main`:
  - `mvn test -DsuiteXmlFile=src/test/resources/testng-deny.xml` (99 TCs)
  - `mvn test -DsuiteXmlFile=src/test/resources/testng-aji.xml` (96 TCs)
  - `mvn clean test` (`testng.xml` — 195 TCs, `headless=true`)
- **Fase 7**: Validasi `reports/ExtentReport.html` (Dark Theme, filter `Tester Deny` / `Tester Aji` / `Selenium Web UI` / `REST Assured API`, Base64 screenshots), sinkronisasi status eksekusi pada `docs/SIT_Scenario_Magang_Hadir.xlsx`, dan lengkapi `README.md` + `walkthrough.md`.
