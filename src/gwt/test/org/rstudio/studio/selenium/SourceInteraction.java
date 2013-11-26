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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.Assert.*;

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
   public void createNewRSourceFile() {
      WebElement menuBar = driver_.findElement(By.className("gwt-MenuBar"));
      WebElement fileMenu = MenuNavigator.findMenuItemByName(menuBar, "File");
      fileMenu.click();
      
      WebElement fileMenuPopup = (new WebDriverWait(driver_, 1))
            .until(ExpectedConditions.presenceOfElementLocated(
                  By.className("gwt-MenuBarPopup")));

      WebElement newFilePopup = MenuNavigator.findMenuItemByName(fileMenuPopup, 
                                                                 "New File");
      newFilePopup.click();
   }
   
   private static WebDriver driver_;
}
