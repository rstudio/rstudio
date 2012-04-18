/*
 * Pager.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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
      @Source("images/PageForwardButton.png")
      ImageResource simplePagerFastForward();

      @Override
      @Source("images/PageForwardButtonDisabled.png")
      ImageResource simplePagerFastForwardDisabled();

      @Override
      @Source("images/PageFirstButton.png")
      ImageResource simplePagerFirstPage();

      @Override
      @Source("images/PageFirstButtonDisabled.png")
      ImageResource simplePagerFirstPageDisabled();

      @Override
      @Source("images/PageLastButton.png")
      ImageResource simplePagerLastPage();

      @Override
      @Source("images/PageLastButtonDisabled.png")
      ImageResource simplePagerLastPageDisabled();

      @Override
      @Source("images/PageNextButton.png")
      ImageResource simplePagerNextPage();

      @Override
      @Source("images/PageNextButtonDisabled.png")
      ImageResource simplePagerNextPageDisabled();

      @Override
      @Source("images/PagePreviousButton.png")
      ImageResource simplePagerPreviousPage();

      @Override
      @Source("images/PagePreviousButtonDisabled.png")
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
