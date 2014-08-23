/*
 * RStudioWebAppDriver.java
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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public class RStudioWebAppDriver
{
   public static WebDriver start() throws Exception {
      driver_ = new RemoteWebDriver(
            new URL("http://localhost:9515/"), DesiredCapabilities.chrome());
      
      driver_.get("http://localhost:4011/");
      return driver_;
   }
   
   public static void stop() {
      driver_.quit();
   }
   
   private static WebDriver driver_;
}
