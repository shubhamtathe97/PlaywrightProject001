package Base;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import com.microsoft.playwright.Page;

import Reports.ExtentManager;
import Utils.ConfigReader;
import Utils.FrameworkConstants;

public class TestBase {

    protected static final Logger log = LogManager.getLogger(TestBase.class);


   // Each thread gets its own Page instead of sharing one, so parallel tests don't overwrite each other.
    private static final ThreadLocal<Page> pageThreadLocal = new ThreadLocal<>();

    protected static Page getPage() {
        return pageThreadLocal.get();
    }

    private static void setPage(Page p) {
        pageThreadLocal.set(p);
    }


    //Tracks which browser this thread is using, so the report can show the right browser for each test.
    private static final ThreadLocal<String> browserNameThreadLocal = new ThreadLocal<>();

    public static String getBrowserName() {
        return browserNameThreadLocal.get();
    }

    private static void setBrowserName(String name) {
        browserNameThreadLocal.set(name);
    }


    //This lock keeps multiple classes from writing to the shared list at the same time and breaking it.
    private static final Object REPORT_LOCK = new Object();


    //Collects each class's video/trace info as it finishes, then writes them all into the report at once at the end — so one class's report update doesn't wipe out another's.
    private static final java.util.List<String> classWidgetBlocks =
            new java.util.ArrayList<>();

    private static final ThreadLocal<Boolean> FINISHED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<String> lastVideoPath = new ThreadLocal<>();

    // Runs ONCE per suite, before any @Test class in that suite runs.
    @BeforeSuite(alwaysRun = true)
    public void suiteSetUp() {
        PropertyConfigurator.configure(ClassLoader.getSystemResource("log4j.properties"));

        log.info("========================================");
        log.info("  TEST SUITE STARTED");
        log.info("  Browser lifecycle: " + ConfigReader.getBrowserLifecycle());
        log.info("  Browser : " + ConfigReader.getBrowser());
        log.info("  Env     : " + ConfigReader.getEnv());
        log.info("  Base URL: " + ConfigReader.getBaseUrl());
        log.info("========================================");
    }

    // Runs ONCE per suite, after EVERY @Test class in that suite has finished.
    @AfterSuite(alwaysRun = true)
    public void suiteTearDown() {
        if (ConfigReader.isClassLifecycle()) {

        	//In CLASS mode, save the report just once at the very end, then add every class's video/trace info together — so no class's save overwrites another's.
        	ExtentManager.getInstance().flush();
            synchronized (REPORT_LOCK) {
                ExtentManager.appendMultiClassBanner(classWidgetBlocks);
            }
            log.debug("Appended trace/video widget for " + classWidgetBlocks.size() + " class(es).");
        } else {
            ExtentManager.getInstance().flush();
        }

        log.debug("Extent report generated at: " + ExtentManager.getReportPath());
        log.info("========================================");
        log.info("  TEST SUITE FINISHED");
        log.info("========================================");
    }

    //In CLASS mode, this opens one browser before the class's tests start, and that same browser is reused for all of them. 
    //It also checks if the suite XML sets a specific browser for this test block (so different classes can run in different browsers at once)
    //— if not, it just uses the default from config.
    @BeforeClass(alwaysRun = true)
    public void classSetUp(ITestContext context) {
        if (ConfigReader.isClassLifecycle()) {
            String label = this.getClass().getSimpleName();
            PlaywrightFactory.setActiveLabel(label);

            String browserParam = context.getCurrentXmlTest().getParameter("browser");

            setPage(PlaywrightFactory.initBrowser(browserParam));
            String browserUsed = (browserParam != null && !browserParam.isEmpty())
                    ? browserParam : ConfigReader.getBrowser();
            setBrowserName(browserUsed);
            log.info("Browser launched ONCE for class: " + label
                    + " [" + browserUsed + "] - reused by every @Test method in this class.");
            getPage().navigate(ConfigReader.getBaseUrl());
            log.debug("Navigated to: " + ConfigReader.getBaseUrl());
        }
    }

