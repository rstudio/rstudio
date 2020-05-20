/*
 * WebWindowOpener.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.common.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.user.client.Window;

import org.rstudio.core.client.Point;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.WindowOpener;
import org.rstudio.studio.client.common.satellite.SatelliteUtils;

public class WebWindowOpener implements WindowOpener
{
   public void openWindow(final GlobalDisplay globalDisplay,
                          final String url,
                          final NewWindowOptions options)
   {
      openWindowInternal(globalDisplay, url, options, "", -1, -1);
   }

   public void openMinimalWindow(GlobalDisplay globalDisplay,
                                 String url,
                                 NewWindowOptions options,
                                 int width,
                                 int height,
                                 boolean showLocation)
   {
      webOpenMinimalWindow(globalDisplay, 
                           url, 
                           options, 
                           width, 
                           height, 
                           showLocation);
   }
   
   public void openWebMinimalWindow(GlobalDisplay globalDisplay,
                                    String url,
                                    NewWindowOptions options,
                                    int width,
                                    int height,
                                    boolean showLocation)
   {
      webOpenMinimalWindow(globalDisplay, 
                           url, 
                           options, 
                           width, 
                           height, 
                           showLocation);
   }
   
   public void openSatelliteWindow(GlobalDisplay globalDisplay,
                                   String viewName,
                                   int width,
                                   int height, 
                                   NewWindowOptions options)
   {
      // build url
      UrlBuilder urlBuilder = Window.Location.createUrlBuilder();
      urlBuilder.setParameter("view", viewName);
      
      // setup options
      if (options == null)
         options = new NewWindowOptions();
      options.setName(SatelliteUtils.getSatelliteWindowName(viewName));
      options.setFocus(true);
      
      // open window (force web codepath b/c desktop needs this so
      // that window.opener is hooked up)
      webOpenMinimalWindow(globalDisplay,
                           urlBuilder.buildString(),
                           options,
                           width,
                           height,
                           false);
   }
   
   protected boolean showPopupBlockedMessage()
   {
      return true;
   }
   
   protected boolean hasProtocol(String url)
   {
      return Pattern.create("^([a-zA-Z]+:)").match(url, 0) != null;
   }
   
   protected boolean isAppUrl(String url)
   {
      return url.startsWith(GWT.getHostPageBaseURL());
   }
   
   // enable callers to prevent subclass implementations from taking
   // the open window by calling this directly
   private void webOpenMinimalWindow(GlobalDisplay globalDisplay,
                                     String url,
                                     NewWindowOptions options,
                                     int width,
                                     int height,
                                     boolean showLocation)
   {
      String loc = showLocation ? "1" : "0";
      String features = "width=" + width + "," +
                        "height=" + height + "," +
                        "menubar=0,toolbar=0,location=" + loc + "," +
                        "status=0,scrollbars=1,resizable=1,directories=0";

      // open window at specific position if specified
      Point pos = options.getPosition();
      if (pos != null)
      {
         features += ",left=" + pos.x + ",top=" + pos.y;
      }
         
      openWindowInternal(globalDisplay, url, options, features, width, height);
   }

   private void openWindowInternal(GlobalDisplay globalDisplay,
                                   final String url,
                                   NewWindowOptions options,
                                   final String features,
                                   final int width,
                                   final int height)
   {
      String name = options.getName();
      final boolean focus = options.isFocus();
      final OperationWithInput<WindowEx> openOperation = options.getCallback();

      if (name == null)
         name = "_blank";

      if (options.appendClientId()
          && !name.equals("_blank")
          && !name.equals("_top")
          && !name.equals("_parent")
          && !name.equals("_self"))
      {
         name += "_" + CLIENT_ID;
      }

      // Need to make the URL absolute because IE resolves relative URLs
      // against the JavaScript file location, not the window.location like
      // the other browsers do
      final String absUrl = Pattern.create("^/|([a-zA-Z]+:)").match(url, 0) == null
                            ? GWT.getHostPageBaseURL() + url
                            : url;

      final String finalName = name;
      WindowEx window = doOpenWindow(absUrl, finalName, features, focus);
      if (window == null)
      {
         if (showPopupBlockedMessage())
         {
            globalDisplay.showPopupBlockedMessage(new Operation()
            {
               public void execute()
               {
                  WindowEx window = doOpenWindow(absUrl,
                                                 finalName,
                                                 features,
                                                 focus);
                  if (window != null)
                  {
                     if (openOperation != null)
                        openOperation.execute(window);
                  }
               }
            });
         }
      }
      else
      {
         if (openOperation != null)
            openOperation.execute(window);
      }
   }

   private native WindowEx doOpenWindow(String url,
                                              String name,
                                              String features,
                                              boolean focus)/*-{
      var window = $wnd.open(url, name, features);
      if (!window)
      {
         // popup was blocked
         return null;
      }

      if (focus)
      {
        try {
           window.focus();
        } catch(e) {}
      }

      return window;
   }-*/;

   public static final String CLIENT_ID = (int)(Math.random() * 10000) + "";

}
