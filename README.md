# NewPlaywrightProject

A Java + Playwright + TestNG UI automation framework, with Excel-driven suite
selection, ExtentReports HTML reporting (with embedded video/trace/screenshot
on failure), and configurable execution modes — sequential, or parallel by
method, by class, or by class **and** browser at the same time.

---

## 1. System Requirements

Install these on the new machine before doing anything else.

| Requirement | Version | Notes |
|---|---|---|
| **Java (JDK)** | **17** | `maven.compiler.source`/`target` are both set to 17 in `pom.xml`. Check with `java -version`. |
| **Apache Maven** | 3.6+ | Check with `mvn -version`. Must be on your system `PATH`. |
| **OS** | Windows, macOS, or Linux | Developed/tested on Windows 11; Playwright supports all three. |
| **Disk space** | ~2–3 GB free | Playwright downloads its own bundled browser binaries (Chromium, Firefox, WebKit) on first run — separate from any browser already installed on the machine. |
| **Internet access** | Required on first setup | To download Maven dependencies and Playwright's browser binaries. |

You do **not** need to separately install Chrome/Firefox/Safari — Playwright
downloads and manages its own browser binaries independently of whatever's
already on the machine.

### Key library versions (from `pom.xml` — for reference, not something you need to install by hand; Maven resolves these automatically)

| Library | Version |
|---|---|
| Microsoft Playwright (Java) | 1.54.0 |
| TestNG | 7.11.0 |
| ExtentReports (aventstack) | 5.1.2 |
| Apache POI (Excel reading) | 5.2.5 |
| Log4j (1.x API bridge + core) | 2.20.0 |
| Apache Commons IO | 2.15.1 |
| MySQL Connector/J | 8.0.33 |
| JavaMail | 1.6.2 |

---

## 2. First-Time Setup on a New Machine

```bash
# 1. Clone / copy the project, then move into it
cd NewPlaywrightProject

# 2. Download all Maven dependencies
mvn clean install -DskipTests

# 3. Download Playwright's browser binaries (Chromium, Firefox, WebKit)
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"
```

Step 3 is easy to forget and is the most common reason a fresh clone fails —
without it, `PlaywrightFactory.initBrowser()` will fail immediately since no
browser binaries exist on the new machine yet.

---

## 3. Project Structure

```
src/test/java/
├── Base/
│   ├── TestBase.java          # Browser lifecycle (SUITE / CLASS / METHOD), suite/class/method hooks
│   └── PlaywrightFactory.java # Launches/closes Playwright browser, context, page, video, trace
├── Pages/
│   ├── BasePage.java          # Shared Playwright action wrappers (legacy - LoginPage no longer extends this)
│   └── LoginPage.java         # Page Object - public Locator fields for the practice site
├── Test/
│   └── LoginTest.java         # The actual @Test cases
├── Listeners/
│   └── TestListener.java      # TestNG ITestListener - drives ExtentReports, screenshots, pass/fail/skip
├── Reports/
│   ├── ExtentManager.java     # ExtentReports singleton + video/trace HTML widget builders
│   └── ExtentTestManager.java # ThreadLocal ExtentTest holder (parallel-safe)
├── Utils/
│   ├── ConfigReader.java      # Reads config.properties, with -D CLI override support
│   ├── ExcelUtils.java        # Reads TestData.xlsx (Apache POI)
│   ├── FrameworkConstants.java# All file paths (reports, screenshots, video, trace, config, Excel)
│   ├── ScreenshotUtils.java   # Screenshot-on-failure capture
│   └── DownloadUtils.java
└── runner/
    └── TestRunner.java        # Excel URL → XML suite dispatcher (the "automatic" entry point)

src/test/resources/
├── config/
│   └── config.properties      # Browser, lifecycle, URL, timeouts, report titles
├── log4j.properties           # Log file level/format (currently INFO - see Logging section)
├── testdata/
│   └── TestData.xlsx          # "Login" sheet: URL -> XML suite-file mapping
└── suites/
    ├── TestNg.xml              # Default Maven entry point -> runs TestRunner (Excel-driven dispatch)
    ├── QA.xml / STAGING.xml     # Client suite files, run directly OR dispatched to by TestRunner
    ├── STAGING_PARALLEL_methods.xml    # parallel="methods"
    ├── STAGING_PARALLEL_classes.xml    # parallel="classes"
    └── STAGING_PARALLEL_multibrowser.xml # parallel="tests", one browser per class

test-output/                   # Generated at runtime - safe to delete, recreated automatically
├── Reports/                   # ExtentReports HTML (ExtentReport_<timestamp>.html)
├── Screenshots/                # Failure screenshots (.png)
├── Exe_Videos/                 # Test/class/suite video recordings (.webm)
└── Trace/                      # Playwright trace files (.zip) - only for failed tests

Logs/
└── PlaywrightProject.log       # Log4j output file
```

---

## 4. Configuration — `config.properties`

Location: `src/test/resources/config/config.properties`

