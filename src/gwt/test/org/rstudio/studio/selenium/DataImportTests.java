/*
 * DataImportTests.java
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

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class DataImportTests
{
   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      driver_ = RStudioWebAppDriver.start();

      (new WebDriverWait(driver_, 10))
        .until(ExpectedConditions.presenceOfElementLocated(
              By.className("gwt-MenuBar")));
   }
   
   @AfterClass
   public static void tearDownAfterClass() throws Exception {
      RStudioWebAppDriver.stop();
   }
   
   @Test
   public void testImportCSVFile() throws Exception {
      WebElement menuEntry = MenuNavigator.getMenuItem(driver_, 
            "Tools", "Import Dataset", "From Text File...");
      
      menuEntry.click();
      final WebElement importFileDialog = 
            DialogTestUtils.waitForModalToAppear(driver_);
     
      // Unfortunately even after the dialog is loaded, the textbox into which
      // we need to type the filename may not yet be focused. Wait for it to be
      // present and focused before continuing.
      (new WebDriverWait(driver_, 5)).until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
             List<WebElement>elements = importFileDialog.findElements(By.tagName(
                   "input"));
             if (elements.size() > 0 &&
                 driver_.switchTo().activeElement().equals(elements.get(0))) {
                return true;
             }
             return false;
          }
      });
 
      Actions typeName = new Actions(driver_);
      File csvFile = new File("test/org/rstudio/studio/selenium/resources/banklist.csv");
      typeName.sendKeys(csvFile.getAbsolutePath());
      typeName.perform();
      DialogTestUtils.respondToModalDialog(driver_, "Open");
      
      // After a moment the modal prompting for the path will disappear, and
      // the modal prompting for input will appear. 
      (new WebDriverWait(driver_, 5)).until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
             List<WebElement>elements = driver_.findElements(By.className(
                   "gwt-DialogBox-ModalDialog"));
             if (elements.size() > 0) {
                if (elements.get(0).getText().contains("Import Dataset")) {
                   return true;
                }
             }
             return false;
          }
      });
 
      
      DialogTestUtils.respondToModalDialog(driver_, "Import");
      ConsoleTestUtils.waitForConsoleContainsText(driver_, "read.csv");
   }
   
   private static WebDriver driver_;
}
