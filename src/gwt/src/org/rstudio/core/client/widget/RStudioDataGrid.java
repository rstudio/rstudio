/*
 * RStudioDataGrid.java
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

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.dom.DomUtils;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
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
   
   public RStudioDataGrid(int max, ProvidesKey<T> keyProvider)
   {
      super(max, keyProvider);
   }
   
   public RStudioDataGrid(int max, DataGrid.Resources res, ProvidesKey<T> keyProvider)
   {
      super(max, res, keyProvider);
   }
   
   @Override
   protected void onLoad()
   {
      super.onLoad();
      
      // None of the below is necessary unless on MacOS since that's the only
      // platform that uses overlay scrollbars.
      if (!BrowseCap.isMacintosh())
         return;
      
      // deferred because GWT's CustomScrollPanel also does some work deferred
      // and we need to make sure this code runs after theirs
      Scheduler.get().scheduleDeferred(() -> hideMacHorizontalScrollbars());
   }
   
   private void hideMacHorizontalScrollbars()
   {
      Element parent = getElement().getParentElement().getParentElement();
      
      // GWT's DataGrid also occasionally displays unwanted horizontal
      // scroll bars. Since we don't ever require horizontal scrolling
      // in the DataGrid objects we show, we just suppress horizontal
      // overflow in any div element that does something with overflow.
      //
      // Again, this is something we'd normally do with CSS but because
      // we need to perform surgery on arbitrary sub-divs which otherwise
      // have no class we must resort to JavaScript + inline styles.
      //
      // https://github.com/rstudio/rstudio/issues/4529
      NodeList<Element> children = DomUtils.querySelectorAll(parent, "div[style*='overflow:']");
      for (int i = 0; i < children.getLength(); i++)
      {
         Element el = children.getItem(i);
         com.google.gwt.dom.client.Style style = el.getStyle();
         
         // skip the element if it has an explicit negative sizing,
         // as this element is not a scrollbar but is instead the
         // actual scrollable container for the table within the
         // DataGrid, and we want to avoid messing with its overflow
         // as that will mess with other scroll height computations
         //
         // see: https://github.com/rstudio/rstudio/issues/4662
         boolean isExplicitlySized =
               style.getLeft().startsWith("-") ||
               style.getTop().startsWith("-") ||
               style.getRight().startsWith("-") ||
               style.getBottom().startsWith("-");
         
         if (isExplicitlySized)
            return;
         
         // okay, hide overflow
         style.setOverflowX(Overflow.HIDDEN);
      }
      
   }
}
