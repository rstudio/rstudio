/*
 * HelpSearchWidget.java
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
package org.rstudio.studio.client.workbench.views.help.search;


import org.rstudio.core.client.widget.SearchDisplay;
import org.rstudio.core.client.widget.SearchWidget;

import com.google.inject.Inject;


public class HelpSearchWidget extends SearchWidget 
                              implements HelpSearch.Display
{
   @Inject
   public HelpSearchWidget(HelpSearchOracle oracle)
   {
      super(oracle);
   }

   @Override
   public SearchDisplay getSearchDisplay()
   {
      return this;
   }
}
