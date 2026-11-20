# 📌 Tasklist Pelaksanaan: Automation Testing Web Karyawan `magang.dikahadir.com` (175 User TCs)

Roadmap implementasi kerangka kerja otomatisasi pengujian **Portal Karyawan / Mobile Mirroring** (`https://magang.dikahadir.com/absen/login`) berbasis **Java 21 LTS**, **Selenium WebDriver `4.52.0`** (default `headless = true`), **REST Assured `6.3.0`**, **TestNG `7.12.0`**, **ExtentReports `5.1.2`**, **Apache POI `5.2.5`**, dan **Jackson `2.17.2`** yang 100% selaras dengan repositori Web Admin (`final-project-admin`).

Total cakupan pengujian: **175 Test Cases User** (**120 Web UI** + **55 REST Assured API**) yang terbagi adil antara **Tester Deny (87 TCs)** dan **Tester Aji (88 TCs)**.

---

## 🧭 Ikhtisar Status 7 Fase & Alokasi Beban Kerja

| Fase | Deskripsi Fase | Output Deliverables | Penanggung Jawab | Status |
| :---: | :--- | :--- | :---: | :---: |
| **Fase 1** | Review Acuan (`WebKaryawanPrompt.md`, `SIT_Scenario_Magang_Hadir.xlsx`, `reference.md`), Discovery Live UI/API & Penyusunan Blueprint | `plan.md`, `tasklist.md` | Shared Foundation | ✅ Selesai |
| **Fase 2** | Maven Setup (`pom.xml` Java 21, Selenium `4.52.0`, REST Assured `6.3.0`), Struktur Package Terisolasi (`deny` & `aji`), & File Testdata | `pom.xml`, `.gitignore`, `.gitattributes`, `.editorconfig`, `docs/SIT_Scenario_Magang_Hadir.xlsx`, `src/test/resources/testdata/*` | Shared Foundation | ✅ Selesai |
| **Fase 3** | Core Thread-Safe Engine, Keamanan Base64, Chrome Kamera/GPS Prefs (`headless=true`), & Hybrid `AdminApiHelper` | `SecurityUtil`, `Constants`, `DriverFactory`, `BasePage`, `BaseTest`, `BaseApiTest`, `AdminApiHelper`, `ScreenshotUtil`, `ExtentManager`, `ExtentTestManager`, `TestListener` | Shared Foundation | ✅ Selesai |
| **Fase 4** | Page Object Model (POM) Web Karyawan Implementation (Inspeksi Locator Live Mobile Mirroring) | POM Deny (`pages/user/deny/*`: 5 Classes) & POM Aji (`pages/user/aji/*`: 5 Classes) | Deny & Aji | ⏳ Pending |
| **Fase 5** | Automated Tests Implementation (120 Web UI + 55 REST Assured API + Hybrid API+UI + CDP Geolocation/Permission + Defect Checks) | `tests/user/deny/*` (60 UI), `tests/user/aji/*` (60 UI), `UserGroupAApiTest` (27 API), `UserGroupBApiTest` (28 API) | Deny (87 TCs) & Aji (88 TCs) | ⏳ Pending |
| **Fase 6** | TestNG Multi-Suite Runner (`testng`, `testng-deny`, `testng-aji`) & Verifikasi Eksekusi Headless | `testng.xml` (175 TCs), `testng-deny.xml` (87 TCs), `testng-aji.xml` (88 TCs) | Deny & Aji | ⏳ Pending |
| **Fase 7** | Validasi `ExtentReport.html`, Sinkronisasi Status SIT, Dokumentasi `README.md` & `walkthrough.md` | `reports/ExtentReport.html`, `README.md`, `walkthrough.md` | Shared Deliverable | ⏳ Pending |

---

## ⚖️ Matriks Pembagian Tugas Adil (Proyek Karyawan — Total 175 TCs)

