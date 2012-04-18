/*
 * DirectoryNavigateEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.files.events;

import com.google.gwt.event.shared.GwtEvent;

import org.rstudio.core.client.files.FileSystemItem;

public class DirectoryNavigateEvent extends GwtEvent<DirectoryNavigateHandler>
{
   public static final GwtEvent.Type<DirectoryNavigateHandler> TYPE =
      new GwtEvent.Type<DirectoryNavigateHandler>();
   
   public DirectoryNavigateEvent(FileSystemItem directory)
   {
      directory_ = directory;
   }
   
   public FileSystemItem getDirectory()
   {
      return directory_;
   }
   
   @Override
   protected void dispatch(DirectoryNavigateHandler handler)
   {
      handler.onDirectoryNavigate(this);
   }

   @Override
   public GwtEvent.Type<DirectoryNavigateHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final FileSystemItem directory_;
}
