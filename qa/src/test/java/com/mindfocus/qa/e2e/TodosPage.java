package com.mindfocus.qa.e2e;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

class TodosPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    TodosPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Step("Open the to-do page from the sidebar")
    TodosPage open() {
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("nav a[href='/todos']"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[name=title]")));
        return this;
    }

    private WebElement row(String title) {
        By row = By.xpath("//ul[contains(@class,'items')]//li[.//span[contains(@class,'grow') and normalize-space()='" + title + "']]");
        return wait.until(ExpectedConditions.visibilityOfElementLocated(row));
    }

    @Step("Add the to-do '{title}'")
    void add(String title) {
        driver.findElement(By.cssSelector("input[name=title]")).sendKeys(title);
        driver.findElement(By.cssSelector("form button[type=submit]")).click();
        row(title);   // waits until it appears in the list
    }

    @Step("Tick the to-do '{title}'")
    void tick(String title) {
        row(title).findElement(By.cssSelector("input.check")).click();
    }
}
