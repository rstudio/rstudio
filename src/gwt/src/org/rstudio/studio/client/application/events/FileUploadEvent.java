/*
 * FileUploadEvent.java
 *
 * Copyright (C) 2019 by RStudio, PBC
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
package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.GwtEvent;

public class FileUploadEvent extends GwtEvent<FileUploadHandler>
{
   public FileUploadEvent(boolean inProgress)
   {
      inProgress_ = inProgress;
   }

   public static final GwtEvent.Type<FileUploadHandler> TYPE =
         new GwtEvent.Type<>();
   
   @Override
   protected void dispatch(FileUploadHandler handler)
   {
      handler.onFileUpload(this);
   }

   @Override
   public GwtEvent.Type<FileUploadHandler> getAssociatedType()
   {
      return TYPE;
   }

   public boolean inProgress()
   {
      return inProgress_;
   }

   private boolean inProgress_;
}
