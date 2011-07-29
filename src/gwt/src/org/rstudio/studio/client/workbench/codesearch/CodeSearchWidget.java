package org.rstudio.studio.client.workbench.codesearch;


import org.rstudio.core.client.widget.SearchDisplay;
import org.rstudio.core.client.widget.SearchWidget;

import com.google.inject.Inject;


public class CodeSearchWidget extends SearchWidget 
                              implements CodeSearch.Display
{
   @Inject
   public CodeSearchWidget(CodeSearchOracle oracle)
   {
      super(oracle);
      oracle_ = oracle;      
   }

   @Override
   public SearchDisplay getSearchDisplay()
   {
      return this;
   }
   
   @Override
   public CodeSearchOracle getSearchOracle()
   {
      return oracle_;
   }
   
   private final CodeSearchOracle oracle_;
}
