/*
 * ResizableHeader.java
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
package org.rstudio.studio.client;

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
import com.google.gwt.user.cellview.client.Header;

public class ResizableHeader extends Header<String>
{
   private static class ResizableHeaderCell extends AbstractCell<String>
   {
      public ResizableHeaderCell(int index)
      {
         super();
         index_ = index;
      }
      
      @Override
      public void render(Context context,
                         String string,
                         SafeHtmlBuilder builder)
      {
         SafeHtml splitter = SafeHtmlUtil.createOpenTag("div",
               "class", RES.styles().splitter(),
               "data-index", "" + index_);
         
         builder
            .append(splitter)
            .appendHtmlConstant("</div>")
            .appendHtmlConstant("<div>")
            .appendEscaped(string)
            .appendHtmlConstant("</div>");
      }
      
      private final int index_;
   }
   
   public ResizableHeader(AbstractCellTable<?> table,
                          String text,
                          int index)
   {
      super(new ResizableHeaderCell(index));
      
      table_ = table;
      text_ = text;
      index_ = index;
      
      MouseDragHandler.addHandler(table_, new MouseDragHandler()
      {
         int leftColumnWidth_ = -1;
         int rightColumnWidth_ = -1;
         
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
            leftColumnWidth_  = rowEl.getChild(index_ - 1).<Element>cast().getOffsetWidth();
            rightColumnWidth_ = rowEl.getChild(index_).<Element>cast().getOffsetWidth();
            return true;
         }
         
         @Override
         public void onDrag(MouseDragEvent event)
         {
            int delta = event.getTotalDelta().getMouseX();
            table_.setColumnWidth(index_ - 1, (leftColumnWidth_ + delta) + "px");
            table_.setColumnWidth(index_, (rightColumnWidth_ - delta) + "px");
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
