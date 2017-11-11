/*
 * ApplicationTutorialEvent.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

package org.rstudio.studio.client.application;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.ApplicationTutorialEvent.Handler;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import jsinterop.annotations.JsType;
import jsinterop.annotations.JsPackage;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class ApplicationTutorialEvent extends CrossWindowEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      void onApplicationTutorialEvent(ApplicationTutorialEvent event);
   }

   // Supported values for Data.message
   
   // API request failed
   // {"message": "error": "api": "<APINAME>", "result": "<API-SPECIFIC>"}
   public static final String API_ERROR = "error";
   
   // API request succeeded
   // {"message": "success": "api": "<APINAME>"}
   public static final String API_SUCCESS = "success";

   // Some type of file save operation was initiated. Doesn't guarantee it was successful.
   // {"message": "fileSave"}
   public static final String FILE_SAVE = "fileSave";
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class Data
   {
      public String message;
      public String api;
      public String result;
   }

   public ApplicationTutorialEvent()
   {
      data_ = new Data();
   }

   public ApplicationTutorialEvent(Data data)
   {
      data_ = data;
   }

   public ApplicationTutorialEvent(String message)
   {
      data_ = new Data();
      data_.message = message;
   }

   public ApplicationTutorialEvent(String message, String api)
   {
      data_ = new Data();
      data_.message = message;
      data_.api = api;
   }

   public ApplicationTutorialEvent(String message, String api, String result)
   {
      data_ = new Data();
      data_.message = message;
      data_.api = api;
      data_.result = result;
   }

   @Override
   public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onApplicationTutorialEvent(this);
   }
   
   public Data getData()
   {
      return data_;
   }
  
   private final Data data_;

   public static final Type<Handler> TYPE = new Type<Handler>(); 
}