| Key | Values | Purpose |
|---|---|---|
| `browser` | `chromium` \| `firefox` \| `webkit` | Default browser when a suite XML doesn't set a per-`<test>` `browser` parameter |
| `headless` | `true` \| `false` | Run with a visible browser window or not |
| `Env` | e.g. `QA` | Shown in the report's System Info panel |
| `browser.lifecycle` | `Suite` \| `Class` \| `Method` | See table below |
| `URL` | full URL string | Used by `TestRunner`'s Excel lookup, and as the page every test navigates to |
| `report.title` / `report.name` | text | Browser tab title / report header banner |
| `auto.open.trace` | `true` \| `false` | Auto-launch Playwright Trace Viewer after a failed test |
| `default.timeout` / `page.load.timeout` | milliseconds | Playwright action / navigation timeouts |
| `retry.count` | integer | (present for future use) |

### Browser lifecycle modes

| Mode | Browser opens | Best for |
|---|---|---|
| **SUITE** | Once for the entire run, closes at the end | Daily sequential runs — single video, single report, lightest resource use |
| **CLASS** | Once per test class, closes after that class's last test | Parallel runs by class (`parallel="classes"` or `parallel="tests"`) — required for correctness, since each class's thread needs its own isolated browser |
| **METHOD** | Fresh browser before every single `@Test`, closes right after | Parallel runs by method (`parallel="methods"`) — required for the same reason, at finer granularity |

Any value can be overridden per-run from the command line without editing
the file:
```
-Dbrowser.lifecycle=CLASS
```

---

## 5. How to Run

All commands run from the project root (same folder as `pom.xml`).

### Daily default — sequential, one browser, one report
```bash
mvn test -DsuiteFile=src/test/resources/suites/STAGING.xml
```

### Parallel by method (fresh browser per test, methods interleaved across the whole class)
```bash
mvn test -DsuiteFile=src/test/resources/suites/STAGING_PARALLEL_methods.xml -Dbrowser.lifecycle=METHOD
```

### Parallel by class (one browser per class, reused for all that class's tests)
```bash
mvn test -DsuiteFile=src/test/resources/suites/STAGING_PARALLEL_classes.xml -Dbrowser.lifecycle=CLASS
```

### Parallel by class, each in a DIFFERENT browser type
```bash
mvn test -DsuiteFile=src/test/resources/suites/STAGING_PARALLEL_multibrowser.xml -Dbrowser.lifecycle=CLASS
```

### Automatic mode — Excel decides which suite runs
```bash
mvn test
```
This runs `TestNg.xml` → `runner.TestRunner`, which reads the `URL` from
`config.properties` (or `-DURL=...` if passed), matches it against the
`Login` sheet in `TestData.xlsx`, and executes whichever suite XML is
mapped to that URL. Add new URL→XML rows to `TestData.xlsx` to extend this.

### Other useful `-D` overrides
```bash
-DURL=https://example.com          # override the execution URL for this run
-Dbrowser=firefox                  # override the default browser
-Dbrowser.lifecycle=METHOD         # override the lifecycle mode
```

---

## 6. Viewing a Trace File

Failed tests get a `.zip` trace file under `test-output/Trace/`. Open it with:
```bash
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="show-trace test-output/Trace/<file>.zip"
```
(The exact command is also printed to the console/clipboard automatically
after a failed run, when `auto.open.trace=false`.)

---

## 7. Reports

Open the latest file in `test-output/Reports/` (`ExtentReport_<timestamp>.html`)
in any browser. It includes:
- Pass/fail/skip status per test
- Screenshot embedded on failure
- Playable video embedded per test/class/suite (depending on lifecycle mode)
- Trace file info + a one-click Trace Viewer command
- **Tags** section — grouped by test class
- **Devices** section — grouped by browser (e.g. `Firefox  (LoginTest2)`), useful when running the multi-browser suite

---

## 8. Logging

`Logs/PlaywrightProject.log` is intentionally minimal — it only contains:
- Suite-started banner + browser launch details
- Suite-finished banner
- `Test Passed` / `Test Failed` / `Test Skipped` status lines

Everything else (navigation, video/trace file paths, screenshot paths,
per-action clicks/types) is logged at `DEBUG` level and filtered out by
`log4j.properties`'s `rootLogger = INFO` setting. To see full verbose
detail for troubleshooting, temporarily change that to `DEBUG` in
`src/test/resources/log4j.properties`.

---

## 9. Troubleshooting

| Symptom | Likely cause |
|---|---|
| `Executable doesn't exist at ...` on first run | Forgot step 3 in Setup — run the Playwright `install` command |
| `No XML mapping found in 'Login' sheet for URL: ...` | Running plain `mvn test` with a `URL` in `config.properties` that has no matching row in `TestData.xlsx` |
| `Mapped suite file does not exist on disk` | The `XML` column in `TestData.xlsx` doesn't match an actual filename under `src/test/resources/suites/` |
| Random cross-test failures only under parallel runs | Check `browser.lifecycle` matches the suite's `parallel` mode (see table in section 4) |
| No video/trace shown in report for a `CLASS`-mode parallel run | Make sure you're on the version of `TestBase.java`/`ExtentManager.java` that collects all classes' widgets and writes them once in `suiteTearDown()` — writing per-class causes each class's `flush()` to erase the previous one's widget |
