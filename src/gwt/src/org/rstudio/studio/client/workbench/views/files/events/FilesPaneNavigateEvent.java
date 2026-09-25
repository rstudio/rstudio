/*
 * FilesPaneNavigateEvent.java
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
package org.rstudio.studio.client.workbench.views.files.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;

// Navigates the Files pane from any window. DirectoryNavigateEvent can't serve
// here: it is window-local, and making it cross-window would double-navigate,
// since server events that raise it are dispatched in every satellite too.
@JavaScriptSerializable
public class FilesPaneNavigateEvent extends CrossWindowEvent<FilesPaneNavigateEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onFilesPaneNavigate(FilesPaneNavigateEvent event);
   }

   public FilesPaneNavigateEvent(String directory, boolean activate)
   {
      directory_ = directory;
      activate_ = activate;
   }

   public FilesPaneNavigateEvent()
   {
   }

   public String getDirectory()
   {
      return directory_;
   }

   public boolean getActivate()
   {
      return activate_;
   }

   // Raise the main window only when asked to show the pane; otherwise a
   // background navigation would steal focus from the satellite.
   @Override
   public int focusMode()
   {
      return activate_ ? MODE_FOCUS : MODE_BACKGROUND;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onFilesPaneNavigate(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   public static final Type<Handler> TYPE = new Type<>();

   private String directory_;
   private boolean activate_;
}
