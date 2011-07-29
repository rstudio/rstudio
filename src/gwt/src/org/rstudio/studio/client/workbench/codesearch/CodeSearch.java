package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.core.client.widget.SearchDisplay;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;

public class CodeSearch
{
   public interface Display 
   {
      SearchDisplay getSearchBox();
   }
   
   public CodeSearch(Display display, 
                     WorkbenchServerOperations server,
                     EventBus eventBus)
   {
      display_ = display;
      server_ = server;
      eventBus_ = eventBus;
   }
   
   
   @SuppressWarnings("unused")
   private Display display_;
   @SuppressWarnings("unused")
   private WorkbenchServerOperations server_;
   @SuppressWarnings("unused")
   private EventBus eventBus_;
}
