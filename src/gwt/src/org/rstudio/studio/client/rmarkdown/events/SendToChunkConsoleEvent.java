/*
 * SendToChunkConsoleEvent.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

package org.rstudio.studio.client.rmarkdown.events;

import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SendToChunkConsoleEvent 
             extends GwtEvent<SendToChunkConsoleEvent.Handler>
{  
   public interface Handler extends EventHandler
   {
      void onSendToChunkConsole(SendToChunkConsoleEvent event);
   }

   public SendToChunkConsoleEvent(String docId, Scope scope, Range range)
   {
      docId_ = docId;
      scope_ = scope;
      range_ = range;
   }

   public String getDocId()
   {
      return docId_;
   }
   
   public int getRow() 
   {
      return scope_.getEnd().getRow();
   }
   
   public Scope getScope()
   {
      return scope_;
   }
   
   public Range getRange()
   {
      return range_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSendToChunkConsole(this);
   }
   
   private final String docId_;
   private Scope scope_;
   private final Range range_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}