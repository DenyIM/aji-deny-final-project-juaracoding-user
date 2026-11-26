# 📌 Tasklist Pelaksanaan: Automation Testing Web Karyawan `magang.dikahadir.com` (SIT v4 — 250 User TCs)

Roadmap implementasi kerangka kerja otomatisasi pengujian **Portal Karyawan / Mobile Mirroring** (`https://magang.dikahadir.com/absen/login`) berbasis **Java 21 LTS**, **Selenium WebDriver `4.52.0`** (default `headless = true`), **REST Assured `6.3.0`**, **TestNG `7.12.0`**, **ExtentReports `5.1.2`**, **Apache POI `5.2.5`**, dan **Jackson `2.17.2`** yang 100% selaras dengan standar terbaru repositori Web Admin (`final-project-admin`).

Total cakupan pengujian SIT v4: **250 Test Cases User** (**140 Web UI** + **110 REST Assured API**) yang terbagi adil antara **Tester Deny (`127 TCs`: `72 UI + 55 API`)** dan **Tester Aji (`123 TCs`: `68 UI + 55 API`)** dengan *Single Source of Truth* pada [docs/SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/docs/SIT_Scenario_Magang_Hadir.xlsx).

---

## 🧭 Ikhtisar Status 7 Fase & Alokasi Beban Kerja

| Fase | Deskripsi Fase | Output Deliverables | Penanggung Jawab | Status |
| :---: | :--- | :--- | :---: | :---: |
| **Fase 1** | Review Acuan (`WebKaryawanPrompt.md`, `docs/SIT_Scenario_Magang_Hadir.xlsx` v4, `reference.md`) & Sinkronisasi Blueprint ke SIT v4 | `plan.md`, `tasklist.md`, `WebKaryawanPrompt.md` (disinkronkan ke SIT v4: 250 TCs) | Shared Foundation | ✅ Selesai |
| **Fase 2** | Maven Setup (`pom.xml` Java 21, Selenium `4.52.0`, REST Assured `6.3.0`), Struktur Package Terisolasi (`deny` & `aji`), & Fixture `testdata/*` | `pom.xml`, `.gitignore`, `.gitattributes`, `.editorconfig`, `docs/SIT_Scenario_Magang_Hadir.xlsx`, `src/test/resources/testdata/*` | Shared Foundation | ✅ Selesai |
| **Fase 3** | Core Thread-Safe Engine, Keamanan Base64, Dual-API (`FeathersJS` + `Next.js BFF`), MUI Helpers, & `AdminApiHelper` SIT v4 | `SecurityUtil`, `Constants`, `DriverFactory`, `BasePage`, `BaseTest`, `BaseApiTest`, `AdminApiHelper`, `ScreenshotUtil`, `ExtentManager`, `ExtentTestManager`, `TestListener` | Shared Foundation | ✅ Selesai |
| **Fase 4** | Page Object Model (POM) Web Karyawan Implementation (Inspeksi Locator Live Mobile Mirroring) | POM Deny (`pages/user/deny/*`: 6 Classes; `UserLoginPage` ✅ di `main`) & POM Aji (`pages/user/aji/*`: 5 Classes) | Deny & Aji | 🔄 In Progress (`UserLoginPage` selesai & terintegrasi di `main`) |
| **Fase 5** | Automated Tests Implementation (140 Web UI + 110 REST Assured API + 6 List InfiniteScroll + 11 E2E Lintas Modul + Strict QA `[KNOWN DEFECT]`) | `tests/user/deny/*` (72 UI; `UserLoginTest` ✅ 12/12 di `main`), `tests/user/aji/*` (68 UI), `UserGroupAApiTest` (55 API), `UserGroupBApiTest` (55 API) | Deny (127 TCs) & Aji (123 TCs) | 🔄 In Progress (`UserLoginTest` 12/12 selesai di `main`) |
| **Fase 6** | 7 TestNG Sequential Suites (`testng`, `deny`, `aji`, `smoke`, `regression`, `positive`, `negative`) & Verifikasi Headless | `src/test/resources/testng*.xml` (7 suite XML sekuensial, `UserLoginTest` terdaftar aktif) | Deny & Aji | 🔄 7 Suite XML Aktif (Login Terverifikasi) |
| **Fase 7** | Validasi `ExtentReport.html`, Sinkronisasi Status SIT v4, Dokumentasi `README.md` & `walkthrough.md` | `reports/ExtentReport.html`, `README.md`, `walkthrough.md` | Shared Deliverable | ⏳ Pending |

