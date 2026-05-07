/*
 * OpenFileInBrowserEvent.java
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
package org.rstudio.studio.client.common.filetypes.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.core.client.files.FileSystemItem;

public class OpenFileInBrowserEvent extends GwtEvent<OpenFileInBrowserEvent.Handler>
{
   public static Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onOpenFileInBrowser(OpenFileInBrowserEvent file);
   }

   public OpenFileInBrowserEvent(FileSystemItem file)
   {
      this(file, false);
   }

   public OpenFileInBrowserEvent(FileSystemItem file, boolean isDownload)
   {
      file_ = file;
      isDownload_ = isDownload;
   }

   public FileSystemItem getFile()
   {
      return file_;
   }

   // true when the user explicitly initiated a download (e.g. clicked a
   // binary file in the Files pane); false for view-style navigations
   // (e.g. clicking a link in source code, or HTML "Show in Browser")
   public boolean isDownload()
   {
      return isDownload_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onOpenFileInBrowser(this);
   }

   private final FileSystemItem file_;
   private final boolean isDownload_;
}
