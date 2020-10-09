/*
 * PopoutDocEvent.java
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
import org.rstudio.studio.client.workbench.views.source.SourceColumn;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class PopoutDocEvent extends CrossWindowEvent<PopoutDocEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onPopoutDoc(PopoutDocEvent e);
   }

   public PopoutDocEvent()
   {
   }

   public PopoutDocEvent(String docId, SourcePosition sourcePosition, SourceColumn column)
   {
      this(new PopoutDocInitiatedEvent(docId, null), sourcePosition, column);
   }

   public PopoutDocEvent(PopoutDocInitiatedEvent originator,
         SourcePosition sourcePosition, SourceColumn column)
   {
      originator_ = originator;
      sourcePosition_ = sourcePosition;
      column_ = column;
   }

   public PopoutDocInitiatedEvent getOriginator()
   {
      return originator_;
   }

   public SourcePosition getSourcePosition()
   {
      return sourcePosition_;
   }

   public SourceColumn getColumn()
   {
      return column_;
   }

   public String getDocId()
   {
      return originator_.getDocId();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onPopoutDoc(this);
   }

   @Override
   public boolean forward()
   {
      return false;
   }

   private SourcePosition sourcePosition_;
   private SourceColumn column_;
   private PopoutDocInitiatedEvent originator_;

   public static final Type<Handler> TYPE = new Type<>();
}
