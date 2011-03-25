/*
 * OpenSourceFileEvent.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.filetypes.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.filetypes.TextFileType;

public class OpenSourceFileEvent extends GwtEvent<OpenSourceFileHandler>
{
   public static final GwtEvent.Type<OpenSourceFileHandler> TYPE =
      new GwtEvent.Type<OpenSourceFileHandler>();

   public OpenSourceFileEvent(FileSystemItem file, TextFileType fileType)
   {
      this(file, fileType, null);
   }

   public OpenSourceFileEvent(FileSystemItem file,
                              TextFileType fileType,
                              String encoding)
   {
      file_ = file;
      fileType_ = fileType;
      encoding_ = encoding;
   }
   
   public FileSystemItem getFile()
   {
      return file_;
   }

   public TextFileType getFileType()
   {
      return fileType_;
   }

   public String getEncoding()
   {
      return encoding_;
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
   private final TextFileType fileType_;
   private final String encoding_;
}
