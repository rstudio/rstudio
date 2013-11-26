/*
 * ConsoleTestUtils.java
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
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.rstudio.core.client.ElementIds;

public class ConsoleTestUtils
{
   public static void waitForConsoleContainsText(WebDriver driver, 
                                                 final String text) {
      final WebElement output = driver.findElement(By.id(
            ElementIds.getElementId(ElementIds.CONSOLE_OUTPUT)));

      (new WebDriverWait(driver, 5)).until(new ExpectedCondition<Boolean>() {
         public Boolean apply(WebDriver d) {
            String outputText = output.getText();
            return outputText.contains(text);
         }
      });
   }
}
