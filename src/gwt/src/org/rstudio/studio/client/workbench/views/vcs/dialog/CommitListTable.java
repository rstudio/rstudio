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

import com.google.gwt.cell.client.AbstractSafeHtmlCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.workbench.views.vcs.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPanel.Styles;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter.CommitListDisplay;

import java.util.ArrayList;

public class CommitListTable extends CellTable<CommitInfo>
      implements CommitListDisplay
{
   private class SubjectColumn extends Column<CommitInfo, CommitInfo>
   {
      private SubjectColumn()
      {
         super(new AbstractSafeHtmlCell<CommitInfo>(new SubjectRenderer()) {
            @Override
            protected void render(Context context,
                                  SafeHtml data,
                                  SafeHtmlBuilder sb)
            {
               if (data != null)
                  sb.append(data);
            }
         });
      }

      @Override
      public CommitInfo getValue(CommitInfo object)
      {
         return object;
      }
   }
   private class SubjectRenderer implements SafeHtmlRenderer<CommitInfo>
   {
      @Override
      public SafeHtml render(CommitInfo commit)
      {
         SafeHtmlBuilder builder = new SafeHtmlBuilder();

         for (String ref : JsUtil.asIterable(commit.getRefs()))
         {
            String style = styles_.ref();
            if (ref.startsWith("refs/heads/"))
            {
               ref = ref.substring("refs/heads/".length());
               style += " " + styles_.branch();
            }
            else if (ref.startsWith("refs/remotes/"))
            {
               ref = ref.substring("refs/remotes/".length());
               style += " " + styles_.remote();
            }
            else if (ref.equals("HEAD"))
            {
               style += " " + styles_.head();
            }

            SafeHtmlUtil.appendSpan(builder, style, ref);
         }
         for (String tag : JsUtil.asIterable(commit.getTags()))
         {
            if (tag.startsWith("refs/tags/"))
               tag = tag.substring("refs/tags/".length());
            SafeHtmlUtil.appendSpan(builder, styles_.tag(), tag);
         }

         builder.appendEscaped(commit.getSubject());

         return builder.toSafeHtml();
      }

      @Override
      public void render(CommitInfo object, SafeHtmlBuilder builder)
      {
         builder.append(render(object));
      }
   }

   public CommitListTable(HistoryPanel.Styles styles)
   {
      super(100,
            GWT.<Resources>create(ChangelistTable.CellTableResources.class));
      styles_ = styles;

      TextColumn<CommitInfo> idCol = new TextColumn<CommitInfo>()
      {
         @Override
         public String getValue(CommitInfo object)
         {
            return object.getId();
         }
      };
      addColumn(idCol, "SHA");

      Column<CommitInfo, CommitInfo> subjectCol = new Column<CommitInfo, CommitInfo>(new AbstractSafeHtmlCell<CommitInfo>(new SubjectRenderer()) {
         @Override
         protected void render(Context context,
                               SafeHtml data,
                               SafeHtmlBuilder sb)
         {
            if (data != null)
               sb.append(data);
         }
      })
      {
         @Override
         public CommitInfo getValue(CommitInfo object)
         {
            return object;
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
   private final Styles styles_;
}
