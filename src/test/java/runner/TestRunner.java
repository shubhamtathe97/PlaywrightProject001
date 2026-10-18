package runner;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.testng.Assert;
import org.testng.TestNG;
import org.testng.annotations.Test;

import Utils.ConfigReader;
import Utils.ExcelUtils;
import Utils.FrameworkConstants;

public class TestRunner {

    private static final Logger log = LogManager.getLogger(TestRunner.class);

    @Test
    public void executeClientSuite() {

        PropertyConfigurator.configure(ClassLoader.getSystemResource("log4j.properties"));

        // 1. Get the execution URL - env-resolved from config.properties,
        //    or a -DURL=.../-Denv=... override (see ConfigReader.getBaseUrl()).
        String executionURL = ConfigReader.getBaseUrl();

        log.info("========================================");
        log.info("Execution URL: " + executionURL);

        // 2. Find the matching row in the Login sheet
        List<Map<String, String>> loginRows = ExcelUtils.getSheetData(FrameworkConstants.SHEET_NAME);

        String matchedXml = null;
        for (Map<String, String> row : loginRows) {
            String sheetUrl = row.getOrDefault("URL", "");
            if (normalize(sheetUrl).equals(normalize(executionURL))) {
                matchedXml = row.get("XML");
                break;
            }
        }

        // 3. Stop if no mapping found - fail loudly rather than silently
        if (matchedXml == null || matchedXml.trim().isEmpty()) {
            log.error("No XML mapping found in '" + FrameworkConstants.SHEET_NAME+ "' sheet for URL: " + executionURL);
            Assert.fail("No XML mapping found for URL: " + executionURL + ". Add a row for it in the 'Login' sheet of TestData.xlsx "+ "(columns: URL | XML).");
            return;
        }

        matchedXml = matchedXml.trim();
        log.info("Matched client XML: " + matchedXml);

        // 4. Build and verify the full path to that suite file
        String xmlPath = FrameworkConstants.SUITE_PATH + File.separator + matchedXml;
        File xmlFile = new File(xmlPath);

        if (!xmlFile.exists()) {
            log.error("Mapped suite file does not exist on disk: " + xmlPath);
            Assert.fail("Mapped suite file does not exist on disk: " + xmlPath
                    + ". Check the 'XML' column in the Login sheet and confirm "
                    + "the file lives under src/test/resources/suites/.");
            return;
        }

        log.info("Executing suite: " + xmlPath);
        log.info("========================================");

        String suiteLabel = matchedXml.replace(".xml", "").replace(".XML", "");
        System.setProperty("activeSuiteName", suiteLabel);

        // 5. Execute the selected suite as a nested TestNG run. Default
        TestNG testNG = new TestNG();
        testNG.setTestSuites(Collections.singletonList(xmlPath));
        testNG.setUseDefaultListeners(false);
        testNG.run();

        log.info("Suite '" + matchedXml + "' finished. See test-output/Report for the Extent report.");

        // 6. Bubble failures up so CI / the outer run reflects the real result
        if (testNG.hasFailure()) {
            Assert.fail("One or more tests failed in suite: " + matchedXml
                    + ". See test-output/Report for details.");
        }
    }

    /** Trims and strips a trailing slash so URL comparisons aren't fooled by it. */
    private static String normalize(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
