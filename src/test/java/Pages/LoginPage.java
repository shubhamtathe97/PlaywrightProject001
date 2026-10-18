package Pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;


public class LoginPage {

    // Own page reference - no longer inherited from BasePage.
    protected Page page;

    // -- Radio Button Example --
    public Locator radio1;
    public Locator radio2;
    public Locator radio3;

    // -- Suggestion Class Example (autocomplete) --
    public Locator countryAutocomplete;

    // -- Dropdown Example --
    public Locator dropdown;

    // -- Checkbox Example --
    public Locator checkboxOption1;
    public Locator checkboxOption2;
    public Locator checkboxOption3;

    // -- Switch Window Example --
    public Locator openWindowButton;

    // -- Switch Tab Example --
    public Locator openTabLink;

    // -- Switch To Alert Example --
    public Locator enterNameInput;
    public Locator alertButton;
    public Locator confirmButton;

    // -- Element Displayed Example --
    public Locator hideButton;
    public Locator showButton;
    public Locator hideShowTextbox;

    // -- Web Table Example --
    public Locator instructorHeader;
    public Locator courseHeader;
    public Locator priceHeader;

    // -- Mouse Hover Example --
    public Locator mouseHoverButton;
    public Locator topLink;

    public LoginPage(Page page) {
        // extended BasePage; now it's just done locally instead of via super().
        this.page = page;

        // Radio buttons - getByText(...).locator("input") because
        // getByRole(RADIO, {name}) does not match (see class comment).
        radio1 = page.getByText("Radio1", new Page.GetByTextOptions().setExact(true))
                .locator("input[type='radio']");
        radio2 = page.getByText("Radio2", new Page.GetByTextOptions().setExact(true))
                .locator("input[type='radio']");
        radio3 = page.getByText("Radio3", new Page.GetByTextOptions().setExact(true))
                .locator("input[type='radio']");

        // Autocomplete textbox - real <input placeholder="Type to Select Countries">
        countryAutocomplete = page.getByPlaceholder("Type to Select Countries");

        // Dropdown - the only <select> on the page.
        dropdown = page.getByRole(AriaRole.COMBOBOX);

        // Checkboxes - same broken-label situation as the radios.
        checkboxOption1 = page.getByText("Option1", new Page.GetByTextOptions().setExact(true))
                .locator("input[type='checkbox']");
        checkboxOption2 = page.getByText("Option2", new Page.GetByTextOptions().setExact(true))
                .locator("input[type='checkbox']");
        checkboxOption3 = page.getByText("Option3", new Page.GetByTextOptions().setExact(true))
                .locator("input[type='checkbox']");

        // Real <button> - getByRole works cleanly here.
        openWindowButton = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Open Window"));

        openTabLink = page.getByRole(AriaRole.LINK,
                new Page.GetByRoleOptions().setName("Open Tab"));

        // <input placeholder="Enter Your Name"> + <input type="submit" value="Alert">.
        // type="submit" inputs ARE exposed with role=button and their
        // "value" as the accessible name - confirmed live.
        enterNameInput = page.getByPlaceholder("Enter Your Name");
        alertButton = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Alert").setExact(true));
        confirmButton = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Confirm"));

        // setExact(true) matters here: without it, "Hide" would also match
        // "Hide/Show Example" style buttons/text elsewhere on the page.
        hideButton = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Hide").setExact(true));
        showButton = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Show").setExact(true));
        hideShowTextbox = page.getByPlaceholder("Hide/Show Example");

        instructorHeader = page.getByRole(AriaRole.COLUMNHEADER,
                new Page.GetByRoleOptions().setName("Instructor"));
        courseHeader = page.getByRole(AriaRole.COLUMNHEADER,
                new Page.GetByRoleOptions().setName("Course"));
        priceHeader = page.getByRole(AriaRole.COLUMNHEADER,
                new Page.GetByRoleOptions().setName("Price"));

        mouseHoverButton = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Mouse Hover"));
        // Only visible after hovering mouseHoverButton - confirmed live.
        topLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Top"));
    }
}