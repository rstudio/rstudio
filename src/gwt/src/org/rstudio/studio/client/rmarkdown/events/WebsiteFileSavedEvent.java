/*
 * WebsiteFileSavedEvent.java
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
package org.rstudio.studio.client.rmarkdown.events;

import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class WebsiteFileSavedEvent extends GwtEvent<WebsiteFileSavedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onWebsiteFileSaved(WebsiteFileSavedEvent event);
   }

   public WebsiteFileSavedEvent(FileSystemItem fsi)
   {
     fsi_ = fsi;
   }

   public FileSystemItem getFileSystemItem()
   {
      return fsi_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onWebsiteFileSaved(this);
   }

   private FileSystemItem fsi_;

   public static final Type<Handler> TYPE = new Type<>();
}
