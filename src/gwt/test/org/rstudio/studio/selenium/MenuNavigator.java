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
import org.openqa.selenium.WebElement;

import static org.junit.Assert.*;

public class MenuNavigator
{
   public static WebElement findMenuItemByName(WebElement menuElement, 
                                               String itemName)
   {
      List<WebElement> menuItems = menuElement.findElements(
            By.className("gwt-MenuItem"));
      WebElement foundMenu = null;
      for (WebElement menuItem: menuItems) {
         if (menuItem.getText().equals(itemName)) {
            foundMenu = menuItem;
         }
      }
      assertNotNull(foundMenu);
      return foundMenu;
   }
}
