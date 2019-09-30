/*
 * GridCellTextColumn.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.Column;

/**
 * A column that displays its contents with a {@link TextCell} and does not make
 * use of view data. Unlike standard TextColumn, cells accept focus via keyboard interface
 * as part of the aria gridcell keyboard interface.
 *
 * @param <T> the row type
 */
public abstract class GridCellTextColumn<T> extends Column<T, String>
{
   private static class GridTextCell extends ClickableTextCell
   {

      public GridTextCell()
      {

      }
   }

   /**
    * Construct a new GridCellTextColumn.
    */
   public GridCellTextColumn() {
      super(new GridTextCell());

   }

}