---

## ⚖️ Matriks Pembagian Tugas Adil (SIT v4 — Total 250 TCs)

| Aspek Kolaborasi | 👤 Tester Deny (127 TCs — 50.8%) | 👤 Tester Aji (123 TCs — 49.2%) |
| :--- | :--- | :--- |
| **Fitur Web UI (Selenium — 140 TCs)** | **72 Web UI TCs**:<br>1. `Autentikasi-Login` (`SIT_USR_UI_001–012`: 12 TCs) *(✅ Selesai & lulus 12/12 di `main`)*<br>2. `Autentikasi-Register` (`SIT_USR_UI_013–025`: 13 TCs)<br>3. `Autentikasi-Reset Password` (`SIT_USR_UI_026–033`: 8 TCs, termasuk OTP)<br>4. `Pengajuan-Izin` (`SIT_USR_UI_076–086`: 11 TCs — *Hybrid* + *InfiniteScroll* `086`)<br>5. `Pengajuan-Cuti & Sakit` (`SIT_USR_UI_099–117`: Cuti `099–107` [9 TCs, `/apps/absent/leave-client` + *InfiniteScroll* `107`] + Sakit `108–117` [10 TCs, `/apps/absent/sick` + *InfiniteScroll* `117`] = 19 TCs)<br>6. `Profil-Download Report` (`SIT_USR_UI_126–129`: 4 TCs `/apps/absent/reports/share_report`)<br>7. `Integrasi Lintas Modul (Hybrid API+UI)` (`SIT_USR_UI_130–134`: 5 TCs E2E) | **68 Web UI TCs**:<br>1. `Presensi-Beranda` (`SIT_USR_UI_034–053`: 20 TCs — *Fake Camera/GPS & CDP*)<br>2. `Presensi-Log Absen` (`SIT_USR_UI_054–064`: 11 TCs + *InfiniteScroll* `063`)<br>3. `Pengajuan-Koreksi Absen` (`SIT_USR_UI_065–075`: 11 TCs — *Hybrid* + *InfiniteScroll* `075`)<br>4. `Pengajuan-Lembur` (`SIT_USR_UI_087–098`: 12 TCs `/apps/absent/overtime-client` — *Hybrid* + *InfiniteScroll* `098`)<br>5. `Profil-Menu Utama & Ubah Password` (`SIT_USR_UI_118–125`: 8 TCs — tanpa field Password Lama)<br>6. `Integrasi Lintas Modul (Hybrid API+UI)` (`SIT_USR_UI_135–140`: 6 TCs E2E, termasuk Kuota Cuti `140` *Known Defect*) |
| **Fitur API (REST Assured — 110 TCs)** | **55 API TCs** (`UserGroupAApiTest`):<br>1. Auth & `/auth-management` Reset Password (`SIT_USR_API_001–010`: 10 TCs)<br>2. Registration & NIK Check (`SIT_USR_API_011–018`: 8 TCs)<br>3. Leaves/Izin/Sakit + Late/Early Permits (`SIT_USR_API_044–049, 056–063`: 14 TCs)<br>4. Multipart `/upload` & BFF `/api/sick` + `/api/activity/sick` (`SIT_USR_API_064–071`: 8 TCs)<br>5. Share Report `/share-report` Export & IDOR (`SIT_USR_API_072–075`: 4 TCs)<br>6. Project-Leave, Profile, Critical IDOR & Mass-Assignment (`SIT_USR_API_053–055, 076–083`: 11 TCs) | **55 API TCs** (`UserGroupBApiTest`):<br>1. Attendance Clock-In/Out, `/activity/request-activity` & `/user-summary` (`SIT_USR_API_019–030, 090–096`: 19 TCs)<br>2. Activity Log (`created_at[$gte]`/`[$lte]`) & IDOR Security (`SIT_USR_API_031–038`: 8 TCs)<br>3. Corrections (`SIT_USR_API_039–043`: 5 TCs)<br>4. Overtimes & `/business-trips` (`SIT_USR_API_050–052, 084–089`: 9 TCs)<br>5. Leader/Client BFF Isolation (`/api/client/*`), Query Hardening & Self-Approval RBAC (`SIT_USR_API_097–110`: 14 TCs) |
| **Scope Package POM** | `com.juaracoding.magang.pages.user.deny.*` (6 Page Classes) | `com.juaracoding.magang.pages.user.aji.*` (5 Page Classes) |
| **Scope Package Test** | `tests.user.deny.*` (7 Test Classes) & `UserGroupAApiTest` | `tests.user.aji.*` (6 Test Classes) & `UserGroupBApiTest` |
| **Git Branch (Opsi B)** | `feature/deny-user-login` *(✅ terintegrasi ke `main`)*, `feature/deny-user-register`, `feature/deny-user-reset-password`, `feature/deny-user-permit`, `feature/deny-user-leave-sick`, `feature/deny-user-download-report`, `feature/deny-user-e2e`, `feature/deny-user-api-a` | `feature/aji-user-attendance`, `feature/aji-user-activity`, `feature/aji-user-correction`, `feature/aji-user-overtime`, `feature/aji-user-profile`, `feature/aji-user-e2e`, `feature/aji-user-api-b` |
| **Suite Runner XML** | `mvn test "-DsuiteXmlFile=src/test/resources/testng-deny.xml"` | `mvn test "-DsuiteXmlFile=src/test/resources/testng-aji.xml"` |

