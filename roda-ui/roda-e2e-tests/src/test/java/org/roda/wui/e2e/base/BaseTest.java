/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.e2e.base;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;

@org.testng.annotations.Test(groups = {"e2e"})
public abstract class BaseTest {

  protected static Playwright playwright;
  protected static Browser browser;

  protected BrowserContext context;
  protected Page page;

  public static final String BASE_URL = System.getProperty("eterna.base.url", "http://localhost:8080");
  public static final String ADMIN_USER = System.getProperty("eterna.admin.user", "admin");
  public static final String ADMIN_PASSWORD = System.getProperty("eterna.admin.password", "eterna");
  public static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("playwright.headless", "true"));
  /**
   * Set -Dplaywright.browser=firefox to use Firefox (better lib compatibility on
   * Fedora/RHEL).
   */
  public static final String BROWSER = System.getProperty("playwright.browser", "chromium");

  @BeforeSuite(alwaysRun = true)
  public static void launchBrowser() throws Exception {
    playwright = Playwright.create();
    BrowserType browserType = "firefox".equalsIgnoreCase(BROWSER) ? playwright.firefox()
      : "webkit".equalsIgnoreCase(BROWSER) ? playwright.webkit() : playwright.chromium();
    browser = browserType.launch(new BrowserType.LaunchOptions().setHeadless(HEADLESS));

    // Health check — fail the suite fast with a clear message if the app isn't up
    APIRequestContext api = playwright.request().newContext(new APIRequest.NewContextOptions().setBaseURL(BASE_URL));
    try {
      APIResponse health = api.get("/api/openapi.json");
      Assert.assertEquals(health.status(), 200,
        "Application not reachable at " + BASE_URL + " — ensure the app is running before executing E2E tests:"
          + "\n  mvn -pl roda-ui/roda-wui -am spring-boot:run -Pdebug-main");
    } finally {
      api.dispose();
    }

    Files.createDirectories(Paths.get("target/e2e-screenshots"));
    Files.createDirectories(Paths.get("target/e2e-traces"));
    Files.createDirectories(Paths.get("target/e2e-videos"));
  }

  @AfterSuite(alwaysRun = true)
  public static void closeBrowser() {
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }

  @BeforeMethod(alwaysRun = true)
  public void createContext() {
    context = browser.newContext(new Browser.NewContextOptions().setRecordVideoDir(Paths.get("target/e2e-videos")));
    context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
    page = context.newPage();
  }

  @AfterMethod(alwaysRun = true)
  public void closeContext(ITestResult result) {
    if (result.getStatus() == ITestResult.FAILURE) {
      String name = result.getTestClass().getRealClass().getSimpleName() + "_" + result.getName();
      context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/e2e-traces/" + name + ".zip")));
      page.screenshot(
        new Page.ScreenshotOptions().setPath(Paths.get("target/e2e-screenshots/" + name + ".png")).setFullPage(true));
    } else {
      context.tracing().stop();
    }
    context.close();
  }

  /**
   * Navigates to the login page and authenticates as the admin user.
   */
  protected void loginAsAdmin() {
    page.navigate(BASE_URL + "/#login");
    page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
    // Username field (first .fieldTextBox)
    page.locator(".fieldTextBox").first().fill(ADMIN_USER);
    // Password field
    page.locator("input[type=password]").fill(ADMIN_PASSWORD);
    page.locator(".login-button").first().click();
    // Wait until we leave the login page
    page.waitForURL(url -> !url.contains("#login") && !url.equals(BASE_URL + "/") && !url.equals(BASE_URL));
    page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
  }
}
