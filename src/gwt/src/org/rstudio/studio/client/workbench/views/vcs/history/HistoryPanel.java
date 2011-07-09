/*
 * HistoryPanel.java
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
package org.rstudio.studio.client.workbench.views.vcs.history;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.studio.client.workbench.views.vcs.history.HistoryPresenter.Display;

import java.util.ArrayList;

public class HistoryPanel extends Composite
   implements Display
{
   public HistoryPanel()
   {
      commitTable_ = new CellTable<CommitInfo>();

      addCommitColumns();

      initWidget(commitTable_);
   }

   private void addCommitColumns()
   {
      TextColumn<CommitInfo> idCol = new TextColumn<CommitInfo>()
      {
         @Override
         public String getValue(CommitInfo object)
         {
            return object.getId();
         }
      };
      commitTable_.addColumn(idCol);

      TextColumn<CommitInfo> subjectCol = new TextColumn<CommitInfo>()
      {
         @Override
         public String getValue(CommitInfo object)
         {
            return object.getSubject();
         }
      };
      commitTable_.addColumn(subjectCol);

      TextColumn<CommitInfo> authorCol = new TextColumn<CommitInfo>()
      {
         @Override
         public String getValue(CommitInfo object)
         {
            return object.getAuthor();
         }
      };
      commitTable_.addColumn(authorCol);

      TextColumn<CommitInfo> dateCol = new TextColumn<CommitInfo>()
      {
         @Override
         public String getValue(CommitInfo object)
         {
            return DateTimeFormat.getFormat(
                  PredefinedFormat.DATE_SHORT).format(object.getDate());
         }
      };
      commitTable_.addColumn(dateCol);
   }

   @Override
   public void setData(ArrayList<CommitInfo> commits)
   {
      commitTable_.setPageSize(commits.size());
      commitTable_.setRowData(commits);
   }

   @Override
   public void showModal()
   {
      ModalDialogBase modal = new ModalDialogBase()
      {
         @Override
         protected Widget createMainWidget()
         {
            this.addCancelButton();

            ScrollPanel scroller =  new ScrollPanel(HistoryPanel.this);
            scroller.setSize("800px", "600px");
            return scroller;
         }
      };

      modal.showModal();
   }

   private final CellTable<CommitInfo> commitTable_;
}
