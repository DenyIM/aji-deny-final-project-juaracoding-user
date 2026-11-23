# 📌 Tasklist Pelaksanaan: Automation Testing Web Karyawan `magang.dikahadir.com` (SIT v3 — 195 User TCs)

Roadmap implementasi kerangka kerja otomatisasi pengujian **Portal Karyawan / Mobile Mirroring** (`https://magang.dikahadir.com/absen/login`) berbasis **Java 21 LTS**, **Selenium WebDriver `4.52.0`** (default `headless = true`), **REST Assured `6.3.0`**, **TestNG `7.12.0`**, **ExtentReports `5.1.2`**, **Apache POI `5.2.5`**, dan **Jackson `2.17.2`** yang 100% selaras dengan repositori Web Admin (`final-project-admin`).

Total cakupan pengujian SIT v3: **195 Test Cases User** (**140 Web UI** + **55 REST Assured API**) yang terbagi adil antara **Tester Deny (99 TCs)** dan **Tester Aji (96 TCs)** dengan *Single Source of Truth* pada [docs/SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/docs/SIT_Scenario_Magang_Hadir.xlsx).

---

## 🧭 Ikhtisar Status 7 Fase & Alokasi Beban Kerja

| Fase | Deskripsi Fase | Output Deliverables | Penanggung Jawab | Status |
| :---: | :--- | :--- | :---: | :---: |
| **Fase 1** | Review Acuan (`WebKaryawanPrompt.md`, `docs/SIT_Scenario_Magang_Hadir.xlsx` v3, `reference.md`) & Sinkronisasi Blueprint | `plan.md`, `tasklist.md` (disinkronkan ke SIT v3: 195 TCs) | Shared Foundation | ✅ Selesai |
| **Fase 2** | Maven Setup (`pom.xml` Java 21, Selenium `4.52.0`, REST Assured `6.3.0`), Struktur Package Terisolasi (`deny` & `aji`), & File Testdata | `pom.xml`, `.gitignore`, `.gitattributes`, `.editorconfig`, `docs/SIT_Scenario_Magang_Hadir.xlsx`, `src/test/resources/testdata/*` | Shared Foundation | ✅ Selesai |
| **Fase 3** | Core Thread-Safe Engine, Keamanan Base64, Chrome Kamera/GPS Prefs (`headless=true`), & Hybrid `AdminApiHelper` | `SecurityUtil`, `Constants`, `DriverFactory`, `BasePage`, `BaseTest`, `BaseApiTest`, `AdminApiHelper`, `ScreenshotUtil`, `ExtentManager`, `ExtentTestManager`, `TestListener` | Shared Foundation | ✅ Selesai |
| **Fase 4** | Page Object Model (POM) Web Karyawan Implementation (Inspeksi Locator Live Mobile Mirroring) | POM Deny (`pages/user/deny/*`: 6 Classes; `UserLoginPage` ✅ di `feature/deny-user-login`) & POM Aji (`pages/user/aji/*`: 5 Classes) | Deny & Aji | 🔄 In Progress (Login selesai di branch fitur) |
| **Fase 5** | Automated Tests Implementation (140 Web UI + 55 REST Assured API + 6 List InfiniteScroll + 11 E2E Lintas Modul + CDP + Known Defect Assertion) | `tests/user/deny/*` (72 UI; `UserLoginTest` ✅ di `feature/deny-user-login`), `tests/user/aji/*` (68 UI), `UserGroupAApiTest` (27 API), `UserGroupBApiTest` (28 API) | Deny (99 TCs) & Aji (96 TCs) | 🔄 In Progress (Login selesai di branch fitur) |
| **Fase 6** | TestNG Multi-Suite Runner (`testng`, `testng-deny`, `testng-aji`) & Verifikasi Eksekusi Headless | `testng.xml` (195 TCs), `testng-deny.xml` (99 TCs), `testng-aji.xml` (96 TCs) *(XML disiapkan di `main`; verifikasi suite menunggu merge fitur)* | Deny & Aji | 🔄 Runner XML Disiapkan |
| **Fase 7** | Validasi `ExtentReport.html`, Sinkronisasi Status SIT, Dokumentasi `README.md` & `walkthrough.md` | `reports/ExtentReport.html`, `README.md`, `walkthrough.md` | Shared Deliverable | ⏳ Pending |

