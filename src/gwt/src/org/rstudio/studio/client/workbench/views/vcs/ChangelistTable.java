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

import com.google.gwt.cell.client.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.studio.client.common.vcs.StatusAndPath;

import java.util.ArrayList;

public class ChangelistTable extends Composite
{
   protected interface CellTableResources extends CellTable.Resources
   {
      ImageResource statusAdded();
      ImageResource statusDeleted();
      ImageResource statusModified();
      ImageResource statusNone();
      ImageResource statusCopied();
      ImageResource statusUntracked();
      ImageResource statusUnmerged();
      ImageResource statusRenamed();

      @Override
      @Source("VCSPaneCellTableStyle.css")
      Style cellTableStyle();
   }

   protected interface Style extends CellTable.Style
   {
      String status();
   }

   private class StatusRenderer implements SafeHtmlRenderer<String>
   {

      @Override
      public SafeHtml render(String str)
      {
         if (str.length() != 2)
            return null;

         ImageResource indexImg = imgForStatus(str.charAt(0));
         ImageResource treeImg = imgForStatus(str.charAt(1));

         SafeHtmlBuilder builder = new SafeHtmlBuilder();
         builder.append(SafeHtmlUtils.fromTrustedString(
               "<span " +
               "class=\"" + resources_.cellTableStyle().status() + "\" " +
               "title=\"" +
               SafeHtmlUtils.htmlEscape(descForStatus(str)) +
               "\">"));

         builder.append(SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(indexImg).getHTML()));
         builder.append(SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(treeImg).getHTML()));

         builder.appendHtmlConstant("</span>");

         return builder.toSafeHtml();
      }

      private String descForStatus(String str)
      {
         return "hey that's cool";
      }

      private ImageResource imgForStatus(char c)
      {
         switch (c)
         {
            case 'A':
               return resources_.statusAdded();
            case 'M':
               return resources_.statusModified();
            case 'D':
               return resources_.statusDeleted();
            case 'R':
               return resources_.statusRenamed();
            case 'C':
               return resources_.statusCopied();
            case '?':
               return resources_.statusUntracked();
            case 'U':
               return resources_.statusUnmerged();
            case ' ':
               return resources_.statusNone();
            default:
               return resources_.statusNone();
         }
      }

      @Override
      public void render(String str, SafeHtmlBuilder builder)
      {
         SafeHtml safeHtml = render(str);
         if (safeHtml != null)
            builder.append(safeHtml);
      }
   }

   public ChangelistTable()
   {
      table_ = new CellTable<StatusAndPath>(
            100, resources_);

      configureTable();

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

   private void configureTable()
   {
      Column<StatusAndPath, Boolean> stagedColumn = new Column<StatusAndPath, Boolean>(new AbstractCell<Boolean>()
      {
         @Override
         public void render(Context context, Boolean value, SafeHtmlBuilder sb)
         {
         }
      })
      {
         @Override
         public Boolean getValue(StatusAndPath object)
         {
            return object.getStatus().charAt(1) == ' ';
         }
      };

      stagedColumn.setSortable(true);
      stagedColumn.setHorizontalAlignment(Column.ALIGN_CENTER);
      table_.addColumn(stagedColumn, "Staged");

      Column<StatusAndPath, String> statusColumn = new Column<StatusAndPath, String>(
            new TextCell(new StatusRenderer()))
      {
         @Override
         public String getValue(StatusAndPath object)
         {
            return object.getStatus();
         }
      };
      statusColumn.setSortable(true);
      statusColumn.setHorizontalAlignment(Column.ALIGN_CENTER);
      table_.addColumn(statusColumn, "Status");

      TextColumn<StatusAndPath> pathColumn = new TextColumn<StatusAndPath>()
      {
         @Override
         public String getValue(StatusAndPath object)
         {
            return object.getPath();
         }
      };
      pathColumn.setSortable(true);
      table_.addColumn(pathColumn, "Path");
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
   private static final CellTableResources resources_ = GWT.<CellTableResources>create(CellTableResources.class);
}
