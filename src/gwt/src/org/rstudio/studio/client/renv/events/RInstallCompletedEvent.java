/*
 * RInstallCompletedEvent.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.renv.events;

import org.rstudio.studio.client.renv.model.RVersionInstall;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Fired when a background installation of R (via rig) finishes.
 */
public class RInstallCompletedEvent extends GwtEvent<RInstallCompletedEvent.Handler>
{
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class Data
   {
      // the version that was requested
      public String version;

      public boolean success;
      public String error;

      // the installed R, when the installation succeeded
      public RVersionInstall installed;
   }

   public interface Handler extends EventHandler
   {
      void onRInstallCompleted(RInstallCompletedEvent event);
   }

   public RInstallCompletedEvent(Data data)
   {
      data_ = data;
   }

   public Data getData()
   {
      return data_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRInstallCompleted(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<>();
}
