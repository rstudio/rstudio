package org.rstudio.studio.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.Keys;

import org.rstudio.core.client.ElementIds;

import static org.junit.Assert.*;

import org.junit.Test; 

import java.util.List;

public class RConsoleInteraction  {
   @Test
   public void testRConsoleInteraction() {
       final WebDriver driver = new FirefoxDriver();

       driver.get("http://localhost:8787/");
       
       (new WebDriverWait(driver, 15)).until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
             List<WebElement>elements = driver.findElements(By.id(
                   ElementIds.getElementId(ElementIds.CONSOLE_INPUT)));
             return elements.size() > 0;
          }
       });
       
       WebElement console = driver.findElement(By.id(
             ElementIds.getElementId(ElementIds.SHELL_WIDGET)));

       console.sendKeys("40 + 2");
       console.sendKeys(Keys.ENTER);
       
       final WebElement output = driver.findElement(By.id(
             ElementIds.getElementId(ElementIds.CONSOLE_OUTPUT)));

       (new WebDriverWait(driver, 5)).until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
             String outputText = output.getText();
             return outputText.contains("42");
          }
       });

       //Close the browser
       driver.quit();
   }
}
