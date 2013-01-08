/*
 * ShowVcsHistoryEvent.java
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
package org.rstudio.studio.client.workbench.views.vcs.common.events;

import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ShowVcsHistoryEvent extends GwtEvent<ShowVcsHistoryEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onShowVcsHistory(ShowVcsHistoryEvent event);
   }

   public ShowVcsHistoryEvent(FileSystemItem fileFilter)
   {
      fileFilter_ = fileFilter;
   }
   
   public FileSystemItem getFileFilter()
   {
      return fileFilter_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onShowVcsHistory(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();
   
   private final FileSystemItem fileFilter_;
}
