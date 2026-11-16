/*
 * DesktopUpdateDownloadedEvent.java
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
package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Fired by the desktop frame when a background download of an RStudio update
 * finishes, successfully or not.
 */
public class DesktopUpdateDownloadedEvent extends GwtEvent<DesktopUpdateDownloadedEvent.Handler>
{
   public static DesktopUpdateDownloadedEvent downloaded(String version)
   {
      return new DesktopUpdateDownloadedEvent(version, null);
   }

   public static DesktopUpdateDownloadedEvent failed(String errorMessage)
   {
      return new DesktopUpdateDownloadedEvent(null, errorMessage);
   }

   private DesktopUpdateDownloadedEvent(String version, String errorMessage)
   {
      version_ = version;
      errorMessage_ = errorMessage;
   }

   public interface Handler extends EventHandler
   {
      void onDesktopUpdateDownloaded(DesktopUpdateDownloadedEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onDesktopUpdateDownloaded(this);
   }

   public boolean succeeded()
   {
      return errorMessage_ == null;
   }

   public String getVersion()
   {
      return version_;
   }

   public String getErrorMessage()
   {
      return errorMessage_;
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final String version_;
   private final String errorMessage_;
}
