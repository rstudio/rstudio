/*
 * FileEditEvent.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.files.FileSystemItem;

public class FileEditEvent extends GwtEvent<FileEditEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onFileEdit(FileEditEvent event);
   }
   
   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public native final FileSystemItem getFile() /*-{
         return this.file;
      }-*/;

      public native final FilePosition getFilePosition() /*-{
         return this.position;
      }-*/;
   }

   public FileEditEvent(Data data)
   {
      file_ = data.getFile();
      position_ = data.getFilePosition();
   }
   
   public FileEditEvent(FileSystemItem file)
   {
      file_ = file;
      position_ = null;
   }

   public FileSystemItem getFile()
   {
      return file_;
   }
   
   public FilePosition getFilePosition()
   {
      return position_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onFileEdit(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final FileSystemItem file_;
   private final FilePosition position_;
}

