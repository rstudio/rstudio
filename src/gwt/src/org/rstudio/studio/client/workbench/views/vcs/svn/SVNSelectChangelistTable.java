/*
 * SVNSelectChangelistTable.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn;

import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.MultiSelectionModel;
import org.rstudio.core.client.cellview.TriStateCheckboxCell;
import org.rstudio.studio.client.common.vcs.StatusAndPath;

import java.util.*;

public class SVNSelectChangelistTable extends SVNChangelistTable
{
   public SVNSelectChangelistTable()
   {
      uncommitableStatuses.add("X");
      uncommitableStatuses.add("?");
      uncommitableStatuses.add("!");
      uncommitableStatuses.add("C");
   }

   @Override
   protected MultiSelectionModel<StatusAndPath> createSelectionModel()
   {
      // Squelch normal table selection, we're using checkboxes instead

      return new MultiSelectionModel<StatusAndPath>()
      {
         @Override
         public void setSelected(StatusAndPath object, boolean selected)
         {
            // do nothing
         }
      };
   }

   @Override
   public ArrayList<StatusAndPath> getSelectedItems()
   {
      throw new UnsupportedOperationException(
            "SVNSelectChangelistTable.getSelectedItems is not supported");
   }

   @Override
   public ArrayList<String> getSelectedPaths()
   {
      ArrayList<String> selectedPaths = new ArrayList<String>();
      for (Map.Entry<String, Boolean> entry : selected_.entrySet())
         if (entry.getValue() != null && entry.getValue())
            selectedPaths.add(entry.getKey());
      return selectedPaths;
   }

   @Override
   public ArrayList<String> getSelectedDiscardablePaths()
   {
      throw new UnsupportedOperationException(
            "SVNSelectChangelistTable.getSelectedDiscardablePaths is not " +
            "supported");
   }

   @Override
   public void setItems(ArrayList<StatusAndPath> items)
   {
      for (StatusAndPath item: items)
      {
         if (selected_.containsKey(item.getPath()))
            continue;

         selected_.put(item.getPath(),
                       !uncommitableStatuses.contains(item.getStatus()));
      }
      super.setItems(items);
   }

   public Column<StatusAndPath, Boolean> getCommitColumn()
   {
      return commitColumn_;
   }

   public void setSelected(StatusAndPath obj, Boolean selected)
   {
      selected_.put(obj.getPath(), selected);
      List<StatusAndPath> list = dataProvider_.getList();
      int index = list.indexOf(obj);
      if (index >= 0)
         list.set(index, list.get(index));
   }

   @Override
   protected void configureTable()
   {
      commitColumn_ = new Column<StatusAndPath, Boolean>(
            new TriStateCheckboxCell<StatusAndPath>(selectionModel_))
      {
         @Override
         public Boolean getValue(StatusAndPath object)
         {
            return selected_.containsKey(object.getPath()) &&
                  selected_.get(object.getPath());
         }
      };

      commitColumn_.setHorizontalAlignment(Column.ALIGN_CENTER);
      commitColumn_.setSortable(true);
      sortHandler_.setComparator(commitColumn_, new Comparator<StatusAndPath>()
      {
         @Override
         public int compare(StatusAndPath a, StatusAndPath b)
         {
            Boolean a1 = commitColumn_.getValue(a);
            Boolean b1 = commitColumn_.getValue(b);
            int a2 = a1 == null ? 0 : a1 ? -1 : 1;
            int b2 = b1 == null ? 0 : b1 ? -1 : 1;
            return a2 - b2;
         }
      });
      table_.addColumn(commitColumn_, "Commit");
      table_.setColumnWidth(commitColumn_, "46px");

      super.configureTable();
   }

   public void clearSelection()
   {
      selected_.clear();
      List<StatusAndPath> list = dataProvider_.getList();
      for (int i = 0; i < list.size(); i++)
         list.set(i, list.get(i));
   }

   private final HashMap<String, Boolean> selected_ =
                                                 new HashMap<String, Boolean>();
   private final HashSet<String> uncommitableStatuses = new HashSet<String>();
   private Column<StatusAndPath, Boolean> commitColumn_;
}
