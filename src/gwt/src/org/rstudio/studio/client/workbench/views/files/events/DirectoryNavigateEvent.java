/*
 * DirectoryNavigateEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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
import com.google.gwt.event.shared.GwtEvent;

import org.rstudio.core.client.files.FileSystemItem;

public class DirectoryNavigateEvent extends GwtEvent<DirectoryNavigateEvent.Handler>
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
      directory_ = directory;
      activate_ = activate;
   }

   public FileSystemItem getDirectory()
   {
      return directory_;
   }

   public boolean getActivate()
   {
      return activate_;
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

   private final FileSystemItem directory_;
   private final boolean activate_;
}
