package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.SearchDisplay;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchOracle;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchResult;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class CodeSearch
{
   public interface Display 
   {
      SearchDisplay getSearchDisplay();
      
      SuggestBox.DefaultSuggestionDisplay getSuggestionDisplay();
      
      CodeSearchOracle getSearchOracle();
      
   }
   
   @Inject
   public CodeSearch(Display display, 
                     final Session session, 
                     final FileTypeRegistry fileTypeRegistry,
                     final EventBus eventBus)
   {
      display_ = display;
      
      SearchDisplay searchDisplay = display_.getSearchDisplay();
      searchDisplay.setAutoSelectEnabled(true);
      
      searchDisplay.addSelectionHandler(new SelectionHandler<Suggestion>() {

         @Override
         public void onSelection(SelectionEvent<Suggestion> event)
         {
            // map back to a code search result
            CodeSearchResult result = 
               display_.getSearchOracle().resultFromSuggestion(
                                                event.getSelectedItem());
            
            // get the active project directory
            SessionInfo sessionInfo = session.getSessionInfo();
            FileSystemItem projDir = sessionInfo.getActiveProjectDir(); 
            
            // calculate full file path and position
            String srcFile = projDir.completePath(result.getContext());
            FileSystemItem srcFileItem = FileSystemItem.createFile(srcFile);
            FilePosition pos = FilePosition.create(result.getLine(), 
                                                   result.getColumn());
            
            // fire editing event
            fileTypeRegistry.editFile(srcFileItem, pos);
         }
      });
      
     searchDisplay.addBlurHandler(new BlurHandler() {
         @Override
         public void onBlur(BlurEvent event)
         { 
            display_.getSearchDisplay().clear();
            display_.getSearchOracle().clear();
         }
     });
     
     searchDisplay.addFocusHandler(new FocusHandler() {
        @Override
        public void onFocus(FocusEvent event)
        { 
           display_.getSearchOracle().clear();
        }
     });
     
     searchDisplay.addValueChangeHandler(new ValueChangeHandler<String>() {
        @Override
        public void onValueChange(ValueChangeEvent<String> event)
        {
           boolean hasSearch = event.getValue().length() != 0;
           
           // set oracle to return suggestions as approproate
           display_.getSearchOracle().setReturnSuggestions(hasSearch);
           
           // hide suggestion display if we don't have a search
           if (!hasSearch)
              display_.getSuggestionDisplay().hideSuggestions();
        }     
     });
   }
   
   public Widget getSearchWidget()
   {
      return (Widget) display_.getSearchDisplay();
   }
    
   private final Display display_;
}
