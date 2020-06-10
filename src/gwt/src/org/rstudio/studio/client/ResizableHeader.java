/*
 * ResizableHeader.java
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
package org.rstudio.studio.client;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.ElementPredicate;
import org.rstudio.core.client.events.MouseDragHandler;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.Header;

public class ResizableHeader extends Header<String>
{
   private static class ResizableHeaderCell<U> extends AbstractCell<U>
   {
      public ResizableHeaderCell(AbstractCellTable<?> table)
      {
         super();
         table_ = table;
      }
      
      @Override
      public void render(Context context,
                         U content,
                         SafeHtmlBuilder builder)
      {
         // If a 'sort' icon is being drawn, we need to re-adjust
         // the left margin size to ensure the splitter is properly
         // positioned.
         boolean isSorted = false;
         Column<?, ?> column = table_.getColumn(context.getColumn());
         ColumnSortList sortList = table_.getColumnSortList();
         if (sortList.size() > 0)
         {
            ColumnSortInfo info = sortList.get(0);
            if (info.getColumn().equals(column))
               isSorted = true;
         }
         
         String marginLeft = isSorted
               ? "-23px"
               : "-7px";
                  
         SafeHtml splitter = SafeHtmlUtil.createOpenTag("div",
               "class", RES.styles().splitter(),
               "style", "margin-left: " + marginLeft + ";",
               "data-index", "" + context.getColumn());
         
         builder
            .append(splitter)
            .appendHtmlConstant("</div>")
            .appendHtmlConstant("<div>")
            .appendEscaped(content.toString())
            .appendHtmlConstant("</div>");
      }
      
      private final AbstractCellTable<?> table_;
   }
   
   public ResizableHeader(AbstractCellTable<?> table, String text)
   {
      super(new ResizableHeaderCell<String>(table));
      
      table_ = table;
      text_ = text;
      index_ = table.getColumnCount();
      
      MouseDragHandler.addHandler(table_, new MouseDragHandler()
      {
         List<Integer> columnWidths_ = new ArrayList<Integer>();
         
         @Override
         public boolean beginDrag(MouseDownEvent event)
         {
            // detect a click on this splitter
            Element targetEl = event.getNativeEvent().getEventTarget().cast();
            if (!targetEl.hasClassName(RES.styles().splitter()))
               return false;
            
            int index = StringUtil.parseInt(targetEl.getAttribute("data-index"), -1);
            if (index != index_)
               return false;
            
            // find the parent table row element
            Element rowEl = DomUtils.findParentElement(
                  targetEl,
                  new ElementPredicate()
                  {
                     @Override
                     public boolean test(Element el)
                     {
                        return el.hasTagName("tr");
                     }
                  });
            
            if (rowEl == null)
               return false;
            
            // initialize column widths
            columnWidths_.clear();
            for (int i = 0, n = rowEl.getChildCount(); i < n; i++)
            {
               Element childEl = rowEl.getChild(i).cast();
               int width = childEl.getOffsetWidth();
               columnWidths_.add(width);
               table_.setColumnWidth(i, width + "px");
            }
            
            return true;
         }
         
         @Override
         public void onDrag(MouseDragEvent event)
         {
            // discover the change in column widths
            int delta = event.getTotalDelta().getMouseX();
            
            // compute the right column width
            int leftWidth = columnWidths_.get(index_ - 1) + delta;
            int rightWidth = columnWidths_.get(index_) - delta;
            
            // avoid issues with resizing too small
            if (leftWidth < 0)
            {
               rightWidth = rightWidth + leftWidth;
               leftWidth = 0;
            }
            
            // update column widths
            table_.setColumnWidth(index_ - 1, leftWidth + "px");
            table_.setColumnWidth(index_, rightWidth + "px");
         }
         
         @Override
         public void endDrag()
         {
         }
         
      });
   }
   
   @Override
   public String getValue()
   {
      return text_;
   }
   
   private final AbstractCellTable<?> table_;
   private final String text_;
   private final int index_;
   
   // Resources, etc ----
   public interface Resources extends ClientBundle
   {
      @Source("ResizableHeader.css")
      Styles styles();
   }
   
   public interface Styles extends CssResource
   {
      String splitter();
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.styles().ensureInjected();
   }
}
