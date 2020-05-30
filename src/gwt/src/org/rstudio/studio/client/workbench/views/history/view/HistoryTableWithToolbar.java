/*
 * HistoryTableWithToolbar.java
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.Rectangle;
import org.rstudio.studio.client.workbench.views.history.HasHistory;
import org.rstudio.studio.client.workbench.views.history.model.HistoryEntry;

import java.util.ArrayList;

public class HistoryTableWithToolbar extends Composite
   implements HasHistory, HasAllKeyHandlers
{
   public HistoryTableWithToolbar(HistoryTable table,
                                  Widget[] leftWidgets,
                                  Widget[] rightWidgets)
   {
      DockLayoutPanel panel = new DockLayoutPanel(Unit.PX);

      Shelf toolbar = new Shelf();
      toolbar.setWidth("100%");
      
      if (leftWidgets != null)
      {
         for (int i = 0; i < leftWidgets.length; i++)
         {
            toolbar.addLeftWidget(leftWidgets[i]);
         }
      }
      if (rightWidgets != null)
      {
         for (int i = 0; i < rightWidgets.length; i++)
         {
            toolbar.addRightWidget(rightWidgets[i]);
         }
      }
      panel.addNorth(toolbar, toolbar.getHeight());

      historyTable_ = table;
      scrollPanel_ = new ScrollPanel(historyTable_);
      scrollPanel_.getElement().getStyle().setProperty("overflowX", "hidden");
      scrollPanel_.setSize("100%", "100%");
      panel.add(scrollPanel_);

      historyTable_.setOwningScrollPanel(scrollPanel_);

      initWidget(panel);
   }

   public void clear()
   {
      historyTable_.clear();
   }

   public void addItems(ArrayList<HistoryEntry> entries, boolean top)
   {
      historyTable_.addItems(entries, top);
   }

   public ArrayList<String> getSelectedValues()
   {
      return historyTable_.getSelectedValues();
   }

   public ArrayList<Long> getSelectedCommandIndexes()
   {
      return historyTable_.getSelectedValues2();
   }

   public boolean moveSelectionUp()
   {
      return historyTable_.moveSelectionUp();
   }

   public boolean moveSelectionDown()
   {
      return historyTable_.moveSelectionDown();
   }

   public HasAllKeyHandlers getKeyTarget()
   {
      return historyTable_;
   }

   public void highlightRows(int offset, int length)
   {
      historyTable_.clearSelection();
      historyTable_.setSelected(offset, length, true);

      Rectangle rect = historyTable_.getSelectionRect();
      if (rect == null)
         return;
      int height = scrollPanel_.getOffsetHeight();
      if (rect.getHeight() > height)
         scrollPanel_.setVerticalScrollPosition(rect.getTop());
      else
         scrollPanel_.setVerticalScrollPosition(
               rect.getTop() - (height - rect.getHeight())/2);
   }

   public HandlerRegistration addKeyUpHandler(KeyUpHandler handler)
   {
      return historyTable_.addKeyUpHandler(handler);
   }

   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return historyTable_.addKeyDownHandler(handler);
   }

   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
   {
      return historyTable_.addKeyPressHandler(handler);      
   }

   public Element getFocusTarget()
   {
      return historyTable_.getElement();
   }

   private final HistoryTable historyTable_;
   private ScrollPanel scrollPanel_;
}
