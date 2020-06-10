/*
 * FileChangeEvent.java
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
import org.rstudio.studio.client.workbench.views.files.model.FileChange;

public class FileChangeEvent extends GwtEvent<FileChangeHandler>
{
   public static final GwtEvent.Type<FileChangeHandler> TYPE =
      new GwtEvent.Type<FileChangeHandler>();
   
   public FileChangeEvent(FileChange fileChange)
   {
      fileChange_ = fileChange;
   }
   
   public FileChange getFileChange()
   {
      return fileChange_;
   }
   
   @Override
   protected void dispatch(FileChangeHandler handler)
   {
      handler.onFileChange(this);
   }

   @Override
   public GwtEvent.Type<FileChangeHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final FileChange fileChange_;
}
