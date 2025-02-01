/*
 * DirectoryContentsWidgetDataGridResources.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.files.filedialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.DataGrid;
import org.rstudio.core.client.theme.RStudioDataGridStyle;

public interface DirectoryContentsWidgetDataGridResources extends DataGrid.Resources 
{
   interface DirectoryContentsWidgetDataGridStyle extends DataGrid.Style
   {
   }
   
   
   @Source({RStudioDataGridStyle.RSTUDIO_DEFAULT_CSS, "DirectoryContentsWidgetDataGridStyle.css"})
   DirectoryContentsWidgetDataGridStyle dataGridStyle();
   
   static DirectoryContentsWidgetDataGridResources INSTANCE =
         GWT.create(DirectoryContentsWidgetDataGridResources.class);

}
