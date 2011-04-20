package org.rstudio.studio.client.workbench.views.packages.ui;

import com.google.gwt.user.cellview.client.CellTable;

public interface PackagesCellTableResources extends CellTable.Resources 
{
   @Source("PackagesCellTableStyle.css")
   CellTable.Style cellTableStyle();    
}