---

## ⚖️ Matriks Pembagian Tugas Adil (SIT v3 — Total 195 TCs)

| Aspek Kolaborasi | 👤 Tester Deny (99 TCs — 50.8%) | 👤 Tester Aji (96 TCs — 49.2%) |
| :--- | :--- | :--- |
| **Fitur Web UI (Selenium — 140 TCs)** | **72 Web UI TCs**:<br>1. `Autentikasi-Login` (`SIT_USR_UI_001–012`: 12 TCs) *(✅ utuh & selesai di `feature/deny-user-login`)*<br>2. `Autentikasi-Register` (`SIT_USR_UI_013–025`: 13 TCs)<br>3. `Autentikasi-Reset Password` (`SIT_USR_UI_026–033`: 8 TCs, termasuk OTP)<br>4. `Pengajuan-Izin` (`SIT_USR_UI_076–086`: 11 TCs — *Hybrid* + *InfiniteScroll* `086`)<br>5. `Pengajuan-Cuti & Sakit` (`SIT_USR_UI_099–117`: Cuti `099–107` [9 TCs, `/apps/absent/leave-client` + *InfiniteScroll* `107`] + Sakit `108–117` [10 TCs, `/apps/absent/sick` + *InfiniteScroll* `117`] = 19 TCs)<br>6. `Profil-Download Report` (`SIT_USR_UI_126–129`: 4 TCs `/apps/absent/reports/share_report`)<br>7. `Integrasi Lintas Modul (Hybrid API+UI)` (`SIT_USR_UI_130–134`: 5 TCs E2E) | **68 Web UI TCs**:<br>1. `Presensi-Beranda` (`SIT_USR_UI_034–053`: 20 TCs — *Fake Camera/GPS & CDP*)<br>2. `Presensi-Log Absen` (`SIT_USR_UI_054–064`: 11 TCs + *InfiniteScroll* `063`)<br>3. `Pengajuan-Koreksi Absen` (`SIT_USR_UI_065–075`: 11 TCs — *Hybrid* + *InfiniteScroll* `075`)<br>4. `Pengajuan-Lembur` (`SIT_USR_UI_087–098`: 12 TCs `/apps/absent/overtime-client` — *Hybrid* + *InfiniteScroll* `098`)<br>5. `Profil-Menu Utama & Ubah Password` (`SIT_USR_UI_118–125`: 8 TCs — tanpa field Password Lama)<br>6. `Integrasi Lintas Modul (Hybrid API+UI)` (`SIT_USR_UI_135–140`: 6 TCs E2E, termasuk Kuota Cuti `140` *Known Defect*) |
| **Fitur API (REST Assured — 55 TCs)** | **27 API TCs** (`UserGroupAApiTest`):<br>1. Auth & `/auth-management` Reset Password (`SIT_USR_API_001–010`: 10 TCs)<br>2. Registration & NIK Check (`SIT_USR_API_011–018`: 8 TCs)<br>3. Leaves/Izin/Cuti/Sakit (`SIT_USR_API_044–049`: 6 TCs)<br>4. Profile & `PATCH /users/{id}` Change Password (`SIT_USR_API_053–055`: 3 TCs) | **28 API TCs** (`UserGroupBApiTest`):<br>1. Attendance Clock-In/Out & SLA (`SIT_USR_API_019–030`: 12 TCs)<br>2. Activity Log & IDOR Security (`SIT_USR_API_031–038`: 8 TCs)<br>3. Corrections (`SIT_USR_API_039–043`: 5 TCs)<br>4. Overtimes (`SIT_USR_API_050–052`: 3 TCs) |
| **Scope Package POM** | `com.juaracoding.magang.pages.user.deny.*` (6 Page Classes) | `com.juaracoding.magang.pages.user.aji.*` (5 Page Classes) |
| **Scope Package Test** | `tests.user.deny.*` (7 Test Classes) & `UserGroupAApiTest` | `tests.user.aji.*` (6 Test Classes) & `UserGroupBApiTest` |
| **Git Branch (Opsi B)** | `feature/deny-user-login` *(selesai)*, `feature/deny-user-register`, `feature/deny-user-reset-password`, `feature/deny-user-permit`, `feature/deny-user-leave-sick`, `feature/deny-user-download-report`, `feature/deny-user-e2e`, `feature/deny-user-api-a` | `feature/aji-user-attendance`, `feature/aji-user-activity`, `feature/aji-user-correction`, `feature/aji-user-overtime`, `feature/aji-user-profile`, `feature/aji-user-e2e`, `feature/aji-user-api-b` |
| **Suite Runner XML** | `mvn test -DsuiteXmlFile=src/test/resources/testng-deny.xml` | `mvn test -DsuiteXmlFile=src/test/resources/testng-aji.xml` |

