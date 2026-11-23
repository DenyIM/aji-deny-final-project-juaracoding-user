# 🤖 Panduan & Master Prompt AI Agent: Automation Testing Web Karyawan (`magang.dikahadir.com`)

> **Tujuan Dokumen**: Berikan file ini sebagai acuan utama kepada Agent AI pada **repositori baru khusus Web Karyawan (Mobile Mirroring)** agar arsitektur, versi library, pola keamanan Base64, integrasi REST Assured, mekanisme Hybrid API + UI, pembagian kerja 2 tester, dan standar pelaporan **100% selaras** dengan repositori **Web Admin (`final-project-admin`)**.

---

## 1. Ringkasan Proyek & Target Aplikasi (SUT)

| Informasi | Nilai / Spesifikasi |
| :--- | :--- |
| **Aplikasi Target (SUT)** | [HADIR Mobile Mirroring](https://magang.dikahadir.com/absen/login) |
| **Domain Pengujian** | Portal **Karyawan / User Magang** (Modul Fitur Mobile + InfiniteScroll + E2E Lintas Modul, **195 Test Cases**: **140 Web UI** + **55 REST Assured API**) |
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
| **Karyawan Utama** | `hadirsqa1@gmail.com` (NIK: `D6201031`, Nama: `Hadir SQA Testing 1`) | `USER_PASSWORD_ENCODED = "U1FBQEhhZGlyMTIzNDU="` | Login UI `/absen/login` & API User Token |
| **Admin (Hybrid Seeder)** | `admin@hadir.com` | `HYBRID_ADMIN_PASSWORD_ENCODED = "TWFnYW5nU1FBX0pDQDEyMw=="` | **Khusus tahap `@Before` / `@After` Hybrid API+UI** (misal Admin menyetujui/menolak pengajuan lewat API agar User UI dapat memverifikasi badge `Approved`/`Rejected` beserta alasan penolakan) |
| **Pool NIK Registrasi** | `F1240001`, `D6240005`, `D7240031`, `D8240010`, `D7240029`, `D7240028`, `D7240027`, `D6240004` | Array `VALID_NIK_POOL` di `Constants.java` | Pengujian registrasi mandiri `/absen/register` |

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
| **Profil & User** | `GET /users`, `PATCH /users/{id}` | Filter `nik=D6201031` atau `email=hadirsqa1@gmail.com`; update profil/password |
| **Aktivitas & Presensi** | `GET /activity` | Riwayat presensi & log aktivitas harian |
| **Koreksi Absen** | `GET /activity-correction`, `POST /activity-correction`, `PATCH /activity-correction/{id}` | Employee submit koreksi (`date_in`, `date_out`, `time_zone`, `is_wfh`); Admin approve/reject via `PATCH` |
| **Izin Off** | `GET /off-permit`, `POST /off-permit`, `PATCH /off-permit/{id}` | Employee submit (`date_off`, `reason`); Admin approve/reject via `PATCH` |
| **Izin Terlambat & Pulang Cepat** | `GET /late-permit`, `GET /early-leave` | Cek riwayat izin terlambat dan pulang cepat |
| **Lembur** | `GET /overtime`, `POST /overtime`, `PATCH /overtime/{id}` | Employee submit (`overtime_in`, `overtime_out`, `notes`, `time_zone`); Admin approve/reject via `PATCH` |
| **Perjalanan Dinas** | `GET /business-trips`, `POST /business-trips`, `PATCH /business-trips/{id}` | Pengajuan & approval perjalanan dinas |
| **Sisa Cuti & Laporan** | `GET /project-leave`, `GET /share-report` | Mengambil ringkasan kuota/sisa cuti & unduh laporan absensi (`/apps/absent/reports/share_report`) |
| **Reset & Upload** | `POST /auth-management`, `POST /upload` | Reset password via `/auth-management` (`sendResetPassword` / `resetPwd`) + OTP di UI; upload foto/file bukti |

> [!NOTE]
> **Pembaruan Akurasi Rute & Form Web Karyawan (Live Discovery)**:
> - **Reset Password (`/absen/forgot-password`)**: Alur menyertakan input **kode OTP**, password baru, dan konfirmasi password.
> - **Ubah Password (`/apps/absent/change-password`)**: Form hanya memiliki field **Password Baru** (`password`) dan **Confirm Password** (`passwordConfirmation`) beserta ikon mata Show/Hide — **tidak ada** field `Password Lama`.
> - **Download Reports (`/apps/absent/reports/share_report`)**: Halaman tersedia di beranda Karyawan untuk export report Mingguan/Bulanan (**4 TCs baru** `SIT_USR_UI_121–124` untuk Tester Deny).
> - **Rute Alias Client**: Portal juga mendukung rute `/apps/absent/leave-client` dan `/apps/absent/overtime-client` sebagai varian tampilan cuti/lembur sisi client.

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
│   └── SIT_Scenario_Magang_Hadir.xlsx       # Single Source of Truth matriks SIT v3 (195 User TCs: 140 UI + 55 API)
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
    │   │       └── aji/                     # AttendancePage, ActivityHistoryPage, CorrectionPage, OvertimePage, UserProfilePage
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
        │   │   ├── BaseApiTest.java         # REST Assured + useRelaxedHTTPSValidation() + /authentication
        │   │   ├── helper/
        │   │   │   └── AdminApiHelper.java  # Hybrid @Before Admin approval/seeding helper
        │   │   └── user/
        │   │       ├── UserGroupAApiTest.java # Tester Deny (27 API TCs)
        │   │       └── UserGroupBApiTest.java # Tester Aji (28 API TCs)
        │   └── tests/user/
        │       ├── deny/                    # UserLoginTest, RegisterTest, ResetPasswordTest, PermitTest, LeaveAndSickTest, DownloadReportTest, UserCrossModuleDenyE2ETest
        │       └── aji/                     # AttendanceTest, ActivityHistoryTest, CorrectionTest, OvertimeTest, UserProfileTest, UserCrossModuleAjiE2ETest
        └── resources/
            ├── testng.xml                   # Master Suite Karyawan (195 TCs, headless=true)
            ├── testng-deny.xml              # Suite khusus Tester Deny (99 TCs)
            ├── testng-aji.xml               # Suite khusus Tester Aji (96 TCs)
            └── testdata/                    # Dummy selfie .jpg/.png, file >5MB, file non-image (.pdf/.mp3)
```

---

## 7. Pembagian Tugas Adil 2 Tester di Proyek Karyawan (Total 195 TCs)

Mengacu pada [docs/SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-admin/docs/SIT_Scenario_Magang_Hadir.xlsx) v3 (kontigu, atomik, memiliki kolom `Severity`, `Priority`, dan `Actual Result / Defect Notes`):

| Tester | Modul Web UI (`140 TCs`) | Modul REST Assured API (`55 TCs`) | Total & Bobot |
| :--- | :--- | :--- | :---: |
| **👤 Tester Deny** | **72 Web UI TCs**:<br>1. `Autentikasi-Login` (`SIT_USR_UI_001–012`: 12 TCs, *utuh*)<br>2. `Autentikasi-Register` (`SIT_USR_UI_013–025`: 13 TCs)<br>3. `Autentikasi-Reset Password` (`SIT_USR_UI_026–033`: 8 TCs, termasuk OTP)<br>4. `Pengajuan-Izin` (`SIT_USR_UI_076–086`: 11 TCs — *Hybrid* + *InfiniteScroll*)<br>5. `Pengajuan-Cuti & Sakit` (`SIT_USR_UI_099–117`: Cuti `099–107` [9] + Sakit `108–117` [10] = 19 TCs + *InfiniteScroll*)<br>6. `Profil-Download Report` (`SIT_USR_UI_126–129`: 4 TCs `/apps/absent/reports/share_report`)<br>7. `Integrasi Lintas Modul (Hybrid API+UI)` — Bagian Deny (`SIT_USR_UI_130–134`: 5 TCs) | **27 API TCs**:<br>1. User Auth & `/auth-management` Reset Password API (`SIT_USR_API_001–010`: 10 TCs)<br>2. Registration & NIK Check API (`SIT_USR_API_011–018`: 8 TCs)<br>3. Leaves/Izin/Sakit API (`SIT_USR_API_044–049`: 6 TCs)<br>4. Profile & `PATCH /users/{id}` Change Password API (`SIT_USR_API_053–055`: 3 TCs) | **99 TCs** (50.8%) |
| **👤 Tester Aji** | **68 Web UI TCs**:<br>1. `Presensi-Beranda` (`SIT_USR_UI_034–053`: 20 TCs)<br>2. `Presensi-Log Absen` (`SIT_USR_UI_054–064`: 11 TCs + *InfiniteScroll*)<br>3. `Pengajuan-Koreksi Absen` (`SIT_USR_UI_065–075`: 11 TCs — *Hybrid* + *InfiniteScroll*)<br>4. `Pengajuan-Lembur` (`SIT_USR_UI_087–098`: 12 TCs — *Hybrid* + *InfiniteScroll*)<br>5. `Profil-Menu Utama & Ubah Password` (`SIT_USR_UI_118–125`: 8 TCs — tanpa Password Lama)<br>6. `Integrasi Lintas Modul (Hybrid API+UI)` — Bagian Aji (`SIT_USR_UI_135–140`: 6 TCs, termasuk E2E Kuota Cuti Pendaftaran) | **28 API TCs**:<br>1. Attendance Clock-In/Out API (`SIT_USR_API_019–030`: 12 TCs)<br>2. Activity Log & IDOR Security API (`SIT_USR_API_031–038`: 8 TCs)<br>3. Corrections API (`SIT_USR_API_039–043`: 5 TCs)<br>4. Overtimes API (`SIT_USR_API_050–052`: 3 TCs) | **96 TCs** (49.2%) |

---

## 8. Catatan Perilaku Aplikasi & Penanganan Known Defect (`Actual Result / Defect Notes`)

Pada SIT v3, perilaku bug pada SUT **tidak lagi ditulis di kolom `Expected Result`**. Kolom `Expected Result` kini berisi perilaku standar aplikasi yang seharusnya, sedangkan perilaku bug live dicatat di kolom **`Actual Result / Defect Notes`** dengan **`Status = Ready (Known Defect)`**:
1. **Broken Image setelah Submit/Upload**: Pada Registrasi, Absen Masuk/Pulang, Log Absen, Lampiran Lembur, dan Surat Sakit, sistem seharusnya menampilkan gambar terunggah utuh, namun di live sering mengalami *broken image* (`naturalWidth == 0` / HTTP 404).
2. **Tidak Ada Batas Maksimal Pengajuan Cuti (`SIT_USR_UI_104` & `SIT_USR_API_047`)**: Sistem seharusnya menolak pengajuan cuti yang melebihi sisa kuota tahunan, namun di live masih lolos tanpa validasi batas kuota.
3. **Kuota `Jumlah Cuti` Pendaftaran User Tidak Sinkron (`SIT_USR_UI_140`)**: `leave_amount` yang diisikan Admin saat pendaftaran user terlihat di form Edit User (`/management/user-v2/edit/[id]`), namun tidak masuk ke data kuota aktif `project-leave` sehingga Karyawan gagal mengajukan cuti di `/apps/absent/leave`.
4. **Pagination List Mobile (`InfiniteScroll`)**: Berbeda dengan Admin yang memakai MUI `TablePagination` (`5/10/25`, `Next/Prev/First/Last`), 6 halaman daftar riwayat Karyawan (`Log Absen`, `Koreksi Absen`, `Izin`, `Lembur`, `Cuti`, `Sakit`) memakai mekanisme scroll bawah (`$skip = 10 * page`) untuk memuat batch data berikutnya.
5. Saat implementasi di `final-project-karyawan` (**Java 21 + Selenium `4.52.0` + REST Assured `6.3.0` + TestNG `7.12.0`**), gunakan assertion terhadap **Expected Result** standar dan tandai kegagalan bug aktual tersebut sebagai **Known Defect** (misalnya catat ke `ExtentTestManager`) sambil mempertahankan kolom `Severity` (`Critical/High/Medium/Low`) dan `Priority` (`P1/P2/P3`).

---

## 9. Aturan Git Workflow (Opsi B — Zero Merge Conflict)

1. Kerjakan Fase 2–3 (Core Foundation) di branch `main` terlebih dahulu.
2. Untuk Fase 4–5 (POM + Test Class), **selalu buat branch baru dari `main`**:
   - Deny: `feature/deny-user-login`, `feature/deny-user-register`, `feature/deny-user-reset-password`, `feature/deny-user-permit`, `feature/deny-user-leave-sick`, `feature/deny-user-download-report`, `feature/deny-user-e2e`, `feature/deny-user-api-a`
   - Aji: `feature/aji-user-attendance`, `feature/aji-user-activity`, `feature/aji-user-correction`, `feature/aji-user-overtime`, `feature/aji-user-profile`, `feature/aji-user-e2e`, `feature/aji-user-api-b`
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
4. Pembagian Tugas 2 Tester Adil (Sesuai SIT v3 — 195 User TCs):
   - Total 195 Test Cases User (140 Web UI + 55 REST Assured API, termasuk 4 TCs Download Report, 6 List InfiniteScroll & 11 TCs Integrasi Lintas Modul E2E) dibagi ke package terisolasi `deny` dan `aji`:
     * Tester Deny (99 TCs = 72 UI + 27 API): package `pages.user.deny`, `tests.user.deny`, dan `UserGroupAApiTest` (Login `001–012` utuh, Register, Reset Password + OTP, Izin + InfiniteScroll, Cuti & Sakit + InfiniteScroll, Download Report `/apps/absent/reports/share_report`, E2E Lintas Modul Deny `130–134` + API Group A).
     * Tester Aji (96 TCs = 68 UI + 28 API): package `pages.user.aji`, `tests.user.aji`, dan `UserGroupBApiTest` (Presensi Beranda/Absen, Log Absen + InfiniteScroll, Koreksi + InfiniteScroll, Lembur + InfiniteScroll, Profil/Ubah Password tanpa Password Lama, E2E Lintas Modul Aji `135–140` + API Group B).
   - Cantumkan ID Test Case (misal `SIT_USR_UI_001`) pada `@Test(description = "...")` dan `ExtentTestManager.logInfo(...)`.
   - Sediakan 3 runner XML: `testng.xml` (195 TCs), `testng-deny.xml` (99 TCs), dan `testng-aji.xml` (96 TCs).

Langkah pertama: buat dulu `plan.md` dan `tasklist.md` (7 fase) untuk proyek Karyawan ini agar saya review sebelum implementasi kode dimulai!
```

