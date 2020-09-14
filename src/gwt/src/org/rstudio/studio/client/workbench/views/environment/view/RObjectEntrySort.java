/*
 * RObjectEntrySort.java
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

import java.util.Comparator;

class RObjectEntrySort implements Comparator<RObjectEntry>
{
   public RObjectEntrySort()
   {
      sortType_ = SORT_AUTO;
      sortColumn_ = ObjectGridColumn.COLUMN_NAME;
      ascending_ = true;
   }

   public void setSortType(int sortType)
   {
      sortType_ = sortType;
   }

   public void setSortColumn(int sortColumn)
   {
      sortColumn_ = sortColumn;
      // default to descending sort for size (largest objects on top--the most
      // likely desired ordering)
      if (sortColumn == ObjectGridColumn.COLUMN_SIZE)
         ascending_ = false;
      else
         ascending_ = true;
   }

   public int getSortColumn()
   {
      return sortColumn_;
   }

   public boolean getAscending()
   {
      return ascending_;
   }

   public void setAscending(boolean ascending)
   {
      ascending_ = ascending;
   }

   public int compare(RObjectEntry first, RObjectEntry second)
   {
      if (ascending_ || sortType_ == SORT_AUTO)
         return compareAscending(first, second);
      else
         return compareAscending(second, first);
   }

   public int compareAscending(RObjectEntry first, RObjectEntry second)
   {
      int result = 0;
      if (sortType_ == SORT_AUTO)
      {
         result = first.getCategory() - second.getCategory();
         if (result == 0)
         {
            result = localeCompare(first.rObject.getName(),
                                   second.rObject.getName());
         }
      }
      else if (sortType_ == SORT_COLUMN)
      {
         switch (sortColumn_)
         {
         case ObjectGridColumn.COLUMN_NAME:
            result = localeCompare(first.rObject.getName(),
                                   second.rObject.getName());
            break;
         case ObjectGridColumn.COLUMN_TYPE:
            result = localeCompare(first.rObject.getType(),
                                   second.rObject.getType());
            break;
         case ObjectGridColumn.COLUMN_LENGTH:
            result = Long.valueOf(first.rObject.getLength())
                              .compareTo(Long.valueOf(second.rObject.getLength()));
            break;
         case ObjectGridColumn.COLUMN_SIZE:
            result = Long.valueOf(first.rObject.getSize())
                              .compareTo(Long.valueOf(second.rObject.getSize()));
            break;
         case ObjectGridColumn.COLUMN_VALUE:
            result = localeCompare(first.getDisplayValue(),
                                   second.getDisplayValue());
            break;
         }
      }
      return result;
   }

   // Gets sort order of two strings. Coerces from undefined/null values to
   // empty strings.
   private native int localeCompare(String first, String second) /*-{
      firstVal = first ? first : "";
      secondVal = second ? second : "";
      return firstVal.localeCompare(secondVal);
   }-*/;

   public static final int SORT_AUTO = 0;
   public static final int SORT_COLUMN = 1;

   private int sortType_;
   private int sortColumn_;
   private boolean ascending_;
}
