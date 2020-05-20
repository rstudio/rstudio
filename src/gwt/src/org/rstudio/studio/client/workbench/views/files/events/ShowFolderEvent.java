/*
 * ShowFolderEvent.java
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
package org.rstudio.studio.client.workbench.views.files.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.core.client.files.FileSystemItem;

public class ShowFolderEvent extends GwtEvent<ShowFolderHandler>
{
   public static final Type<ShowFolderHandler> TYPE =
         new Type<ShowFolderHandler>();

   public ShowFolderEvent(FileSystemItem path)
   {
      path_ = path;
   }

   public FileSystemItem getPath()
   {
      return path_;
   }

   @Override
   public Type<ShowFolderHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(ShowFolderHandler handler)
   {
      handler.onShowFolder(this);
   }

   private final FileSystemItem path_;
}
