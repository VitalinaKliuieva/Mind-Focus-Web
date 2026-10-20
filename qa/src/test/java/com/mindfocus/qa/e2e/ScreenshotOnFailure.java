package com.mindfocus.qa.e2e;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.util.function.Supplier;

/** Attaches a screenshot to the Allure report when a browser test fails. Runs before the browser is closed. */
class ScreenshotOnFailure implements AfterTestExecutionCallback {
    private final Supplier<WebDriver> driver;

    ScreenshotOnFailure(Supplier<WebDriver> driver) {
        this.driver = driver;
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getExecutionException().isPresent() && driver.get() instanceof TakesScreenshot shooter) {
            byte[] png = shooter.getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment("screenshot on failure", "image/png", new ByteArrayInputStream(png), "png");
        }
    }
}
