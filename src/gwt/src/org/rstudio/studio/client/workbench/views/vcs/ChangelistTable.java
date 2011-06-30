/*
 * ChangelistTable.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import org.rstudio.core.client.DuplicateHelper;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.events.SelectionChangedEvent;
import org.rstudio.core.client.widget.events.SelectionChangedHandler;
import org.rstudio.studio.client.common.vcs.StatusAndPath;

import java.util.ArrayList;
import java.util.HashMap;

public class ChangelistTable extends Composite
{
   protected interface CellTableResources extends CellTable.Resources
   {
      @Override
      @Source("VCSPaneCellTableStyle.css")
      Style cellTableStyle();
   }

   protected interface Style extends CellTable.Style
   {
   }

   public ChangelistTable()
   {
      table_ = new CellTable<StatusAndPath>(
            100, (CellTable.Resources) GWT.create(CellTableResources.class));

      TextColumn<StatusAndPath> statusColumn = new TextColumn<StatusAndPath>()
      {
         @Override
         public String getValue(StatusAndPath object)
         {
            return object.getStatus().replaceAll(" ", "\u00A0");
         }
      };
      table_.addColumn(statusColumn);

      TextColumn<StatusAndPath> pathColumn = new TextColumn<StatusAndPath>()
      {
         @Override
         public String getValue(StatusAndPath object)
         {
            return object.getPath();
         }
      };
      table_.addColumn(pathColumn);

      selectionModel_ = new MultiSelectionModel<StatusAndPath>(
            new ProvidesKey<StatusAndPath>()
            {
               @Override
               public Object getKey(StatusAndPath item)
               {
                  return item.getPath();
               }
            });
      table_.setSelectionModel(selectionModel_);

      table_.setSize("100%", "auto");

      initWidget(new ScrollPanel(table_));
   }

   public HandlerRegistration addSelectionChangeHandler(
         SelectionChangeEvent.Handler handler)
   {
      return selectionModel_.addSelectionChangeHandler(handler);
   }

   public void setItems(ArrayList<StatusAndPath> items)
   {
      items_ = items;
      table_.setPageSize(items.size());
      table_.setRowData(items);
   }

   public ArrayList<String> getSelectedPaths()
   {
      SelectionModel<? super StatusAndPath> selectionModel = table_.getSelectionModel();

      ArrayList<String> results = new ArrayList<String>();
      for (StatusAndPath item : items_)
      {
         if (selectionModel.isSelected(item))
            results.add(item.getPath());
      }
      return results;
   }

   private final CellTable<StatusAndPath> table_;
   private ArrayList<StatusAndPath> items_;
   private final MultiSelectionModel<StatusAndPath> selectionModel_;
}
