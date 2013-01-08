/*
 * FlexTableEx.java
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
package org.rstudio.studio.client.workbench.views.workspace.table;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlexTable;

public class FlexTableEx extends FlexTable
{
   public FlexTableEx()
   {
   }

   public HandlerRegistration addMouseOverHandler(MouseOverHandler handler)
   {
      return addDomHandler(handler, MouseOverEvent.getType());
   }

   public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler)
   {
      return addDomHandler(handler, MouseMoveEvent.getType());
   }

   public HandlerRegistration addMouseOutHandler(MouseOutHandler handler)
   {
      return addDomHandler(handler, MouseOutEvent.getType());
   }

   public int getRowForEvent(NativeEvent nativeEvent)
   {
      Element td = getEventTargetCell(Event.as(nativeEvent));
      if (td == null) {
         return -1;
      }

      Element tr = DOM.getParent(td);
      Element body = DOM.getParent(tr);
      return DOM.getChildIndex(body, tr);
   }

   public TableRowElement getRowElement(int row)
   {
      TableElement table = getElement().cast();
      return table.getRows().getItem(row);
   }
}
