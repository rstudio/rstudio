/*
 * ShowWarningBarEvent.java
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
package org.rstudio.studio.client.workbench.events;

import com.google.gwt.event.shared.EventHandler;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

import com.google.gwt.event.shared.GwtEvent;


public class ShowWarningBarEvent extends GwtEvent<ShowWarningBarEvent.Handler>
{
   public ShowWarningBarEvent(boolean severe, String message)
   {
      data_ = new Data();
      data_.severe = severe;
      data_.message = message;
   }

   public ShowWarningBarEvent(ShowWarningBarEvent.Data message)
   {
      data_ = message;
   }

   public interface Handler extends EventHandler
   {
      void onShowWarningBar(ShowWarningBarEvent event);
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class Data
   {
      public boolean severe;
      public String message;
   }

   public boolean isSevere()
   {
      return data_.severe;
   }

   public String getMessage()
   {
      return data_.message;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onShowWarningBar(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   public static final GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

   private final Data data_;
}
