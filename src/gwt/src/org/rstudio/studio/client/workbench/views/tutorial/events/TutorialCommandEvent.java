/*
 * TutorialCommandEvent.java
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
package org.rstudio.studio.client.workbench.views.tutorial.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

@JavaScriptSerializable
public class TutorialCommandEvent extends CrossWindowEvent<TutorialCommandEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data() {}
      public final native String getType()           /*-{ return this["type"]; }-*/;
      public final native JavaScriptObject getData() /*-{ return this["data"]; }-*/;
   }

   public TutorialCommandEvent()
   {
      type_ = "";
      data_ = JavaScriptObject.createObject();
   }

   public TutorialCommandEvent(Data data)
   {
      type_ = data.getType();
      data_ = data.getData();
   }

   public TutorialCommandEvent(String type, JavaScriptObject data)
   {
      type_ = type;
      data_ = data;
   }

   public final String getType()
   {
      return type_;
   }

   @SuppressWarnings("unchecked")
   public final <T extends JavaScriptObject> T getData()
   {
      T casted = (T) data_;
      return casted;
   }

   private final String type_;
   private final JavaScriptObject data_;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onTutorialCommand(TutorialCommandEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTutorialCommand(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   public static final String TYPE_STARTED = "started";
   public static final String TYPE_STOP = "stop";
   public static final String TYPE_NAVIGATE = "navigate";
   public static final String TYPE_INDEXING_COMPLETED = "indexing_completed";
   public static final String TYPE_LAUNCH_DEFAULT_TUTORIAL = "launch_default_tutorial";

}
