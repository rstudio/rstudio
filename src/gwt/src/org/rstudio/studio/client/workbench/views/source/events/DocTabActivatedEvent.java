/*
 * DocTabActivatedEvent.java
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

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class DocTabActivatedEvent 
             extends CrossWindowEvent<DocTabActivatedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onDocTabActivated(DocTabActivatedEvent event);
   }

   public static final GwtEvent.Type<DocTabActivatedEvent.Handler> TYPE =
      new GwtEvent.Type<DocTabActivatedEvent.Handler>();
   
   public DocTabActivatedEvent()
   {
   }
   
   public DocTabActivatedEvent(String path, String id)
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
   protected void dispatch(DocTabActivatedEvent.Handler handler)
   {
      handler.onDocTabActivated(this);
   }

   @Override
   public GwtEvent.Type<DocTabActivatedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private String path_;
   private String id_;
}
