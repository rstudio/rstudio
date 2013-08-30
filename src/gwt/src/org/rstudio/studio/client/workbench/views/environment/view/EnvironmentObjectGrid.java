package org.rstudio.studio.client.workbench.views.environment.view;

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
              });
   }
}
