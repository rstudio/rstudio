/*
 * CodeSearch.java
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
package org.rstudio.studio.client.workbench.codesearch;

import java.util.ArrayList;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.XRef;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.SearchDisplay;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeHandler;
import org.rstudio.studio.client.workbench.views.source.events.XRefNavigationEvent;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
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
      void onCancel();
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
                     final FileTypeRegistry fileTypeRegistry,
                     final EventBus eventBus)
   {
      display_ = display;
      events_  = eventBus;
      
      final SearchDisplay searchDisplay = display_.getSearchDisplay();
      searchDisplay.setAutoSelectEnabled(true);
      
      searchDisplay.addSelectionHandler(new SelectionHandler<Suggestion>() {

         @Override
         public void onSelection(SelectionEvent<Suggestion> event)
         {
            CodeNavigationTarget target = 
               display_.getSearchOracle().navigationTarget(
                     searchDisplay.getLastValue(),
                     event.getSelectedItem());
              
            // create full file path and position
            String srcFile = target.getFile();
            final FileSystemItem srcItem = FileSystemItem.createFile(srcFile);
            final FilePosition pos = target.getPosition();
            final XRef xref = target.getXRef();
            
            // fire editing event (delayed so the Enter keystroke 
            // doesn't get routed into the source editor)
            Scheduler.get().scheduleDeferred(() ->
            {
               display_.getSearchDisplay().clear();
               display_.getSearchOracle().clear();

               if (observer_ != null)
                  observer_.onCompleted();

               if (xref != null && xref.hasXRefString())
               {
                  events_.fireEvent(new XRefNavigationEvent(xref, srcItem, false));
               }
               else
               {
                  srcItem.setFocusOnNavigate(true);
                  fileTypeRegistry.editFile(srcItem, pos);
               }
            });
         }
      });
      
      searchDisplay.addCloseHandler(new CloseHandler<SearchDisplay>() {
         @Override
         public void onClose(CloseEvent<SearchDisplay> event)
         {
            display_.getSearchDisplay().clear();
            
            if (observer_ != null)
              observer_.onCancel();
         }
      });
     
     // various conditions invalidate the search oracle's cache
      
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
     
     eventBusHandlers_.add(
           eventBus.addHandler(FileChangeEvent.TYPE, new FileChangeHandler() {
        @Override
        public void onFileChange(FileChangeEvent event)
        {           
           // if this was an R file then invalidate the cache
           CodeSearchOracle oracle = display_.getSearchOracle();
           if (oracle.hasCachedResults())
           {
              FileSystemItem fsi = event.getFileChange().getFile();
              if (fsi.getExtension().toLowerCase().equals(".r"))
                 oracle.clear();
           }
        } 
     }));
     
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
   
   // notify the CodeSearch that the search is completed so that it 
   // can un-hook from EventBus events
   public void detachEventBusHandlers()
   {
      for (int i=0; i<eventBusHandlers_.size(); i++)
         eventBusHandlers_.get(i).removeHandler();
      eventBusHandlers_.clear();
   }
     
   private final Display display_;
   private final EventBus events_;
   
   private Observer observer_ = null;
   private ArrayList<HandlerRegistration> eventBusHandlers_ = 
                                 new ArrayList<HandlerRegistration>();
}
