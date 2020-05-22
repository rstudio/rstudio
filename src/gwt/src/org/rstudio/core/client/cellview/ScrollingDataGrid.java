/*
 * ScrollingDataGrid.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

package org.rstudio.core.client.cellview;

import org.rstudio.core.client.widget.RStudioDataGrid;

import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.view.client.ProvidesKey;

// this class extends GWT's DataGrid with a single method that gives us access
// to the scrolling panel used by the grid, which we need in order to
// manipulate the scroll position directly (e.g. to save and restore it)
public class ScrollingDataGrid<T> extends RStudioDataGrid<T>
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
