package Reports;

import com.aventstack.extentreports.ExtentTest;

//Holds the current ExtentTest per thread. Using ThreadLocal if you later switch TestNG to run test methods in parallel
public final class ExtentTestManager {

    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    private ExtentTestManager() { }

    public static ExtentTest getTest() {
        return extentTest.get();
    }

    public static void setTest(ExtentTest test) {
        extentTest.set(test);
    }

    public static void unload() {
        extentTest.remove();
    }
}