---

## 📋 Rincian Tugas per Fase

### 🔹 Fase 1: Perencanaan, Pemetaan SIT v4 & Review Blueprint
- [x] **1.1. Telaah Dokumen Acuan Proyek (SIT v4)**
  - [x] Salin dan selaraskan [docs/SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/docs/SIT_Scenario_Magang_Hadir.xlsx) serta [WebKaryawanPrompt.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/WebKaryawanPrompt.md) dari `final-project-admin` (termasuk perbaikan referensi Download Reports `SIT_USR_UI_126–129`).
  - [x] Verifikasi seluruh **250 Test Cases User** (`SIT_USR_UI_001–140` dan `SIT_USR_API_001–110`) termasuk kolom `Test Suite`, `Severity`, `Priority`, `Actual Result / Defect Notes`, dan `Manual Check`.
- [x] **1.2. Penyusunan & Sinkronisasi `plan.md` & `tasklist.md` (7 Fase)**
  - [x] Sinkronkan [plan.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/plan.md) dan [tasklist.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/tasklist.md) ke SIT v4 (250 TCs: Deny 127 / Aji 123).
- [x] **1.3. Persetujuan (Sign-off) User**
  - [x] Implementation plan sinkronisasi SIT v4 disetujui dan dieksekusi di `main`.

---

### 🔹 Fase 2: Inisialisasi Proyek Maven, Struktur Package & Testdata
- [x] **2.1. Konfigurasi `pom.xml` (Java 21 LTS — Selaras Admin)**
  - [x] Set `maven.compiler.source/target` ke `21` dan deklarasi versi eksplisit `selenium-java` (`4.52.0`), `rest-assured` / `json-path` / `json-schema-validator` (`6.3.0`), `testng` (`7.12.0`), `extentreports` (`5.1.2`), `poi` & `poi-ooxml` (`5.2.5`), `jackson-databind` (`2.17.2`), `commons-io` (`2.16.1`), `slf4j-simple` (`2.0.16`).
  - [x] Konfigurasi `maven-surefire-plugin` (`3.5.0`) dengan default `<headless>true</headless>` dan `<suiteXmlFile>src/test/resources/testng.xml</suiteXmlFile>`.
