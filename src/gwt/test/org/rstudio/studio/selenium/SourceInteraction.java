/*
 * SourceInteraction.java
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

import java.net.URL;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.rstudio.core.client.ElementIds;

public class SourceInteraction
{
   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      driver_ = new RemoteWebDriver(
            new URL("http://localhost:9515/"), DesiredCapabilities.chrome());
      
      driver_.get("http://localhost:8787/");

      (new WebDriverWait(driver_, 10))
        .until(ExpectedConditions.presenceOfElementLocated(
              By.className("gwt-MenuBar")));
   }
   
   @AfterClass
   public static void tearDownAfterClass() throws Exception {
      driver_.quit();
   }
   
   @Test
   public void createAndSourceRFile() {
      createRFile();
      
      // Type some code into the file. Note that the matching brace is auto 
      // completed.
      Actions a = new Actions(driver_);
      a.sendKeys("f <- function() {" + Keys.ENTER);
      a.sendKeys(Keys.TAB + "42");
      a.perform();
      
      // Source the entire file
      WebElement sourceMenuEntry = MenuNavigator.getMenuItem(driver_, 
            "Code", "Source");
      sourceMenuEntry.click();
      
      // Wait for the console to contain the string "source"
      ConsoleTestUtils.waitForConsoleContainsText(driver_, "source(");
      
      closeUnsavedRFile();
   }
   
   @Test
   public void findAndReplace() {
      createRFile();
      
      // Type some code into the file
      String preReplaceCode = "foo <- 'bar'";
      Actions a = new Actions(driver_);
      a.sendKeys(preReplaceCode + Keys.ENTER);
      a.perform();
      
      // Find the ACE editor instance that the code appears in. (CONSIDER: 
      // This is not the best way to find the code editor instance.)
      WebElement editor = null;
      List<WebElement> editors = driver_.findElements(
            By.className("ace_content"));
      for (WebElement e: editors) {
         if (e.getText().contains(preReplaceCode)) {
            editor = e;
            break;
         }
      }
      Assert.assertNotNull(editor);
      
      // Invoke find and replace
      WebElement findMenuEntry = MenuNavigator.getMenuItem(driver_, 
            "Edit", "Find...");
      findMenuEntry.click();
      
      // Wait for the find and replace panel to come up
      (new WebDriverWait(driver_, 2))
        .until(ExpectedConditions.presenceOfElementLocated(
              By.id(ElementIds.getElementId(ElementIds.FIND_REPLACE_BAR))));
      
      // Type the text and the text to be replaced (replace 'bar' with 'foo')
      Actions rep = new Actions(driver_);
      rep.sendKeys("bar" + Keys.TAB + "foo" + Keys.ENTER);
      rep.perform();
      
      DialogTestUtils.respondToModalDialog(driver_, "OK");
      
      Actions dismiss = new Actions(driver_);
      dismiss.sendKeys(Keys.ESCAPE);
      dismiss.perform();
      
      // Ensure that the source has been updated
      Assert.assertTrue(editor.getText().contains("foo <- 'foo'"));
      
      closeUnsavedRFile();
   }
   
   private void createRFile() {
      WebElement newRScriptMenuEntry = MenuNavigator.getMenuItem(driver_,
            "File", "New File", "R Script");
      newRScriptMenuEntry.click();
      
      // Wait for the "Untitled" buffer to appear
      (new WebDriverWait(driver_, 5)).until(new ExpectedCondition<Boolean>() {
         public Boolean apply(WebDriver d) {
            List<WebElement>elements = driver_.findElements(By.className(
                   "gwt-TabLayoutPanelTab-selected"));
            for (WebElement e: elements) {
               if (e.getText().startsWith("Untitled")) {
                  return true;
               }
            }
            return false;
         }
      });
   }
   
   private void closeUnsavedRFile() {
      WebElement closeEntry = MenuNavigator.getMenuItem(driver_,
            "File", "Close");
      closeEntry.click();
      DialogTestUtils.respondToModalDialog(driver_, "Don't Save");
   }
   
   private static WebDriver driver_;
}
