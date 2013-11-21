package org.rstudio.studio.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class BootRStudio  {
    public static void main(String[] args) {
        WebDriver driver = new FirefoxDriver();

        driver.get("http://localhost:8787/");
        WebElement username = driver.findElement(By.name("username"));
        username.sendKeys("Bob");

        WebElement password = driver.findElement(By.name("password"));
        password.sendKeys("b0bsp4ssw0rd");
        password.submit();

        // Check the title of the page
        System.out.println("Page title is: " + driver.getTitle());

        //Close the browser
        driver.quit();
    }
}
