# 🤖 Panduan & Master Prompt AI Agent: Automation Testing Web Karyawan (`magang.dikahadir.com`)

> **Tujuan Dokumen**: Berikan file ini sebagai acuan utama kepada Agent AI pada **repositori baru khusus Web Karyawan (Mobile Mirroring)** agar arsitektur, versi library, pola keamanan Base64, integrasi REST Assured, mekanisme Hybrid API + UI, pembagian kerja 2 tester, dan standar pelaporan **100% selaras** dengan repositori **Web Admin (`final-project-admin`)**.

---

## 1. Ringkasan Proyek & Target Aplikasi (SUT)

| Informasi | Nilai / Spesifikasi |
| :--- | :--- |
| **Aplikasi Target (SUT)** | [HADIR Mobile Mirroring](https://magang.dikahadir.com/absen/login) |
| **Domain Pengujian** | Portal **Karyawan / User Magang** (10 Modul Fitur Mobile, **175 Test Cases**: **120 Web UI** + **55 REST Assured API**) |
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
| **Sisa Cuti** | `GET /project-leave` | Mengambil ringkasan kuota & sisa cuti |

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
│   └── SIT_Scenario_Magang_Hadir.xlsx       # Salinan/rujukan matriks SIT (175 User TCs)
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
    │   │       ├── deny/                    # UserLoginPage, RegisterPage, ResetPasswordPage, PermitPage, LeaveAndSickPage
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
        │       ├── deny/                    # UserLoginTest, RegisterTest, ResetPasswordTest, PermitTest, LeaveAndSickTest
        │       └── aji/                     # AttendanceTest, ActivityHistoryTest, CorrectionTest, OvertimeTest, UserProfileTest
        └── resources/
            ├── testng.xml                   # Master Suite Karyawan (175 TCs, headless=true)
            ├── testng-deny.xml              # Suite khusus Tester Deny (87 TCs)
            ├── testng-aji.xml               # Suite khusus Tester Aji (88 TCs)
            └── testdata/                    # Dummy selfie .jpg/.png, file >5MB, file non-image (.pdf/.mp3)
```

---

## 7. Pembagian Tugas Adil 2 Tester di Proyek Karyawan (Total 175 TCs)

Mengacu pada [SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-admin/SIT_Scenario_Magang_Hadir.xlsx) (tanpa mengubah isi skenario existing):

| Tester | Modul Web UI (`120 TCs`) | Modul REST Assured API (`55 TCs`) | Total & Bobot |
| :--- | :--- | :--- | :---: |
| **👤 Tester Deny** | **60 Web UI TCs**:<br>1. `Autentikasi-Login` (`SIT_USR_UI_001–012`: 12 TCs)<br>2. `Autentikasi-Register` (`SIT_USR_UI_013–025`: 13 TCs)<br>3. `Autentikasi-Reset Password` (`SIT_USR_UI_026–033`: 8 TCs)<br>4. `Pengajuan-Izin` (`SIT_USR_UI_075–084`: 10 TCs — *Hybrid*)<br>5. `Pengajuan-Cuti & Sakit` (`SIT_USR_UI_096–112`: 17 TCs) | **27 API TCs**:<br>1. User Auth & Reset Password API (`SIT_USR_API_001–010`: 10 TCs)<br>2. Registration & NIK Check API (`SIT_USR_API_011–018`: 8 TCs)<br>3. Leaves/Izin/Sakit API (`SIT_USR_API_044–049`: 6 TCs)<br>4. Profile & Change Password API (`SIT_USR_API_053–055`: 3 TCs) | **87 TCs** (~49.7%) |
| **👤 Tester Aji** | **60 Web UI TCs**:<br>1. `Presensi-Masuk & Keluar (Absen)` (`SIT_USR_UI_034–053`: 20 TCs)<br>2. `Presensi-Log Absen & Aktivitas` (`SIT_USR_UI_054–064`: 11 TCs)<br>3. `Pengajuan-Koreksi Absen` (`SIT_USR_UI_065–074`: 10 TCs — *Hybrid*)<br>4. `Pengajuan-Lembur` (`SIT_USR_UI_085–095`: 11 TCs — *Hybrid*)<br>5. `Profil-Menu & Ubah Password` (`SIT_USR_UI_113–120`: 8 TCs) | **28 API TCs**:<br>1. Attendance Clock-In/Out API (`SIT_USR_API_019–030`: 12 TCs)<br>2. Activity Log & IDOR Security API (`SIT_USR_API_031–038`: 8 TCs)<br>3. Corrections API (`SIT_USR_API_039–043`: 5 TCs)<br>4. Overtimes API (`SIT_USR_API_050–052`: 3 TCs) | **88 TCs** (~50.3%) |

---

## 8. Catatan Perilaku Aplikasi & Verifikasi Defect Aktual

Skenario bertanda **`[Defect/Negatif]`** pada SIT merupakan pengujian **verifikasi bug aktual** pada SUT (agar hasil test jujur dan akurat):
1. **Broken Image setelah Submit/Upload**: Pada Registrasi (`024`), Absen Masuk (`042`), Absen Pulang (`049`), Log Absen (`056`), Lampiran Lembur (`091`), Surat Sakit (`106`–`107`), gambar tampil saat preview namun gagal dimuat/diunduh setelah disubmit (`naturalWidth == 0` atau HTTP 404).
2. **Tidak Ada Batas Maksimal Pengajuan Cuti (`101`)**: Pengajuan cuti dengan rentang puluhan hari (> 30 hari) tetap lolos tanpa validasi batas kuota.
3. Gunakan assertion yang memverifikasi kondisi defect tersebut sesuai kolom *Hasil yang Diharapkan* di `SIT_Scenario_Magang_Hadir.xlsx` (atau tandai catatan defect di `ExtentTestManager` bila melakukan *soft/defect logging*).

---

## 9. Aturan Git Workflow (Opsi B — Zero Merge Conflict)

1. Kerjakan Fase 2–3 (Core Foundation) di branch `main` terlebih dahulu.
2. Untuk Fase 4–5 (POM + Test Class), **selalu buat branch baru dari `main`**:
   - Deny: `feature/deny-user-login`, `feature/deny-user-register`, `feature/deny-user-reset-password`, `feature/deny-user-permit`, `feature/deny-user-leave-sick`, `feature/deny-user-api-a`
   - Aji: `feature/aji-user-attendance`, `feature/aji-user-activity`, `feature/aji-user-correction`, `feature/aji-user-overtime`, `feature/aji-user-profile`, `feature/aji-user-api-b`
3. Dilarang mengubah file pondasi bersama (`BasePage`, `BaseTest`, `BaseApiTest`, `Constants`, `DriverFactory`, `testng*.xml`) di dalam branch fitur agar Pull Request ke `main` bebas konflik.
4. Di dalam `TestListener.java`, deteksi kategori otomatis berdasarkan nama package: `.deny.` → `"Tester Deny"`, `.aji.` → `"Tester Aji"`, `.api.` → `"REST Assured API"`, `.tests.` → `"Selenium Web UI"`.

---

## 10. 📋 Copy-Paste Master Prompt untuk Agent AI Proyek Karyawan

> **Salin seluruh teks di dalam blok kode berikut dan tempelkan ke Agent AI di folder repositori proyek Web Karyawan baru Anda (bersama salinan file `SIT_Scenario_Magang_Hadir.xlsx` dan `reference.md`):**

```text
Tolong baca file @WebKaryawanPrompt.md, @SIT_Scenario_Magang_Hadir.xlsx, dan @reference.md terlebih dahulu sebagai acuan mutlak proyek ini.

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
4. Pembagian Tugas 2 Tester Adil (Sesuai SIT tanpa mengubah isi skenario):
   - Total 175 Test Cases User (120 Web UI + 55 REST Assured API) dibagi ke package terisolasi `deny` dan `aji`:
     * Tester Deny (87 TCs = 60 UI + 27 API): package `pages.user.deny`, `tests.user.deny`, dan `UserGroupAApiTest` (Login, Register, Reset Password, Izin, Cuti & Sakit + API Group A).
     * Tester Aji (88 TCs = 60 UI + 28 API): package `pages.user.aji`, `tests.user.aji`, dan `UserGroupBApiTest` (Presensi Masuk/Keluar, Log Absen, Koreksi, Lembur, Profil + API Group B).
   - Cantumkan ID Test Case (misal `SIT_USR_UI_001`) pada `@Test(description = "...")` dan `ExtentTestManager.logInfo(...)`.
   - Sediakan 3 runner XML: `testng.xml` (175 TCs), `testng-deny.xml` (87 TCs), dan `testng-aji.xml` (88 TCs).

Langkah pertama: buat dulu `plan.md` dan `tasklist.md` (7 fase) untuk proyek Karyawan ini agar saya review sebelum implementasi kode dimulai!
```

