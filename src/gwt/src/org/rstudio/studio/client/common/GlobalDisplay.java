/*
 * GlobalDisplay.java
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
package org.rstudio.studio.client.common;

import com.google.gwt.user.client.Command;

import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.Point;
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
         return name_;
      }

      public void setName(String name)
      {
         this.name_ = name;
      }

      public boolean isFocus()
      {
         return focus_;
      }

      public void setFocus(boolean focus)
      {
         this.focus_ = focus;
      }

      public OperationWithInput<WindowEx> getCallback()
      {
         return callback_;
      }

      public void setCallback(OperationWithInput<WindowEx> callback)
      {
         this.callback_ = callback;
      }
      
      public void setPosition(Point pos)
      {
         position_ = pos;
      }
      
      public Point getPosition()
      {
         return position_;
      }
      
      // only applicable in desktop mode--by default windows will not load
      // non-local content
      public void setAllowExternalNavigation(boolean allow)
      {
         allowExternalNavigation_ = allow;
      }
      
      public boolean allowExternalNavigation()
      {
         return allowExternalNavigation_;
      }
      
      // only applicable in desktop mode--by default windows showing web content
      // get a basic web navigation toolbar
      public void setShowDesktopToolbar(boolean show)
      {
         showDesktopToolbar_ = show;
      }
      
      public boolean showDesktopToolbar()
      {
         return showDesktopToolbar_;
      }
      
      public void setAppendClientId(boolean append)
      {
         appendClientId_ = append;
      }
      
      public boolean appendClientId()
      {
         return appendClientId_;
      }

      private Point position_ = null;
      private String name_ = "_blank";
      private boolean focus_ = true;
      private OperationWithInput<WindowEx> callback_;
      private boolean allowExternalNavigation_ = false;
      private boolean showDesktopToolbar_ = true;
      private boolean appendClientId_ = false;
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

   public abstract void openWebMinimalWindow(String url,
                                             boolean showLocation,
                                             int width,
                                             int height,
                                             NewWindowOptions options);

   
   public abstract void openSatelliteWindow(String name, int width, int height);

   public abstract void openSatelliteWindow(String name, int width, int height,
                                   NewWindowOptions options);

   public abstract void bringWindowToFront(String name);
   
   public abstract void showHtmlFile(String path);
   
   public abstract void showWordDoc(String path);
   
   public abstract void showPptPresentation(String path);
   
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

   public abstract void showLicenseWarningBar(boolean severe, String message);
   public abstract void showWarningBar(boolean severe, String message);
   public abstract void hideWarningBar();

   public abstract ProgressIndicator getProgressIndicator(String errorCaption);
}
