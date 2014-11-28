package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.HasMouseMoveHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Grid;

public class GridWithMouseHandlers
       extends Grid
       implements HasMouseOutHandlers, HasMouseOverHandlers, HasMouseMoveHandlers
{

    public class Cell extends com.google.gwt.user.client.ui.HTMLTable.Cell
    {
        public Cell(int rowIndex, int cellIndex)
        {
            super(rowIndex, cellIndex);
        }
    }

    public GridWithMouseHandlers(int rows, int cols)
    {
        super(rows, cols);
    }

    public Cell getCellForEvent(MouseEvent event)
    {
        Element td = getEventTargetCell(Event.as(event.getNativeEvent()));
        if (td == null)
            return null;

        int row = TableRowElement.as(td.getParentElement()).getSectionRowIndex();
        int column = TableCellElement.as(td).getCellIndex();
        return new Cell(row, column);
    }
    
    public Cell getCell(int row, int column)
    {
       return new Cell(row, column);
    }
    
    @Override
    public HandlerRegistration addMouseOverHandler(MouseOverHandler handler)
    {
        return this.addDomHandler(handler, MouseOverEvent.getType());
    }

    @Override
    public HandlerRegistration addMouseOutHandler(MouseOutHandler handler)
    {
        return this.addDomHandler(handler, MouseOutEvent.getType());
    }

    @Override
    public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler)
    {
        return this.addDomHandler(handler, MouseMoveEvent.getType());
    }
    
    public void clearColumn(int column)
    {
       for (int i = 0; i < getRowCount(); i++)
          setText(i, column, "");
    }
    
    private Cell selectedCell_;

} 