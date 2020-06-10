/*
 * GitChangelistTable.java
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
package org.rstudio.studio.client.workbench.views.vcs.git;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.cellview.client.Column;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.TriStateCheckboxCell;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.common.events.StageUnstageEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.StageUnstageHandler;

import java.util.ArrayList;
import java.util.Comparator;

public class GitChangelistTable extends ChangelistTable
{
   public void toggleStaged(boolean moveSelection)
   {
      ArrayList<StatusAndPath> items = getSelectedItems();
      if (items.size() > 0)
      {
         boolean unstage = StringUtil.isCharAt(items.get(0).getStatus(), ' ', 1);
         fireEvent(new StageUnstageEvent(unstage, items));

         if (moveSelection)
         {
            moveSelectionDown();
         }
      }
   }

   @Override
   protected SafeHtmlRenderer<String> getStatusRenderer()
   {
      return new GitStatusRenderer();
   }

   @Override
   protected void configureTable()
   {
      final Column<StatusAndPath, Boolean> stagedColumn = new Column<StatusAndPath, Boolean>(
            new TriStateCheckboxCell<StatusAndPath>(selectionModel_))
      {
         @Override
         public Boolean getValue(StatusAndPath object)
         {
            return "??".equals(object.getStatus()) ? Boolean.FALSE :
                   StringUtil.isCharAt(object.getStatus(), ' ', 1) ? Boolean.TRUE :
                   StringUtil.isCharAt(object.getStatus(), ' ', 0) ? Boolean.FALSE :
                   null;
         }
      };

      stagedColumn.setHorizontalAlignment(Column.ALIGN_CENTER);
      stagedColumn.setFieldUpdater(new FieldUpdater<StatusAndPath, Boolean>()
      {
         @Override
         public void update(final int index,
                            final StatusAndPath object,
                            Boolean value)
         {
            fireEvent(new StageUnstageEvent(!value, getSelectedItems()));
         }
      });
      stagedColumn.setSortable(true);
      sortHandler_.setComparator(stagedColumn, new Comparator<StatusAndPath>()
      {
         @Override
         public int compare(StatusAndPath a, StatusAndPath b)
         {
            Boolean a1 = stagedColumn.getValue(a);
            Boolean b1 = stagedColumn.getValue(b);
            int a2 = a1 == null ? 0 : a1 ? -1 : 1;
            int b2 = b1 == null ? 0 : b1 ? -1 : 1;
            return a2 - b2;
         }
      });
      table_.addColumn(stagedColumn, "Staged");
      table_.setColumnWidth(stagedColumn, "46px");

      super.configureTable();
   }

   public HandlerRegistration addStageUnstageHandler(StageUnstageHandler handler)
   {
      return addHandler(handler, StageUnstageEvent.TYPE);
   }
}
