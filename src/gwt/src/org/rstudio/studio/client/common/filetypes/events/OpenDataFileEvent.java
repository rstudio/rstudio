/*
 * OpenDataFileEvent.java
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

public class OpenDataFileEvent extends GwtEvent<OpenDataFileEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onOpenDataFile(OpenDataFileEvent event);
   }

   public OpenDataFileEvent(FileSystemItem file)
   {
      file_ = file;
   }

   public FileSystemItem getFile()
   {
      return file_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onOpenDataFile(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final FileSystemItem file_;
}
