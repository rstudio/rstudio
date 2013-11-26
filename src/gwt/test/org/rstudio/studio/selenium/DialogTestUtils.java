/*
 * DialogTestUtils.java
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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class DialogTestUtils
{
   public static void respondToModalDialog(final WebDriver driver, 
                                           String response) {
      // Wait for the dialog to appear
      WebElement dialog = (new WebDriverWait(driver, 2))
        .until(ExpectedConditions.presenceOfElementLocated(
              By.className("gwt-DialogBox-ModalDialog")));
      
      // Find the button requested and invoke it
      List<WebElement> buttons = 
            dialog.findElements(By.className("gwt-Button-DialogAction"));
      for (WebElement button: buttons) {
         if (button.getText().equals(response)) {
            button.click();
            break;
         }
      }
      
      // Wait for the dialog to disappear
      (new WebDriverWait(driver, 5)).until(new ExpectedCondition<Boolean>() {
         public Boolean apply(WebDriver d) {
            List<WebElement>elements = driver.findElements(By.className(
                  "gwt-DialogBox-ModalDialog"));
            return elements.size() == 0;
         }
      });
   }
}