---

## 📋 Rincian Tugas per Fase

### 🔹 Fase 1: Perencanaan, Pemetaan SIT v3 & Review Blueprint
- [x] **1.1. Telaah Dokumen Acuan Proyek (SIT v3)**
  - [x] Baca dan selaraskan spesifikasi terbaru dari [WebKaryawanPrompt.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/WebKaryawanPrompt.md) dan [reference.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/reference.md).
  - [x] Ekstrak dan verifikasi seluruh **195 Test Cases User** (`SIT_USR_UI_001–140` dan `SIT_USR_API_001–055`) termasuk kolom `Severity`, `Priority`, `Actual Result / Defect Notes`, serta cakupan *InfiniteScroll* (`$skip=10*page`) pada [docs/SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/docs/SIT_Scenario_Magang_Hadir.xlsx).
  - [x] Selaraskan konvensi dan arsitektur dengan repositori **Web Admin (`final-project-admin`)**.
- [x] **1.2. Penyusunan & Sinkronisasi `plan.md` & `tasklist.md` (7 Fase)**
  - [x] Sinkronkan [plan.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/plan.md) ke SIT v3 (195 TCs).
  - [x] Sinkronkan [tasklist.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/tasklist.md) ke SIT v3 (195 TCs).
- [x] **1.3. Persetujuan (Sign-off) User**
  - [x] Blueprint awal disetujui; sinkronisasi SIT v3 siap direview di `main`.

---

### 🔹 Fase 2: Inisialisasi Proyek Maven, Struktur Package & Testdata
- [x] **2.1. Konfigurasi `pom.xml` (Java 21 LTS)**
  - [x] Set `maven.compiler.source/target` ke `21`.
  - [x] Daftarkan dependensi: `selenium-java` (`4.52.0`), `rest-assured-bom` (`6.3.0` + `rest-assured`, `json-path`, `json-schema-validator`), `testng` (`7.12.0`), `extentreports` (`5.1.2`), `poi` & `poi-ooxml` (`5.2.5`), `jackson-databind` (`2.17.2`), `commons-io` (`2.16.1`), `slf4j-simple` (`2.0.16`).
  - [x] Konfigurasi `maven-compiler-plugin` (`3.13.0`) dan `maven-surefire-plugin` (`3.5.0`) dengan default `<headless>true</headless>` serta `<suiteXmlFiles>` menunjuk ke `src/test/resources/testng.xml` (dapat dioverride `-Dheadless=false` dan `-DsuiteXmlFile=...`).
