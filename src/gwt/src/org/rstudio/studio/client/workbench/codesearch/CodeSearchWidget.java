package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchServerOperations;


public class CodeSearchWidget extends SearchWidget
{
   public CodeSearchWidget(CodeSearchServerOperations server)
   {
      super(new CodeSearchOracle(server));
   }
}
