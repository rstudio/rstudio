package org.rstudio.studio.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.Assert.*;

public class BootRStudio  {
    public static void main(String[] args) {
        WebDriver driver = new FirefoxDriver();

        driver.get("http://localhost:8787/");

        // Check the title of the page
        assertEquals(driver.getTitle(), "RStudio");

        //Close the browser
        driver.quit();
    }
}
