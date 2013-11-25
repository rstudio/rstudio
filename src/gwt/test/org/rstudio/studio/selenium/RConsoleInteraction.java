package org.rstudio.studio.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.Keys;
import org.rstudio.core.client.ElementIds;

import static org.junit.Assert.*;

import org.junit.Test; 

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class RConsoleInteraction  {
   @Test
   public void testRConsoleInteraction() throws MalformedURLException {
       final WebDriver driver = new RemoteWebDriver(
             new URL("http://localhost:9515/"), DesiredCapabilities.chrome());
       
       driver.get("http://localhost:8787/");
       
       // Wait for the console panel to load
       (new WebDriverWait(driver, 15)).until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
             List<WebElement>elements = driver.findElements(By.id(
                   ElementIds.getElementId(ElementIds.CONSOLE_INPUT)));
             return elements.size() > 0;
          }
       });
       
       WebElement console = driver.findElement(By.id(
             ElementIds.getElementId(ElementIds.SHELL_WIDGET)));

       // Test evaluating a basic expression with R 
       console.sendKeys(Keys.chord(Keys.CONTROL, "l"));
       console.sendKeys(Keys.ESCAPE);
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
       
       // Test clearing the console
       console.sendKeys(Keys.chord(Keys.CONTROL, "l"));

       (new WebDriverWait(driver, 5)).until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
             String outputText = output.getText();
             return !outputText.contains("42");
          }
       });
       
       // Test invoking and cancelling auto-complete

       List<WebElement>elements = driver.findElements(By.id(
             ElementIds.getElementId(ElementIds.POPUP_COMPLETIONS)));
       assertEquals(elements.size(), 0);

       console.sendKeys(Keys.chord(Keys.CONTROL, "l"));
       console.sendKeys("print");
       console.sendKeys(Keys.TAB);
       
       (new WebDriverWait(driver, 5)).until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
             List<WebElement>elements = driver.findElements(By.id(
                   ElementIds.getElementId(ElementIds.POPUP_COMPLETIONS)));
             return elements.size() > 0;
          }
       });

       console.sendKeys(Keys.ESCAPE);

       (new WebDriverWait(driver, 5)).until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
             List<WebElement>elements = driver.findElements(By.id(
                   ElementIds.getElementId(ElementIds.POPUP_COMPLETIONS)));
             return elements.size() == 0;
          }
       });

       // Close the browser
       driver.quit();
   }
}
