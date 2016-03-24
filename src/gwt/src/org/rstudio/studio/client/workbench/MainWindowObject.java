/*
 * MainWindowObject.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Window;

/**
 * This class can be used to get / set fields on the main window from satellite
 * windows. You should only use this when passing along POD JavaScript objects;
 * e.g. don't use this with GWT objects.
 */
public class MainWindowObject
{
   public static final <T> void set(String key, T value)
   {
      setImpl(key, value, RSTUDIO);
   }
   
   public static final <T> T get(String key)
   {
      return getImpl(key, RSTUDIO);
   }
   
   // Private ----
   
   private static final native <T> void setImpl(String key,
                                                T value,
                                                JavaScriptObject object)
   /*-{
      object[key] = value;
   }-*/;
   
   private static final native <T> T getImpl(String key,
                                             JavaScriptObject object)
   /*-{
      return object[key];
   }-*/;
   
   private static final native Window getMainWindow() /*-{
      var wnd = $wnd;
      while (wnd.opener != null)
         wnd = wnd.opener;
      return wnd;
   }-*/;
   
   private static final native JavaScriptObject getRStudioObject(Window wnd) /*-{
      if (wnd.$RStudio == null)
         wnd.$RStudio = {};
      return wnd.$RStudio;
   }-*/;
   
   private static final Window MAIN_WINDOW = getMainWindow();
   private static final JavaScriptObject RSTUDIO = getRStudioObject(MAIN_WINDOW);
   
   public static final String LAST_FOCUSED_EDITOR        = "last_focused_editor";
   public static final String LAST_FOCUSED_SOURCE_WINDOW = "last_focused_source_window";
}
