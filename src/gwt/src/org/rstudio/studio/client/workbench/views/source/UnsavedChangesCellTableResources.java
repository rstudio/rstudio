/*
 * PackagesCellTableResources.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source;


import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;

public interface UnsavedChangesCellTableResources extends CellTable.Resources 
{
   static UnsavedChangesCellTableResources INSTANCE = 
      (UnsavedChangesCellTableResources)GWT.create(UnsavedChangesCellTableResources.class) ;

   interface Style extends CellTable.Style
   {
   }
  
   @Source("UnsavedChangesCellTableStyle.css")
   Style cellTableStyle();
}
