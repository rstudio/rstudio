/*
 * OpenSourceFileEvent.java
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
package org.rstudio.studio.client.common.filetypes.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.model.NavigationMethods;

@JavaScriptSerializable
public class OpenSourceFileEvent extends CrossWindowEvent<OpenSourceFileEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onOpenSourceFile(OpenSourceFileEvent event);
   }

   public OpenSourceFileEvent()
   {
   }
   
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getFileName() /*-{
         return this.file_name;
      }-*/;

      public final native int getLineNumber() /*-{
         return this.line_number;
      }-*/;

      public final native int getColumnNumber() /*-{
         return this.column_number;
      }-*/;
   }
   
   public static OpenSourceFileEvent fromData(Data data)
   {
      FileSystemItem destFile = FileSystemItem.createFile(
            data.getFileName());
      FilePosition pos = FilePosition.create(data.getLineNumber(),
            data.getColumnNumber());
      FileTypeRegistry registry = RStudioGinjector.INSTANCE.getFileTypeRegistry();
      return new OpenSourceFileEvent(
         destFile, 
         pos, 
         registry.getTextTypeForFile(destFile)
      );
         
   }

   public OpenSourceFileEvent(FileSystemItem file, TextFileType fileType)
   {
      this(file, null, fileType);
   }

   public OpenSourceFileEvent(FileSystemItem file,
                              FilePosition position,
                              TextFileType fileType)
   {
      this(file, position, fileType, NavigationMethods.DEFAULT);
   }

   public OpenSourceFileEvent(FileSystemItem file,
                              FilePosition position,
                              TextFileType fileType,
                              int navMethod)
   {
      this(file, position, fileType, true, navMethod);
   }

   public OpenSourceFileEvent(FileSystemItem file,
                              FilePosition position,
                              TextFileType fileType,
                              boolean moveCursor,
                              int navMethod)
   {
      file_ = file;
      position_ = position;
      fileType_ = fileType;
      moveCursor_ = moveCursor;
      navigationMethod_ = navMethod;
   }

   @Override
   public boolean forward()
   {
      return false;
   }

   public FileSystemItem getFile()
   {
      return file_;
   }

   public TextFileType getFileType()
   {
      if (fileType_ == null)
      {
         return RStudioGinjector.INSTANCE.getFileTypeRegistry()
                                         .getTextTypeForFile(file_);
      }
      else
      {
         return fileType_;
      }
   }

   public boolean getMoveCursor()
   {
      return moveCursor_;
   }

   public FilePosition getPosition()
   {
      return position_;
   }

   public int getNavigationMethod()
   {
      return navigationMethod_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onOpenSourceFile(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private FileSystemItem file_;
   private FilePosition position_;
   private TextFileType fileType_;
   private boolean moveCursor_;
   private int navigationMethod_;
}
