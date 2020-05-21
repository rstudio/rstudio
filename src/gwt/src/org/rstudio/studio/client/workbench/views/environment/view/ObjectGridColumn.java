/*
 * ObjectGridColumn.java
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
package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;

public abstract class ObjectGridColumn extends Column<RObjectEntry, String>
{
   public ObjectGridColumn(Cell<String> cell, 
                           String columnName, 
                           int columnWidth,
                           int columnType,
                           final EnvironmentObjectDisplay.Host host)
   {
      super(cell);
      columnName_ = columnName;
      columnWidth_ = columnWidth;
      columnType_ = columnType;
      setSortable(true);
      header_ = new Header<String>(new ClickableTextCell())
      {
         @Override
         public String getValue()
         {
            return columnName_;
         }
      };
      header_.setUpdater(new ValueUpdater<String>()
      {
         @Override
         public void update(String value)
         {
            if (host.getSortColumn() == columnType_)
            {
               host.toggleAscendingSort();
            }
            else
            {
               host.setSortColumn(columnType_);
            }
         }
      });
   }
   
   public String getName()
   {
      return columnName_;
   }
   
   public int getWidth()
   {
      return columnWidth_;
   }
   
   public void setWidth(int width)
   {
      columnWidth_ = width;
   }
   
   public int getType()
   {
      return columnType_;
   }
   
   public Header<String> getHeader()
   {
      return header_;
   }
   
   public static final int COLUMN_NAME = 0;
   public static final int COLUMN_TYPE = 1;
   public static final int COLUMN_LENGTH = 2;
   public static final int COLUMN_SIZE = 3;
   public static final int COLUMN_VALUE = 4;
   
   private String columnName_;
   private int columnWidth_;
   private int columnType_;
   private Header<String> header_;
}
