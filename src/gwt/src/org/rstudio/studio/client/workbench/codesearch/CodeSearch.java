package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.SearchDisplay;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchResult;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class CodeSearch
{
   public interface Display 
   {
      SearchDisplay getSearchDisplay();
      
      CodeSearchOracle getSearchOracle();
      
   }
   
   @Inject
   public CodeSearch(Display display, 
                     EventBus eventBus)
   {
      display_ = display;
      
      display_.getSearchDisplay().addSelectionHandler(
                                    new SelectionHandler<Suggestion>() {

         @Override
         public void onSelection(SelectionEvent<Suggestion> event)
         {
            CodeSearchResult result = 
               display_.getSearchOracle().resultFromSuggestion(
                                                event.getSelectedItem());
            
            Debug.log(result.getFunctionName() + " " + result.getContext());
            
         }
         
      });
   }
   
   public Widget getSearchWidget()
   {
      return (Widget) display_.getSearchDisplay();
   }
   
   
   private final Display display_;
}
