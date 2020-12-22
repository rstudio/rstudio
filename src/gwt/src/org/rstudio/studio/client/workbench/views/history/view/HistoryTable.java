/*
 * HistoryTable.java
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
package org.rstudio.studio.client.workbench.views.history.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableColElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import org.rstudio.core.client.widget.FastSelectTable;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.history.HasHistory;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;
import org.rstudio.studio.client.workbench.views.history.view.HistoryEntryItemCodec.TimestampMode;
import org.rstudio.studio.client.workbench.views.history.view.HistoryPane.Resources;

import java.util.ArrayList;

public class HistoryTable extends FastSelectTable<HistoryEntry, String, Long>
   implements HasHistory
{
   public HistoryTable(String commandClassName,
                       String timestampClassName,
                       String selectedClassName,
                       TimestampMode timestampMode,
                       final Commands commands)
   {
      super(new HistoryEntryItemCodec(commandClassName,
                                      timestampClassName,
                                      timestampMode,
                                      timestampMode == TimestampMode.ITEM),
            selectedClassName,
            true,
            true,
            "History Entry Table");

      searchResult_ = timestampMode == TimestampMode.ITEM;
      
      applyWidthConstraints();

      final Resources res = GWT.create(Resources.class);
      setStyleName(res.styles().historyTable());
      addStyleName("rstudio-fixed-width-font");
      FontSizer.applyNormalFontSize(this);

      if (searchResult_)
      {
         addMouseDownHandler(new MouseDownHandler()
         {
            public void onMouseDown(MouseDownEvent event)
            {
               Element el = DOM.eventGetTarget((Event) event.getNativeEvent());
               if (el != null
                   && el.getTagName().equalsIgnoreCase("div")
                   && el.getClassName().contains(res.styles().disclosure()))
               {
                  // disclosure click
                  commands.historyShowContext().execute();
               }
            }
         });
      }
   }

   private void applyWidthConstraints()
   {
      createAndAppendCol("100%");
      createAndAppendCol("105");
      if (searchResult_)
         createAndAppendCol("23");
   }

   private TableColElement createAndAppendCol(String width)
   {
      TableColElement col = Document.get().createColElement();
      col.setWidth(width);
      getElement().appendChild(col);
      lastCol_ = col;
      return col;
   }

   @Override
   protected void addToTop(TableSectionElement tbody)
   {
      getElement().insertAfter(tbody, lastCol_);
   }

   @Override
   public void clear()
   {
      super.clear();
      applyWidthConstraints();
   }

   public ArrayList<Long> getSelectedCommandIndexes()
   {
      return getSelectedValues2();
   }

   public HasAllKeyHandlers getKeyTarget()
   {
      return this;
   }

   public com.google.gwt.dom.client.Element getFocusTarget()
   {
      return getElement();
   }

   private TableColElement lastCol_;
   private boolean searchResult_;
}