- [x] **2.2. Konfigurasi Git, Editor & Direktori Standar**
  - [x] Buat `.gitignore`, `.gitattributes`, dan `.editorconfig`.
  - [x] Gunakan `docs/SIT_Scenario_Magang_Hadir.xlsx` sebagai satu-satunya file SIT v3 (hapus duplikat lama di root).
  - [x] Buat struktur package `com.juaracoding.magang.{drivers,pages/user/{deny,aji},reports,utils}` pada `src/main/java` dan `{listeners,api/{helper,user},tests/user/{deny,aji}}` pada `src/test/java`.
  - [x] Siapkan file dummy di `src/test/resources/testdata/`: gambar valid `.jpg`/`.png` (selfie/SKD/SPK), file non-gambar (`.pdf`, `.txt`, `.mp3`), dan file berukuran `> 5MB`.

---

### 🔹 Fase 3: Core Thread-Safe Engine, Keamanan Base64 & Hybrid API+UI Helper
- [x] **3.1. Utilitas Keamanan & Konstanta (`utils/`)**
  - [x] Implementasi `SecurityUtil.java` (`encode` / `decode` berbasis `java.util.Base64`).
  - [x] Implementasi & sinkronisasi `Constants.java` ke SIT v3: URL Mobile (`/absen/login`, `/absen/register`, `/absen/forgot-password`, `/absen/reset-password-request`, `/absen/reset-password`, `/apps/absent/reports/share_report`, serta rute alias `/apps/absent/leave-client` & `/apps/absent/overtime-client`), `API_BASE_URI = "https://api-dev.dikahadir.com/hadir-api-magang"`, endpoint FeathersJS riil (`/authentication`, `/auth-management`, `/users`, `/activity`, `/activity/request-activity`, `/activity-correction`, `/off-permit`, `/late-permit`, `/early-leave`, `/overtime`, `/project-leave`, `/sick`, `/share-report`, `/upload`, `/location-point`, `/business-trips`), `USER_PASSWORD_ENCODED`, `HYBRID_ADMIN_PASSWORD_ENCODED`, dan `VALID_NIK_POOL`.
- [x] **3.2. Thread-Safe WebDriver, Mobile Camera/GPS & Base POM**
  - [x] Implementasi `DriverFactory.java` (`ThreadLocal<WebDriver>`, Chrome default `headless = true` overridable via `-Dheadless=false`, `--use-fake-ui-for-media-stream`, `--use-fake-device-for-media-stream`, serta Chrome `prefs` allow `geolocation` & `media_stream_camera`).
  - [x] Implementasi `BasePage.java` (Explicit Wait 15s, `click` dengan fallback JS, `type`, `getText`, `isDisplayed`, `scrollToElement`, helper CDP `HasCdp` untuk Geolocation override & Permission block, serta helper pengecekan broken image `naturalWidth`).
  - [x] Implementasi `BaseTest.java` (`@BeforeMethod` default `headless=true` dan `@AfterMethod` `quitDriver()`).
- [x] **3.3. REST Assured Engine, Hybrid Helper & ExtentReports**
  - [x] Implementasi `BaseApiTest.java` (`RestAssured.useRelaxedHTTPSValidation()`, `RequestSpecBuilder` dengan `.relaxedHTTPSValidation()`, login `POST /authentication`, ekstraksi JWT `accessToken` & `user.id`).
  - [x] Implementasi `AdminApiHelper.java` untuk pola Hybrid `@Before` (seeding pengajuan sebagai Employee lalu `PATCH` Approve/Reject beserta alasan penolakan sebagai Admin via `HYBRID_ADMIN_PASSWORD_ENCODED`).
  - [x] Implementasi `ScreenshotUtil.java`, `ExtentManager.java` (Dark Theme + metadata 195 TCs), `ExtentTestManager.java` (`logInfo`, `logPass`, `logFail`), dan `TestListener.java` (auto-tag `.deny.` / `GroupA` → `Tester Deny`, `.aji.` / `GroupB` → `Tester Aji`, `.api.` → `REST Assured API`, `.tests.` → `Selenium Web UI`, serta Base64 screenshot saat gagal).
  - [x] Verifikasi kompilasi dengan `mvn clean test-compile`.

