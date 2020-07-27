/*
 * SaveFailedEvent.java
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

package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SaveFailedEvent extends GwtEvent<SaveFailedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onSaveFailed(SaveFailedEvent event);
   }

   public static final GwtEvent.Type<SaveFailedEvent.Handler> TYPE =
      new GwtEvent.Type<SaveFailedEvent.Handler>();
   
   public SaveFailedEvent(String path,
                          String id)
   {
      path_ = path;
      id_ = id;
   }
   
   public String getPath()
   {
      return path_;
   }

   public String getId()
   {
      return id_;
   }
   
   @Override
   protected void dispatch(SaveFailedEvent.Handler handler)
   {
      handler.onSaveFailed(this);
   }

   @Override
   public GwtEvent.Type<SaveFailedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final String path_;
   private final String id_;
}
