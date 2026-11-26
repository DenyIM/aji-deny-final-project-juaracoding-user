# 📌 Tasklist Pelaksanaan: Automation Testing Web Karyawan `magang.dikahadir.com` (SIT v5 — 276 User TCs)

Roadmap implementasi kerangka kerja otomatisasi pengujian **Portal Karyawan / Mobile Mirroring** (`https://magang.dikahadir.com/absen/login`) berbasis **Java 21 LTS**, **Selenium WebDriver `4.52.0`** (default `headless = true`), **REST Assured `6.3.0`**, **TestNG `7.12.0`**, **ExtentReports `5.1.2`**, **Apache POI `5.2.5`**, dan **Jackson `2.17.2`** yang 100% selaras dengan standar terbaru repositori Web Admin (`final-project-admin`).

Total cakupan pengujian SIT v5: **276 Test Cases User** (**160 Web UI** + **116 REST Assured API**) yang terbagi adil antara **Tester Deny (`139 TCs`: `84 UI + 55 API`)** dan **Tester Aji (`137 TCs`: `76 UI + 61 API`)** dengan *Single Source of Truth* pada [docs/SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/docs/SIT_Scenario_Magang_Hadir.xlsx).

---

## 🧭 Ikhtisar Status 7 Fase & Alokasi Beban Kerja

| Fase | Deskripsi Fase | Output Deliverables | Penanggung Jawab | Status |
| :---: | :--- | :--- | :---: | :---: |
| **Fase 1** | Review Acuan (`WebKaryawanPrompt.md`, `docs/SIT_Scenario_Magang_Hadir.xlsx` v5, `reference.md`) & Sinkronisasi Blueprint ke SIT v5 | `plan.md`, `tasklist.md`, `WebKaryawanPrompt.md` (disinkronkan ke SIT v5: 276 TCs) | Shared Foundation | ✅ Selesai |
| **Fase 2** | Maven Setup (`pom.xml` Java 21, Selenium `4.52.0`, REST Assured `6.3.0`), Struktur Package Terisolasi (`deny` & `aji`), & Fixture `testdata/*` | `pom.xml`, `.gitignore`, `.gitattributes`, `.editorconfig`, `docs/SIT_Scenario_Magang_Hadir.xlsx`, `src/test/resources/testdata/*` | Shared Foundation | ✅ Selesai |
| **Fase 3** | Core Thread-Safe Engine, Keamanan Base64, Dual-API (`FeathersJS` + `Next.js BFF`), MUI Helpers, & `AdminApiHelper` SIT v5 | `SecurityUtil`, `Constants`, `DriverFactory`, `BasePage`, `BaseTest`, `BaseApiTest`, `AdminApiHelper`, `ScreenshotUtil`, `ExtentManager`, `ExtentTestManager`, `TestListener` | Shared Foundation | ✅ Selesai |
| **Fase 4** | Page Object Model (POM) Web Karyawan Implementation (Inspeksi Locator Live Mobile Mirroring) | POM Deny (`pages/user/deny/*`: 6 Classes; `UserLoginPage` ✅ di `main`) & POM Aji (`pages/user/aji/*`: 6 Classes) | Deny & Aji | 🔄 In Progress (`UserLoginPage` selesai & terintegrasi di `main`) |
| **Fase 5** | Automated Tests Implementation (160 Web UI + 116 REST Assured API + 6 List InfiniteScroll + 11 E2E Lintas Modul + Strict QA `[KNOWN DEFECT]`) | `tests/user/deny/*` (84 UI; `UserLoginTest` ✅ 12/12 di `main`), `tests/user/aji/*` (76 UI), `UserGroupAApiTest` (55 API), `UserGroupBApiTest` (61 API) | Deny (139 TCs) & Aji (137 TCs) | 🔄 In Progress (`UserLoginTest` 12/12 selesai di `main`) |
| **Fase 6** | 7 TestNG Sequential Suites (`testng`, `deny`, `aji`, `smoke`, `regression`, `positive`, `negative`) & Verifikasi Headless | `src/test/resources/testng*.xml` (7 suite XML sekuensial, `UserLoginTest` terdaftar aktif) | Deny & Aji | 🔄 7 Suite XML Aktif (Login Terverifikasi) |
| **Fase 7** | Validasi `ExtentReport.html`, Sinkronisasi Status SIT v5, Dokumentasi `README.md` & `walkthrough.md` | `reports/ExtentReport.html`, `README.md`, `walkthrough.md` | Shared Deliverable | ⏳ Pending |

