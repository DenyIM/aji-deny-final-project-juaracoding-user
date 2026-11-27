# 🤖 Panduan & Master Prompt AI Agent: Automation Testing Web Karyawan (`magang.dikahadir.com`)

> **Tujuan Dokumen**: Berikan file ini sebagai acuan utama kepada Agent AI pada **repositori baru khusus Web Karyawan (Mobile Mirroring)** agar arsitektur, versi library, pola keamanan Base64, integrasi REST Assured, mekanisme Hybrid API + UI, pembagian kerja 2 tester, dan standar pelaporan **100% selaras** dengan repositori **Web Admin (`final-project-admin`)**.

---

## 1. Ringkasan Proyek & Target Aplikasi (SUT)

| Informasi | Nilai / Spesifikasi |
| :--- | :--- |
| **Aplikasi Target (SUT)** | [HADIR Mobile Mirroring](https://magang.dikahadir.com/absen/login) |
| **Domain Pengujian** | Portal **Karyawan / User Magang** (Modul Fitur Mobile + Kehadiranmu + Leader/Staff + InfiniteScroll + E2E Lintas Modul, **304 Test Cases (SIT v5.1)**: **160 Web UI** + **144 REST Assured API**) |
| **Bahasa Pemrograman** | **Java 21 LTS** |
| **Web UI Automation** | **Selenium WebDriver `4.52.0`** (Chrome default **`headless = true`**, W3C Standard, Selenium Manager) |
| **API & Hybrid Engine** | **REST Assured `6.3.0`** (`rest-assured`, `json-path`, `json-schema-validator`) |
| **Test Runner** | **TestNG `7.12.0`** |
| **Reporting Engine** | **ExtentReports `5.1.2`** (`ExtentSparkReporter` Dark Theme + Base64 Failure Screenshot) |
| **Excel & JSON Utilities** | **Apache POI `5.2.5`** (`poi`, `poi-ooxml`), **Jackson Databind `2.17.2`**, **Commons IO `2.16.1`**, **SLF4J `2.0.16`** |
| **Build & Runner Plugin** | Apache Maven `3.9+`, `maven-compiler-plugin` `3.13.0`, `maven-surefire-plugin` `3.5.0` |

---

## 2. Kredensial & Keamanan Base64 (Wajib Diterapkan)

**ATURAN KERAS GITHUB**: Dilarang keras menyimpan password dalam bentuk plaintext di dalam file `.java`, komentar kode, `testng.xml`, atau dokumentasi yang di-commit ke repositori publik. Gunakan `SecurityUtil.decode(...)` berbasis `java.util.Base64` di runtime.

| Peran Akun | Email / NIK | Konstanta Base64 di `Constants.java` | Penggunaan |
| :--- | :--- | :--- | :--- |
| **Karyawan Utama** | `hadirsqa1@gmail.com` (NIK: `D8240010`, Nama: `Hadir SQA Testing 1`, Role: Leader/Supervisor) | `USER_PASSWORD_ENCODED = "U1FBQEhhZGlyMTIzNDU="` | Login UI `/absen/login`, API User Token, & Pengujian Fitur Karyawan |
| **Admin (Hybrid Seeder)** | `admin@hadir.com` | `HYBRID_ADMIN_PASSWORD_ENCODED = "TWFnYW5nU1FBX0pDQDEyMw=="` | **Khusus tahap `@Before` / `@After` Hybrid API+UI** (misal Admin menyetujui/menolak pengajuan lewat API agar User UI dapat memverifikasi badge `Approved`/`Rejected` beserta alasan penolakan) |
| **Pool NIK Registrasi** | `D7231978`, ..., `F1240001`, `D6240005`, `D7240031`, `D7240029` | Array `VALID_NIK_POOL` di `Constants.java` (34 NIK belum terdaftar, mengecualikan `D8240010`) | Pengujian registrasi mandiri `/absen/register` |

---

## 3. Referensi Backend API Riil (Hasil Inspeksi Aplikasi Live)

> [!IMPORTANT]
> Hasil inspeksi *Next.js bundle* dan pengujian langsung terhadap server aplikasi menunjukkan:
> - **Backend API Base URI sebenarnya** adalah **`https://api-dev.dikahadir.com/hadir-api-magang`** (framework **FeathersJS**), bukan `https://magang.dikahadir.com/api/...`.
> - Sertifikat TLS `api-dev.dikahadir.com` menggunakan rantai sertifikat staging, sehingga `BaseApiTest` **WAJIB** memanggil **`RestAssured.useRelaxedHTTPSValidation()`** (dan `.relaxedHTTPSValidation()` pada `RequestSpecBuilder`).
> - Endpoint autentikasi riil adalah **`POST /authentication`** (mengembalikan **HTTP 201** dengan field `accessToken` dan `user.id`).

### Daftar Endpoint Terverifikasi pada `https://api-dev.dikahadir.com/hadir-api-magang`
| Modul | HTTP Method & Path | Catatan Penggunaan (API Test & Hybrid) |
| :--- | :--- | :--- |
| **Autentikasi** | `POST /authentication` | Payload JSON: `{"email": "...", "password": "...", "strategy": "local"}` → Ambil `accessToken` & `user.id` |
| **Profil & User** | `GET /users`, `PATCH /users/{id}`, `GET /users?upliner_id={id}` | Filter `nik=D8240010` atau `email=hadirsqa1@gmail.com`; update profil/password; ambil daftar staff bawahan Leader |
| **Aktivitas & Presensi** | `GET /activity`, `GET /user-summary` | Riwayat presensi (`created_at[$gte]`/`[$lte]`) & statistik ringkasan `Kehadiranmu` |
| **Koreksi Absen** | `GET /activity-correction`, `POST /activity-correction`, `PATCH /activity-correction/{id}` | Employee submit koreksi (`date_in`, `date_out`, `time_zone`, `is_wfh`); Admin approve/reject via `PATCH` |
| **Izin Off** | `GET /off-permit`, `POST /off-permit`, `PATCH /off-permit/{id}` | Employee submit (`date_off`, `reason`); Admin approve/reject via `PATCH` |
| **Izin Terlambat & Pulang Cepat** | `GET /late-permit`, `GET /early-leave` | Cek riwayat izin terlambat dan pulang cepat |
| **Lembur** | `GET /overtime`, `POST /overtime`, `PATCH /overtime/{id}` | Employee submit (`overtime_in`, `overtime_out`, `notes`, `time_zone`); Admin approve/reject via `PATCH` |
| **Perjalanan Dinas** | `GET /business-trips`, `POST /business-trips`, `PATCH /business-trips/{id}` | Pengajuan & approval perjalanan dinas |
| **Sisa Cuti & Laporan** | `GET /project-leave`, `GET /share-report` | Mengambil ringkasan kuota/sisa cuti & unduh laporan absensi (`/apps/absent/reports/share_report`) |
| **Reset & Upload** | `POST /auth-management`, `POST /upload` | Reset password via `/auth-management` (`sendResetPassword` / `resetPwd`) + OTP di UI; upload foto/file bukti |

> [!NOTE]
> **Pembaruan Akurasi Rute & Form Web Karyawan (Live Discovery SIT v5)**:
> - **Register (`/absen/register`)**: Form langsung menampilkan 5 field aktif (`#nik`, `#fullname`, `#email`, `#password` + ikon mata Show/Hide, Upload Selfie `#selfie`) beserta tombol `Daftar` — tidak ada langkah terpisah `Cek NIK` atau auto-fill divisi sebelum submit.
> - **Reset Password (`/absen/reset-password-request` & `/absen/reset-password`)**: Alur menyertakan request email, input **kode OTP**, Password Baru, Konfirmasi Password (2 ikon mata), dan tautan kembali ke Login.
> - **Beranda, Kehadiranmu & Lupa Pulang (`/apps/absent` & `/apps/absent/correction/[id]`)**: Beranda memiliki filter **Bulan & Tahun** pada seksi `Kehadiranmu` (7 kartu metrik ringkasan), serta penanganan dialog **"Anda belum melakukan absen keluar"** menuju halaman `/apps/absent/correction/[id]`.
> - **Ubah Password (`/apps/absent/change-password`)**: Form hanya memiliki field **Password Baru** (`password`) dan **Confirm Password** (`passwordConfirmation`) beserta 2 ikon mata Show/Hide serta modal konfirmasi **`Sukses mengganti password`** (`Oke`) — **tidak ada** field `Password Lama`.
> - **Download Reports (`/apps/absent/reports/share_report`)**: Halaman tersedia di beranda Karyawan untuk export report **Harian, Mingguan, dan Bulanan** (**6 TCs** `SIT_USR_UI_136–141` untuk Tester Deny).
> - **Leader / Staff & Approval Bawahan (`/apps/absent/staff`, `/apps/absent/leave/[id]`, `/apps/absent/overtime/[id]`)**: Karyawan dengan bawahan (`upliner`) dapat memantau menu **Staff** (Lihat Absen, Sisa Cuti, Staff) serta Approve/Reject pengajuan bawahan (**8 TCs** `SIT_USR_UI_142–149` untuk Tester Aji).
> - **Rute Alias Client**: Portal juga mendukung rute `/apps/absent/leave-client` (beserta Tab `Info Cuti` 6 metrik) dan `/apps/absent/overtime-client` sebagai varian tampilan cuti/lembur sisi client.
> - **Next.js BFF Routes (`/api/*` di `BASE_URL`)**: Rute `/api/login`, `/api/users/summary`, `/api/activity/user-last-activity`, `/api/activity/user-check-correction`, `/api/sick`, `/api/activity/sick`, `/api/leave/project-leave`, `/api/client/*`, dan rute Leader `/api/leave/[id]` dicakup di `SIT_USR_API_068–071, 077, 097–099, 111–116`.

### Tools API yang Direkomendasikan
- **Inspeksi Live**: **Chrome DevTools → Network Tab (Fetch/XHR)** (gunakan fitur *Copy as cURL* saat melakukan aksi di `/apps/absent/*` untuk melihat payload multipart/JSON terbaru, khususnya untuk endpoint upload foto atau pengajuan cuti).
- **Manual Testing**: **Postman** atau **Bruno**.
- **Automation Testing**: **REST Assured `6.3.0`** di dalam `BaseApiTest` dan `AdminApiHelper`.

---

## 4. Pola Hybrid API + UI di Proyek Karyawan (`AdminApiHelper`)

Untuk skenario UI Karyawan yang bergantung pada tindakan Admin HRD (seperti melihat status **Approved**, melihat **alasan penolakan HRD** pada kartu Izin/Koreksi/Lembur, atau memastikan data riwayat tersedia):
1. **`@BeforeMethod` (REST Assured — Role Employee & Admin)**:
   - Login sebagai Employee (`POST /authentication`) → buat pengajuan baru via REST Assured (`POST /off-permit`, `POST /activity-correction`, atau `POST /overtime`) → ambil `requestId`.
   - Login sebagai Admin (`POST /authentication` memakai `HYBRID_ADMIN_PASSWORD_ENCODED`) → jalankan `PATCH /off-permit/{id}` atau `PATCH /activity-correction/{id}` untuk mengubah status menjadi `Approved` atau `Rejected` beserta alasan penolakan.
2. **`@Test` (Selenium Web UI — Role Karyawan)**:
   - Buka browser Chrome (`headless = true`), login ke `/absen/login` sebagai `hadirsqa1@gmail.com`.
   - Buka `/apps/absent/permit`, `/apps/absent/correction`, atau `/apps/absent/overtime` → verifikasi status badge berubah dan teks alasan penolakan HRD tampil akurat di UI.

---

## 5. Konfigurasi Khusus WebDriver Mobile Mirroring (`DriverFactory.java`)

Karena Web Karyawan adalah aplikasi **Mobile Mirroring** yang mengakses **Kamera (Selfie)** dan **Geolocation (GPS Radius Kantor)**, konfigurasi `ChromeOptions` di `DriverFactory.java` harus menyertakan:
- **Default Headless**: `boolean headless` default `"true"` (bisa dioverride via `-Dheadless=false`).
- **Mobile-friendly Viewport atau Standard Desktop**: `--window-size=430,932` (opsional untuk simulasi mobile) atau `--window-size=1920,1080` sesuai kebutuhan locator responsif.
- **Fake Camera & Auto-Allow Media/Geolocation**:
  ```java
  chromeOptions.addArguments("--use-fake-ui-for-media-stream");
  chromeOptions.addArguments("--use-fake-device-for-media-stream");
  // Opsional file video .y4m jika dibutuhkan stream kamera spesifik:
  // chromeOptions.addArguments("--use-file-for-fake-video-capture=src/test/resources/testdata/dummy_face.y4m");

  Map<String, Object> prefs = new HashMap<>();
  prefs.put("credentials_enable_service", false);
  prefs.put("profile.password_manager_enabled", false);
  prefs.put("profile.default_content_setting_values.geolocation", 1); // 1 = Allow
  prefs.put("profile.default_content_setting_values.media_stream_camera", 1); // 1 = Allow
  chromeOptions.setExperimentalOption("prefs", prefs);
  ```
- Untuk skenario negatif GPS luar radius / izin lokasi diblokir (`SIT_USR_UI_043`, `044`, `045`), gunakan **Chrome DevTools Protocol (CDP)** via `((HasDevTools) driver)` / `Emulation.setGeolocationOverride` atau `Browser.setPermission` pada instance `ChromeDriver`.

---

## 6. Struktur Direktori Standar (Mirroring Proyek Admin)

```text
final-project-karyawan/
├── pom.xml                                  # Java 21, Selenium 4.52.0, REST Assured 6.3.0, TestNG 7.12.0, ExtentReports 5.1.2
├── .gitignore
├── .gitattributes
├── .editorconfig
├── README.md
├── plan.md
├── tasklist.md
├── docs/
│   └── SIT_Scenario_Magang_Hadir.xlsx       # Single Source of Truth matriks SIT v5 (276 User TCs: 160 UI + 116 API + kolom Manual Check)
├── reports/
│   ├── ExtentReport.html
│   └── screenshots/
└── src/
    ├── main/java/com/juaracoding/magang/
    │   ├── drivers/
    │   │   └── DriverFactory.java           # ThreadLocal WebDriver, headless=true default + camera/GPS prefs
    │   ├── pages/
    │   │   ├── BasePage.java                # Explicit wait 15s, click fallback JS, type, isDisplayed, scroll
    │   │   └── user/
    │   │       ├── deny/                    # UserLoginPage, RegisterPage, ResetPasswordPage, PermitPage, LeaveAndSickPage, DownloadReportPage
    │   │       └── aji/                     # AttendancePage, ActivityHistoryPage, CorrectionPage, OvertimePage, UserProfilePage, LeaderStaffPage
    │   ├── reports/
    │   │   ├── ExtentManager.java           # Dark Theme SparkReporter
    │   │   └── ExtentTestManager.java       # ThreadLocal logging
    │   └── utils/
    │       ├── Constants.java               # Base64 credentials, URLs, endpoints
    │       ├── SecurityUtil.java            # Base64 decoder/encoder
    │       └── ScreenshotUtil.java          # Base64 & PNG screenshot
    └── test/
        ├── java/com/juaracoding/magang/
        │   ├── BaseTest.java                # @BeforeMethod / @AfterMethod WebDriver lifecycle
        │   ├── listeners/
        │   │   └── TestListener.java        # Auto-tag Tester Deny/Aji, UI/API, Base64 screenshot on failure
        │   ├── api/
        │   │   ├── BaseApiTest.java         # REST Assured + useRelaxedHTTPSValidation() + /authentication & /api/login
        │   │   ├── helper/
        │   │   │   └── AdminApiHelper.java  # Hybrid @Before Admin approval/seeding helper
        │   │   └── user/
        │   │       ├── UserGroupAApiTest.java # Tester Deny (55 API TCs)
        │   │       └── UserGroupBApiTest.java # Tester Aji (61 API TCs)
        │   └── tests/user/
        │       ├── deny/                    # UserLoginTest, RegisterTest, ResetPasswordTest, PermitTest, LeaveAndSickTest, DownloadReportTest, UserCrossModuleDenyE2ETest
        │       └── aji/                     # AttendanceTest, ActivityHistoryTest, CorrectionTest, OvertimeTest, UserProfileTest, LeaderStaffTest, UserCrossModuleAjiE2ETest
        └── resources/
            ├── testng.xml                   # Master Suite Karyawan (276 TCs, headless=true)
            ├── testng-deny.xml              # Suite khusus Tester Deny (139 TCs)
            ├── testng-aji.xml               # Suite khusus Tester Aji (137 TCs)
            └── testdata/                    # Dummy selfie .jpg/.png, file >5MB, file non-image (.pdf/.mp3)
```

---

## 7. Pembagian Tugas Adil 2 Tester di Proyek Karyawan (Total 276 TCs)

Mengacu pada [docs/SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-admin/docs/SIT_Scenario_Magang_Hadir.xlsx) v5 (kontigu, atomik, memiliki kolom `Severity`, `Priority`, `Test Suite`, `Actual Result / Defect Notes`, dan `Manual Check`):

| **Tester** | **Modul Web UI (`160 TCs`)** | **Modul REST Assured API (`144 TCs`)** | **Total & Bobot** |
| :--- | :--- | :--- | :---: |
| **👤 Tester Deny** | **84 Web UI TCs** (`001–035, 078–091, 103–126, 136–141, 150–154`) | **71 API TCs** (`001–018, 044–049, 053–083, 117–132` — Auth/JWT Tamper, BFF Login, Permits IDOR/Self-Approval, Upload Spoofing & `/uploads/*`, Leaves/Cancel RBAC, Share-Report, Mass-Assignment) | **155 TCs** (51.0%) |
| **👤 Tester Aji** | **76 Web UI TCs** (`036–077, 092–102, 127–135, 142–149, 155–160`) | **73 API TCs** (`019–043, 050–052, 084–116, 133–144` — Attendance, Geofence `/location-point` RBAC, Activity Log, Corrections/Overtimes Non-Upliner IDOR, Business Trips, BFF Summary/Staff Isolation) | **149 TCs** (49.0%) |

---

## 8. Catatan Perilaku Aplikasi & Penanganan Known Defect (`Actual Result / Defect Notes`)

Pada SIT v5, perilaku bug pada SUT **tidak lagi ditulis di kolom `Expected Result`**. Kolom `Expected Result` kini berisi perilaku standar aplikasi yang seharusnya, sedangkan perilaku bug live dicatat di kolom **`Actual Result / Defect Notes`** dengan **`Status = Ready (Known Defect)`** (**7 TCs** pada portal User: `6 UI + 1 API`):
1. **Broken Image setelah Submit/Upload**: Pada Registrasi (`SIT_USR_UI_024`), Avatar & Foto Presensi Beranda (`SIT_USR_UI_046`), Foto Check-In Log Absen (`SIT_USR_UI_061`), dan Bukti Surat Sakit (`SIT_USR_UI_120`), sistem seharusnya menampilkan gambar terunggah utuh, namun di live mengalami *broken image* (`naturalWidth == 0` / HTTP 404).
2. **Tidak Ada Batas Maksimal Pengajuan Cuti (`SIT_USR_UI_109` & `SIT_USR_API_047`)**: Sistem seharusnya menolak pengajuan cuti yang melebihi sisa kuota tahunan, namun di live masih lolos tanpa validasi batas kuota.
3. **Kuota `Jumlah Cuti` Pendaftaran User Tidak Sinkron (`SIT_USR_UI_160`)**: `leave_amount` yang diisikan Admin saat pendaftaran user terlihat di form Edit User (`/management/user-v2/edit/[id]`), namun tidak masuk ke data kuota aktif `project-leave` sehingga Karyawan gagal mengajukan cuti di `/apps/absent/leave-client`.
4. **Pagination List Mobile (`InfiniteScroll`)**: Berbeda dengan Admin yang memakai MUI `TablePagination` (`5/10/25`, `Next/Prev/First/Last`), 6 halaman daftar riwayat Karyawan (`Log Absen`, `Koreksi Absen`, `Izin`, `Lembur`, `Cuti`, `Sakit`) memakai mekanisme scroll bawah (`$skip = 10 * page`) untuk memuat batch data berikutnya (plus pagination daftar Staff Leader pada `/apps/absent/staff`).
5. Saat implementasi di `final-project-karyawan` (**Java 21 + Selenium `4.52.0` + REST Assured `6.3.0` + TestNG `7.12.0`**), gunakan assertion terhadap **Expected Result** standar dan tandai kegagalan bug aktual tersebut sebagai **Known Defect** (misalnya catat ke `ExtentTestManager` dengan prefix `"[KNOWN DEFECT] ..."`) sambil mempertahankan kolom `Severity` (`Critical/High/Medium/Low`) dan `Priority` (`P1/P2/P3`).

---

## 9. Aturan Git Workflow (Opsi B — Zero Merge Conflict)

1. Kerjakan Fase 2–3 (Core Foundation) di branch `main` terlebih dahulu.
2. Untuk Fase 4–5 (POM + Test Class), **selalu buat branch baru dari `main`**:
   - Deny: `feature/deny-user-login`, `feature/deny-user-register`, `feature/deny-user-reset-password`, `feature/deny-user-permit`, `feature/deny-user-leave-sick`, `feature/deny-user-download-report`, `feature/deny-user-e2e`, `feature/deny-user-api-a`
   - Aji: `feature/aji-user-attendance`, `feature/aji-user-activity`, `feature/aji-user-correction`, `feature/aji-user-overtime`, `feature/aji-user-profile`, `feature/aji-user-leader-staff`, `feature/aji-user-e2e`, `feature/aji-user-api-b`
3. Dilarang mengubah file pondasi bersama (`BasePage`, `BaseTest`, `BaseApiTest`, `Constants`, `DriverFactory`, `testng*.xml`) di dalam branch fitur agar Pull Request ke `main` bebas konflik.
4. Di dalam `TestListener.java`, deteksi kategori otomatis berdasarkan nama package: `.deny.` → `"Tester Deny"`, `.aji.` → `"Tester Aji"`, `.api.` → `"REST Assured API"`, `.tests.` → `"Selenium Web UI"`.

---

## 10. 📋 Copy-Paste Master Prompt untuk Agent AI Proyek Karyawan

> **Salin seluruh teks di dalam blok kode berikut dan tempelkan ke Agent AI di folder repositori proyek Web Karyawan baru Anda (bersama salinan file `docs/SIT_Scenario_Magang_Hadir.xlsx` dan `reference.md`):**

```text
Tolong baca file @WebKaryawanPrompt.md, @docs/SIT_Scenario_Magang_Hadir.xlsx, dan @reference.md terlebih dahulu sebagai acuan mutlak proyek ini.

Saya ingin membangun proyek automation testing baru KHUSUS bagian KARYAWAN (Mobile Mirroring: https://magang.dikahadir.com/absen/login) yang 100% selaras arsitektur, versi tools, keamanan, dan konvensinya dengan proyek Web Admin saya.

Ikuti seluruh spesifikasi berikut secara ketat:
1. Tech Stack & Build:
   - Java 21 LTS, Apache Maven, Selenium WebDriver 4.52.0, REST Assured 6.3.0, TestNG 7.12.0, ExtentReports 5.1.2 (Dark Theme + Base64 failure screenshot), Apache POI 5.2.5, Jackson 2.17.2.
   - Chrome WAJIB default `headless = true` (di DriverFactory.java, BaseTest.java, pom.xml, dan semua testng*.xml), namun dapat dioverride dengan `-Dheadless=false`.
   - Aktifkan `--use-fake-ui-for-media-stream`, `--use-fake-device-for-media-stream`, dan geolocation allow di ChromeOptions untuk kebutuhan presensi kamera/GPS.
2. Keamanan Kredensial:
   - Gunakan `SecurityUtil.java` (`java.util.Base64`) untuk decode password di runtime.
   - Jangan pernah menuliskan password plaintext di kode, komentar, atau XML (gunakan konstanta `USER_PASSWORD_ENCODED = "U1FBQEhhZGlyMTIzNDU="` dan `HYBRID_ADMIN_PASSWORD_ENCODED = "TWFnYW5nU1FBX0pDQDEyMw=="`).
3. API & Hybrid API + UI:
   - Gunakan `API_BASE_URI = "https://api-dev.dikahadir.com/hadir-api-magang"` dengan `RestAssured.useRelaxedHTTPSValidation()` dan endpoint login `POST /authentication`.
   - Sediakan `AdminApiHelper.java` untuk pola Hybrid API + UI di tahap `@Before`: jika test UI Karyawan membutuhkan status Approved/Rejected dari Admin (misal verifikasi status dan alasan penolakan pada Izin, Koreksi, atau Lembur), lakukan seeding pengajuan + approval/rejection via REST Assured di `@Before`, lalu verifikasi tampilannya di UI Karyawan dengan Selenium.
4. Pembagian Tugas 2 Tester Adil (Sesuai SIT v5.1 — 304 User TCs):
   - Total 304 Test Cases User (160 Web UI + 144 REST Assured API, termasuk Kehadiranmu Bulan/Tahun, Dialog Lupa Pulang `/correction/[id]`, Leader/Staff `/apps/absent/staff` & Approval bawahan, Info Cuti, Download Report Harian/Mingguan/Bulanan, 6 List InfiniteScroll, 11 TCs Integrasi Lintas Modul E2E, BFF/Leader APIs `SIT_USR_API_111–116`, serta Pengayaan Security/Live Endpoint `SIT_USR_API_117–144`) dibagi ke package terisolasi `deny` dan `aji`:
     * Tester Deny (155 TCs = 84 UI + 71 API): package `pages.user.deny`, `tests.user.deny`, dan `UserGroupAApiTest` (Login `001–012`, Register `013–025`, Reset Password + OTP `026–035`, Izin `078–091`, Cuti `103–115`, Sakit `116–126`, Download Report `136–141`, E2E Lintas Modul Deny `150–154` + 71 API Group A `001–018, 044–049, 053–083, 117–132`).
     * Tester Aji (149 TCs = 76 UI + 73 API): package `pages.user.aji`, `tests.user.aji`, dan `UserGroupBApiTest` (Beranda & Lupa Pulang `036–058`, Log Absen `059–065`, Koreksi `066–077`, Lembur `092–102`, Profil/Ubah Password `127–135`, Leader-Staff & Approval `142–149`, E2E Lintas Modul Aji `155–160` + 73 API Group B `019–043, 050–052, 084–116, 133–144`).
   - Cantumkan ID Test Case (misal `SIT_USR_UI_001`) pada `@Test(description = "...")` dan `ExtentTestManager.logInfo(...)`.
   - Sediakan 3 runner utama: `testng.xml` (304 TCs), `testng-deny.xml` (155 TCs), dan `testng-aji.xml` (149 TCs), ditambah 4 category suites (`testng-smoke/regression/positive/negative.xml`).

Langkah pertama: buat dulu `plan.md` dan `tasklist.md` (7 fase) untuk proyek Karyawan ini agar saya review sebelum implementasi kode dimulai!
```

