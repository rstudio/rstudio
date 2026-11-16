/*
 * DesktopUpdateDownloadProgressEvent.java
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
 * Fired by the desktop frame as a background download of an RStudio update
 * gets further along: once for each new whole percentage.
 */
public class DesktopUpdateDownloadProgressEvent extends GwtEvent<DesktopUpdateDownloadProgressEvent.Handler>
{
   public DesktopUpdateDownloadProgressEvent(String version, int percent)
   {
      version_ = version;
      percent_ = percent;
   }

   public interface Handler extends EventHandler
   {
      void onDesktopUpdateDownloadProgress(DesktopUpdateDownloadProgressEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onDesktopUpdateDownloadProgress(this);
   }

   public String getVersion()
   {
      return version_;
   }

   /** How much of the update has been downloaded, from 0 to 100. */
   public int getPercent()
   {
      return percent_;
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final String version_;
   private final int percent_;
}
