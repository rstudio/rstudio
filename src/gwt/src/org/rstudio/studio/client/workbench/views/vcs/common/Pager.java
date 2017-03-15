/*
 * Pager.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.vcs.common;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.Range;

public class Pager extends SimplePager
{
   interface SimplePagerResources extends SimplePager.Resources
   {
      @Override
      @Source("images/PageForwardButton_2x.png")
      ImageResource simplePagerFastForward();

      @Override
      @Source("images/PageForwardButtonDisabled_2x.png")
      ImageResource simplePagerFastForwardDisabled();

      @Override
      @Source("images/PageFirstButton_2x.png")
      ImageResource simplePagerFirstPage();

      @Override
      @Source("images/PageFirstButtonDisabled_2x.png")
      ImageResource simplePagerFirstPageDisabled();

      @Override
      @Source("images/PageLastButton_2x.png")
      ImageResource simplePagerLastPage();

      @Override
      @Source("images/PageLastButtonDisabled_2x.png")
      ImageResource simplePagerLastPageDisabled();

      @Override
      @Source("images/PageNextButton_2x.png")
      ImageResource simplePagerNextPage();

      @Override
      @Source("images/PageNextButtonDisabled_2x.png")
      ImageResource simplePagerNextPageDisabled();

      @Override
      @Source("images/PagePreviousButton_2x.png")
      ImageResource simplePagerPreviousPage();

      @Override
      @Source("images/PagePreviousButtonDisabled_2x.png")
      ImageResource simplePagerPreviousPageDisabled();

      @Override
      @Source({"com/google/gwt/user/cellview/client/SimplePager.css",
               "SimplePagerStyle.css"})
      SimplePagerStyle simplePagerStyle();
   }

   interface SimplePagerStyle extends SimplePager.Style
   {
   }

   public Pager(int pageSize,
                int fastForwardRows)
   {
      super(TextLocation.CENTER,
            GWT.<SimplePagerResources>create(SimplePagerResources.class),
            fastForwardRows > 0, fastForwardRows, fastForwardRows > 0);
      getElement().setAttribute("align", "center");
      setPageSize(pageSize);
   }

   @Override
   protected String createText()
   {
      final HasRows display = getDisplay();
      if (display.getVisibleRange().getStart() == display.getRowCount())
         return "";

      String text = super.createText();

      if (display.isRowCountExact())
         return "Commits " + text;
      else
      {
         int pos = text.indexOf(" of ");
         return "Commits " + (pos >= 0 ? text.substring(0, pos) : text);
      }
   }

   @Override
   public void setPageStart(int index) {
      HasRows display = getDisplay();
      if (display != null) {
         Range range = display.getVisibleRange();
         int pageSize = range.getLength();
         index = Math.max(0, index);
         if (index != range.getStart()) {
            display.setVisibleRange(index, pageSize);
         }
      }
   }

}
