/*
 * SaveFileEvent.java
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
package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SaveFileEvent extends GwtEvent<SaveFileEvent.Handler>
{
   public SaveFileEvent(String path, String fileType, String encoding)
   {
      path_ = path;
      fileType_ = fileType;
      encoding_ = encoding;
   }

   public final String getPath() { return path_; }
   public final String getFileType() { return fileType_; }
   public final String getEncoding() { return encoding_; }
   public final boolean isAutosave() { return path_ == null; }

   private final String path_;
   private final String fileType_;
   private final String encoding_;

   // Boilerplate ----

   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onSaveFile(SaveFileEvent event);
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSaveFile(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }
}

