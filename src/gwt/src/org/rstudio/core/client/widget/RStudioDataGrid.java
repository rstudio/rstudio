/*
 * RStudioDataGrid.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

import java.util.List;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.view.client.ProvidesKey;

public class RStudioDataGrid<T> extends DataGrid<T>
{
   public RStudioDataGrid()
   {
      super();
   }
   
   public RStudioDataGrid(int max, DataGrid.Resources res)
   {
      super(max, res);
   }
   
   public RStudioDataGrid(int max, DataGrid.Resources res, ProvidesKey<T> keyProvider)
   {
      super(max, res, keyProvider);
   }
   
   @Override
   public void onAttach()
   {
      super.onAttach();
      
      // None of the below is necessary unless on MacOS since that's the only
      // platform that uses overlay scrollbars.
      if (!BrowseCap.isMacintosh())
         return;
      
      // GWT's DataGrid implementation adds a handful of nodes with aggressive
      // inline styles in the header and footer of the grid. They're designed to
      // help with scrolling, but in newer versions of Chromium, they cause
      // horizontal overlay scrollbars to appear when the grid is created and
      // whenever it's resized. The below tweaks these elements so that they
      // don't have a scrollbar.
      //
      // Typically we'd use CSS here but these elements are buried, have no
      // assigned class, and have all their style attributes applied inline, so
      // instead we scan the attached DOM subtree and change the inline styles.
      Element parent = getElement().getParentElement().getParentElement();
      List<Node> matches = DomUtils.findNodes(parent, 10, true, false, node -> 
      {
         // Ignore text nodes, etc.
         if (node.getNodeType() != Node.ELEMENT_NODE)
            return false;

         com.google.gwt.dom.client.Style style = Element.as(node).getStyle();
         
         // The scroll helpers are hidden, and set to appear behind the grid.
         return style.getZIndex() == "-1" && 
                style.getOverflow() == "scroll" &&
                style.getVisibility() == "hidden";
      });
      
      for (Node match: matches)
      {
         // Don't show scrollbars on these elements
         Element.as(match).getStyle().setOverflow(Overflow.HIDDEN);
      }
   }
}
