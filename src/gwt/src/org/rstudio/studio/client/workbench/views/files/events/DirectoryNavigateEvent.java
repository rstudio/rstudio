/*
 * DirectoryNavigateEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

// Cross-window so a satellite (e.g. a popped-out source window) can navigate
// the main window's Files pane. Server-raised instances are dispatched
// locally in each window and never forwarded, so they don't navigate twice.
@JavaScriptSerializable
public class DirectoryNavigateEvent extends CrossWindowEvent<DirectoryNavigateEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getDirectory() /*-{
         return this.directory;
      }-*/;

      public final native boolean getActivate() /*-{
         return this.activate;
      }-*/;
   }

   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onDirectoryNavigate(DirectoryNavigateEvent event);
   }

   public DirectoryNavigateEvent()
   {
   }

   public DirectoryNavigateEvent(Data data)
   {
      this(FileSystemItem.createDir(data.getDirectory()), data.getActivate());
   }

   public DirectoryNavigateEvent(FileSystemItem directory)
   {
      this(directory, false);
   }

   public DirectoryNavigateEvent(FileSystemItem directory,
                                 boolean activate)
   {
      directory_ = directory.getPath();
      activate_ = activate;
   }

   public FileSystemItem getDirectory()
   {
      return FileSystemItem.createDir(directory_);
   }

   public boolean getActivate()
   {
      return activate_;
   }

   // Raise the main window only when asked to show the pane; a background
   // navigation shouldn't steal focus from the satellite.
   @Override
   public int focusMode()
   {
      return activate_ ? MODE_FOCUS : MODE_BACKGROUND;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onDirectoryNavigate(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private String directory_;
   private boolean activate_;
}
