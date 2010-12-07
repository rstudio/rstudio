/*
 * WebWindowOpener.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.impl;

import com.google.gwt.user.client.Window;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.common.WindowOpener;

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
      String loc = showLocation ? "1" : "0";
      String features = "width=" + width + "," +
                        "height=" + height + "," +
                        "menubar=0,toolbar=0,location=" + loc + "," +
                        "status=0,scrollbars=1,resizable=1,directories=0";

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

      if (!name.equals("_blank")
          && !name.equals("_top")
          && !name.equals("_parent")
          && !name.equals("_self"))
      {
         name += "_" + clientId;
      }

      final String finalName = name;
      WindowEx window = doOpenWindow(url, finalName, features, focus);
      if (window == null)
      {
         globalDisplay.showYesNoMessage(
               GlobalDisplay.MSG_POPUP_BLOCKED,
               "Popup Blocked",
               "We attempted to open an external browser window, but " +
               "the action was prevented by your popup blocker. You " +
               "can attempt to open the window again by pressing the " +
               "\"Try Again\" button below.\n\n" +
               "NOTE: To prevent seeing this message in the future, you " +
               "should configure your browser to allow popup windows " +
               "for " + Window.Location.getHostName() + ".",
               false,
               new Operation()
               {
                  public void execute()
                  {
                     WindowEx window = doOpenWindow(url,
                                                    finalName,
                                                    features,
                                                    focus);
                     if (window != null)
                     {
                        if (width > 0 && height > 0)
                           window.resizeInnerTo(width, height);
                        if (openOperation != null)
                           openOperation.execute(window);
                     }
                  }
               },
               new Operation()
               {
                  public void execute()
                  {

                  }
               },
               null,
               "Try Again",
               "Cancel",
               true);
      }
      else
      {
         if (width > 0 && height > 0)
            window.resizeInnerTo(width, height);
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

   private static final String clientId = (int)(Math.random() * 10000) + "";

}
