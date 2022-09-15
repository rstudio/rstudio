/*
 * OpenPresentationSourceFileEvent.java
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

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.filetypes.TextFileType;

public class OpenPresentationSourceFileEvent extends GwtEvent<OpenPresentationSourceFileEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onOpenPresentationSourceFile(OpenPresentationSourceFileEvent event);
   }

   public OpenPresentationSourceFileEvent(FileSystemItem file,
                                          TextFileType fileType,
                                          FilePosition position)
   {
      this(file, fileType, position, null);
   }


   public OpenPresentationSourceFileEvent(FileSystemItem file,
                                          TextFileType fileType,
                                          String pattern)
   {
      this(file, fileType, null, pattern);
   }


   public OpenPresentationSourceFileEvent(FileSystemItem file,
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
   protected void dispatch(Handler handler)
   {
      handler.onOpenPresentationSourceFile(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final FileSystemItem file_;
   private final FilePosition position_;
   private final TextFileType fileType_;
   private final String pattern_;
}
