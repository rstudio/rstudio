package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.ProvidesKey;

// this class extends GWT's DataGrid with a single method that gives us access
// to the scrolling panel used by the grid, which we need in order to
// manipulate the scroll position directly (e.g. to save and restore it)
public class ScrollingDataGrid<T> extends DataGrid<T>
{
   public ScrollingDataGrid(int pageSize, ProvidesKey<T> keyProvider)
   {
      super(pageSize, keyProvider);
   }

   public ScrollPanel getScrollPanel() {
      HeaderPanel header = (HeaderPanel) getWidget();
      return (ScrollPanel) header.getContentWidget();
   }
}