---

## ⚖️ Matriks Pembagian Tugas Adil (SIT v5 — Total 276 TCs)

| Aspek Kolaborasi | 👤 Tester Deny (139 TCs — 50.4%) | 👤 Tester Aji (137 TCs — 49.6%) |
| :--- | :--- | :--- |
| **Fitur Web UI (Selenium — 160 TCs)** | **84 Web UI TCs**:<br>1. `Autentikasi-Login` (`SIT_USR_UI_001–012`: 12 TCs) *(✅ Selesai & lulus 12/12 di `main`)*<br>2. `Autentikasi-Register` (`SIT_USR_UI_013–025`: 13 TCs — form 5 field, ikon mata & Known Defect foto)<br>3. `Autentikasi-Reset Password` (`SIT_USR_UI_026–035`: 10 TCs, termasuk OTP & 2 ikon mata)<br>4. `Pengajuan-Izin` (`SIT_USR_UI_078–091`: 14 TCs — 3 tab, Total, Reset, Reject Notes, `< Kembali`, *InfiniteScroll*)<br>5. `Pengajuan-Cuti` (`SIT_USR_UI_103–115`: 13 TCs `/apps/absent/leave-client` — Tab Info Cuti 6 metrik, Kalender Batal/Simpan, *InfiniteScroll*)<br>6. `Pengajuan-Sakit` (`SIT_USR_UI_116–126`: 11 TCs `/apps/absent/sick` — Upload ulang, Kalender Batal/Simpan, *InfiniteScroll*)<br>7. `Profil-Download Report` (`SIT_USR_UI_136–141`: 6 TCs `/apps/absent/reports/share_report` — Harian/Mingguan/Bulanan)<br>8. `Integrasi Lintas Modul (Hybrid API+UI)` (`SIT_USR_UI_150–154`: 5 TCs E2E) | **76 Web UI TCs**:<br>1. `Presensi-Beranda & Lupa Pulang` (`SIT_USR_UI_036–058`: 23 TCs — Kehadiranmu Bulan/Tahun + 7 kartu, Fake Camera/GPS CDP & `/correction/[id]`)<br>2. `Presensi-Log Absen` (`SIT_USR_UI_059–065`: 7 TCs + *InfiniteScroll*)<br>3. `Pengajuan-Koreksi Absen` (`SIT_USR_UI_066–077`: 12 TCs — Drawer, Reset, Lihat Selengkapnya, `< Kembali`, *InfiniteScroll*)<br>4. `Pengajuan-Lembur` (`SIT_USR_UI_092–102`: 11 TCs `/apps/absent/overtime-client` — Reset, Total, `< Kembali`, *InfiniteScroll*)<br>5. `Profil-Menu Utama & Ubah Password` (`SIT_USR_UI_127–135`: 9 TCs — Burger drawer, 2 field + 2 ikon mata, modal Sukses)<br>6. `Leader-Staff & Approval Bawahan` (`SIT_USR_UI_142–149`: 8 TCs — `/apps/absent/staff`, `/leave/[id]`, `/overtime/[id]`)<br>7. `Integrasi Lintas Modul (Hybrid API+UI)` (`SIT_USR_UI_155–160`: 6 TCs E2E, termasuk Kuota Cuti `160` *Known Defect*) |
| **Fitur API (REST Assured — 116 TCs)** | **55 API TCs** (`UserGroupAApiTest`):<br>1. Auth & `/auth-management` Reset Password (`SIT_USR_API_001–010`: 10 TCs)<br>2. Registration & NIK Check (`SIT_USR_API_011–018`: 8 TCs; NIK aktif `D8240010`)<br>3. Leaves/Izin/Sakit + Late/Early Permits (`SIT_USR_API_044–049, 056–063`: 14 TCs)<br>4. Multipart `/upload` & BFF `/api/sick` + `/api/activity/sick` (`SIT_USR_API_064–071`: 8 TCs)<br>5. Share Report `/share-report` Export & IDOR (`SIT_USR_API_072–075`: 4 TCs)<br>6. Project-Leave, Profile, Critical IDOR & Mass-Assignment (`SIT_USR_API_053–055, 076–083`: 11 TCs) | **61 API TCs** (`UserGroupBApiTest`):<br>1. Attendance Clock-In/Out, `/activity/request-activity` & `/user-summary` (`SIT_USR_API_019–030, 090–096`: 19 TCs)<br>2. Activity Log (`created_at[$gte]`/`[$lte]`) & IDOR Security (`SIT_USR_API_031–038`: 8 TCs)<br>3. Corrections (`SIT_USR_API_039–043`: 5 TCs)<br>4. Overtimes & `/business-trips` (`SIT_USR_API_050–052, 084–089`: 9 TCs)<br>5. Leader/Client BFF Isolation (`/api/client/*`), Query Hardening, Self-Approval RBAC & BFF Summary/Last-Activity/Check-Correction/Staff (`SIT_USR_API_097–116`: 20 TCs) |
| **Scope Package POM** | `com.juaracoding.magang.pages.user.deny.*` (6 Page Classes) | `com.juaracoding.magang.pages.user.aji.*` (6 Page Classes) |
| **Scope Package Test** | `tests.user.deny.*` (7 Test Classes) & `UserGroupAApiTest` | `tests.user.aji.*` (7 Test Classes) & `UserGroupBApiTest` |
| **Git Branch (Opsi B)** | `feature/deny-user-login` *(✅ terintegrasi ke `main`)*, `feature/deny-user-register`, `feature/deny-user-reset-password`, `feature/deny-user-permit`, `feature/deny-user-leave-sick`, `feature/deny-user-download-report`, `feature/deny-user-e2e`, `feature/deny-user-api-a` | `feature/aji-user-attendance`, `feature/aji-user-activity`, `feature/aji-user-correction`, `feature/aji-user-overtime`, `feature/aji-user-profile`, `feature/aji-user-leader-staff`, `feature/aji-user-e2e`, `feature/aji-user-api-b` |
| **Suite Runner XML** | `mvn test "-DsuiteXmlFile=src/test/resources/testng-deny.xml"` | `mvn test "-DsuiteXmlFile=src/test/resources/testng-aji.xml"` |

