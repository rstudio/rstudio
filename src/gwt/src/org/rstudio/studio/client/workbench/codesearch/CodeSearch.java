/*
 * CodeSearch.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.SearchDisplay;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.codesearch.model.CodeNavigationTarget;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
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
   public interface Observer
   {
      String getCueText();
      void onCompleted();
   }
   
   
   public interface Display 
   {
      SearchDisplay getSearchDisplay();
      
      void setCueText(String text);
      
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
            CodeNavigationTarget target = 
               display_.getSearchOracle().navigationTargetFromSuggestion(
                                                event.getSelectedItem());
            
            // get the active project directory
            SessionInfo sessionInfo = session.getSessionInfo();
            FileSystemItem projDir = sessionInfo.getActiveProjectDir(); 
            
            // calculate full file path and position
            String srcFile = projDir.completePath(target.getProjectFile());
            final FileSystemItem srcItem = FileSystemItem.createFile(srcFile);
            final FilePosition pos = target.getPosition();  
            
            // fire editing event (delayed so the Enter keystroke 
            // doesn't get routed into the source editor)
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
               @Override
               public void execute()
               {
                  display_.getSearchDisplay().clear();
                  display_.getSearchOracle().clear();

                  if (observer_ != null)
                     observer_.onCompleted();
                  
                  fileTypeRegistry.editFile(srcItem, pos);
               }
            });
         }
      });
     
     searchDisplay.addBlurHandler(new BlurHandler() {
         @Override
         public void onBlur(BlurEvent event)
         { 
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
           if (!hasSearch)
           {
              display_.getSearchOracle().invalidateSearches();
              display_.getSuggestionDisplay().hideSuggestions();
           }
        }     
     });
     
     searchDisplay.addKeyDownHandler(new KeyDownHandler() {

        @Override
        public void onKeyDown(KeyDownEvent event)
        {
           // eat key-up if the suggestions are showing (since the 
           // suggestions menu is taking these and if we take it
           // the cursor will go to the beginning of the selection)
           if (display_.getSuggestionDisplay().isSuggestionListShowing() &&
               (event.getNativeKeyCode() == KeyCodes.KEY_UP))
           {
              event.preventDefault();
              event.stopPropagation();
           }
        }
        
     });
   }
   
   public Widget getSearchWidget()
   {
      return (Widget) display_.getSearchDisplay();
   }
   
   public void setObserver(Observer observer)
   {
      observer_ = observer;
      
      if (observer_ != null)
      {
         String cueText = observer_.getCueText();
         if (cueText != null)
            display_.setCueText(cueText);
      }
   }
     
   private final Display display_;
   private Observer observer_ = null;
}
