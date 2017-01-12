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

import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Window;

/**
 * This class can be used to get / set fields on the main window from satellite
 * windows. You should only use this when passing along POD JavaScript objects
 * or 'primitive' Java types that GWT can easily handle, e.g. String, Integer.
 */
public class MainWindowObject<T>
{
   private interface DefaultProvider<V>
   {
      V defaultValue();
   }
   
   private MainWindowObject(String key, DefaultProvider<T> provider)
   {
      key_ = key;
      provider_ = provider;
   }
   
   public final void set(T value)
   {
      JavaScriptObject rstudioObject = getRStudioObject(getRStudioMainWindow());
      setImpl(key_, value, rstudioObject);
   }
   
   @SuppressWarnings("unchecked")
   public final T get()
   {
      JavaScriptObject rstudioObject = getRStudioObject(getRStudioMainWindow());
      if (!hasImpl(key_, rstudioObject))
         return provider_.defaultValue();
      
      // Work around JDK 1.6 compiler issues with casting from JSO to T
      JavaScriptObject object = getImpl(key_, rstudioObject);
      T casted = (T) object;
      return casted;
   }
   
   // Private Methods ----
   
   private static final native <T> void setImpl(String key,
                                                T value,
                                                JavaScriptObject object)
   /*-{
      object[key] = value;
   }-*/;
   
   private static final native JavaScriptObject getImpl(String key,
                                                        JavaScriptObject object)
   /*-{
      return object[key];
   }-*/;
   
   private static final native boolean hasImpl(String key, JavaScriptObject object)
   /*-{
      return object.hasOwnProperty(key);
   }-*/;
   
   private static final native Window getRStudioMainWindow() /*-{
      for (var wnd = $wnd; wnd != null; wnd = wnd.opener)
         if (!!wnd.$RStudio)
            return wnd;
      return $wnd;
   }-*/;
   
   private static final native JavaScriptObject getRStudioObject(Window wnd) /*-{
      return wnd.$RStudio;
   }-*/;
   
   private final String key_;
   private final DefaultProvider<T> provider_;
   
   // Helper Classes ----
   
   public static final MainWindowObject<String> lastFocusedEditor()
   {
      return new MainWindowObject<String>(LAST_FOCUSED_EDITOR, new DefaultProvider<String>()
      {
         @Override
         public String defaultValue()
         {
            return "";
         }
      });
   }
   
   public static final MainWindowObject<String> lastFocusedWindow()
   {
      return new MainWindowObject<String>(LAST_FOCUSED_WINDOW, new DefaultProvider<String>()
      {
         @Override
         public String defaultValue()
         {
            return "";
         }
      });
   }
   
   public static final MainWindowObject<String> lastFocusedSourceWindow()
   {
      return new MainWindowObject<String>(LAST_FOCUSED_SOURCE_WINDOW, new DefaultProvider<String>()
      {
         @Override
         public String defaultValue()
         {
            return "";
         }
      });
   }
   
   public static final MainWindowObject<RAddins> rAddins()
   {
      return new MainWindowObject<RAddins>(R_ADDINS, new DefaultProvider<RAddins>()
      {
         @Override
         public RAddins defaultValue()
         {
            return RAddins.createDefault();
         }
      });
   }
   
   private static final String LAST_FOCUSED_EDITOR        = "last_focused_editor";
   private static final String LAST_FOCUSED_WINDOW        = "last_focused_window";
   private static final String LAST_FOCUSED_SOURCE_WINDOW = "last_focused_source_window";
   private static final String R_ADDINS                   = "r_addins";
}