     //In CLASS mode, this closes the browser after the class's last test and saves its video/trace info to be added to the report later —
    //it doesn't touch the report file directly here (that happens once, at the end, in suiteTearDown()). In METHOD mode, this does nothing.
    @AfterClass(alwaysRun = true)
    public void classTearDown() {
        if (ConfigReader.isClassLifecycle()) {
            String label = this.getClass().getSimpleName();

            //In CLASS mode, one video covers the whole class,
            String finalVideoPath = closeAndRenameVideo(label, true, true);
            String tracePath = PlaywrightFactory.getTracePath();

            if (finalVideoPath != null || tracePath != null) {
                String traceCommand = (tracePath != null)
                        ? PlaywrightFactory.buildTraceCommand(Paths.get(tracePath))
                        : null;
                String block = ExtentManager.buildClassWidgetBlock(label, finalVideoPath, tracePath, traceCommand);
                synchronized (REPORT_LOCK) {
                    classWidgetBlocks.add(block);
                }
            }
            log.debug("Finished class: " + label);
        }
    }

 
    //In METHOD mode, this opens a fresh browser for every test, using whichever browser this test block specifies (so cross-browser suites still work).
    @BeforeMethod(alwaysRun = true)
    public void methodSetUp(Method method, ITestContext context) {
        if (ConfigReader.isMethodLifecycle()) {
            FINISHED.set(false);
            // TC_Login_001_verifyPageTitle_20260906_120000.webm
            PlaywrightFactory.setActiveLabel(method.getName());

            String browserParam = context.getCurrentXmlTest().getParameter("browser");

            setPage(PlaywrightFactory.initBrowser(browserParam));
            String browserUsed = (browserParam != null && !browserParam.isEmpty())
                    ? browserParam : ConfigReader.getBrowser();
            setBrowserName(browserUsed);
            getPage().navigate(ConfigReader.getBaseUrl());
            log.debug("Navigated to: " + ConfigReader.getBaseUrl() + " [" + browserUsed + "]");
        }
    }


    //This closes the browser after passed or skipped tests
    @AfterMethod(alwaysRun = true)
    public void methodTearDown(ITestResult result) {
        if (ConfigReader.isMethodLifecycle()) {
            finishTest(result);
        }
    }


    //Only runs in METHOD mode (does nothing in CLASS mode). Closes the browser, saves this test's video under its own name, and updates the report
    public static synchronized String finishTest(ITestResult result) {
        if (!ConfigReader.isMethodLifecycle()) {
            return null;
        }
        if (FINISHED.get()) {
            return lastVideoPath.get();
        }
        FINISHED.set(true);

        String testName = result.getMethod().getMethodName();

        //Video and trace are only kept for failed tests
        boolean isFailure = (result.getStatus() == ITestResult.FAILURE);
        String finalVideoPath = closeAndRenameVideo(testName, isFailure, isFailure);
        lastVideoPath.set(finalVideoPath);

        String tracePath = PlaywrightFactory.getTracePath();
        if (tracePath != null) {
            log.debug("Trace saved for '" + testName + "': " + tracePath);
        }

        //Save the report after every test, so it's always up to date.
        ExtentManager.getInstance().flush();

        return finalVideoPath;
    }


    //Closes the browser. Saves the video with a proper name if keepVideo is true, otherwise deletes it.
    private static String closeAndRenameVideo(String label, boolean saveTrace, boolean keepVideo) {
        PlaywrightFactory.closeBrowser(saveTrace);

        String videoPath = PlaywrightFactory.getVideoPath();

        if (videoPath == null || videoPath.isEmpty()) {
            if (ConfigReader.isVideoEnabled()) {
                log.warn("Video path is not available for: " + label);
            } else {
                log.debug("Video recording disabled via config.properties - skipping for: " + label);
            }
            return null;
        }

        Path oldVideoPath = Paths.get(videoPath);

        if (!keepVideo) {
            try {
                Files.deleteIfExists(oldVideoPath);
                log.debug("Video discarded (test did not fail) - deleted: " + oldVideoPath);
            } catch (Exception e) {
                log.warn("Could not delete video for '" + label + "': " + oldVideoPath, e);
            }
            return null;
        }

        String finalVideoPath = videoPath;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(Calendar.getInstance().getTime());

            String newFileName = label + "_" + timeStamp + ".webm";

            Path videoDirectory = Paths.get(FrameworkConstants.VIDEO_PATH);
            Files.createDirectories(videoDirectory);

            Path newVideoPath = videoDirectory.resolve(newFileName);

            Files.move(oldVideoPath, newVideoPath, StandardCopyOption.REPLACE_EXISTING);
            finalVideoPath = newVideoPath.toAbsolutePath().toString();

            log.debug("========================================");
            log.debug("Video saved successfully:");
            log.debug(finalVideoPath);
            log.debug("========================================");
        } catch (Exception e) {
            log.error("Failed to rename/move video for '" + label + "': " + videoPath, e);
        }

        return finalVideoPath;
    }
}