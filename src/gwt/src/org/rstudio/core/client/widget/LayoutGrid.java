/*
 * LayoutGrid.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.Grid;

/**
 * Use in place of <code>Grid</code> if for layout only. Use <code>Grid</code>
 * for showing tabular data.
 */
public class LayoutGrid extends Grid
{
   public LayoutGrid()
   {
      super();
      Roles.getPresentationRole().set(getElement());
   }

   public LayoutGrid(int rows, int columns)
   {
      super(rows, columns);
      Roles.getPresentationRole().set(getElement());
   }
}
