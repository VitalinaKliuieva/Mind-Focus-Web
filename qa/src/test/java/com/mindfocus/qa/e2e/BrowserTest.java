package com.mindfocus.qa.e2e;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindfocus.qa.support.Settings;
import com.mindfocus.qa.support.TestUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

@Tag("e2e")
abstract class BrowserTest {
    protected WebDriver driver;

    @RegisterExtension
    final ScreenshotOnFailure screenshots = new ScreenshotOnFailure(() -> driver);

    @BeforeEach
    void startBrowser() {
        ChromeOptions options = new ChromeOptions();
        if (Settings.HEADLESS) options.addArguments("--headless=new");
        options.addArguments("--window-size=1400,1000");
        driver = new ChromeDriver(options);   // Selenium Manager finds or downloads the matching driver
    }

    @AfterEach
    void stopBrowser() {
        if (driver != null) driver.quit();
    }

    /**
     * Opens the app already logged in by putting the token into localStorage.
     * Much faster than the login form, which has its own tests.
     */
    protected void openLoggedIn(TestUser user) {
        new LoginPage(driver).open().login(user.email(), user.password());
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.urlContains("/today"));
    }
}
