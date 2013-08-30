package org.rstudio.studio.client.workbench.views.environment.view;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.common.FilePathUtils;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.user.cellview.client.Column;

public class EnvironmentObjectGrid extends EnvironmentObjectDisplay
{
   public EnvironmentObjectGrid(EnvironmentObjectDisplay.Host host,
                                EnvironmentObjectsObserver observer)
   {
      super(host, observer);
      createColumns();
   }

   private void createColumns()
   {
      addColumn(new Column<RObjectEntry, String>(
              new ClickableTextCell())
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getName();
                  }
              }, "Name");
      addColumn(new Column<RObjectEntry, String>(
              new ClickableTextCell())
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getType();
                  }
              }, "Type");
      addColumn(new Column<RObjectEntry, String>(
              new ClickableTextCell())
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return (new Integer(object.rObject.getLength())).toString();
                  }
              }, "Length");
      addColumn(new Column<RObjectEntry, String>(
              new ClickableTextCell())
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return StringUtil.formatFileSize(object.rObject.getSize());
                  }
              }, "Size");
      addColumn(new Column<RObjectEntry, String>(
              new ClickableTextCell())
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getValue();
                  }
              }, "Value");
   }
}
