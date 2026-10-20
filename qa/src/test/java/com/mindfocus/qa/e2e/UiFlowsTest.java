package com.mindfocus.qa.e2e;

import com.mindfocus.qa.support.Settings;
import com.mindfocus.qa.support.TestUser;
import com.mindfocus.qa.support.Users;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("MindFocus")
@Feature("UI")
class UiFlowsTest extends BrowserTest {

    private WebDriverWait waitFor() {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
    }
    @Test
    @Tag("smoke")
    @Story("Registration")
    @Severity(SeverityLevel.BLOCKER)
    void a_new_user_can_register_and_reaches_an_empty_dashboard() {
        new LoginPage(driver).open().register("Ann", Users.uniqueEmail(), Settings.PASSWORD);
        waitFor().until(ExpectedConditions.urlContains("/today"));
        assertThat(new DashboardPage(driver).todoCounter()).isEqualTo("0/0");
    }

    @Test
    @Story("Login")
    void a_wrong_password_shows_an_error_and_stays_on_the_login_page() {
        TestUser user = Users.register();
        LoginPage page = new LoginPage(driver).open();
        page.login(user.email(), "wrong-password");
        assertThat(page.errorText()).contains("Wrong email or password");
        assertThat(driver.getCurrentUrl()).endsWith("/login");
    }

    @Test
    @Story("Login")
    void the_login_form_works_for_an_existing_user() {
        TestUser user = Users.register();
        new LoginPage(driver).open().login(user.email(), user.password());
        waitFor().until(ExpectedConditions.urlContains("/today"));
    }

    @Test
    @Story("Access control")
    @Severity(SeverityLevel.CRITICAL)
    void a_protected_page_redirects_to_login_without_a_session() {
        driver.get(Settings.BASE_URL + "/notes");
        waitFor().until(ExpectedConditions.urlContains("/login"));
    }

    @Test
    @Tag("smoke")
    @Story("The dashboard adapts after every action")
    @Severity(SeverityLevel.CRITICAL)
    void the_dashboard_counter_follows_the_to_do_list() throws Exception {
        openLoggedIn(Users.register());
        DashboardPage dashboard = new DashboardPage(driver);
        dashboard.waitForTodoCounter("0/0");

        new TodosPage(driver).open().add("write the QA report");
        dashboard.open().waitForTodoCounter("0/1");

        new TodosPage(driver).open().tick("write the QA report");
        dashboard.open().waitForTodoCounter("1/1");
        assertThat(dashboard.insights()).anyMatch(line -> line.contains("finished 1 thing today"));
    }
}
