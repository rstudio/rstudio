/*
 * NewWorkingCopyEvent.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.events;


import org.rstudio.studio.client.common.filetypes.EditableFileType;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class NewWorkingCopyEvent extends GwtEvent<NewWorkingCopyEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onNewWorkingCopy(NewWorkingCopyEvent event);
   }

   public NewWorkingCopyEvent(EditableFileType type, String path,
         String contents)
   {
      type_ = type;
      path_ = path;
      contents_ = contents;
   }

   public EditableFileType getType()
   {
      return type_;
   }

   public String getPath()
   {
      return path_;
   }

   public String getContents()
   {
      return contents_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onNewWorkingCopy(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final EditableFileType type_;
   private final String path_;
   private final String contents_;
}
