package com.mindfocus.qa.e2e;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

class DashboardPage {
    private static final By TODO_COUNTER = By.cssSelector(".card.pink .big");

    private final WebDriver driver;
    private final WebDriverWait wait;

    DashboardPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Step("Open the dashboard from the sidebar")
    DashboardPage open() {
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("nav a[href='/today']"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(TODO_COUNTER));
        return this;
    }

    String todoCounter() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(TODO_COUNTER));
        return driver.findElement(TODO_COUNTER).getText().trim();
    }

    @Step("The to-do counter on the dashboard shows {expected}")
    void waitForTodoCounter(String expected) {
        wait.until(ExpectedConditions.textToBe(TODO_COUNTER, expected));
    }

    List<String> insights() {
        return driver.findElements(By.cssSelector("ul.typed li")).stream().map(WebElement::getText).toList();
    }
}