| Aspek Kolaborasi | 👤 Tester Deny (87 TCs — 49.7%) | 👤 Tester Aji (88 TCs — 50.3%) |
| :--- | :--- | :--- |
| **Fitur Web UI (Selenium — 120 TCs)** | **60 Web UI TCs**:<br>1. `Autentikasi-Login` (`SIT_USR_UI_001–012`: 12 TCs)<br>2. `Autentikasi-Register` (`SIT_USR_UI_013–025`: 13 TCs)<br>3. `Autentikasi-Reset Password` (`SIT_USR_UI_026–033`: 8 TCs)<br>4. `Pengajuan-Izin` (`SIT_USR_UI_075–084`: 10 TCs — *Hybrid*)<br>5. `Pengajuan-Cuti & Sakit` (`SIT_USR_UI_096–112`: 17 TCs) | **60 Web UI TCs**:<br>1. `Presensi-Beranda` (`SIT_USR_UI_034–053`: 20 TCs — *Fake Camera/GPS & CDP*)<br>2. `Presensi-Log Absen` (`SIT_USR_UI_054–064`: 11 TCs)<br>3. `Pengajuan-Koreksi Absen` (`SIT_USR_UI_065–074`: 10 TCs — *Hybrid*)<br>4. `Pengajuan-Lembur` (`SIT_USR_UI_085–095`: 11 TCs — *Hybrid*)<br>5. `Profil-Menu Utama & Ubah Password` (`SIT_USR_UI_113–120`: 8 TCs) |
| **Fitur API (REST Assured — 55 TCs)** | **27 API TCs** (`UserGroupAApiTest`):<br>1. Auth & Reset Password (`SIT_USR_API_001–010`: 10 TCs)<br>2. Registration & NIK Check (`SIT_USR_API_011–018`: 8 TCs)<br>3. Leaves/Izin/Cuti/Sakit (`SIT_USR_API_044–049`: 6 TCs)<br>4. Profile & Change Password (`SIT_USR_API_053–055`: 3 TCs) | **28 API TCs** (`UserGroupBApiTest`):<br>1. Attendance Clock-In/Out & SLA (`SIT_USR_API_019–030`: 12 TCs)<br>2. Activity Log & IDOR Security (`SIT_USR_API_031–038`: 8 TCs)<br>3. Corrections (`SIT_USR_API_039–043`: 5 TCs)<br>4. Overtimes (`SIT_USR_API_050–052`: 3 TCs) |
| **Scope Package POM** | `com.juaracoding.magang.pages.user.deny.*` | `com.juaracoding.magang.pages.user.aji.*` |
| **Scope Package Test** | `tests.user.deny.*` & `UserGroupAApiTest` | `tests.user.aji.*` & `UserGroupBApiTest` |
| **Git Branch (Opsi B)** | `feature/deny-user-login`, `feature/deny-user-register`, `feature/deny-user-reset-password`, `feature/deny-user-permit`, `feature/deny-user-leave-sick`, `feature/deny-user-api-a` | `feature/aji-user-attendance`, `feature/aji-user-activity`, `feature/aji-user-correction`, `feature/aji-user-overtime`, `feature/aji-user-profile`, `feature/aji-user-api-b` |
| **Suite Runner XML** | `mvn test -DsuiteXmlFile=src/test/resources/testng-deny.xml` | `mvn test -DsuiteXmlFile=src/test/resources/testng-aji.xml` |

---

## 📋 Rincian Tugas per Fase

### 🔹 Fase 1: Perencanaan, Pemetaan SIT & Review Blueprint
- [x] **1.1. Telaah Dokumen Acuan Proyek**
  - [x] Baca dan selaraskan spesifikasi dari [WebKaryawanPrompt.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/WebKaryawanPrompt.md) dan [reference.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/reference.md).
  - [x] Ekstrak dan verifikasi seluruh **175 Test Cases User** (`SIT_USR_UI_001–120` dan `SIT_USR_API_001–055`) beserta pemetaan pembagian tugas pada [SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/SIT_Scenario_Magang_Hadir.xlsx).
  - [x] Selaraskan konvensi dan arsitektur dengan repositori **Web Admin (`final-project-admin`)**.
- [x] **1.2. Penyusunan `plan.md` & `tasklist.md` (7 Fase)**
  - [x] Buat [plan.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/plan.md).
  - [x] Buat [tasklist.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/tasklist.md).
- [x] **1.3. Persetujuan (Sign-off) User sebelum Implementasi Kode**
  - [x] Review `plan.md` dan `tasklist.md` oleh user sebelum memulai Fase 2.

---

