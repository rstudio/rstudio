/*
 * UserPrefsChangedEvent.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.prefs.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.core.client.JavaScriptObject;

@JavaScriptSerializable
public class UserPrefsChangedEvent extends CrossWindowEvent<UserPrefsChangedHandler>
{
   public static final String GLOBAL_TYPE = "global";
   public static final String PROJECT_TYPE = "project";
   
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }
      
      public final native static Data create(String type, JsObject prefs) /*-{
         return {
            "type" : type,
            "prefs": prefs
         };
      }-*/;
      
      public final native String getType() /*-{
         return this.type;
      }-*/;
      
      public final native JsObject getPrefs() /*-{
         return this.prefs;
      }-*/;
   }
   
   public static final Type<UserPrefsChangedHandler> TYPE = new Type<UserPrefsChangedHandler>();
   
   public UserPrefsChangedEvent()
   {
   }

   public UserPrefsChangedEvent(Data data)
   {
      data_ = data;
   }

   public String getType()
   {
      return data_.getType();
   }
   
   public JsObject getUIPrefs()
   {
      return data_.getPrefs();
   }

   @Override
   public Type<UserPrefsChangedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(UserPrefsChangedHandler handler)
   {
      handler.onUiPrefsChanged(this);
   }

   private Data data_;
}
