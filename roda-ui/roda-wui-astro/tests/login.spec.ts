import { test, expect } from "@playwright/test";

const BASE_URL = "http://localhost:4321";

async function fillLoginForm(page: any, username: string, password: string) {
  // Wait for React island to hydrate before filling controlled inputs
  await page.waitForLoadState("networkidle");
  await page.locator("input#username").fill(username);
  await page.locator("input#password").fill(password);
  await page.locator("button[type=submit]").click();
}

test.describe("Login flow", () => {
  test("shows login page when unauthenticated", async ({ page }) => {
    await page.goto(BASE_URL + "/browse");
    await expect(page).toHaveURL(/\/login/);
    await expect(page.locator("input#username")).toBeVisible();
    await expect(page.locator("input#password")).toBeVisible();
  });

  test("shows error on wrong password", async ({ page }) => {
    await page.goto(BASE_URL + "/login");
    await fillLoginForm(page, "admin", "wrongpassword");
    // Spring Boot returns "Unauthorized access" for bad credentials
    await expect(page.locator("text=Unauthorized access")).toBeVisible({ timeout: 10000 });
  });

  test("logs in successfully and redirects to home", async ({ page }) => {
    await page.goto(BASE_URL + "/login");
    await fillLoginForm(page, "admin", "eterna");

    // Should redirect away from login
    await expect(page).not.toHaveURL(/\/login/, { timeout: 15000 });

    // Welcome page should have the ETERNA heading
    await expect(page.locator("h1").filter({ hasText: "Welcome" }).or(
      page.locator("h1").filter({ hasText: "ETERNA" })
    ).first()).toBeVisible({ timeout: 10000 });
  });

  test("authenticated user can browse", async ({ page }) => {
    // Log in
    await page.goto(BASE_URL + "/login");
    await fillLoginForm(page, "admin", "eterna");
    await expect(page).not.toHaveURL(/\/login/, { timeout: 15000 });

    // Navigate to browse
    await page.goto(BASE_URL + "/browse");
    await expect(page).toHaveURL(/\/browse/);
    // AIP list should be rendered — look for a table or loading indicator
    await expect(page.locator("table").or(page.locator("[data-testid='aip-list']")).or(page.getByText("No results"))).toBeVisible({ timeout: 15000 });
  });

  test("logout redirects to login page", async ({ page }) => {
    // Log in
    await page.goto(BASE_URL + "/login");
    await fillLoginForm(page, "admin", "eterna");
    await expect(page).not.toHaveURL(/\/login/, { timeout: 15000 });

    // Logout via Spring Boot's logout filter at /logout
    await page.goto(BASE_URL + "/logout");
    // After logout, navigating to a protected page should redirect to login
    await page.goto(BASE_URL + "/browse");
    await expect(page).toHaveURL(/\/login/, { timeout: 10000 });
  });
});
