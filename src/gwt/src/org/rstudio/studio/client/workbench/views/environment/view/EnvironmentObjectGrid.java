package org.rstudio.studio.client.workbench.views.environment.view;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
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
      columns_.add(new Column<RObjectEntry, String>(
              new ClickableTextCell())
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getName();
                  }
              });
      columns_.add(new Column<RObjectEntry, String>(
              new ClickableTextCell())
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getType();
                  }
              });
      columns_.add(new Column<RObjectEntry, String>(
              new ClickableTextCell())
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return (new Integer(object.rObject.getLength())).toString();
                  }
              });
      columns_.add(new Column<RObjectEntry, String>(
              new ClickableTextCell())
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return StringUtil.formatFileSize(object.rObject.getSize());
                  }
              });
      columns_.add(new Column<RObjectEntry, String>(
              new ClickableTextCell())
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
         row.startTD().className(style_.objectGridHeader()).text("Name").endTD();
         row.startTD().className(style_.objectGridHeader()).text("Type").endTD();
         row.startTD().className(style_.objectGridHeader()).text("Length").endTD();
         row.startTD().className(style_.objectGridHeader()).text("Size").endTD();
         row.startTD().className(style_.objectGridHeader()).text("Value").endTD();
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
         TableRowBuilder row = startRow();

         if (!rowValue.visible)
            return;

         for (int i = 0; i < columns_.size(); i++)
         {
            TableCellBuilder nameCol = row.startTD();
            nameCol.className(style_.objectGridColumn());
            renderCell(nameCol, createContext(i), columns_.get(i), rowValue);
            nameCol.endTD();
         }
         
         row.end();
      }
   }
   
   private ArrayList<Column<RObjectEntry, String>> columns_ = 
         new ArrayList<Column<RObjectEntry, String>>();
   private Style style_;
}