---

## 📋 Rincian Tugas per Fase

### 🔹 Fase 1: Perencanaan, Pemetaan SIT v5 & Review Blueprint
- [x] **1.1. Telaah Dokumen Acuan Proyek (SIT v5)**
  - [x] Selaraskan [docs/SIT_Scenario_Magang_Hadir.xlsx](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/docs/SIT_Scenario_Magang_Hadir.xlsx) & [WebKaryawanPrompt.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/WebKaryawanPrompt.md) dengan struktur Admin SIT dan hasil audit bundle Next.js live (`magang.dikahadir.com`).
  - [x] Verifikasi seluruh **276 Test Cases User** (`SIT_USR_UI_001–160` dan `SIT_USR_API_001–116`) termasuk kolom `Test Suite`, `Severity`, `Priority`, `Actual Result / Defect Notes`, dan `Manual Check`, tanpa mengubah satupun dari 451 Admin TCs (`SIT_ADM_*`).
- [x] **1.2. Penyusunan & Sinkronisasi `plan.md` & `tasklist.md` (7 Fase)**
  - [x] Sinkronkan [plan.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/plan.md) dan [tasklist.md](file:///c:/Users/Asus/Documents/JuaraCoding/Final/final-project-karyawan/tasklist.md) ke SIT v5 (276 TCs: Deny 139 / Aji 137).
- [x] **1.3. Persetujuan (Sign-off) User**
  - [x] Audit & upgrade SIT v5 disetujui User untuk disinkronkan di `main` (beserta salinan dokumen ke repositori Admin tanpa menyentuh kode `.java`/`.xml` Admin).

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
  - [x] Sinkronisasi `Constants.java` ke SIT v5: URL Mobile + `/apps/absent/staff`, rute alias client, `API_BASE_URI`, `API_STATIC_UPLOADS_BASE_URI = "https://api-dev.dikahadir.com/uploads"`, `API_USER_SUMMARY_ENDPOINT = "/user-summary"`, `API_CANCEL_LEAVE_ENDPOINT = "/cancel-leave"`, rute Next.js BFF (`/api/login`, `/api/users/summary`, `/api/activity/user-last-activity`, `/api/activity/user-check-correction`, `/api/sick`, `/api/activity/sick`, `/api/leave/project-leave`, `/api/client/*`), `USER_NIK = "D8240010"`, serta 34 NIK pada `VALID_NIK_POOL`.
- [x] **3.2. Thread-Safe WebDriver, Mobile Camera/GPS & Base POM (`BasePage.java`)**
  - [x] `DriverFactory.java` (`ThreadLocal<WebDriver>`, Chrome default `headless = true`, fake media stream & allow geolocation/camera).
  - [x] `BasePage.java` (Explicit Wait 15s, `safeNavigate`, `openSwipeableDrawerWithRetry`, `openMuiSelectWithMouseDown`, `dispatchCalendarDayMouseEvents`, `scrollToBottomForInfiniteScroll`, `clearBrowserStorageAndCookies`, CDP `HasCdp`, serta `isImageBroken`).
  - [x] `BaseTest.java` (`@BeforeMethod` default `headless=true` dan `@AfterMethod` `quitDriver()`).
- [x] **3.3. Dual-API Engine, `AdminApiHelper` SIT v5 & ExtentReports**
  - [x] `BaseApiTest.java` (FeathersJS Bearer spec + Next.js BFF Cookie `hadir_app_magang` spec via `POST /api/login`).
  - [x] Sinkronisasi `AdminApiHelper.java` ke temuan live Admin: `POST /cancel-leave` (`leave_trans_id`, `lv_approve_notes`), `GET /project-leave/{userId}`, `uploadTestPhoto` dengan MIME eksplisit (`image/jpeg` / `image/png`), `createPendingLatePermit`, `createPendingEarlyLeave`, `createPendingBusinessTrip`, `findUserIdByEmail`, `findAvailableNikFromPool`, `createSelfRegisteredMobileUser`, `isVerified: true` pada `updateUserActiveStatusAsAdmin`, serta `queryActivitiesByDateRange` memakai `created_at[$gte]` & `created_at[$lte]`.
  - [x] Update metadata `ExtentManager.java` ke SIT v5 (`276 TCs`: Deny `139` / Aji `137`).

---

### 🔹 Fase 4: Implementasi Web Karyawan Page Object Model (POM)
*(Kerjakan setiap modul baru pada feature branch terpisah sesuai aturan Git Workflow Opsi B)*

#### 👤 Bagian Tester Deny (`src/main/java/com/juaracoding/magang/pages/user/deny/`):
- [x] **4.1.** `UserLoginPage.java` (`/absen/login`: input email/password, ikon mata show/hide password, tombol Masuk, alert error, proteksi redirect `/apps/absent`, menu burger & tombol Logout) — **✅ Selesai & terintegrasi di `main`.**
- [ ] **4.2.** `RegisterPage.java` (`/absen/register`: tautan `disini` dari Login, form 5 field aktif `#nik`, `#fullname`, `#email`, `#password` + ikon mata Show/Hide, Upload Selfie `#selfie` + preview/deteksi broken image & hasil upload, tombol Daftar, tautan `Masuk`).
- [ ] **4.3.** `ResetPasswordPage.java` (`/absen/reset-password-request` & `/absen/reset-password`: request reset email, input **kode OTP**, Password Baru & Konfirmasi Password + 2 ikon mata Show/Hide, link kembali ke Login, penanganan OTP invalid/kadaluarsa).
- [ ] **4.4.** `PermitPage.java` (`/apps/absent/permit`: 3 Tab Izin Terlambat, Pulang Cepat, Izin Off, card counter `Total`, form ajukan, tombol Reset, modal Batal/Close, verifikasi kartu status `Pending` / `Approved` / `Rejected` + alasan penolakan, tombol `< Kembali` header, serta *InfiniteScroll* `$skip=10*page`).
- [ ] **4.5.** `LeaveAndSickPage.java` (`/apps/absent/leave-client` & `/apps/absent/sick`: Tab `Info Cuti` 6 metrik + Tab `Ajukan Cuti`, form Cuti + Kalender `Batal`/`Simpan`, detail `/apps/absent/leave/[id]`, form Sakit + upload & upload ulang SKD, preview & view/download SKD, tombol Reset & `< Kembali`, serta *InfiniteScroll* `$skip=10*page`).
- [ ] **4.6.** `DownloadReportPage.java` (`/apps/absent/reports/share_report`: akses menu Download Reports dari beranda `/apps/absent`, dropdown Tipe Report **Harian / Mingguan / Bulanan**, tombol `Download Report`, tombol `< Kembali`, notifikasi validasi/data kosong).

#### 👤 Bagian Tester Aji (`src/main/java/com/juaracoding/magang/pages/user/aji/`):
- [ ] **4.7.** `AttendancePage.java` (`/apps/absent`: header profil & avatar, seksi **Kehadiranmu** dengan filter Bulan/Tahun & 7 kartu metrik, jam digital, info shift, modal kamera Absen Masuk/Pulang, dialog **"Anda belum melakukan absen keluar"** -> `/apps/absent/correction/[id]`, capture & retake selfie, pilihan WFO/WFH, notes, badge Tepat Waktu/Terlambat, verifikasi broken image).
- [ ] **4.8.** `ActivityHistoryPage.java` (`/apps/absent/activity`: daftar riwayat presensi, filter Start/End Date, detail jam & lokasi, Notes Masuk/Keluar, *InfiniteScroll* `$skip=10*page`, tombol `< Kembali`, cek broken image `Photo Check In`).
- [ ] **4.9.** `CorrectionPage.java` (`/apps/absent/correction`: drawer Pengajuan Koreksi Baru, input jam masuk/pulang & tanggal, tombol `Reset`, filter tanggal, `Lihat Selengkapnya`, verifikasi status `Pending` / `Approved` / `Rejected` + alasan, tombol `< Kembali`, serta *InfiniteScroll* `$skip=10*page`).
- [ ] **4.10.** `OvertimePage.java` (`/apps/absent/overtime-client` / `/apps/absent/overtime`: daftar riwayat lembur & card counter `Total`, modal ajukan lembur, upload lampiran SPK + preview, detail `/apps/absent/overtime/[id]`, tombol Reset & Batal, Reject Notes, `< Kembali`, serta *InfiniteScroll* `$skip=10*page`).
- [ ] **4.11.** `UserProfilePage.java` (Burger Menu di `/apps/absent`, navigasi ke `/apps/absent/change-password`: **HANYA** field **Password Baru** `password` & **Confirm Password** `passwordConfirmation` + 2 ikon mata Show/Hide + modal `"Sukses mengganti password"` (`Oke`) + tombol `< Kembali` — **tanpa** field Password Lama).
- [ ] **4.12.** `LeaderStaffPage.java` (`/apps/absent/staff`, `/apps/absent/leave/[id]`, `/apps/absent/overtime/[id]`: menu `Staff` pada Leader/Upliner `D8240010`, daftar & filter staff, sub-menu Lihat Absen / Sisa Cuti / Staff, serta Approve/Reject pengajuan cuti & lembur bawahan).

---

### 🔹 Fase 5: Implementasi Automated Test Suites (276 User TCs)
*(Setiap `@Test` wajib mencantumkan ID SIT pada `description`, `ExtentTestManager.logInfo(...)`, serta grup `"regression"` + `"positive"`/`"negative"` [+ `"smoke"` untuk critical path]. Untuk `Ready (Known Defect)`, wajib assert Expected Result standar dengan prefix `"[KNOWN DEFECT] ..."`.)*

#### 👤 Bagian Tester Deny (`139 TCs = 84 UI + 55 API`):
- [x] **5.1.** `UserLoginTest.java` (`SIT_USR_UI_001–012`: 12 TCs — login valid/negatif, SQLi, XSS, proteksi auth `/apps/absent`, logout & browser back; grup `smoke`/`regression`/`positive`/`negative`) — **✅ Selesai & lulus 12/12 di `main`.**
- [ ] **5.2.** `RegisterTest.java` (`SIT_USR_UI_013–025`: 13 TCs — pool NIK valid `VALID_NIK_POOL`, ikon mata `015`, NIK invalid `016` & NIK terpakai `D8240010` `017`, upload valid/non-image, link Masuk, dan Strict QA `[KNOWN DEFECT]` `024`).
- [ ] **5.3.** `ResetPasswordTest.java` (`SIT_USR_UI_026–035`: 10 TCs — `/absen/reset-password-request` & `/absen/reset-password`: request reset email, input **kode OTP**, 2 ikon mata show/hide, OTP invalid/kadaluarsa, link kembali ke Login).
- [ ] **5.4.** `PermitTest.java` (`SIT_USR_UI_078–091`: 14 TCs — 3 Tab Terlambat/Pulang Cepat/Izin Off, counter Total, Reset, verifikasi kartu status & Reject Notes via `AdminApiHelper`, `< Kembali`, serta *InfiniteScroll* `090`).
- [ ] **5.5.** `LeaveAndSickTest.java` (`SIT_USR_UI_103–126`: 24 TCs — Cuti `103–115` di `/apps/absent/leave-client` [Tab Info Cuti, Kalender Batal/Simpan, *InfiniteScroll*] & Sakit `116–126` di `/apps/absent/sick` [upload ulang, Kalender, *InfiniteScroll*], serta Strict QA `[KNOWN DEFECT]` `109`, `120`).
- [ ] **5.6.** `DownloadReportTest.java` (`SIT_USR_UI_136–141`: 6 TCs — navigasi `/apps/absent/reports/share_report`, download report **Harian / Mingguan / Bulanan** via `POST /share-report`, validasi tipe report kosong, periode tanpa data, `< Kembali`).
- [ ] **5.7.** `UserCrossModuleDenyE2ETest.java` (`SIT_USR_UI_150–154`: 5 TCs — E2E Lintas Modul Hybrid API+UI: aktif/nonaktif user via `PATCH /users/{id}` `isVerified=true` `150–151`, Approve/Reject Izin Off `152–153`, `POST /cancel-leave` & sinkronisasi kuota `154`).
- [ ] **5.8.** `UserGroupAApiTest.java` (`SIT_USR_API_001–018`, `044–049`, `053–083`: 55 TCs — `/authentication`, `/auth-management` termasuk invalid OTP/action `081–082`, `/users` NIK `D8240010`/register/IDOR/Mass-Assignment/DELETE `013, 053–055, 078–080, 083`, `/off-permit`, `/late-permit` `056–059`, `/early-leave` `060–063`, `/upload` security `064–067`, Next.js BFF `/api/sick` & `/api/activity/sick` `068–071`, `/share-report` `072–075`, `/project-leave` & BFF `/api/leave/project-leave/{other_id}` `045–047, 076–077` termasuk `[KNOWN DEFECT]` `047`).

#### 👤 Bagian Tester Aji (`137 TCs = 76 UI + 61 API`):
- [ ] **5.9.** `AttendanceTest.java` (`SIT_USR_UI_036–058`: 23 TCs — Header, **Kehadiranmu** filter Bulan/Tahun & 7 kartu metrik, kamera fake stream, WFO/WFH, Clock-In/Out, dialog `"Anda belum melakukan absen keluar"` -> `/apps/absent/correction/[id]`, CDP geolocation & permission `050–052`, serta Strict QA `[KNOWN DEFECT]` `046`).
- [ ] **5.10.** `ActivityHistoryTest.java` (`SIT_USR_UI_059–065`: 7 TCs — Log Absen `/apps/absent/activity`, filter tanggal, detail, *InfiniteScroll*, tombol `< Kembali`, serta Strict QA `[KNOWN DEFECT]` `061`).
- [ ] **5.11.** `CorrectionTest.java` (`SIT_USR_UI_066–077`: 12 TCs — pengajuan koreksi masuk/pulang, validasi tanggal masa depan, Reset, `Lihat Selengkapnya`, filter, verifikasi status & Reject Notes, `< Kembali`, serta *InfiniteScroll*).
- [ ] **5.12.** `OvertimeTest.java` (`SIT_USR_UI_092–102`: 11 TCs — `/apps/absent/overtime-client` / `/apps/absent/overtime`, pengajuan lembur, card Total, Reset, upload SPK, detail `[id]`, Reject Notes, `< Kembali`, *InfiniteScroll*).
- [ ] **5.13.** `UserProfileTest.java` (`SIT_USR_UI_127–135`: 9 TCs — Burger menu drawer `127` & `/apps/absent/change-password` 2 field + 2 ikon mata `128–135`, modal `"Sukses mengganti password"` `"Oke"`, tombol `< Kembali`).
- [ ] **5.14.** `LeaderStaffTest.java` (`SIT_USR_UI_142–149`: 8 TCs — menu Staff `/apps/absent/staff`, daftar & filter staff bawahan, Lihat Absen / Sisa Cuti / Staff, Approve/Reject cuti `/apps/absent/leave/[id]` & lembur `/apps/absent/overtime/[id]`).
- [ ] **5.15.** `UserCrossModuleAjiE2ETest.java` (`SIT_USR_UI_155–160`: 6 TCs — E2E Lintas Modul Hybrid API+UI: geofence `GET /location-point` + CDP Clock-In `155`, Clock-In -> `/apps/absent/activity` & `GET /activity` `156`, Approve/Reject Koreksi `157–158`, Approve/Reject Lembur `159`, serta Strict QA `[KNOWN DEFECT]` sinkron Jumlah Cuti pendaftaran Admin `leave_amount=12` -> `GET /project-leave/{userId}` & `/apps/absent/leave-client` `160`).
- [ ] **5.16.** `UserGroupBApiTest.java` (`SIT_USR_API_019–043`, `050–052`, `084–116`: 61 TCs — `/activity` dengan `created_at[$gte]`/`[$lte]`, `/activity/request-activity` Clock-In/Out & GPS validation `026–027, 090–093`, `/user-summary` `094–095`, SLA `<3000ms`, IDOR `038, 089, 096, 103`, `/activity-correction` `039–043, 102, 108, 110`, `/overtime` `050–052, 101, 104, 106`, `/business-trips` `084–089`, Next.js BFF `/api/client/*` RBAC `097–099`, Data Leak/DoS/Operator Injection/Self-Approval `100–110`, serta BFF Summary/Last-Activity/Check-Correction & Leader Staff APIs `111–116`).

---

### 🔹 Fase 6: Konfigurasi 7 Suite Runner Sekuensial & Verifikasi Eksekusi Headless
- [x] **6.1.** Sinkronkan `src/test/resources/testng.xml` (Master Sequential Suite SIT v5: **276 TCs**, `browser=chrome`, `headless=true`, `UserLoginTest` terdaftar aktif).
- [x] **6.2.** Sinkronkan `testng-deny.xml` (**139 TCs**, sekuensial, `UserLoginTest` aktif) & `testng-aji.xml` (**137 TCs**, sekuensial).
- [x] **6.3.** Buat 4 suite kategori selaras Admin: `testng-smoke.xml` (`smoke`), `testng-regression.xml` (`regression`), `testng-positive.xml` (`positive`), dan `testng-negative.xml` (`negative`).
- [ ] **6.4.** Jalankan verifikasi penuh seluruh 276 TCs pada ketujuh suite XML setelah seluruh feature branch Deny & Aji selesai di-merge ke `main`.

---

### 🔹 Fase 7: Validasi ExtentReports, Sinkronisasi Status SIT & Finalisasi
- [ ] **7.1.** Validasi `reports/ExtentReport.html`: pastikan Dark Theme aktif, seluruh 276 Test ID tercatat, tag `Tester Deny` / `Tester Aji` / `Selenium Web UI` / `REST Assured API` / `smoke` / `regression` / `positive` / `negative` terisi, dan Base64 screenshot + pesan `[KNOWN DEFECT]` terekam akurat.
- [ ] **7.2.** Sinkronisasi hasil eksekusi aktual ke `docs/SIT_Scenario_Magang_Hadir.xlsx`.
- [ ] **7.3.** Buat `README.md` akhir dan perbarui `walkthrough.md`.
