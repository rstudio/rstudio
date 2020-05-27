/*
 * HelpSearchWidget.java
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
package org.rstudio.studio.client.workbench.views.help.search;


import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.SearchDisplay;
import org.rstudio.core.client.widget.SearchWidget;

import com.google.inject.Inject;


public class HelpSearchWidget extends SearchWidget 
                              implements HelpSearch.Display
{
   @Inject
   public HelpSearchWidget(HelpSearchOracle oracle)
   {
      super("Search help", oracle);
      ElementIds.assignElementId(this, ElementIds.SW_HELP);
   }

   @Override
   public SearchDisplay getSearchDisplay()
   {
      return this;
   }
}
