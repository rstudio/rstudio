/*
 * RStudioAPIShowDialogEvent.java
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

package org.rstudio.studio.client.common.rstudioapi.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RStudioAPIShowDialogEvent extends GwtEvent<RStudioAPIShowDialogEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {  
      }
      
      public final native String getMessage() /*-{
         return this.message;
      }-*/;

      public final native String getTitle() /*-{
         return this.title;
      }-*/;

      public final native boolean getPrompt() /*-{
         return this.prompt || false;
      }-*/;

      public final native String getPromptDefault() /*-{
         return this["default"];
      }-*/;

      public final native boolean getQuestion() /*-{
         return this.question || false;
      }-*/;

      public final native String getOK() /*-{
         return this.ok;
      }-*/;

      public final native String getCancel() /*-{
         return this.cancel;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onRStudioAPIShowDialogEvent(RStudioAPIShowDialogEvent event);
   }

   public RStudioAPIShowDialogEvent(Data data)
   {
      data_ = data;
   }
   
   public String getMessage()
   {
      return data_.getMessage();
   }

   public String getTitle()
   {
      return data_.getTitle();
   }

   public boolean getPrompt()
   {
      return data_.getPrompt();
   }

   public String getPromptDefault()
   {
      return data_.getPromptDefault();
   }

   public boolean getQuestion()
   {
      return data_.getQuestion();
   }

   public String getOK()
   {
      return data_.getOK();
   }

   public String getCancel()
   {
      return data_.getCancel();
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRStudioAPIShowDialogEvent(this);
   }
   
   private final Data data_;
  
   public static final Type<Handler> TYPE = new Type<Handler>();
}