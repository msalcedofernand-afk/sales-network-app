import { test, expect } from "@playwright/test";

test("login is reachable and exposes accessible fields", async ({ page }) => {
  await page.goto("/login");
  await expect(page).toHaveTitle(/Sales Network|VV/i);
  await expect(page.getByLabel(/correo/i)).toBeVisible();
  await expect(page.getByLabel(/contraseña/i)).toBeVisible();
  await expect(page.getByRole("button", { name: /ingresar|entrar|iniciar/i })).toBeVisible();
});

test("catalog route does not render a broken page", async ({ page }) => {
  const response = await page.goto("/catalogo");
  expect(response?.status()).toBeLessThan(500);
  await expect(page.locator("main")).toBeVisible();
});
