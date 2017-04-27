/*
 * EnvironmentObjectGrid.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.environment.view;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.workbench.views.environment.view.RObjectEntry.Categories;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.AbstractCellTableBuilder;
import com.google.gwt.user.cellview.client.AbstractHeaderOrFooterBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionModel;

public class EnvironmentObjectGrid extends EnvironmentObjectDisplay
{
   public interface Style extends CssResource
   {
      String objectGridColumn();
      String objectGridHeader();
      String checkColumn();
      String objectGrid();
      String valueColumn();
      String decoratedValueCol();
   }

   public interface Resources extends ClientBundle
   {
      @Source("EnvironmentObjectGrid.css")
      Style style();
   }

   public EnvironmentObjectGrid(EnvironmentObjectDisplay.Host host,
                                EnvironmentObjectsObserver observer,
                                String environmentName)
   {
      super(host, observer, environmentName);
      style_ = ((Resources)GWT.create(Resources.class)).style();
      style_.ensureInjected();
      selection_ = new MultiSelectionModel<RObjectEntry>(
              RObjectEntry.KEY_PROVIDER);
      
      createColumns();
      setTableBuilder(new EnvironmentObjectGridBuilder(this));
      setHeaderBuilder(new GridHeaderBuilder(this, false));
      setSkipRowHoverCheck(true);
      setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
      setSelectionModel(selection_, 
         DefaultSelectionEventManager.<RObjectEntry>createCheckboxManager(0));
      addStyleName(style_.objectGrid());
   }

   // Returns the objects that should be considered selected. 
   // - If one or more objects are manually selected, that set of objects is 
   //   returned. 
   // - If no objects are manually selected but the list is filtered, the
   //   the objects that match the filter are returned.
   // - If no selection or filter is present, an empty list is returned
   //   (generally this causes operations to act on the whole list) 
   @Override
   public List<String> getSelectedObjects()
   {
      boolean hasFilter = !host_.getFilterText().isEmpty();
      ArrayList<String> selectedObjectNames = new ArrayList<String>();
      ArrayList<String> filteredObjectNames = new ArrayList<String>();
      List<RObjectEntry> objects = getVisibleItems();
      for (RObjectEntry object: objects)
      {
         if (object.visible)
         {
            if (hasFilter)
            {
               filteredObjectNames.add(object.rObject.getName());
            }
            if (selection_.isSelected(object))
            {
               selectedObjectNames.add(object.rObject.getName());
            }
         }
      }
      return selectedObjectNames.size() == 0 ? filteredObjectNames :
                                               selectedObjectNames;
   }

   @Override
   public void clearSelection()
   {
      setSelectAll(false);
      redrawHeaders();
   }
   
   @Override
   public void setEnvironmentName(String environmentName)
   {
      // When the environment changes, we need to redraw the headers to 
      // (possibly) adjust for the presence or absence of the selection
      // column.
      super.setEnvironmentName(environmentName);
      if (columns_.size() > 0) 
      {
         columns_.get(0).setWidth(selectionEnabled() ? 20 : 25);
      }
      setColumnWidths();
   }
   
   // Private methods ---------------------------------------------------------

   private void createColumns()
   {
      checkColumn_ = new Column<RObjectEntry, Boolean>(
            new CheckboxCell(false, false))
            {
               @Override
               public Boolean getValue(RObjectEntry value)
               {
                  return selection_.isSelected(value); 
               }
            };
      addColumn(checkColumn_);
      checkHeader_ = new Header<Boolean>(new CheckboxCell())
      {
         @Override
         public Boolean getValue()
         {
            return selectAll_;
         }
      };
      checkHeader_.setUpdater(new ValueUpdater<Boolean>()
      {
         @Override
         public void update(Boolean value)
         {
            if (selectAll_ != value)
            {
               setSelectAll(value);
            }
         }
      });

      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(filterRenderer_), "Name", 
              selectionEnabled() ? 20 : 25,
              ObjectGridColumn.COLUMN_NAME, host_)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getName();
                  }
              });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(), "Type", 15, 
              ObjectGridColumn.COLUMN_TYPE, host_)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getType();
                  }
              });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(), "Length", 10, 
              ObjectGridColumn.COLUMN_LENGTH, host_)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return (new Integer(object.rObject.getLength())).toString();
                  }
              });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(), "Size", 12, 
              ObjectGridColumn.COLUMN_SIZE, host_)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return StringUtil.formatFileSize(object.rObject.getSize());
                  }
              });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(filterRenderer_), 
              "Value", 38, 
              ObjectGridColumn.COLUMN_VALUE, host_)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.getDisplayValue();
                  }
              });
      for (ObjectGridColumn column: columns_)
      {
         if (column.getType() == ObjectGridColumn.COLUMN_VALUE)
         {
            attachClickToInvoke(column);
         }
         addColumn(column);
      }
      setColumnWidths();
   }
   
   private void setSelectAll(boolean selected)
   {
      List<RObjectEntry> objects = getVisibleItems();
      for (RObjectEntry object: objects)
      {
         if (object.visible)
         {
            selection_.setSelected(object, selected);
         }
      }
      selectAll_ = selected;
   }

   private class GridHeaderBuilder 
           extends AbstractHeaderOrFooterBuilder<RObjectEntry>
   {
      public GridHeaderBuilder(AbstractCellTable<RObjectEntry> table,
                               boolean isFooter)
      {
         super(table, isFooter);
         setSortIconStartOfLine(false);
      }

      @Override
      protected boolean buildHeaderOrFooterImpl()
      {
         // Render the "select all" checkbox header cell
         TableRowBuilder row = startRow();
         
         if (selectionEnabled())
         {
            TableCellBuilder selectAll = row.startTH();
            selectAll.className(style_.objectGridHeader() + " " +
                                "rstudio-themes-background" + " " +
                                style_.checkColumn());
            renderHeader(selectAll, new Cell.Context(0, 0, null), checkHeader_);
            selectAll.end();
         }
   
         // Render a header for each column
         for (int i = 0; i < columns_.size(); i++)
         {
            String sortClassName = i == host_.getSortColumn() ? 
              (host_.getAscendingSort() ? "dataGridSortedHeaderAscending" : "dataGridSortedHeaderDescending") : 
              "";

            ObjectGridColumn col = columns_.get(i);
            TableCellBuilder cell = row.startTH();
            cell.className(style_.objectGridHeader() + " " +
                           "rstudio-themes-background" + " " +
                           sortClassName);
            Cell.Context context = new Cell.Context(0, i, null);
            renderSortableHeader(cell, context, col.getHeader(), 
                  i == host_.getSortColumn(), 
                  host_.getAscendingSort());
            cell.endTH();
         }
         row.end();
         return true;
      }
   }
   
   private void setColumnWidths()
   {
      int start = 0;
      if (selectionEnabled())
      {
         setColumnWidth(start++, "5%");
      }
      else
      {
         // Clear the width of the last column (it's going to go away entirely 
         // if we're dropping the selection column). 
         clearColumnWidth(columns_.size());
      }
      for (int i = 0; i < columns_.size(); i++)
      {
         setColumnWidth(
               start + i,
               new Integer(columns_.get(i).getWidth()).toString() + "%");
      }
   }

   // builds individual rows of the object table
   private class EnvironmentObjectGridBuilder
           extends AbstractCellTableBuilder<RObjectEntry>
   {

      public EnvironmentObjectGridBuilder(
            AbstractCellTable<RObjectEntry> cellTable)
      {
         super(cellTable);
      }

      @Override
      protected void buildRowImpl(RObjectEntry rowValue, int absRowIndex)
      {
         if (!rowValue.visible)
            return;

         TableRowBuilder row = startRow();

         if (selectionEnabled())
         {
            TableCellBuilder check = row.startTD();
            check.className(style_.checkColumn());
            check.style().width(5, Unit.PCT);
            renderCell(check, createContext(0), checkColumn_, rowValue);
            check.endTD();
         }

         for (int i = 0; i < columns_.size(); i++)
         {
            ObjectGridColumn col = columns_.get(i);
            TableCellBuilder td = row.startTD();
            String className = style_.objectGridColumn();
            if (col.getType() == ObjectGridColumn.COLUMN_VALUE)
            {
               className += " " + style_.valueColumn();
               boolean isClickable =
                     host_.enableClickableObjects() &&
                     rowValue.getCategory() != Categories.Value;
               if (isClickable)
               {
                  className += " " + style_.decoratedValueCol();
                  
                  switch (rowValue.getCategory())
                  {
                  case Categories.Function:
                     className += " " + ThemeStyles.INSTANCE.environmentFunctionCol();
                     break;
                  case Categories.Data:
                     if (rowValue.isHierarchical())
                        className += " " + ThemeStyles.INSTANCE.environmentHierarchicalCol();
                     else
                        className += " " + ThemeStyles.INSTANCE.environmentDataFrameCol();
                     break;
                  default:
                        // no styling
                  }
                  
                  className += " " + ThemeStyles.INSTANCE.handCursor();
               }
               if (rowValue.isPromise())
               {
                  className += " " + environmentStyle_.unevaluatedPromise();
               }
               td.title(rowValue.getDisplayValue());
            }
            if (col.getType() == ObjectGridColumn.COLUMN_NAME)
            {
               td.title(rowValue.rObject.getName());
            }
            td.className(className);
            renderCell(td, createContext(i+1), col, rowValue);
            td.endTD();
         }
         
         row.end();
      }
   }
   
   private Column<RObjectEntry, Boolean> checkColumn_;
   private Header<Boolean> checkHeader_;
   private ArrayList<ObjectGridColumn> columns_ = 
         new ArrayList<ObjectGridColumn>();
   private Style style_;
   private SelectionModel<RObjectEntry> selection_;
   private boolean selectAll_ = false;
}
