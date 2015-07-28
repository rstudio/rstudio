/*
 * PopoutDocEvent.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

import org.rstudio.core.client.Point;
import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
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
      posX_ = 0;
      posY_ = 0;
   }
   
   public PopoutDocEvent(String docId, Point position, 
         SourcePosition sourcePosition)
   {
      docId_ = docId;
      if (position != null)
      { 
         posX_ = position.getX();
         posY_ = position.getY();
      }
      sourcePosition_ = sourcePosition;
   }

   public String getDocId()
   {
      return docId_;
   }
   
   public Point getPosition()
   {
      if (posX_ == 0 && posY_ == 0)
         return null;
      return new Point(posX_, posY_);
   }
   
   public SourcePosition getSourcePosition()
   {
      return sourcePosition_;
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

   private String docId_;
   private int posX_;
   private int posY_;
   private SourcePosition sourcePosition_;
   
   public static final Type<Handler> TYPE = new Type<Handler>();
}