- [x] **2.2. Konfigurasi Git, Editor & Fixture Testdata SIT v4**
  - [x] Pertahankan `docs/SIT_Scenario_Magang_Hadir.xlsx` sebagai satu-satunya file SIT (tanpa duplikat di root).
  - [x] Lengkapi `src/test/resources/testdata/` dengan `valid_photo.jpg`, `oversized_photo.jpg`, `valid_selfie.jpg`, `valid_selfie.png`, `oversized_image.jpg`, `invalid_upload.pdf`, `invalid_upload.txt`, dan `invalid_upload.mp3`.

---

### 🔹 Fase 3: Core Thread-Safe Engine, Keamanan Base64, Dual-API & Hybrid `AdminApiHelper`
- [x] **3.1. Utilitas Keamanan & Konstanta (`utils/`)**
  - [x] Implementasi `SecurityUtil.java` (`encode` / `decode` berbasis `java.util.Base64`, nol plaintext password).
  - [x] Sinkronisasi `Constants.java` ke SIT v4: URL Mobile + rute alias client, `API_BASE_URI`, `API_STATIC_UPLOADS_BASE_URI = "https://api-dev.dikahadir.com/uploads"`, `API_USER_SUMMARY_ENDPOINT = "/user-summary"`, `API_CANCEL_LEAVE_ENDPOINT = "/cancel-leave"`, rute Next.js BFF (`/api/login`, `/api/sick`, `/api/activity/sick`, `/api/leave/project-leave`, `/api/client/generate-report`, `/api/client/leave-report`, `/api/client/overtime-report`), serta 35 NIK pada `VALID_NIK_POOL`.
- [x] **3.2. Thread-Safe WebDriver, Mobile Camera/GPS & Base POM (`BasePage.java`)**
  - [x] `DriverFactory.java` (`ThreadLocal<WebDriver>`, Chrome default `headless = true`, fake media stream & allow geolocation/camera).
  - [x] `BasePage.java` (Explicit Wait 15s, `safeNavigate`, `openSwipeableDrawerWithRetry`, `openMuiSelectWithMouseDown`, `dispatchCalendarDayMouseEvents`, `scrollToBottomForInfiniteScroll`, `clearBrowserStorageAndCookies`, CDP `HasCdp`, serta `isImageBroken`).
  - [x] `BaseTest.java` (`@BeforeMethod` default `headless=true` dan `@AfterMethod` `quitDriver()`).
- [x] **3.3. Dual-API Engine, `AdminApiHelper` SIT v4 & ExtentReports**
  - [x] `BaseApiTest.java` (FeathersJS Bearer spec + Next.js BFF Cookie `hadir_app_magang` spec via `POST /api/login`).
  - [x] Sinkronisasi `AdminApiHelper.java` ke temuan live Admin: `POST /cancel-leave` (`leave_trans_id`, `lv_approve_notes`), `GET /project-leave/{userId}`, `uploadTestPhoto` dengan MIME eksplisit (`image/jpeg` / `image/png`), `createPendingLatePermit`, `createPendingEarlyLeave`, `createPendingBusinessTrip`, `findUserIdByEmail`, `findAvailableNikFromPool`, `createSelfRegisteredMobileUser`, `isVerified: true` pada `updateUserActiveStatusAsAdmin`, serta `queryActivitiesByDateRange` memakai `created_at[$gte]` & `created_at[$lte]`.
  - [x] Update metadata `ExtentManager.java` ke SIT v4 (`250 TCs`: Deny `127` / Aji `123`).

---

### 🔹 Fase 4: Implementasi Web Karyawan Page Object Model (POM)
*(Kerjakan setiap modul baru pada feature branch terpisah sesuai aturan Git Workflow Opsi B)*

#### 👤 Bagian Tester Deny (`src/main/java/com/juaracoding/magang/pages/user/deny/`):
- [x] **4.1.** `UserLoginPage.java` (`/absen/login`: input email/password, ikon mata show/hide password, tombol Masuk, alert error, proteksi redirect `/apps/absent`, menu burger & tombol Logout) — **✅ Selesai & terintegrasi di `main`.**
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