### 🔹 Fase 2: Inisialisasi Proyek Maven, Struktur Package & Testdata
- [x] **2.1. Konfigurasi `pom.xml` (Java 21 LTS)**
  - [x] Set `maven.compiler.source/target` ke `21`.
  - [x] Daftarkan dependensi: `selenium-java` (`4.52.0`), `rest-assured-bom` (`6.3.0` + `rest-assured`, `json-path`, `json-schema-validator`), `testng` (`7.12.0`), `extentreports` (`5.1.2`), `poi` & `poi-ooxml` (`5.2.5`), `jackson-databind` (`2.17.2`), `commons-io` (`2.16.1`), `slf4j-simple` (`2.0.16`).
  - [x] Konfigurasi `maven-compiler-plugin` (`3.13.0`) dan `maven-surefire-plugin` (`3.5.0`) dengan default `<headless>true</headless>` serta `<suiteXmlFiles>` menunjuk ke `src/test/resources/testng.xml` (dapat dioverride `-Dheadless=false` dan `-DsuiteXmlFile=...`).
- [x] **2.2. Konfigurasi Git, Editor & Direktori Standar**
  - [x] Buat `.gitignore`, `.gitattributes`, dan `.editorconfig`.
  - [x] Salin `SIT_Scenario_Magang_Hadir.xlsx` ke `docs/SIT_Scenario_Magang_Hadir.xlsx`.
  - [x] Buat struktur package `com.juaracoding.magang.{drivers,pages/user/{deny,aji},reports,utils}` pada `src/main/java` dan `{listeners,api/{helper,user},tests/user/{deny,aji}}` pada `src/test/java`.
  - [x] Siapkan file dummy di `src/test/resources/testdata/`: gambar valid `.jpg`/`.png` (selfie/SKD/SPK), file non-gambar (`.pdf`, `.txt`, `.mp3`/`.zip`), dan file berukuran `> 5MB`.

---

### 🔹 Fase 3: Core Thread-Safe Engine, Keamanan Base64 & Hybrid API+UI Helper
- [x] **3.1. Utilitas Keamanan & Konstanta (`utils/`)**
  - [x] Implementasi `SecurityUtil.java` (`encode` / `decode` berbasis `java.util.Base64`).
  - [x] Implementasi `Constants.java`: URL Mobile (`https://magang.dikahadir.com/absen/login`), `API_BASE_URI = "https://api-dev.dikahadir.com/hadir-api-magang"`, endpoint riil (`/authentication`, `/users`, `/activity`, `/activity-correction`, `/off-permit`, `/late-permit`, `/early-leave`, `/overtime`, `/project-leave`), `USER_EMAIL`, `USER_PASSWORD_ENCODED = "U1FBQEhhZGlyMTIzNDU="`, `HYBRID_ADMIN_EMAIL`, `HYBRID_ADMIN_PASSWORD_ENCODED = "TWFnYW5nU1FBX0pDQDEyMw=="`, dan `VALID_NIK_POOL`.
- [x] **3.2. Thread-Safe WebDriver, Mobile Camera/GPS & Base POM**
  - [x] Implementasi `DriverFactory.java` (`ThreadLocal<WebDriver>`, Chrome default `headless = true` overridable via `-Dheadless=false`, `--use-fake-ui-for-media-stream`, `--use-fake-device-for-media-stream`, serta Chrome `prefs` allow `geolocation` & `media_stream_camera`).
  - [x] Implementasi `BasePage.java` (Explicit Wait 15s, `click` dengan fallback JS, `type`, `getText`, `isDisplayed`, `scrollToElement`, helper CDP untuk Geolocation override & Permission block, serta helper pengecekan broken image `naturalWidth`).
  - [x] Implementasi `BaseTest.java` (`@BeforeMethod` default `headless=true`, navigasi ke `/absen/login`, dan `@AfterMethod` `quitDriver()`).
- [x] **3.3. REST Assured Engine, Hybrid Helper & ExtentReports**
  - [x] Implementasi `BaseApiTest.java` (`RestAssured.useRelaxedHTTPSValidation()`, `RequestSpecBuilder` dengan `.relaxedHTTPSValidation()`, login `POST /authentication`, ekstraksi JWT `accessToken` & `user.id`).
  - [x] Implementasi `AdminApiHelper.java` untuk pola Hybrid `@Before` (seeding pengajuan sebagai Employee lalu `PATCH` Approve/Reject beserta alasan penolakan sebagai Admin via `HYBRID_ADMIN_PASSWORD_ENCODED`).
  - [x] Implementasi `ScreenshotUtil.java`, `ExtentManager.java` (Dark Theme), `ExtentTestManager.java` (`logInfo`, `logPass`, `logFail`), dan `TestListener.java` (auto-tag `.deny.` → `Tester Deny`, `.aji.` → `Tester Aji`, `.api.` → `REST Assured API`, `.tests.` → `Selenium Web UI`, serta Base64 screenshot saat gagal).
  - [x] Verifikasi kompilasi dengan `mvn clean test-compile`.

