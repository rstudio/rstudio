/*
 * MenuNavigator.java
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
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.Assert.*;

public class MenuNavigator
{
   public static WebElement findMenuItemByName(WebElement menuElement, 
                                               String itemName) {
      List<WebElement> menuItems = menuElement.findElements(
            By.className("gwt-MenuItem"));
      WebElement foundMenu = null;
      for (WebElement menuItem: menuItems) {
         if (menuItem.getText().startsWith(itemName)) {
            foundMenu = menuItem;
            break;
         }
      }
      assertNotNull(foundMenu);
      return foundMenu;
   }
   
   
   public static WebElement getMenuItem(WebDriver driver, String level1, 
                                        String level2) {
      WebElement menuBar = driver.findElement(By.className("gwt-MenuBar"));
      WebElement menu1 = findMenuItemByName(menuBar, level1);
      menu1.click();
      
      WebElement menu1Popup = (new WebDriverWait(driver, 1))
            .until(ExpectedConditions.presenceOfElementLocated(
                  By.className("gwt-MenuBarPopup")));

      return findMenuItemByName(menu1Popup, level2);
   }
   
   public static WebElement getMenuItem(final WebDriver driver, String level1, 
                                        String level2, String level3)
   {
      WebElement popupItem = getMenuItem(driver, level1, level2);
      Actions action = new Actions(driver);
      action.moveToElement(popupItem).build().perform();
      
      // Wait for there to be two popups open (the level1 and level2 menus)
      (new WebDriverWait(driver, 1)).until(new ExpectedCondition<Boolean>() {
         public Boolean apply(WebDriver d) {
            List<WebElement>elements = driver.findElements(
                  By.className("gwt-MenuBarPopup"));
            return elements.size() > 1;
         }
      });
      
      // Get the second popup menu
      List<WebElement>elements = driver.findElements(
            By.className("gwt-MenuBarPopup"));
      WebElement menu2popup = elements.get(1);
      
      return findMenuItemByName(menu2popup, level3);
   }
}
