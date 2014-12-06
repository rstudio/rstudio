package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.HasMouseMoveHandlers;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Grid;

public class GridEx extends Grid implements HasMouseMoveHandlers
{
      public GridEx(int rows, int cols)
      {
         super(rows, cols) ;
         sinkEvents(Event.ONMOUSEMOVE) ;
      }

      public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler)
      {
         return addHandler(handler, MouseMoveEvent.getType()) ;
      }
      
      public int getRowForEvent(MouseMoveEvent event)
      {
         Element td = getEventTargetCell(Event.as(event.getNativeEvent()));
         if (td == null) {
           return -1;
         }

         Element tr = DOM.getParent(td);
         Element body = DOM.getParent(tr);
         int row = DOM.getChildIndex(body, tr);

         return row ;
       }
      
      public void clearAllCells()
      {
         for (int i = 0; i < getRowCount(); i++)
            for (int j = 0; j < getColumnCount(); j++)
               setText(i, j, "");
      }
      
      public void clearColumn(int column)
      {
         for (int i = 0; i < getRowCount(); i++)
            setText(i, column, "");
      }

}
