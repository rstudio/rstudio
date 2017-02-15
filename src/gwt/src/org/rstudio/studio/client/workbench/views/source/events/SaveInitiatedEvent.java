/*
 * SaveInitiatedEvent.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

public class SaveInitiatedEvent extends GwtEvent<SaveInitiatedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onSaveInitiated(SaveInitiatedEvent event);
   }

   public static final GwtEvent.Type<SaveInitiatedEvent.Handler> TYPE =
      new GwtEvent.Type<SaveInitiatedEvent.Handler>();
   
   public SaveInitiatedEvent(String path, String id)
   {
      path_ = path;
      id_ = id;
   }
   
   public String getPath()
   {
      return path_;
   }

   public String getDocId()
   {
      return id_;
   }
   
   @Override
   protected void dispatch(SaveInitiatedEvent.Handler handler)
   {
      handler.onSaveInitiated(this);
   }

   @Override
   public GwtEvent.Type<SaveInitiatedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final String path_;
   private final String id_;
}