package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.views.help.search.Search;

public class CodeSearch
{
   public interface Display 
   {
      Search.Display getSearchBox();
   }
   
   public CodeSearch(Display display, 
                     WorkbenchServerOperations server,
                     EventBus eventBus)
   {
      display_ = display;
      server_ = server;
      eventBus_ = eventBus;
   }
   
   
   private Display display_;
   private WorkbenchServerOperations server_;
   private EventBus eventBus_;
}
