/*
 * LayoutGrid.java
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
package org.rstudio.core.client.widget;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Pair;
import org.rstudio.core.client.StringUtil;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;

/**
 * Use in place of <code>Grid</code> if for layout only. Use <code>Grid</code>
 * for showing tabular data.
 */
public class LayoutGrid extends Grid
{
   public static class TwoColumnLayoutGridBuilder
   {
      public TwoColumnLayoutGridBuilder()
      {
         widgets_ = new ArrayList<>();
      }
      
      public void add(String label, Widget widget)
      {
         widgets_.add(new Pair<>(label, widget));
      }
      
      public LayoutGrid get()
      {
         LayoutGrid grid = new LayoutGrid(widgets_.size(), 2);
         for (int i = 0, n = widgets_.size(); i < n; i++)
         {
            FormLabel label = new FormLabel(
                  StringUtil.ensureColonSuffix(widgets_.get(i).first),
                  widgets_.get(i).second);
            
            grid.setWidget(i, 0, label);
            grid.setWidget(i, 1, widgets_.get(i).second);
         }
         return grid;
      }
      
      private final List<Pair<String, Widget>> widgets_;
      
   }
   
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
