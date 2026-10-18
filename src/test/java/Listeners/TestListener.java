package Listeners;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;

import Base.PlaywrightFactory;
import Base.TestBase;
import Reports.ExtentManager;
import Reports.ExtentTestManager;
import Utils.ScreenshotUtils;

import java.nio.file.Paths;

//video playback, adds missing trace info, restores full error logs, and locks report updates so parallel runs don't break the dashboard charts.
public class TestListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);
    private final ScreenshotUtils screenshotUtils = new ScreenshotUtils();
    private ExtentReports extent;

    @Override
    public void onStart(ITestContext context) {
        Reporter.setEscapeHtml(false);
        extent = ExtentManager.getInstance();
        log.debug("Started test: " + context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        // extent.flush() is deliberately NOT called here - TestBase owns
        // the flush timing (after trace/video are actually on disk).
        log.debug("Finished test: " + context.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();


        //Locks all report-writing calls, not just one, fixed the parallel-run chart
        ExtentTest extentTest;
        String className = result.getTestClass().getRealClass().getSimpleName();
        synchronized (ExtentManager.LOCK) {
            extentTest = extent.createTest(testName, description);


            //Tags each test with its class name, and shows the browser used (e.g. "Firefox (ClassName)") in the report.
            extentTest.assignCategory(className);

            String browserName = TestBase.getBrowserName();
            if (browserName != null && !browserName.isEmpty()) {
                String capitalized = Character.toUpperCase(browserName.charAt(0)) + browserName.substring(1);
                extentTest.assignDevice(capitalized + "  (" + className + ")");
            }
        }
        ExtentTestManager.setTest(extentTest);

        log.debug("New Test Started: " + result.getName());
        // No console print here by design - only PASS/FAIL print to console.
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = result.getMethod().getMethodName();

        synchronized (ExtentManager.LOCK) {
            ExtentTestManager.getTest().log(Status.PASS, "Test passed: " + testName);
        }
        log.info("Test Passed: " + result.getName());
        System.out.println("[PASS] " + testName);

        //In method mode this closes the browser now; in suite mode it does nothing, since the browser must stay open.
        TestBase.finishTest(result);
        ExtentTestManager.unload();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();

        log.info("Test Failed: " + result.getName());
        System.out.println("[FAIL] " + testName);

        ExtentTest extentTest = ExtentTestManager.getTest();
        synchronized (ExtentManager.LOCK) {
            extentTest.log(Status.FAIL, "Test failed: " + testName);

            if (result.getThrowable() != null) {
                extentTest.log(Status.FAIL, result.getThrowable());
            }
        }


        //Takes the screenshot while the page is still open — no lock needed since it's per-thread file work, not shared report data.
        String screenshotPath = screenshotUtils.screenShot(result);


        //In method mode, closes the browser now so this test's video/trace are ready to attach — in suite mode it does nothing, since the browser stays open for later tests.
        String videoPath = TestBase.finishTest(result);
        String tracePath = PlaywrightFactory.getTracePath();

        synchronized (ExtentManager.LOCK) {
            if (screenshotPath != null) {
                try {
                    extentTest.fail("Screenshot on failure",
                            MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
                } catch (Exception e) {
                    log.error("Failed to attach screenshot to Extent report for '"
                            + result.getName() + "'.", e);
                }
            }


            //Built as real HTML5 video markup, since Extent's image helper can't play .webm files.
            if (videoPath != null && !videoPath.isEmpty()) {
                extentTest.fail("Video on failure:<br>" + ExtentManager.buildVideoHtml(videoPath));
            }


            //Previously the trace file only existed on disk in method mode — now it's shown directly in the report too, just like suite mode already does.
            if (tracePath != null && !tracePath.isEmpty()) {
                String traceCommand = PlaywrightFactory.buildTraceCommand(Paths.get(tracePath));
                extentTest.info(ExtentManager.buildTraceHtml(tracePath, traceCommand));
            }
        }

        ExtentTestManager.unload();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = result.getMethod().getMethodName();

        ExtentTest extentTest = ExtentTestManager.getTest();
        synchronized (ExtentManager.LOCK) {
            extentTest.log(Status.SKIP, "Test skipped: " + testName);
            if (result.getThrowable() != null) {
                extentTest.log(Status.SKIP, result.getThrowable());
            }
        }
        log.info("Test Skipped: " + result.getName());
        // No console print here by design - only PASS/FAIL print to console.

        TestBase.finishTest(result);
        ExtentTestManager.unload();
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        log.info("Test Failed But Within Success Percentage: " + result.getName());
    }
}