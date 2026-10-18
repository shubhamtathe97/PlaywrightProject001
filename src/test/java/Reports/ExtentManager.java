package Reports;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import Utils.ConfigReader;
import Utils.FrameworkConstants;

public final class ExtentManager {

    private static final Logger log = LogManager.getLogger(ExtentManager.class);

    private static ExtentReports extent;
    private static String reportPath;

    private ExtentManager() { /* Utility class - no instantiation */ }

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            extent = createInstance();
        }
        return extent;
    }

    private static ExtentReports createInstance() {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(Calendar.getInstance().getTime());

        // Report file name follows the client XML suite file name (e.g. QA_20260101_101500.html)
        String fileName = resolveReportPrefix() + timeStamp + ".html";

        new File(FrameworkConstants.REPORTS_PATH).mkdirs();
        reportPath = FrameworkConstants.REPORTS_PATH + File.separator + fileName;

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setTheme(Theme.DARK);

        String reportTitle = ConfigReader.get("report.title", "Automation Test Report");
        String reportName = ConfigReader.get("report.name", "Playwright - Execution Report");

        sparkReporter.config().setDocumentTitle(reportTitle);
        sparkReporter.config().setReportName(reportName);
        sparkReporter.config().setTimeStampFormat("EEEE, MMMM dd, yyyy, hh:mm a '('zzz')'");


        //Fixes long content getting cut off, while skipping .card-body so it doesn't break the dashboard charts.
        sparkReporter.config().setCss(
                ".test-content, .test-content-detail, .detail-body, "
              + ".r-tab-panel, .test-wrapper, .test { "
              + "  max-height: none !important; "
              + "  height: auto !important; "
              + "  overflow: visible !important; "
              + "} "
              + "pre, .step-details, .exception, .stack-trace { "
              + "  white-space: pre-wrap !important; "
              + "  word-break: break-word !important; "
              + "}"
        );

        ExtentReports extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);

        extentReports.setSystemInfo("Browser", ConfigReader.getBrowser());
        extentReports.setSystemInfo("Environment", ConfigReader.getEnv());
        extentReports.setSystemInfo("Base URL", ConfigReader.getBaseUrl());
        extentReports.setSystemInfo("Headless", String.valueOf(ConfigReader.isHeadless()));
        extentReports.setSystemInfo("OS", System.getProperty("os.name"));
        extentReports.setSystemInfo("Executed By", System.getProperty("user.name"));

        return extentReports;
    }

    /**
     * Builds the report-name prefix from the client XML suite file name.
     * Order: 1) "activeSuiteName" set by TestRunner (XML picked from the Login sheet)
     *        2) -DsuiteFile / -DsuiteXmlFile passed directly (e.g. mvn test -DsuiteFile=...QA.xml)
     *        3) fallback to the old default "ExtentReport_"
     */
    private static String resolveReportPrefix() {
        String suiteName = System.getProperty("activeSuiteName");

        if (suiteName == null || suiteName.trim().isEmpty()) {
            String suiteFile = System.getProperty("suiteFile");
            if (suiteFile == null || suiteFile.trim().isEmpty()) {
                suiteFile = System.getProperty("suiteXmlFile");
            }
            if (suiteFile != null && !suiteFile.trim().isEmpty()) {
                suiteName = new File(suiteFile.trim()).getName().replaceAll("(?i)\\.xml$", "");
            }
        }

        if (suiteName == null || suiteName.trim().isEmpty()) {
            return FrameworkConstants.EXTENT_REPORT_NAME;
        }

        // Remove characters that are not allowed in file names
        String safe = suiteName.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        return safe + "_";
    }

    public static String getReportPath() {
        return reportPath;
    }


    //Prevents parallel threads from corrupting shared report data, which was breaking the dashboard charts.
    public static final Object LOCK = new Object();


    //Builds a proper playable video block, since Extent's image helper can't handle .webm files correctly.
    public static String buildVideoHtml(String videoPath) {
        if (videoPath == null || videoPath.isEmpty()) return "";
        String videoUri = toFileUri(videoPath);
        return "<video controls preload=\"metadata\" "
             + "style=\"width:100%;max-width:480px;max-height:280px;background:#000;"
             + "display:block;margin:6px 0;border-radius:4px;\">"
             + "<source src=\"" + escape(videoUri) + "\" type=\"video/webm\">"
             + "Your browser can't play this video inline - open it directly: "
             + "<a href=\"" + escape(videoUri) + "\" target=\"_blank\">" + escape(videoPath) + "</a>"
             + "</video>"
             + "<div style=\"font-size:11px;color:#888;word-break:break-all;\">" + escape(videoPath) + "</div>";
    }


    //Adds a clickable trace-file link in the report
    public static String buildTraceHtml(String tracePath, String traceCommand) {
        if (tracePath == null || tracePath.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"margin-top:4px;\"><b>Trace file:</b> ").append(escape(tracePath)).append("<br>")
          .append("Open with <a href=\"https://trace.playwright.dev/\" target=\"_blank\">trace.playwright.dev</a> ")
          .append("and drag the file above onto the page - no install or server needed.<br>");
        if (traceCommand != null) {
            sb.append("Or run this from the project root:<br>")
              .append("<code style=\"background:#1e1e1e;color:#ddd;padding:2px 6px;border-radius:3px;")
              .append("display:inline-block;margin-top:4px;word-break:break-all;\">")
              .append(escape(traceCommand)).append("</code>");
        }
        sb.append("</div>");
        return sb.toString();
    }


    //Builds one class's video/trace section for the combined report widget — used only in class mode.
    public static String buildClassWidgetBlock(String label, String videoPath, String tracePath, String traceCommand) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"margin-bottom:16px;padding-bottom:14px;border-bottom:1px solid #444;\">")
          .append("<b style=\"color:#4a90d9;font-size:14px;\">").append(escape(label)).append("</b><br>");

        if (videoPath != null && !videoPath.isEmpty()) {
            sb.append("<div style=\"margin-top:6px;\"><b style=\"color:#4a90d9;\">Video:</b><br>")
              .append(buildVideoHtml(videoPath)).append("</div>");
        }
        if (tracePath != null && !tracePath.isEmpty()) {
            sb.append(buildTraceHtml(tracePath, traceCommand));
        }
        sb.append("</div>");
        return sb.toString();
    }


    //Adds one floating button showing every class's video/trace
    public static void appendMultiClassBanner(java.util.List<String> classWidgetBlocks) {
        if (reportPath == null || classWidgetBlocks == null || classWidgetBlocks.isEmpty()) return;

        try {
            Path reportFile = Paths.get(reportPath);
            String html = new String(Files.readAllBytes(reportFile), StandardCharsets.UTF_8);

            StringBuilder widget = new StringBuilder();

            widget.append("<div id=\"trace-video-fab\" title=\"Trace &amp; Video\" ")
                  .append("style=\"position:fixed;bottom:22px;right:22px;width:52px;height:52px;")
                  .append("border-radius:50%;background:#4a90d9;color:#fff;display:flex;")
                  .append("align-items:center;justify-content:center;")
                  .append("box-shadow:0 2px 10px rgba(0,0,0,0.5);cursor:pointer;z-index:999999;\" ")
                  .append("onclick=\"var p=document.getElementById('trace-video-panel');")
                  .append("p.style.display=(p.style.display==='none'||p.style.display==='')")
                  .append("?'block':'none';\">")
                  .append("<i class=\"fa fa-video-camera\" style=\"font-size:20px;\"></i>")
                  .append("</div>");

            widget.append("<div id=\"trace-video-panel\" style=\"display:none;position:fixed;")
                  .append("bottom:86px;right:22px;width:460px;max-width:92vw;max-height:80vh;")
                  .append("overflow:auto;background:#2d2d2d;color:#ddd;border-radius:8px;")
                  .append("box-shadow:0 6px 24px rgba(0,0,0,0.55);padding:16px 18px;")
                  .append("font-family:sans-serif;font-size:13px;line-height:1.7;z-index:999999;\">")
                  .append("<div style=\"display:flex;justify-content:space-between;")
                  .append("align-items:center;margin-bottom:10px;\">")
                  .append("<b style=\"color:#4a90d9;font-size:15px;\">Trace &amp; Video - by class</b>")
                  .append("<span style=\"cursor:pointer;font-size:18px;color:#aaa;\" ")
                  .append("onclick=\"document.getElementById('trace-video-panel')")
                  .append(".style.display='none';\">&times;</span>")
                  .append("</div>")
                  .append("<div style=\"color:#999;margin-bottom:10px;\">")
                  .append("browser.lifecycle=CLASS - each class ran in its own browser, so each has ")
                  .append("its own video/trace below.</div>");

            for (String block : classWidgetBlocks) {
                widget.append(block);
            }
            widget.append("</div>");

            int bodyClose = html.lastIndexOf("</body>");
            if (bodyClose == -1) {
                log.warn("Could not find </body> in report HTML - skipping trace/video widget.");
                return;
            }

            String updatedHtml = html.substring(0, bodyClose)
                    + widget
                    + html.substring(bodyClose);

            Files.write(reportFile, updatedHtml.getBytes(StandardCharsets.UTF_8));
            log.debug("Trace/video widget added to report for " + classWidgetBlocks.size() + " class(es).");
        } catch (IOException e) {
            log.warn("Could not add multi-class trace/video widget to report: " + e.getMessage());
        }
    }

    private static String toFileUri(String path) {
        try {
            return Paths.get(path).toUri().toString();
        } catch (Exception e) {
            // Fall back to the raw path if URI conversion somehow fails -
            // better to show something than nothing.
            return path;
        }
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}