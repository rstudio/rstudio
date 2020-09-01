/*
 * ScrollToPositionEvent.java
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

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

@JavaScriptSerializable
public class ScrollToPositionEvent extends CrossWindowEvent<ScrollToPositionEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onScrollToPosition(ScrollToPositionEvent event);
   }

   public ScrollToPositionEvent()
   {
   }

   public ScrollToPositionEvent(int line, int column, boolean moveCursor)
   {
      line_ = line;
      column_ = column;
      moveCursor_ = moveCursor;
   }

   public int getLine()
   {
      return line_;
   }

   public int getColumn()
   {
      return column_;
   }

   public boolean getMoveCursor()
   {
      return moveCursor_;
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onScrollToPosition(this);
   }

   private int line_;
   private int column_;
   private boolean moveCursor_;
   public static final Type<Handler> TYPE = new Type<>();
}