### 🔹 Fase 5: Implementasi Automated Test Suites (250 User TCs)
*(Setiap `@Test` wajib mencantumkan ID SIT pada `description`, `ExtentTestManager.logInfo(...)`, serta grup `"regression"` + `"positive"`/`"negative"` [+ `"smoke"` untuk critical path]. Untuk `Ready (Known Defect)`, wajib assert Expected Result standar dengan prefix `"[KNOWN DEFECT] ..."`.)*

#### 👤 Bagian Tester Deny (`127 TCs = 72 UI + 55 API`):
- [x] **5.1.** `UserLoginTest.java` (`SIT_USR_UI_001–012`: 12 TCs — login valid/negatif, SQLi, XSS, proteksi auth `/apps/absent`, logout & browser back; grup `smoke`/`regression`/`positive`/`negative`) — **✅ Selesai & lulus 12/12 di `main`.**
- [ ] **5.2.** `RegisterTest.java` (`SIT_USR_UI_013–025`: 13 TCs — pool NIK valid/invalid/terpakai, upload valid/non-image, dan Strict QA `[KNOWN DEFECT]` `024`).
- [ ] **5.3.** `ResetPasswordTest.java` (`SIT_USR_UI_026–033`: 8 TCs — request reset email valid/invalid/kosong, submit password baru dengan **kode OTP** & OTP invalid/kadaluarsa).
- [ ] **5.4.** `PermitTest.java` (`SIT_USR_UI_076–086`: 11 TCs — Izin Terlambat, Pulang Cepat, Izin Off, verifikasi kartu status via `AdminApiHelper`, serta *InfiniteScroll* `086`).
- [ ] **5.5.** `LeaveAndSickTest.java` (`SIT_USR_UI_099–117`: 19 TCs — Cuti `099–107` di `/apps/absent/leave-client` [*InfiniteScroll* `107`] & Sakit `108–117` di `/apps/absent/sick` [*InfiniteScroll* `117`], validasi tanggal, upload >5MB/non-image, serta Strict QA `[KNOWN DEFECT]` `104`, `110`, `111`).
- [ ] **5.6.** `DownloadReportTest.java` (`SIT_USR_UI_126–129`: 4 TCs — navigasi `/apps/absent/reports/share_report`, download report Mingguan/Bulanan via `POST /share-report`, validasi tipe report kosong, periode tanpa data).
- [ ] **5.7.** `UserCrossModuleDenyE2ETest.java` (`SIT_USR_UI_130–134`: 5 TCs — E2E Lintas Modul Hybrid API+UI: aktif/nonaktif user via `PATCH /users/{id}` `isVerified=true` `130–131`, Approve/Reject Izin Off `132–133`, `POST /cancel-leave` & sinkronisasi kuota `134`).
- [ ] **5.8.** `UserGroupAApiTest.java` (`SIT_USR_API_001–018`, `044–049`, `053–083`: 55 TCs — `/authentication`, `/auth-management` termasuk invalid OTP/action `081–082`, `/users` NIK/register/IDOR/Mass-Assignment/DELETE `053–055, 078–080, 083`, `/off-permit`, `/late-permit` `056–059`, `/early-leave` `060–063`, `/upload` security `064–067`, Next.js BFF `/api/sick` & `/api/activity/sick` `068–071`, `/share-report` `072–075`, `/project-leave` & BFF `/api/leave/project-leave/{other_id}` `045–047, 076–077` termasuk `[KNOWN DEFECT]` `047`).

