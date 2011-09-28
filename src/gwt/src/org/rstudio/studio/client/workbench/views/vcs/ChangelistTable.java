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

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.*;
import org.rstudio.core.client.cellview.ColumnSortInfo;
import org.rstudio.core.client.cellview.TriStateCheckboxCell;
import org.rstudio.core.client.widget.ProgressPanel;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.views.vcs.events.StageUnstageEvent;
import org.rstudio.studio.client.workbench.views.vcs.events.StageUnstageHandler;

import java.util.ArrayList;
import java.util.Comparator;

public class ChangelistTable extends Composite
{
   public interface CellTableResources extends CellTable.Resources
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
      @Source("ascendingArrow.png")
      ImageResource cellTableSortAscending();

      @Override
      @Source("descendingArrow.png")
      ImageResource cellTableSortDescending();

      @Override
      @Source("ChangelistTable.css")
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
         // TODO: Provide a suitable tooltip value for status
         return "";
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

      dataProvider_ = new ListDataProvider<StatusAndPath>();
      sortHandler_ = new ColumnSortEvent.ListHandler<StatusAndPath>(
            dataProvider_.getList());
      table_.addColumnSortHandler(sortHandler_);

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
      dataProvider_.addDataDisplay(table_);

      configureTable();

      table_.setSize("100%", "auto");

      layout_ = new LayoutPanel();
      ScrollPanel scrollPanel = new ScrollPanel(table_);
      layout_.add(scrollPanel);
      layout_.setWidgetTopBottom(scrollPanel, 0, Unit.PX, 0, Unit.PX);
      layout_.setWidgetLeftRight(scrollPanel, 0, Unit.PX, 0, Unit.PX);
      progressPanel_ = new ProgressPanel();
      progressPanel_.getElement().getStyle().setBackgroundColor("white");
      layout_.add(progressPanel_);
      layout_.setWidgetTopBottom(progressPanel_, 0, Unit.PX, 0, Unit.PX);
      layout_.setWidgetLeftRight(progressPanel_, 0, Unit.PX, 0, Unit.PX);

      setProgress(true);

      initWidget(layout_);
   }

   public void showProgress()
   {
      setProgress(true);
   }

   protected void setProgress(boolean showProgress)
   {
      if (showProgress)
      {
         layout_.setWidgetVisible(progressPanel_, true);
         progressPanel_.beginProgressOperation(300);
      }
      else
      {
         layout_.setWidgetVisible(progressPanel_, false);
         progressPanel_.endProgressOperation();
      }
   }

   private void configureTable()
   {
      final Column<StatusAndPath, Boolean> stagedColumn = new Column<StatusAndPath, Boolean>(
            new TriStateCheckboxCell<StatusAndPath>(selectionModel_))
      {
         @Override
         public Boolean getValue(StatusAndPath object)
         {
            return "??".equals(object.getStatus()) ? Boolean.FALSE :
                   object.getStatus().charAt(1) == ' ' ? Boolean.TRUE :
                   object.getStatus().charAt(0) == ' ' ? Boolean.FALSE :
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
      table_.setColumnWidth(stagedColumn, "45px");


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
      table_.setColumnWidth(statusColumn, "55px");
      sortHandler_.setComparator(statusColumn, new Comparator<StatusAndPath>()
      {
         @Override
         public int compare(StatusAndPath a, StatusAndPath b)
         {
            return a.getStatus().compareTo(b.getStatus());
         }
      });

      TextColumn<StatusAndPath> pathColumn = new TextColumn<StatusAndPath>()
      {
         @Override
         public String getValue(StatusAndPath object)
         {
            return object.getPath();
         }
      };
      pathColumn.setSortable(true);
      sortHandler_.setComparator(pathColumn, new Comparator<StatusAndPath>()
      {
         @Override
         public int compare(StatusAndPath a, StatusAndPath b)
         {
            return a.getPath().compareToIgnoreCase(b.getPath());
         }
      });
      table_.addColumn(pathColumn, "Path");
   }

   public HandlerRegistration addSelectionChangeHandler(
         SelectionChangeEvent.Handler handler)
   {
      return selectionModel_.addSelectionChangeHandler(handler);
   }

   public void setItems(ArrayList<StatusAndPath> items)
   {
      setProgress(false);
      table_.setPageSize(items.size());
      dataProvider_.getList().clear();
      dataProvider_.getList().addAll(items);
      ColumnSortEvent.fire(table_,
                           table_.getColumnSortList());

   }

   public ArrayList<StatusAndPath> getSelectedItems()
   {
      SelectionModel<? super StatusAndPath> selectionModel = table_.getSelectionModel();

      ArrayList<StatusAndPath> results = new ArrayList<StatusAndPath>();
      for (StatusAndPath item : dataProvider_.getList())
      {
         if (selectionModel.isSelected(item))
            results.add(item);
      }
      return results;
   }

   public ArrayList<String> getSelectedPaths()
   {
      SelectionModel<? super StatusAndPath> selectionModel = table_.getSelectionModel();

      ArrayList<String> results = new ArrayList<String>();
      for (StatusAndPath item : dataProvider_.getList())
      {
         if (selectionModel.isSelected(item))
            results.add(item.getPath());
      }
      return results;
   }

   public void setSelectedStatusAndPaths(ArrayList<StatusAndPath> selectedPaths)
   {
      for (StatusAndPath path : selectedPaths)
         selectionModel_.setSelected(path, true);
   }

   public ArrayList<String> getSelectedDiscardablePaths()
   {
      SelectionModel<? super StatusAndPath> selectionModel = table_.getSelectionModel();

      ArrayList<String> results = new ArrayList<String>();
      for (StatusAndPath item : dataProvider_.getList())
      {
         if (selectionModel.isSelected(item) && item.isDiscardable())
            results.add(item.getPath());
      }
      return results;
   }

   public HandlerRegistration addStageUnstageHandler(StageUnstageHandler handler)
   {
      return addHandler(handler, StageUnstageEvent.TYPE);
   }

   public void setSortOrder(JsArray<ColumnSortInfo> sortInfoArray)
   {
      ColumnSortInfo.setSortList(table_, sortInfoArray);
   }

   public JsArray<ColumnSortInfo> getSortOrder()
   {
      return ColumnSortInfo.getSortList(table_);
   }

   public int getSortOrderHashCode()
   {
      return table_.getColumnSortList().hashCode();
   }

   public ArrayList<StatusAndPath> getSelectedStatusAndPaths()
   {
      ArrayList<StatusAndPath> results = new ArrayList<StatusAndPath>();
      for (StatusAndPath path : dataProvider_.getList())
         if (table_.getSelectionModel().isSelected(path))
            results.add(path);
      return results;
   }

   private final CellTable<StatusAndPath> table_;
   private final MultiSelectionModel<StatusAndPath> selectionModel_;
   private final ColumnSortEvent.ListHandler<StatusAndPath> sortHandler_;
   private final ListDataProvider<StatusAndPath> dataProvider_;
   private final ProgressPanel progressPanel_;
   private LayoutPanel layout_;
   private static final CellTableResources resources_ = GWT.<CellTableResources>create(CellTableResources.class);
}
