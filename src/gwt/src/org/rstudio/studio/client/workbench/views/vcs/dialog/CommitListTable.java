/*
 * CommitListTable.java
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
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.rstudio.studio.client.workbench.views.vcs.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.CommitListDisplay;

import java.util.ArrayList;

public class CommitListTable extends CellTable<CommitInfo>
      implements CommitListDisplay
{
   public CommitListTable()
   {
      super(100,
            GWT.<Resources>create(ChangelistTable.CellTableResources.class));

      TextColumn<CommitInfo> idCol = new TextColumn<CommitInfo>()
      {
         @Override
         public String getValue(CommitInfo object)
         {
            return object.getId();
         }
      };
      addColumn(idCol, "SHA");

      TextColumn<CommitInfo> subjectCol = new TextColumn<CommitInfo>()
      {
         @Override
         public String getValue(CommitInfo object)
         {
            return object.getSubject();
         }
      };
      addColumn(subjectCol, "Subject");

      TextColumn<CommitInfo> authorCol = new TextColumn<CommitInfo>()
      {
         @Override
         public String getValue(CommitInfo object)
         {
            return object.getAuthor();
         }
      };
      addColumn(authorCol, "Author");

      TextColumn<CommitInfo> dateCol = new TextColumn<CommitInfo>()
      {
         @Override
         public String getValue(CommitInfo object)
         {
            return DateTimeFormat.getFormat(
                  PredefinedFormat.DATE_SHORT).format(object.getDate());
         }
      };
      addColumn(dateCol, "Date");
      setColumnWidth(dateCol, "120px");

      selectionModel_ = new SingleSelectionModel<CommitInfo>();
      setSelectionModel(selectionModel_);
   }

   public void setData(ArrayList<CommitInfo> commits)
   {
      setPageSize(commits.size());
      setRowData(commits);
   }

   public HandlerRegistration addSelectionChangeHandler(SelectionChangeEvent.Handler handler)
   {
      return selectionModel_.addSelectionChangeHandler(handler);
   }

   @Override
   public CommitInfo getSelectedCommit()
   {
      return selectionModel_.getSelectedObject();
   }

   private final SingleSelectionModel<CommitInfo> selectionModel_;
}