---

### 🔹 Fase 4: Implementasi Web Karyawan Page Object Model (POM)
*(Kerjakan pada feature branch terpisah sesuai aturan Git Workflow Opsi B; lakukan inspeksi DOM/locator live terlebih dahulu)*

#### 👤 Bagian Tester Deny (`src/main/java/com/juaracoding/magang/pages/user/deny/`):
- [x] **4.1.** `UserLoginPage.java` (`/absen/login`: input email/password, ikon mata show/hide password, tombol Masuk, alert error, proteksi redirect `/apps/absent`, menu burger & tombol Logout) — *✅ Utuh & selesai di branch `feature/deny-user-login` (belum di-merge ke `main`).*
- [ ] **4.2.** `RegisterPage.java` (`/absen/register`: tautan Daftar Akun Baru, pencarian NIK, auto-fill Nama & Divisi, input email/password, upload foto wajah + preview & deteksi broken image, tautan Masuk).
- [ ] **4.3.** `ResetPasswordPage.java` (`/absen/forgot-password` / `/absen/reset-password-request` & `/absen/reset-password`: request reset email, input **kode OTP**, Password Baru & Konfirmasi Password, penanganan OTP invalid/kadaluarsa).
- [ ] **4.4.** `PermitPage.java` (`/apps/absent/permit`: form Izin Terlambat, Izin Pulang Cepat, tab Izin Off, modal Batal/Close, verifikasi kartu status `Pending` / `Approved` / `Rejected` + alasan penolakan, serta *InfiniteScroll* `$skip=10*page`).
- [ ] **4.5.** `LeaveAndSickPage.java` (`/apps/absent/leave-client` / `/apps/absent/leave` & `/apps/absent/sick`: cek Sisa Kuota Cuti, form Pengajuan Cuti, detail cuti `/apps/absent/leave/[id]`, form Sakit + upload SKD, preview & view/download SKD, filter histori tanggal, serta *InfiniteScroll* `$skip=10*page` pada Cuti & Sakit).
- [ ] **4.6.** `DownloadReportPage.java` (`/apps/absent/reports/share_report`: akses menu Download Reports dari beranda `/apps/absent`, dropdown Tipe Report Harian/Mingguan/Bulanan, tombol `Download Report`, notifikasi validasi/data kosong).

#### 👤 Bagian Tester Aji (`src/main/java/com/juaracoding/magang/pages/user/aji/`):
- [ ] **4.7.** `AttendancePage.java` (`/apps/absent`: kartu profil, jam digital, info shift, modal kamera Absen Masuk/Pulang, capture & retake selfie, pilihan WFO/WFH, notes, tombol Masuk/Pulang, badge Tepat Waktu/Terlambat, verifikasi broken image).
- [ ] **4.8.** `ActivityHistoryPage.java` (`/apps/absent/activity`: daftar riwayat presensi, filter Start/End Date, detail jam & lokasi, Notes Masuk/Keluar, tombol Ajukan Koreksi dari kartu, *InfiniteScroll* `$skip=10*page` muat data berikutnya, tombol Back, cek broken image).
- [ ] **4.9.** `CorrectionPage.java` (`/apps/absent/correction`: modal Pengajuan Koreksi Baru, input jam masuk/pulang & tanggal, tombol Reset, filter status/tanggal, verifikasi status `Pending` / `Approved` / `Rejected` + alasan, serta *InfiniteScroll* `$skip=10*page`).
- [ ] **4.10.** `OvertimePage.java` (`/apps/absent/overtime-client` / `/apps/absent/overtime`: daftar riwayat lembur, modal ajukan lembur, upload lampiran SPK + verifikasi preview & view/download, halaman detail `/apps/absent/overtime/[id]`, filter tanggal, tombol Batal, serta *InfiniteScroll* `$skip=10*page`).
- [ ] **4.11.** `UserProfilePage.java` (Burger Menu di `/apps/absent`, navigasi ke `/apps/absent/change-password`: **HANYA** field **Password Baru** `password` & **Confirm Password** `passwordConfirmation` + ikon mata show/hide + tombol Simpan — **tanpa** field Password Lama).

