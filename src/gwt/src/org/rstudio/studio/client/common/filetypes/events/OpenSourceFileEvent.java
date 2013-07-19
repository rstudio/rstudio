/*
 * OpenSourceFileEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
import org.rstudio.studio.client.common.filetypes.TextFileType;

public class OpenSourceFileEvent extends GwtEvent<OpenSourceFileHandler>
{
   public static final GwtEvent.Type<OpenSourceFileHandler> TYPE =
      new GwtEvent.Type<OpenSourceFileHandler>();

   public enum NavigationMethod
   {
      Default,
      HighlightLine,
      DebugStep,
      DebugFrame, 
      DebugEnd
   }
   
   public OpenSourceFileEvent(FileSystemItem file, TextFileType fileType)
   {
      this(file, null, fileType);
   }
   
   public OpenSourceFileEvent(FileSystemItem file, 
                              FilePosition position, 
                              TextFileType fileType)
   {
      this(file, position, fileType, NavigationMethod.Default);
   }
   
   public OpenSourceFileEvent(FileSystemItem file, 
                              FilePosition position, 
                              TextFileType fileType,
                              NavigationMethod navMethod)
   {
      file_ = file;
      position_ = position;
      fileType_ = fileType;  
      navigationMethod_ = navMethod;
   }
   
   public FileSystemItem getFile()
   {
      return file_;
   }

   public TextFileType getFileType()
   {
      return fileType_;
   }
   
   public FilePosition getPosition()
   {
      return position_;
   }
   
   public NavigationMethod getNavigationMethod()
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
   
   private final FileSystemItem file_;
   private final FilePosition position_;
   private final TextFileType fileType_;
   private final NavigationMethod navigationMethod_;
}