#### 👤 Bagian Tester Aji (`123 TCs = 68 UI + 55 API`):
- [ ] **5.9.** `AttendanceTest.java` (`SIT_USR_UI_034–053`: 20 TCs — kamera fake stream, WFO/WFH, Clock-In/Out, CDP geolocation luar radius `043`, CDP block kamera/lokasi `044–045`, serta Strict QA `[KNOWN DEFECT]` `042`, `049`).
- [ ] **5.10.** `ActivityHistoryTest.java` (`SIT_USR_UI_054–064`: 11 TCs — Log Absen, filter tanggal, detail, *InfiniteScroll* `063`, tombol koreksi, serta Strict QA `[KNOWN DEFECT]` `056`).
- [ ] **5.11.** `CorrectionTest.java` (`SIT_USR_UI_065–075`: 11 TCs — pengajuan koreksi masuk/pulang, validasi tanggal masa depan, reset, filter, verifikasi status, serta *InfiniteScroll* `075`).
- [ ] **5.12.** `OvertimeTest.java` (`SIT_USR_UI_087–098`: 12 TCs — `/apps/absent/overtime-client` / `/apps/absent/overtime`, pengajuan lembur, upload SPK, detail `[id]`, filter, *InfiniteScroll* `098`, serta Strict QA `[KNOWN DEFECT]` `093`).
- [ ] **5.13.** `UserProfileTest.java` (`SIT_USR_UI_118–125`: 8 TCs — Burger menu `118` & `/apps/absent/change-password` 2 field `119–125`: Password Baru & Confirm Password tanpa Password Lama).
- [ ] **5.14.** `UserCrossModuleAjiE2ETest.java` (`SIT_USR_UI_135–140`: 6 TCs — E2E Lintas Modul Hybrid API+UI: geofence `GET /location-point` + CDP Clock-In `135`, Clock-In -> `/apps/absent/activity` & `GET /activity` `136`, Approve/Reject Koreksi `137–138`, Approve/Reject Lembur `139`, serta Strict QA `[KNOWN DEFECT]` sinkron Jumlah Cuti pendaftaran Admin `leave_amount=12` -> `GET /project-leave/{userId}` & `/apps/absent/leave` `140`).
- [ ] **5.15.** `UserGroupBApiTest.java` (`SIT_USR_API_019–043`, `050–052`, `084–110`: 55 TCs — `/activity` dengan `created_at[$gte]`/`[$lte]`, `/activity/request-activity` Clock-In/Out & GPS validation `026–027, 090–093`, `/user-summary` `094–095`, SLA `<3000ms`, IDOR `038, 089, 096, 103`, `/activity-correction` `039–043, 102, 108, 110`, `/overtime` `050–052, 101, 104, 106`, `/business-trips` `084–089`, Next.js BFF `/api/client/*` RBAC `097–099`, Data Leak/DoS/Operator Injection/Self-Approval `100–110`).

---

### 🔹 Fase 6: Konfigurasi 7 Suite Runner Sekuensial & Verifikasi Eksekusi Headless
- [x] **6.1.** Sinkronkan `src/test/resources/testng.xml` (Master Sequential Suite SIT v4: **250 TCs**, `browser=chrome`, `headless=true`, `UserLoginTest` terdaftar aktif).
- [x] **6.2.** Sinkronkan `testng-deny.xml` (**127 TCs**, sekuensial, `UserLoginTest` aktif) & `testng-aji.xml` (**123 TCs**, sekuensial).
- [x] **6.3.** Buat 4 suite kategori selaras Admin: `testng-smoke.xml` (`smoke`), `testng-regression.xml` (`regression`), `testng-positive.xml` (`positive`), dan `testng-negative.xml` (`negative`).
- [ ] **6.4.** Jalankan verifikasi penuh seluruh 250 TCs pada ketujuh suite XML setelah seluruh feature branch Deny & Aji selesai di-merge ke `main`.

---

### 🔹 Fase 7: Validasi ExtentReports, Sinkronisasi Status SIT & Finalisasi
- [ ] **7.1.** Validasi `reports/ExtentReport.html`: pastikan Dark Theme aktif, seluruh 250 Test ID tercatat, tag `Tester Deny` / `Tester Aji` / `Selenium Web UI` / `REST Assured API` / `smoke` / `regression` / `positive` / `negative` terisi, dan Base64 screenshot + pesan `[KNOWN DEFECT]` terekam akurat.
- [ ] **7.2.** Sinkronisasi hasil eksekusi aktual ke `docs/SIT_Scenario_Magang_Hadir.xlsx`.
- [ ] **7.3.** Buat `README.md` akhir dan perbarui `walkthrough.md`.
