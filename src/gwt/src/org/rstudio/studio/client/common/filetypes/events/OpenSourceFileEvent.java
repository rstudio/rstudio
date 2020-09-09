/*
 * OpenSourceFileEvent.java
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
package org.rstudio.studio.client.common.filetypes.events;

import com.google.gwt.event.shared.GwtEvent;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.model.NavigationMethods;

@JavaScriptSerializable
public class OpenSourceFileEvent extends CrossWindowEvent<OpenSourceFileHandler>
{
   public static final GwtEvent.Type<OpenSourceFileHandler> TYPE =
      new GwtEvent.Type<OpenSourceFileHandler>();

   public OpenSourceFileEvent()
   {
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
   protected void dispatch(OpenSourceFileHandler handler)
   {
      handler.onOpenSourceFile(this);
   }

   @Override
   public GwtEvent.Type<OpenSourceFileHandler> getAssociatedType()
   {
      return TYPE;
   }

   private FileSystemItem file_;
   private FilePosition position_;
   private TextFileType fileType_;
   private boolean moveCursor_;
   private int navigationMethod_;
}
