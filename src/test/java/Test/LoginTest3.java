package Test;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.function.Consumer;

import Listeners.TestListener;
import Base.TestBase;
import Pages.LoginPage;

@Listeners(TestListener.class)
public class LoginTest3 extends TestBase {

    private static final Logger log = LogManager.getLogger(LoginTest.class);

    // Page object for this class. Created once per test method in initPageObjects().
    // NOTE: a plain field is safe for sequential runs and parallel="classes"/"tests"
    // (each class has its own instance). It is NOT safe for parallel="methods".
    private LoginPage BP;

    // TestNG runs TestBase's @BeforeMethod first, so getPage() is already valid here.
    @BeforeMethod(alwaysRun = true)
    public void initPageObjects() {
        BP = new LoginPage(getPage());
    }

    @Test(priority = 1)
    public void TC_Login_001_verifyRadioButtonSelection() {
        BP.radio2.check();

        Assert.assertTrue(BP.radio2.isChecked(), "Radio2 should be checked");
        Assert.assertFalse(BP.radio1.isChecked(), "Radio1 should NOT be checked");
        Assert.assertFalse(BP.radio3.isChecked(), "Radio3 should NOT be checked");
    }

    @Test(priority = 2)
    public void TC_Login_002_verifyCheckboxSelection() {
        BP.checkboxOption1.check();
        BP.checkboxOption3.check();

        Assert.assertTrue(BP.checkboxOption1.isChecked(), "Option1 should be checked");
        Assert.assertFalse(BP.checkboxOption2.isChecked(), "Option2 should NOT be checked");
        Assert.assertTrue(BP.checkboxOption3.isChecked(), "Option3 should be checked");
    }

    @Test(priority = 3)
    public void TC_Login_003_verifyCountryAutocompleteSuggestion() {
        BP.countryAutocomplete.fill("ind");
        // jQuery UI autocomplete list takes a moment to render.
        getPage().waitForTimeout(800);

        Locator indiaSuggestion =
                getPage().getByText("India", new Page.GetByTextOptions().setExact(true));
        Assert.assertTrue(indiaSuggestion.isVisible(), "'India' suggestion should appear in the list");

        indiaSuggestion.click();
        Assert.assertEquals(BP.countryAutocomplete.inputValue(), "India");
    }

    @Test(priority = 4)
    public void TC_Login_004_verifyDropdownSelection() {
        BP.dropdown.selectOption("option2");

        Assert.assertEquals(BP.dropdown.inputValue(), "option2");
    }

    @Test(priority = 5)
    public void TC_Login_005_verifyAlertMessageOnAlertButton() {
        BP.enterNameInput.fill("Claude");

        // Dialog handler MUST be registered before the click that
        // triggers the alert - confirmed live. Using onDialog/offDialog
        // (not onceDialog) since that's the exact pattern BasePage.java
        // already uses elsewhere in this codebase.
        final String[] alertMessage = new String[1];
        Consumer<Dialog> handler = dialog -> {
            alertMessage[0] = dialog.message();
            dialog.accept();
        };
        getPage().onDialog(handler);

        BP.alertButton.click();
        getPage().waitForTimeout(500);
        getPage().offDialog(handler);

        Assert.assertEquals(alertMessage[0],
                "Hello Claude, share this practice page and share your knowledge");
    }

    @Test(priority = 6)
    public void TC_Login_006_verifyHideShowElement() {
        BP.hideButton.click();
        Assert.assertFalse(BP.hideShowTextbox.isVisible(), "Textbox should be hidden after clicking Hide");

        BP.showButton.click();
        Assert.assertTrue(BP.hideShowTextbox.isVisible(), "Textbox should be visible again after clicking Show");
    }

    @Test(priority = 7)
    public void TC_Login_007_verifyWebTableHeadersVisible() {
        Assert.assertTrue(BP.instructorHeader.isVisible(), "Instructor header should be visible");
        Assert.assertTrue(BP.courseHeader.isVisible(), "Course header should be visible");
        Assert.assertTrue(BP.priceHeader.isVisible(), "Price header should be visible");
    }

    @Test(priority = 8)
    public void TC_Login_008_verifyMouseHoverRevealsTopLink() {
        // Not visible until hovered - confirmed live.
        BP.mouseHoverButton.hover();
        getPage().waitForTimeout(500);

        Assert.assertTrue(BP.topLink.isVisible(), "'Top' link should appear after hovering Mouse Hover button");
    }
}