/*
 * WorkbenchTests.java
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WorkbenchTests
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
   public void testWorkbenchPersistance() throws Exception {
      // Clear out the workspace and make sure it's clear
      WebElement menuItem = MenuNavigator.getMenuItem(driver_, 
            "Session", "Clear Workspace...");
      menuItem.click();
      DialogTestUtils.respondToModalDialog(driver_, "Yes");
      
      ConsoleTestUtils.beginConsoleInteraction(driver_);
      (new Actions(driver_))
         .sendKeys(Keys.ESCAPE + "ls()" + Keys.ENTER)
         .perform();
      
      ConsoleTestUtils.waitForConsoleContainsText(driver_, "character(0)");
      
      // Add a variable to the workspace
      (new Actions(driver_))
         .sendKeys(Keys.ESCAPE + "selenium <- function() { 42 }" + Keys.ENTER)
         .perform();

      // Save the workspace 
      WebElement saveItem = MenuNavigator.getMenuItem(driver_, 
            "Session", "Save Workspace As...");
      saveItem.click();
      
      WebElement saveDialog = DialogTestUtils.waitForModalToAppear(driver_);
      DialogTestUtils.waitForFocusedInput(driver_, saveDialog);
      
      File tempFile = File.createTempFile("rstudio-selenium-workspace-", ".RData");
      String workspaceFilePath = tempFile.getAbsolutePath();
      tempFile.delete();
      
      (new Actions(driver_))
         .sendKeys(workspaceFilePath + Keys.ENTER)
         .perform();
      
      // TODO: Clear workspace again, load saved workspace, and verify that
      // variable is present again
   }
   
   private static WebDriver driver_;
}