---

### 🔹 Fase 4: Implementasi Web Karyawan Page Object Model (POM)
*(Kerjakan pada feature branch terpisah sesuai aturan Git Workflow Opsi B; lakukan inspeksi DOM/locator live terlebih dahulu)*

#### 👤 Bagian Tester Deny (`src/main/java/com/juaracoding/magang/pages/user/deny/`):
- [ ] **4.1.** `UserLoginPage.java` (`/absen/login`: input email/password, ikon mata show/hide password, tombol Masuk, alert error, proteksi redirect `/apps/absent`, menu logout).
- [ ] **4.2.** `RegisterPage.java` (`/absen/register`: tautan Daftar Akun Baru, pencarian NIK, auto-fill Nama & Divisi, input email/password, upload foto wajah + preview & deteksi broken image, tautan Masuk).
- [ ] **4.3.** `ResetPasswordPage.java` (halaman Lupa Password & Reset Password: request OTP/link reset, validasi email, input password baru & konfirmasi, penanganan token kadaluarsa).
- [ ] **4.4.** `PermitPage.java` (`/apps/absent/permit`: form Izin Terlambat, Izin Pulang Cepat, tab Izin Off, modal Batal/Close, verifikasi kartu status `Pending` / `Approved` / `Rejected` + alasan penolakan).
- [ ] **4.5.** `LeaveAndSickPage.java` (`/apps/absent/leave` & `/apps/absent/sick`: cek Sisa Kuota Cuti, form Pengajuan Cuti, detail cuti `/apps/absent/leave/[id]`, form Sakit + upload SKD, preview & view/download SKD, filter histori tanggal).

#### 👤 Bagian Tester Aji (`src/main/java/com/juaracoding/magang/pages/user/aji/`):
- [ ] **4.6.** `AttendancePage.java` (`/apps/absent`: kartu profil, jam digital, info shift, modal kamera Absen Masuk/Pulang, capture & retake selfie, pilihan WFO/WFH, notes, tombol Masuk/Pulang, badge Tepat Waktu/Terlambat, verifikasi broken image).
- [ ] **4.7.** `ActivityHistoryPage.java` (`/apps/absent/activity`: daftar riwayat presensi, filter Start/End Date, detail jam & lokasi, Notes Masuk/Keluar, tombol Ajukan Koreksi dari kartu, Load More/Pagination, tombol Back, cek broken image).
- [ ] **4.8.** `CorrectionPage.java` (`/apps/absent/correction`: modal Pengajuan Koreksi Baru, input jam masuk/pulang & tanggal, tombol Reset, filter status/tanggal, verifikasi status `Pending` / `Approved` / `Rejected` + alasan).
- [ ] **4.9.** `OvertimePage.java` (`/apps/absent/overtime`: daftar riwayat lembur, modal ajukan lembur, upload lampiran SPK + verifikasi preview & broken view/download, halaman detail `/apps/absent/overtime/[id]`, filter tanggal, tombol Batal).
- [ ] **4.10.** `UserProfilePage.java` (Burger Menu di `/apps/absent`, navigasi ke `/apps/absent/change-password`, field Password Lama/Baru/Konfirmasi, ikon mata show/hide, tombol Submit, pesan error/sukses).

---

### 🔹 Fase 5: Implementasi Automated Test Suites (175 User TCs)
*(Setiap `@Test` wajib mencantumkan ID SIT pada `description` dan `ExtentTestManager.logInfo(...)`)*

