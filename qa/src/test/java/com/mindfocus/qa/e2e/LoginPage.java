package com.mindfocus.qa.e2e;

import com.mindfocus.qa.support.Settings;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Page object: every selector of the login page lives here, so a UI change is fixed in one place. */
class LoginPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Step("Open the login page")
    LoginPage open() {
        driver.get(Settings.BASE_URL + "/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[name=email]")));
        return this;
    }

    private void fill(String name, String value) {
        var field = driver.findElement(By.cssSelector("input[name=" + name + "]"));
        field.clear();
        field.sendKeys(value);
    }

    private void submit() {
        driver.findElement(By.cssSelector("button[type=submit]")).click();
    }

    @Step("Log in as {email}")
    void login(String email, String password) {
        fill("email", email);
        fill("password", password);
        submit();
    }

    @Step("Register {email}")
    void register(String name, String email, String password) {
        driver.findElement(By.cssSelector("button.switch")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[name=name]")));
        fill("name", name);
        fill("email", email);
        fill("password", password);
        submit();
    }

    String errorText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".error"))).getText();
    }
}
