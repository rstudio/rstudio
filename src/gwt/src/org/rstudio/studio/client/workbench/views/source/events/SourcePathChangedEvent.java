/*
 * SourcePathChangedEvent.java
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

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class SourcePathChangedEvent 
             extends CrossWindowEvent<SourcePathChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onSourcePathChanged(SourcePathChangedEvent event);
   }

   public static final GwtEvent.Type<SourcePathChangedEvent.Handler> TYPE =
      new GwtEvent.Type<SourcePathChangedEvent.Handler>();
   
   public SourcePathChangedEvent()
   {
      from_ = null;
      to_ = null;
   }

   public SourcePathChangedEvent(String from, String to)
   {
      from_ = from;
      to_ = to;
   }
   
   public String getFrom()
   {
      return from_;
   }

   public String getTo()
   {
      return to_;
   }
   
   @Override
   protected void dispatch(SourcePathChangedEvent.Handler handler)
   {
      handler.onSourcePathChanged(this);
   }

   @Override
   public GwtEvent.Type<SourcePathChangedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private String from_;
   private String to_;
}
