package Utils;

import java.io.File;

/**
 * FrameworkConstants
 * Central place for all framework-wide constant values.
 */
public final class FrameworkConstants {

    private FrameworkConstants() {  }

    // ──────────────────────────────────────────────
    // PROJECT PATHS
    // ──────────────────────────────────────────────
    public static final String USER_DIR       = System.getProperty("user.dir");
    public static final String RESOURCES_PATH = USER_DIR + File.separator + "src/test/resources";
    public static final String CONFIG_PATH    = RESOURCES_PATH + File.separator + "config" + File.separator + "config.properties";
    public static final String EXCEL_PATH     = RESOURCES_PATH + File.separator + "testdata" + File.separator + "TestData.xlsx";
    public static final String SUITE_PATH     = RESOURCES_PATH + File.separator + "suites";

    // ──────────────────────────────────────────────
    // REPORT & LOGS
    // ──────────────────────────────────────────────
    public static final String REPORTS_PATH       = USER_DIR + File.separator + "test-output" + File.separator + "Reports";
    public static final String SCREENSHOTS_PATH   = USER_DIR + File.separator + "test-output" + File.separator + "Screenshots";
    public static final String DOWNLOADS_PATH     = USER_DIR + File.separator + "test-output" + File.separator + "Downloads";

    public static final String VIDEO_PATH         = USER_DIR + File.separator + "test-output" + File.separator + "Exe_Videos";
    public static final String TRACE_PATH         = USER_DIR + File.separator + "test-output" + File.separator + "Trace";
    public static final String EXTENT_REPORT_NAME = "ExtentReport_";

    // ──────────────────────────────────────────────
    // EXCEL SHEET NAMES
    // ──────────────────────────────────────────────
    public static final String SHEET_NAME        = "Login";

}