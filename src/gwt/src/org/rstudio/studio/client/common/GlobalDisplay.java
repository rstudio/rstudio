/*
 * GlobalDisplay.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common;

import com.google.gwt.user.client.Command;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.*;

public abstract class GlobalDisplay extends MessageDisplay
{
   public static class NewWindowOptions
   {
      public NewWindowOptions()
      {
      }

      public String getName()
      {
         return name;
      }

      public void setName(String name)
      {
         this.name = name;
      }

      public boolean isFocus()
      {
         return focus;
      }

      public void setFocus(boolean focus)
      {
         this.focus = focus;
      }

      public OperationWithInput<WindowEx> getCallback()
      {
         return callback;
      }

      public void setCallback(OperationWithInput<WindowEx> callback)
      {
         this.callback = callback;
      }

      public boolean alwaysUseBrowser()
      {
         return alwaysUseBrowser_;
      }

      public void setAlwaysUseBrowser(boolean alwaysUseBrowser)
      {
         alwaysUseBrowser_ = alwaysUseBrowser;
      }

      private String name = "_blank";
      private boolean focus = true;
      private boolean alwaysUseBrowser_ = false;
      private OperationWithInput<WindowEx> callback;
   }
   
   public abstract void openWindow(String url);
   public abstract void openWindow(String url, NewWindowOptions options);

   public abstract void openProgressWindow(String name,
                                    String message,
                                    OperationWithInput<WindowEx> openOperation);
   
   public abstract void openMinimalWindow(String url, int width, int height);

   public abstract void openMinimalWindow(String url,
                                   boolean showLocation,
                                   int width,
                                   int height);

   public abstract void openMinimalWindow(String url,
                                   boolean showLocation,
                                   int width,
                                   int height,
                                   NewWindowOptions options);
   
   public abstract void openSatelliteWindow(String name, int width, int height);

   public abstract void openEmailComposeWindow(String to, String subject);
   
   public void openRStudioLink(String linkName)
   {
      openRStudioLink(linkName, true);
   }
   
   public abstract void openRStudioLink(String linkName, 
                                        boolean includeVersionInfo);

   /**
    * Shows a non-modal progress message. Execute the returned command
    * to dismiss.
    */
   public abstract Command showProgress(String message);

   public abstract void showWarningBar(boolean severe, String message);
   public abstract void hideWarningBar();

   public abstract ProgressIndicator getProgressIndicator(String errorCaption);
}
