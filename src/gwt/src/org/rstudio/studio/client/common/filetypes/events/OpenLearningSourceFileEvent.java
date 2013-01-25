/*
 * OpenLearningSourceFileEvent.java
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

public class OpenLearningSourceFileEvent extends GwtEvent<OpenLearningSourceFileHandler>
{
   public static final GwtEvent.Type<OpenLearningSourceFileHandler> TYPE =
      new GwtEvent.Type<OpenLearningSourceFileHandler>();

   public OpenLearningSourceFileEvent(FileSystemItem file, 
                                      TextFileType fileType,
                                      FilePosition position)
   {
      this(file, fileType, position, null);
   }
   
  
   public OpenLearningSourceFileEvent(FileSystemItem file, 
                                      TextFileType fileType,
                                      String pattern)
   {
      this(file, fileType, null, pattern);
   }

   
   public OpenLearningSourceFileEvent(FileSystemItem file, 
                                      TextFileType fileType,
                                      FilePosition position, 
                                      String pattern)
   {
      file_ = file;
      position_ = position;
      fileType_ = fileType;  
      pattern_ = pattern;
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
   
   public String getPattern()
   {
      return pattern_;
   }

   @Override
   protected void dispatch(OpenLearningSourceFileHandler handler)
   {
      handler.onOpenLearningSourceFile(this);
   }

   @Override
   public GwtEvent.Type<OpenLearningSourceFileHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final FileSystemItem file_;
   private final FilePosition position_;
   private final TextFileType fileType_;
   private final String pattern_;
}