#### 👤 Bagian Tester Deny (`87 TCs = 60 UI + 27 API`):
- [ ] **5.1.** `UserLoginTest.java` (`SIT_USR_UI_001–012`: 12 TCs — login valid/negatif, SQLi, XSS, proteksi auth `/apps/absent`, logout & browser back).
- [ ] **5.2.** `RegisterTest.java` (`SIT_USR_UI_013–025`: 13 TCs — pool NIK valid/invalid/terpakai, upload valid/non-image, dan verifikasi defect broken image `024`).
- [ ] **5.3.** `ResetPasswordTest.java` (`SIT_USR_UI_026–033`: 8 TCs — request reset email valid/invalid/kosong, submit password baru & token kadaluarsa).
- [ ] **5.4.** `PermitTest.java` (`SIT_USR_UI_075–084`: 10 TCs — Izin Terlambat, Pulang Cepat, Izin Off, serta skenario **Hybrid API+UI** pada `SIT_USR_UI_083` via `AdminApiHelper`).
- [ ] **5.5.** `LeaveAndSickTest.java` (`SIT_USR_UI_096–112`: 17 TCs — Cuti & Sakit, validasi tanggal, upload >5MB/non-image, serta verifikasi defect `101`, `106`, `107`).
- [ ] **5.6.** `UserGroupAApiTest.java` (`SIT_USR_API_001–018`, `044–049`, `053–055`: 27 TCs — Auth/JWT, Forgot/Reset Password, NIK & Register, Leaves/Izin/Sakit termasuk defect `047`, Profile & Change Password).

#### 👤 Bagian Tester Aji (`88 TCs = 60 UI + 28 API`):
- [ ] **5.7.** `AttendanceTest.java` (`SIT_USR_UI_034–053`: 20 TCs — kamera fake stream, WFO/WFH, Clock-In/Out, CDP geolocation luar radius `043`, CDP block kamera/lokasi `044–045`, serta defect broken image `042`, `049`).
- [ ] **5.8.** `ActivityHistoryTest.java` (`SIT_USR_UI_054–064`: 11 TCs — Log Absen, filter tanggal, detail, pagination, tombol koreksi, serta defect broken image `056`).
- [ ] **5.9.** `CorrectionTest.java` (`SIT_USR_UI_065–074`: 10 TCs — pengajuan koreksi masuk/pulang, validasi tanggal masa depan, reset, filter, dan verifikasi status via **Hybrid API+UI** `AdminApiHelper`).
- [ ] **5.10.** `OvertimeTest.java` (`SIT_USR_UI_085–095`: 11 TCs — pengajuan lembur, upload SPK, detail `[id]`, filter, **Hybrid API+UI**, dan verifikasi defect attachment `091`).
- [ ] **5.11.** `UserProfileTest.java` (`SIT_USR_UI_113–120`: 8 TCs — Burger menu & Ubah Password valid/negatif).
- [ ] **5.12.** `UserGroupBApiTest.java` (`SIT_USR_API_019–043`, `050–052`: 28 TCs — Clock-In/Out, SLA `<3000ms`, Activity Log & IDOR Check `038`, Corrections, Overtimes).

---

### 🔹 Fase 6: Konfigurasi Suite Runner & Verifikasi Eksekusi Headless
- [ ] **6.1.** Buat `src/test/resources/testng.xml` (Master Suite Karyawan: **175 TCs**, parameter default `browser=chrome`, `headless=true`, `parallel="classes"`).
- [ ] **6.2.** Buat `src/test/resources/testng-deny.xml` (**87 TCs**, `headless=true`) dan `src/test/resources/testng-aji.xml` (**88 TCs**, `headless=true`).
- [ ] **6.3.** Jalankan verifikasi API & suite terpisah lalu suite gabungan:
  - `mvn test -Dtest=UserGroupAApiTest,UserGroupBApiTest`
  - `mvn test -DsuiteXmlFile=src/test/resources/testng-deny.xml`
  - `mvn test -DsuiteXmlFile=src/test/resources/testng-aji.xml`
  - `mvn clean test`

---

### 🔹 Fase 7: Validasi ExtentReports, Sinkronisasi Status SIT & Finalisasi
- [ ] **7.1.** Validasi `reports/ExtentReport.html`: pastikan Dark Theme aktif, seluruh 175 Test ID tercatat, tag `Tester Deny` / `Tester Aji` / `Selenium Web UI` / `REST Assured API` terisi, dan Base64 screenshot tersemat pada kasus gagal/defect.
- [ ] **7.2.** Sinkronisasi hasil eksekusi aktual ke `SIT_Scenario_Magang_Hadir.xlsx` (dan salinannya di `docs/`).
- [ ] **7.3.** Buat `README.md` (panduan setup, struktur, arsitektur Hybrid, eksekusi headless/non-headless) dan `walkthrough.md` ringkasan hasil uji.

