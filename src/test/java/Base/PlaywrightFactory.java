package Base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ViewportSize;

import Utils.ConfigReader;
import Utils.FrameworkConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * PlaywrightFactory
 * Supports: chromium | firefox | webkit
 * Controlled via config.properties or -Dbrowser=chromium
 */
public class PlaywrightFactory {

    private static final Logger log = LogManager.getLogger(PlaywrightFactory.class);

    // ThreadLocal for parallel-safe execution
    private static final ThreadLocal<Playwright>     PF_Playwright = new ThreadLocal<>();
    private static final ThreadLocal<Browser>        PF_Browser    = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> PF_Context    = new ThreadLocal<>();
    private static final ThreadLocal<Page>           PF_Page       = new ThreadLocal<>();
    private static final ThreadLocal<String>         PF_VideoPath  = new ThreadLocal<>();
    private static final ThreadLocal<String>         PF_TracePath  = new ThreadLocal<>();
    private static final ThreadLocal<String>        PF_ActiveLabel = new ThreadLocal<>();  

    private PlaywrightFactory() { }

    // ── Public accessors ──────────────────────────

    public static Page           getPage()    { return PF_Page.get(); }
    public static Browser        getBrowser() { return PF_Browser.get(); }
    public static BrowserContext getContext() { return PF_Context.get(); }

    
    // get active class label per thread avoid overriding
    public static void setActiveLabel(String label) {
        PF_ActiveLabel.set(label);
    }

    
    //get video path 
    public static String getVideoPath() {
        return PF_VideoPath.get();
    }

  //get trace path after browser closed
    public static String getTracePath() {
        return PF_TracePath.get();
    }

    // ── Lifecycle ─────────────────────────────────

    public static Page initBrowser() {
        return initBrowser(null);
    }

    //overloaded method to get browser on top of config file
    public static Page initBrowser(String browserOverride) {
        String browserName = (browserOverride != null && !browserOverride.isEmpty())
                ? browserOverride.toLowerCase(): ConfigReader.getBrowser().toLowerCase();
                
        boolean headless   = ConfigReader.isHeadless();

        log.info("Initialising browser: [" + browserName + "] | Headless: [" + headless + "]");

        Playwright playwright = Playwright.create();
        PF_Playwright.set(playwright);

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setTimeout(ConfigReader.getDefault_Timeout());

        Browser browser;

        switch (browserName) {
            case "firefox":
            	log.info("Launching Firefox browser");
                browser = playwright.firefox().launch(launchOptions);
                break;
            case "webkit":
            	log.info("Launching Webkit browser");
                browser = playwright.webkit().launch(launchOptions);
                break;
            default:
            	log.info("Launching Chrome browser");
                browser = playwright.chromium().launch(launchOptions);
                break;
        }

        PF_Browser.set(browser);

        boolean videoEnabled = ConfigReader.isVideoEnabled();
        boolean traceEnabled = ConfigReader.isTraceEnabled();

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setViewportSize(new ViewportSize(1536, 864))
                .setAcceptDownloads(true)
                .setIgnoreHTTPSErrors(true)
                .setBaseURL(ConfigReader.getBaseUrl());

        // Only record video when enabled in config.properties (video.enabled)
        if (videoEnabled) {
            contextOptions.setRecordVideoDir(Paths.get(FrameworkConstants.VIDEO_PATH));
        } else {
            log.info("Video recording disabled via config.properties (video.enabled=false)");
        }

        BrowserContext context = browser.newContext(contextOptions);
        context.setDefaultTimeout(ConfigReader.getDefault_Timeout());
        context.setDefaultNavigationTimeout(ConfigReader.getPageload_Timeout());
        PF_Context.set(context);

        // Only start tracing when enabled in config.properties (trace.enabled)
        if (traceEnabled) {
            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
                    .setSources(true));
        } else {
            log.info("Trace capturing disabled via config.properties (trace.enabled=false)");
        }

        Page page = context.newPage();
        PF_Page.set(page);
        // Clear any previous video path for this thread
        PF_VideoPath.remove();

        log.debug("Browser launched -> Base URL: " + ConfigReader.getBaseUrl());
        return page;
    }

    //closed the browser and save trace for fail test case
    public static void closeBrowser() {
        closeBrowser(true);
    }


    // save trace for fail test class/method to desire location, always return true for fail test cases for pass test it false so don't save trace for pass cases.
    public static void closeBrowser(boolean saveTrace) {
    	
        if (PF_Page.get() == null) {
            return;
        }

        log.debug("Closing Playwright resources after execution");
        try {
            Page page = PF_Page.get();
            BrowserContext context = PF_Context.get();

            boolean videoEnabled = ConfigReader.isVideoEnabled();
            boolean traceEnabled = ConfigReader.isTraceEnabled();

            if (videoEnabled && page != null && page.video() != null) {
                try {
                    Path vp = page.video().path();
                    if (vp != null) {
                        PF_VideoPath.set(vp.toAbsolutePath().toString());
                        log.debug("Video recorded at: " + vp.toAbsolutePath());
                    }
                } catch (Exception e) {
            		log.debug(e.getMessage(),e);
                    log.warn("Could not retrieve video path: " + e.getMessage());
                }
            }

            if (context != null && traceEnabled) {
                try {
                    if (saveTrace) {
                    	//set class or method name in active thread
                        String suiteLabel = PF_ActiveLabel.get();
                        if (suiteLabel == null || suiteLabel.isEmpty()) {
                            suiteLabel = "Suite";
                        }
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                                .format(Calendar.getInstance().getTime());

                        Path traceDir = Paths.get(FrameworkConstants.TRACE_PATH);
                        Files.createDirectories(traceDir);

                        Path tracePath = traceDir.resolve(suiteLabel + "_" + timeStamp + ".zip");
                        context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));

                        PF_TracePath.set(tracePath.toAbsolutePath().toString());
                        log.debug("Trace saved: " + tracePath.toAbsolutePath());
                    } else {
                        context.tracing().stop();
                        PF_TracePath.remove();
                        log.debug("Trace discarded (test did not fail) - no .zip written.");
                    }
                } catch (Exception e) {
                    log.warn("Could not stop/save trace: " + e.getMessage());
                }
            } else if (context != null) {
                // trace.enabled=false -> tracing was never started, nothing to stop
                PF_TracePath.remove();
            }

            if (page    != null) page.close();
            if (context != null) context.close(); 
            if (PF_Browser.get()    != null) PF_Browser.get().close();
            if (PF_Playwright.get() != null) PF_Playwright.get().close();

        } finally {
            PF_Page.remove();
            PF_Context.remove();
            PF_Browser.remove();
            PF_Playwright.remove();
            PF_ActiveLabel.remove();
            // NOTE: PF_VideoPath is NOT removed here; TestBase reads it after this call
        }
        log.debug("Playwright closed sucessfully.");
    }

    public static String buildTraceCommand(Path tracePath) {
        return "mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI "
                + "-Dexec.args=\"show-trace " + tracePath.toAbsolutePath() + "\"";
    }

    public static BrowserContext newIncognitoContext() {
        return PF_Browser.get().newContext(
                new Browser.NewContextOptions().setAcceptDownloads(true)
        );
    }
}