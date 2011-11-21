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
package org.rstudio.studio.client.workbench.views.vcs.common;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.*;
import org.rstudio.core.client.cellview.ColumnSortInfo;
import org.rstudio.core.client.cellview.TriStateCheckboxCell;
import org.rstudio.core.client.theme.RStudioCellTableStyle;
import org.rstudio.core.client.widget.MultiSelectCellTable;
import org.rstudio.core.client.widget.ProgressPanel;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.views.vcs.common.events.StageUnstageEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.StageUnstageHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class ChangelistTable extends Composite
   implements HasKeyDownHandlers, HasClickHandlers, HasMouseDownHandlers
{
   public interface ChangelistTableCellTableResources extends CellTable.Resources
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
      @Source({RStudioCellTableStyle.RSTUDIO_DEFAULT_CSS,
               "ChangelistTableCellTableStyle.css"})
      ChangelistTableCellTableStyle cellTableStyle();
   }

   protected interface ChangelistTableCellTableStyle extends CellTable.Style
   {
      String status();
   }

   /**
    * The whole point of this subclass is to force CellTable to update its
    * cellIsEditing private member more often. This is to work around a bug
    * where the ChangelistTable can get into a state where mouse clicks don't
    * change the selection, because TriStateCheckboxCell gets a mouseover event
    * but not a matching mouseout.
    */
   protected static class NotEditingTextCell extends TextCell
   {
      public NotEditingTextCell()
      {
         super();
         init();
      }

      public NotEditingTextCell(SafeHtmlRenderer<String> renderer)
      {
         super(renderer);
         init();
      }

      private void init()
      {
         Set<String> inheritedEvents = getConsumedEvents();
         if (inheritedEvents != null)
            consumedEvents_.addAll(inheritedEvents);
         consumedEvents_.add("mouseover");
      }

      @Override
      public Set<String> getConsumedEvents()
      {
         return consumedEvents_;
      }

      @Override
      public boolean isEditing(Context context, Element parent, String value)
      {
         return false;
      }

      private Set<String> consumedEvents_ = new HashSet<String>();
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
      table_ = new MultiSelectCellTable<StatusAndPath>(100, resources_);

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

   public void setSelectFirstItemByDefault(boolean selectFirstItemByDefault)
   {
      selectFirstItemByDefault_ = selectFirstItemByDefault;
   }

   public void toggleStaged(boolean moveSelection)
   {
      ArrayList<StatusAndPath> items = getSelectedItems();
      if (items.size() > 0)
      {
         boolean unstage = items.get(0).getStatus().charAt(1) == ' ';
         fireEvent(new StageUnstageEvent(unstage, items));

         if (moveSelection)
         {
            moveSelectionDown();
         }
      }
   }

   public void moveSelectionDown()
   {
      if (getSelectedItems().size() == 1)
         {
            table_.moveSelection(false, false);
         }
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
      table_.setColumnWidth(stagedColumn, "46px");


      Column<StatusAndPath, String> statusColumn = new Column<StatusAndPath, String>(
            new NotEditingTextCell(new StatusRenderer()))
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
      table_.setColumnWidth(statusColumn, "56px");
      sortHandler_.setComparator(statusColumn, new Comparator<StatusAndPath>()
      {
         @Override
         public int compare(StatusAndPath a, StatusAndPath b)
         {
            return a.getStatus().compareTo(b.getStatus());
         }
      });

      Column<StatusAndPath, String> pathColumn = new Column<StatusAndPath, String>(
            new NotEditingTextCell())
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
         private String[] splitDirAndName(String path)
         {
            int index = path.lastIndexOf("/");
            if (index < 0)
               index = path.lastIndexOf("\\");
            if (index < 0)
               return new String[] { "", path };
            else
               return new String[] { path.substring(0, index),
                                     path.substring(index + 1) };
         }

         @Override
         public int compare(StatusAndPath a, StatusAndPath b)
         {
            String[] splitA = splitDirAndName(a.getPath());
            String[] splitB = splitDirAndName(b.getPath());
            int result = splitA[0].compareTo(splitB[0]);
            if (result == 0)
               result = splitA[1].compareTo(splitB[1]);
            return result;
         }
      });
      table_.addColumn(pathColumn, "Path");

      table_.getColumnSortList().push(pathColumn);
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

      if (selectFirstItemByDefault_)
      {
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               if (table_.getVisibleItemCount() > 0
                   && selectionModel_.getSelectedSet().isEmpty())
               {
                  selectionModel_.setSelected(table_.getVisibleItem(0), true);
               }
            }
         });
      }
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
      selectionModel_.clear();
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

   public void selectNextUnselectedItem()
   {
      boolean selectNext = false;
      for (StatusAndPath path : table_.getVisibleItems())
      {
         if (selectionModel_.isSelected(path))
            selectNext = true;
         else if (selectNext)
         {
            ArrayList<StatusAndPath> selection = new ArrayList<StatusAndPath>();
            selection.add(path);
            setSelectedStatusAndPaths(selection);
            return;
         }
      }
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

   @Override
   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return table_.addKeyDownHandler(handler);
   }

   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return table_.addClickHandler(handler);
   }

   @Override
   public HandlerRegistration addMouseDownHandler(MouseDownHandler handler)
   {
      return table_.addMouseDownHandler(handler);
   }

   public void focus()
   {
      table_.setFocus(true);
   }

   private final MultiSelectCellTable<StatusAndPath> table_;
   private final MultiSelectionModel<StatusAndPath> selectionModel_;
   private final ColumnSortEvent.ListHandler<StatusAndPath> sortHandler_;
   private final ListDataProvider<StatusAndPath> dataProvider_;
   private final ProgressPanel progressPanel_;
   private LayoutPanel layout_;
   private boolean selectFirstItemByDefault_;
   private static final ChangelistTableCellTableResources resources_ = GWT.<ChangelistTableCellTableResources>create(ChangelistTableCellTableResources.class);
}
