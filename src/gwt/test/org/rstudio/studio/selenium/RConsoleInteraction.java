package org.rstudio.studio.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.Keys;
import org.rstudio.core.client.ElementIds;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test; 

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class RConsoleInteraction {
   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      driver_ = new RemoteWebDriver(
            new URL("http://localhost:9515/"), DesiredCapabilities.chrome());
      
      driver_.get("http://localhost:8787/");
      
       // Wait for the console panel to load
      (new WebDriverWait(driver_, 15)).until(new ExpectedCondition<Boolean>() {
         public Boolean apply(WebDriver d) {
            List<WebElement>elements = driver_.findElements(By.id(
                  ElementIds.getElementId(ElementIds.CONSOLE_INPUT)));
            return elements.size() > 0;
         }
      });

      // Click on the shell
      WebElement console = driver_.findElement(By.id(
            ElementIds.getElementId(ElementIds.SHELL_WIDGET)));

      console.click();
   }
   
   @AfterClass
   public static void tearDownAfterClass() throws Exception {
      driver_.quit();
   }
   
   @Test
   public void testBasicRInteraction() {
       Actions do42 = new Actions(driver_);
       do42.sendKeys(Keys.chord(Keys.CONTROL, "l"));
       do42.sendKeys(Keys.ESCAPE);
       do42.sendKeys("41 + 1");
       do42.sendKeys(Keys.ENTER);
       do42.perform();

       final WebElement output = driver_.findElement(By.id(
             ElementIds.getElementId(ElementIds.CONSOLE_OUTPUT)));

       (new WebDriverWait(driver_, 5)).until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
             String outputText = output.getText();
             return outputText.contains("42");
          }
       });
   }
   
   @Test
   public void testPopupCompletion() {
       // Test invoking and cancelling auto-complete
       List<WebElement>elements = driver_.findElements(By.id(
             ElementIds.getElementId(ElementIds.POPUP_COMPLETIONS)));
       assertEquals(elements.size(), 0);

       Actions popup = new Actions(driver_);
       popup.sendKeys(Keys.ESCAPE);
       popup.sendKeys("print");
       popup.sendKeys(Keys.TAB);
       popup.perform();
       
       (new WebDriverWait(driver_, 5)).until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
             List<WebElement>elements = driver_.findElements(By.id(
                   ElementIds.getElementId(ElementIds.POPUP_COMPLETIONS)));
             return elements.size() > 0;
          }
       });

       Actions close = new Actions(driver_);
       close.sendKeys(Keys.ESCAPE).perform();

       (new WebDriverWait(driver_, 5)).until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
             List<WebElement>elements = driver_.findElements(By.id(
                   ElementIds.getElementId(ElementIds.POPUP_COMPLETIONS)));
             return elements.size() == 0;
          }
       });
   }

   private static WebDriver driver_;
}
