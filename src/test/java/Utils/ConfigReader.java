package Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


//Loads config values once and gives simple getters
public class ConfigReader {

   private static final Logger log = LogManager.getLogger(ConfigReader.class);
   private static Properties properties;

   static {
       loadProperties();
   }

   private ConfigReader() { /* Utility class */ }

   private static void loadProperties() {
       properties = new Properties();
       try (FileInputStream fis = new FileInputStream(FrameworkConstants.CONFIG_PATH)) {
           properties.load(fis);
           log.info("config.properties loaded successfully from:");
       } catch (IOException e) {
           log.error("Failed to load config.properties:");
           throw new RuntimeException("Cannot load config.properties – check path: " + FrameworkConstants.CONFIG_PATH, e);
       }
   }

   /** Returns raw property value for the given key */
   public static String get(String key) {
	    String value = System.getProperty(key); // -D flag takes priority

	    if (value == null || value.trim().isEmpty()) {
	        value = properties.getProperty(key);
	    }
	    if (value == null) {
	        throw new RuntimeException("Property not found: " + key);
	    }
	    return value.trim();
	}

   //Returns property value or defaultValue if not found 
   public static String get(String key, String defaultValue) {
       try {
           return get(key);
       } catch (RuntimeException e) {
           return defaultValue;
       }
   }

   // ── Typed convenience getters ──────────────────

   public static String getBrowser() {
	    return get("browser", "chromium");
	}

	public static boolean isHeadless() {
	    return Boolean.parseBoolean(get("headless", "false"));
	}

	public static double getDefault_Timeout() {
	    return Double.parseDouble(get("default.timeout"));
	}
	
	public static double getPageload_Timeout() {
		return Double.parseDouble(get("page.load.timeout"));
	}
	
	public static String getEnv() {
	    return get("Env", "QA");
	}

	//The single execution URL, matched as-is against the Login sheet. 
	public static String getBaseUrl() {
	    return get("URL");
	}

	public static String getBrowserLifecycle() {
	    return get("browser.lifecycle", "CLASS").trim().toUpperCase();
	}

	public static boolean isClassLifecycle() {
	    return "CLASS".equals(getBrowserLifecycle());
	}

	public static boolean isMethodLifecycle() {
	    return "METHOD".equals(getBrowserLifecycle());
	}

	// ── Video / Trace toggles ───────────────────
	public static boolean isVideoEnabled() {
	    return Boolean.parseBoolean(get("video.enabled", "true"));
	}

	public static boolean isTraceEnabled() {
	    return Boolean.parseBoolean(get("trace.enabled", "true"));
	}
}