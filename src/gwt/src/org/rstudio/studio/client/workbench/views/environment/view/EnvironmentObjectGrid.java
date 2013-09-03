package org.rstudio.studio.client.workbench.views.environment.view;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ClickableTextCell;
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
   }

   public interface Resources extends ClientBundle
   {
      @Source("EnvironmentObjectGrid.css")
      Style style();
   }

   public EnvironmentObjectGrid(EnvironmentObjectDisplay.Host host,
                                EnvironmentObjectsObserver observer)
   {
      super(host, observer);
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
   }

   private void createColumns()
   {
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(filterRenderer_), "Name", 20, 
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
              new ClickableTextCell(), "Size", 15, 
              ObjectGridColumn.COLUMN_SIZE, host_)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return StringUtil.formatFileSize(object.rObject.getSize());
                  }
              });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(filterRenderer_), "Value", 35, 
              ObjectGridColumn.COLUMN_VALUE, host_)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getValue();
                  }
              });
      checkColumn_ = new Column<RObjectEntry, Boolean>(
            new CheckboxCell(true, true))
            {
               @Override
               public Boolean getValue(RObjectEntry value)
               {
                  return selection_.isSelected(value); 
               }
            };
      addColumn(checkColumn_);
      for (Column<RObjectEntry, String> column: columns_)
      {
         addColumn(column);
      }
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
         TableRowBuilder row = startRow();
         // Render an empty header cell for the check column
         row.startTD().className(style_.objectGridHeader()).end();

         for (int i = 0; i < columns_.size(); i++)
         {
            ObjectGridColumn col = columns_.get(i);
            TableCellBuilder cell = row.startTD();
            cell.className(style_.objectGridHeader());
            cell.style().width(col.getWidth(), Unit.PCT);
            Cell.Context context = new Cell.Context(0, i, null);
            renderSortableHeader(cell, context, col.getHeader(), 
                  i == host_.getSortColumn(), false);
            cell.endTD();
         }
         row.end();
         return true;
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

         TableCellBuilder check = row.startTD();
         check.className(style_.checkColumn());
         renderCell(check, createContext(0), checkColumn_, rowValue);
         check.endTD();

         for (int i = 0; i < columns_.size(); i++)
         {
            ObjectGridColumn col = columns_.get(i);
            TableCellBuilder td = row.startTD();
            td.className(style_.objectGridColumn());
            td.style().width(col.getWidth(), Unit.PCT);
            renderCell(td, createContext(i+1), col, rowValue);
            td.endTD();
         }
         
         row.end();
      }
   }
   
   private Column<RObjectEntry, Boolean> checkColumn_;
   private ArrayList<ObjectGridColumn> columns_ = 
         new ArrayList<ObjectGridColumn>();
   private Style style_;
   private SelectionModel<RObjectEntry> selection_;
}