---

### 🔹 Fase 5: Implementasi Automated Test Suites (195 User TCs)
*(Setiap `@Test` wajib mencantumkan ID SIT pada `description` dan `ExtentTestManager.logInfo(...)`. Untuk kasus `Ready (Known Defect)`, assertion memverifikasi Expected Result standar dan mencatat temuan defect live beserta `Severity`/`Priority` ke `ExtentTestManager`.)*

#### 👤 Bagian Tester Deny (`99 TCs = 72 UI + 27 API`):
- [x] **5.1.** `UserLoginTest.java` (`SIT_USR_UI_001–012`: 12 TCs — login valid/negatif, SQLi, XSS, proteksi auth `/apps/absent`, logout & browser back) — *✅ Utuh & lulus 12/12 di branch `feature/deny-user-login` (belum di-merge ke `main`).*
- [ ] **5.2.** `RegisterTest.java` (`SIT_USR_UI_013–025`: 13 TCs — pool NIK valid/invalid/terpakai, upload valid/non-image, dan assertion Expected Result standar + catatan Known Defect `024`).
- [ ] **5.3.** `ResetPasswordTest.java` (`SIT_USR_UI_026–033`: 8 TCs — request reset email valid/invalid/kosong, submit password baru dengan **kode OTP** & OTP invalid/kadaluarsa).
- [ ] **5.4.** `PermitTest.java` (`SIT_USR_UI_076–086`: 11 TCs — Izin Terlambat, Pulang Cepat, Izin Off, verifikasi kartu status via `AdminApiHelper`, serta *InfiniteScroll* `086`).
- [ ] **5.5.** `LeaveAndSickTest.java` (`SIT_USR_UI_099–117`: 19 TCs — Cuti `099–107` di `/apps/absent/leave-client` [*InfiniteScroll* `107`] & Sakit `108–117` di `/apps/absent/sick` [*InfiniteScroll* `117`], validasi tanggal, upload >5MB/non-image, serta assertion Expected Result standar + catatan Known Defect `104`, `110`, `111`).
- [ ] **5.6.** `DownloadReportTest.java` (`SIT_USR_UI_126–129`: 4 TCs — navigasi `/apps/absent/reports/share_report`, download report Mingguan/Bulanan via `POST /share-report`, validasi tipe report kosong, periode tanpa data).
- [ ] **5.7.** `UserCrossModuleDenyE2ETest.java` (`SIT_USR_UI_130–134`: 5 TCs — E2E Lintas Modul Hybrid API+UI: aktif/nonaktif user via Admin API `130–131`, Approve/Reject Izin Off `132–133`, Cancel Cuti & sinkronisasi kuota `134`).
- [ ] **5.8.** `UserGroupAApiTest.java` (`SIT_USR_API_001–018`, `044–049`, `053–055`: 27 TCs — `/authentication`, `/auth-management`, `/users` NIK & register, `/off-permit`, `/project-leave` termasuk Known Defect `047`, `/sick`, `PATCH /users/{id}`).

