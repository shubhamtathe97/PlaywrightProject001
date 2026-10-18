package Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.ITestResult;
import org.testng.Reporter;

import com.microsoft.playwright.Page;

import Base.PlaywrightFactory;

public class ScreenshotUtils {

    private static final Logger log = LogManager.getLogger(ScreenshotUtils.class);


    //Takes a screenshot, logs it into TestNG's report, and returns its path so it can also be attached to the Extent report without capturing it twice.
    public String captureScreenShot(ITestResult result) {
        try {
            Page page = PlaywrightFactory.getPage();

            if (page == null) {
                log.warn("Cannot capture screenshot for '" + result.getName()
                        + "' - Playwright page is not available (browser already closed).");
                return null;
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

            //Class name is added to the filename so two classes failing on the same test at the same time don't overwrite each other's screenshot.
            String className = result.getTestClass().getRealClass().getSimpleName();
            String fileName = className + "_" + result.getName() + "_" + timeStamp + ".png";
            File target = new File( FrameworkConstants.SCREENSHOTS_PATH, fileName);
            target.getParentFile().mkdirs();

            //Takes a screenshot of just the visible viewport, not the full scrollable page.
            page.screenshot(new Page.ScreenshotOptions().setPath(target.toPath()) );

            //Converts the path to a file:// URI so the image link still works no matter where the report file itself is opened from.
            String screenshotPath = target.toURI().toString();

            Reporter.log(
                    "<div style='margin-top:8px;'>" + "<b>Screenshot on failure:</b><br/>"+ "<a href=\"" + screenshotPath + "\" "+ "target=\"_blank\">"
                    + "<img src=\"" + screenshotPath + "\" "+ "alt=\"Screenshot on failure\" "+ "title=\"Click to open full screenshot\" "
                    + "style=\""+ "width:300px;" + "height:300px;"+ "object-fit:contain;"+ "border:1px solid #999;"+ "cursor:pointer;"
                    + "\"/>"
                    + "</a>"
                    + "</div>"
            );
            log.debug("Screenshot captured: " + target.getAbsolutePath());

            //Returns the same file:// URI instead of a raw Windows path, so the screenshot renders correctly in the Extent report on any OS.
            return screenshotPath;

        } catch (Exception e) {
            log.error(
                    "Failed to capture screenshot for '"+ result.getName()+ "'.", e);
            return null;
        }
    }


    //Only takes a screenshot if the test actually failed.
    public String screenShot(ITestResult result) {

        if (ITestResult.FAILURE == result.getStatus()) {
            return captureScreenShot(result);
        }
        return null;
    }
}