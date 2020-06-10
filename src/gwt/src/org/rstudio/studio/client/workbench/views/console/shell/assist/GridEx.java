/*
 * GridEx.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.HasMouseMoveHandlers;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import org.rstudio.core.client.widget.LayoutGrid;

public class GridEx extends LayoutGrid implements HasMouseMoveHandlers
{
   public GridEx(int rows, int cols)
   {
      super(rows, cols);
      sinkEvents(Event.ONMOUSEMOVE);
   }

   public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler)
   {
      return addHandler(handler, MouseMoveEvent.getType());
   }

   public int getRowForEvent(MouseMoveEvent event)
   {
      Element td = getEventTargetCell(Event.as(event.getNativeEvent()));
      if (td == null)
      {
         return -1;
      }

      Element tr = DOM.getParent(td);
      Element body = DOM.getParent(tr);
      int row = DOM.getChildIndex(body, tr);

      return row;
   }
}

