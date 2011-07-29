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
