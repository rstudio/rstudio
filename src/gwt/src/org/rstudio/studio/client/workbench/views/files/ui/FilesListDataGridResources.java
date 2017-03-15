/*
 * FilesListDataGridResources.java
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

package org.rstudio.studio.client.workbench.views.files.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.user.cellview.client.DataGrid;
import org.rstudio.core.client.theme.RStudioDataGridStyle;

public interface FilesListDataGridResources extends DataGrid.Resources 
{
   static FilesListDataGridResources INSTANCE =
    (FilesListDataGridResources)GWT.create(FilesListDataGridResources.class);

   @Source("ascendingArrow_2x.png")
   @ImageOptions(flipRtl = true)
   ImageResource cellTableSortAscending();

   /**
    * Icon used when a column is sorted in descending order.
    */
   @Source("descendingArrow_2x.png")
   @ImageOptions(flipRtl = true)
   ImageResource cellTableSortDescending();
   
   interface FilesListDataGridStyle extends DataGrid.Style
   {
   }
   
   @Source({RStudioDataGridStyle.RSTUDIO_DEFAULT_CSS, "FilesListDataGridStyle.css"})
   FilesListDataGridStyle dataGridStyle();
}