#### 👤 Bagian Tester Aji (`96 TCs = 68 UI + 28 API`):
- [ ] **5.9.** `AttendanceTest.java` (`SIT_USR_UI_034–053`: 20 TCs — kamera fake stream, WFO/WFH, Clock-In/Out, CDP geolocation luar radius `043`, CDP block kamera/lokasi `044–045`, serta assertion Expected Result standar + catatan Known Defect `042`, `049`).
- [ ] **5.10.** `ActivityHistoryTest.java` (`SIT_USR_UI_054–064`: 11 TCs — Log Absen, filter tanggal, detail, *InfiniteScroll* `063`, tombol koreksi, serta assertion Expected Result standar + catatan Known Defect `056`).
- [ ] **5.11.** `CorrectionTest.java` (`SIT_USR_UI_065–075`: 11 TCs — pengajuan koreksi masuk/pulang, validasi tanggal masa depan, reset, filter, verifikasi status, serta *InfiniteScroll* `075`).
- [ ] **5.12.** `OvertimeTest.java` (`SIT_USR_UI_087–098`: 12 TCs — `/apps/absent/overtime-client` / `/apps/absent/overtime`, pengajuan lembur, upload SPK, detail `[id]`, filter, *InfiniteScroll* `098`, serta assertion Expected Result standar + catatan Known Defect `093`).
- [ ] **5.13.** `UserProfileTest.java` (`SIT_USR_UI_118–125`: 8 TCs — Burger menu `118` & `/apps/absent/change-password` 2 field `119–125`: Password Baru & Confirm Password tanpa Password Lama).
- [ ] **5.14.** `UserCrossModuleAjiE2ETest.java` (`SIT_USR_UI_135–140`: 6 TCs — E2E Lintas Modul Hybrid API+UI: geofence `GET /location-point` + CDP Clock-In `135`, Clock-In -> `/apps/absent/activity` & `GET /activity` `136`, Approve/Reject Koreksi `137–138`, Approve/Reject Lembur `139`, serta **Known Defect** sinkron Jumlah Cuti pendaftaran Admin `Jumlah Cuti=12` -> `/apps/absent/leave` `140`).
- [ ] **5.15.** `UserGroupBApiTest.java` (`SIT_USR_API_019–043`, `050–052`: 28 TCs — `/activity`, `/activity/request-activity` Clock-Out, SLA `<3000ms`, IDOR Check `038`, `/activity-correction`, `/overtime`).

---

### 🔹 Fase 6: Konfigurasi Suite Runner & Verifikasi Eksekusi Headless
- [x] **6.1.** Buat `src/test/resources/testng.xml` (Master Suite Karyawan SIT v3: **195 TCs**, parameter default `browser=chrome`, `headless=true`, `parallel="classes"`).
- [x] **6.2.** Buat `src/test/resources/testng-deny.xml` (**99 TCs**, `headless=true`) & `testng-aji.xml` (**96 TCs**, `headless=true`).
- [ ] **6.3.** Jalankan verifikasi API & suite terpisah lalu suite gabungan *(setelah Fase 4–5 seluruh branch fitur selesai & di-merge ke `main`)*:
  - `mvn test -Dtest=UserGroupAApiTest,UserGroupBApiTest`
  - `mvn test -DsuiteXmlFile=src/test/resources/testng-deny.xml`
  - `mvn test -DsuiteXmlFile=src/test/resources/testng-aji.xml`
  - `mvn clean test`

---

### 🔹 Fase 7: Validasi ExtentReports, Sinkronisasi Status SIT & Finalisasi
- [ ] **7.1.** Validasi `reports/ExtentReport.html`: pastikan Dark Theme aktif, seluruh 195 Test ID tercatat, tag `Tester Deny` / `Tester Aji` / `Selenium Web UI` / `REST Assured API` terisi, dan Base64 screenshot tersemat pada kasus gagal/defect.
- [ ] **7.2.** Sinkronisasi hasil eksekusi aktual ke `docs/SIT_Scenario_Magang_Hadir.xlsx`.
- [ ] **7.3.** Buat `README.md` (panduan setup, struktur, arsitektur Hybrid, eksekusi headless/non-headless) dan `walkthrough.md` ringkasan hasil uji.
