/*
 * RConsoleInteraction.java
 *
 * Copyright (C) 2009-13 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.Keys;
import org.rstudio.core.client.ElementIds;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test; 

import java.util.List;

import junit.framework.Assert;

public class RConsoleInteraction {
   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      driver_ = RStudioWebAppDriver.start();
      
      ConsoleTestUtils.beginConsoleInteraction(driver_);
   }
   
   @AfterClass
   public static void tearDownAfterClass() throws Exception {
      RStudioWebAppDriver.stop();
   }
   
   @Test
   public void testBasicRInteraction() {
       Actions do42 = new Actions(driver_);
       do42.sendKeys(Keys.chord(Keys.CONTROL, "l"));
       do42.sendKeys(Keys.ESCAPE);
       do42.sendKeys("41 + 1");
       do42.sendKeys(Keys.ENTER);
       do42.perform();

       ConsoleTestUtils.waitForConsoleContainsText(driver_, "42");
   }
   
   @Test
   public void testPopupCompletion() {
       // Test invoking autocomplete
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

       // Test cancelling autocomplete once invoked
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
   
   @Test
   public void testPlotGeneration() {
      ConsoleTestUtils.resumeConsoleInteraction(driver_);

      Actions plotCars = new Actions(driver_);
      plotCars.sendKeys(Keys.ESCAPE + "plot(cars)" + Keys.ENTER);
      plotCars.perform();
      
      // Wait for the Plot window to activate
      final WebElement plotWindow = (new WebDriverWait(driver_, 5))
        .until(ExpectedConditions.presenceOfElementLocated(
              By.id(ElementIds.getElementId(ElementIds.PLOT_IMAGE_FRAME))));
      
      // Wait for a plot to appear in the window
      Assert.assertEquals(plotWindow.getTagName(), "iframe");
      driver_.switchTo().frame(plotWindow);

      (new WebDriverWait(driver_, 5))
        .until(ExpectedConditions.presenceOfElementLocated(By.tagName("img")));
      
      // Switch back to document context
      driver_.switchTo().defaultContent();
   }

   @Test
   public void testInvokeHelp() {
      ConsoleTestUtils.resumeConsoleInteraction(driver_);
      Actions help = new Actions(driver_);
      help.sendKeys(Keys.ESCAPE + "?lapply" + Keys.ENTER);
      help.perform();

      // Wait for the Help window to activate
      final WebElement helpWindow = (new WebDriverWait(driver_, 5))
        .until(ExpectedConditions.presenceOfElementLocated(
              By.id(ElementIds.getElementId(ElementIds.HELP_FRAME))));

      // Wait for help to appear in the window
      Assert.assertEquals(helpWindow.getTagName(), "iframe");
      driver_.switchTo().frame(helpWindow);
      
      (new WebDriverWait(driver_, 5))
        .until(ExpectedConditions.textToBePresentInElement(
              By.tagName("body"), "lapply"));
      
      // Switch back to document context
      driver_.switchTo().defaultContent();
   }

   private static WebDriver driver_;
}
