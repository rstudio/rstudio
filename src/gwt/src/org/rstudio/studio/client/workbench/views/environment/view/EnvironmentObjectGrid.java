package org.rstudio.studio.client.workbench.views.environment.view;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;

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

public class EnvironmentObjectGrid extends EnvironmentObjectDisplay
{
   public interface Style extends CssResource
   {
      String objectGridColumn();
      String objectGridHeader();
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
      createColumns();
      setTableBuilder(new EnvironmentObjectGridBuilder(this));
      setHeaderBuilder(new GridHeaderBuilder(this, false));
   }

   private void createColumns()
   {
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(filterRenderer_), "Name", 20, 
              ObjectGridColumn.COLUMN_NAME)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getName();
                  }
              });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(), "Type", 15, 
              ObjectGridColumn.COLUMN_TYPE)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getType();
                  }
              });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(), "Length", 10, 
              ObjectGridColumn.COLUMN_LENGTH)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return (new Integer(object.rObject.getLength())).toString();
                  }
              });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(), "Size", 15, 
              ObjectGridColumn.COLUMN_SIZE)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return StringUtil.formatFileSize(object.rObject.getSize());
                  }
              });
      columns_.add(new ObjectGridColumn(
              new ClickableTextCell(filterRenderer_), "Value", 40, 
              ObjectGridColumn.COLUMN_VALUE)
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getValue();
                  }
              });
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
      }

      @Override
      protected boolean buildHeaderOrFooterImpl()
      {
         TableRowBuilder row = startRow();
         for (ObjectGridColumn col: columns_)
         {
            TableCellBuilder cell = row.startTD();
            cell.className(style_.objectGridHeader());
            cell.style().width(col.getWidth(), Unit.PCT);
            cell.text(col.getName());
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

         for (int i = 0; i < columns_.size(); i++)
         {
            ObjectGridColumn col = columns_.get(i);
            TableCellBuilder td = row.startTD();
            td.className(style_.objectGridColumn());
            td.style().width(col.getWidth(), Unit.PCT);
            renderCell(td, createContext(i), col, rowValue);
            td.endTD();
         }
         
         row.end();
      }
   }
   
   private ArrayList<ObjectGridColumn> columns_ = 
         new ArrayList<ObjectGridColumn>();
   private Style style_;
}
