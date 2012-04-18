/*
 * HelpSearch.java
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

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.widget.SearchDisplay;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;

public class HelpSearch
{
   public interface Display 
   {
      SearchDisplay getSearchDisplay();  
   }
   
   @Inject
   public HelpSearch(Display display,
                     HelpServerOperations server,
                     EventBus eventBus)
   {
      display_ = display ;
      eventBus_ = eventBus ;
      server_ = server ;
      
      display_.getSearchDisplay().addSelectionCommitHandler(
                                 new SelectionCommitHandler<String>() {
         public void onSelectionCommit(SelectionCommitEvent<String> event)
         {
            server_.search(event.getSelectedItem(), 
                           new SimpleRequestCallback<JsArrayString>() {
               public void onResponseReceived(JsArrayString url)
               {
                  if (url != null && url.length() > 0)
                     eventBus_.fireEvent(new ShowHelpEvent(url.get(0))) ;
               }
            }) ;
         }
      }) ;
   }

   public Widget getSearchWidget()
   {
      return (Widget) display_.getSearchDisplay();
   }
   
   private final HelpServerOperations server_ ;
   private final EventBus eventBus_ ;
   private final Display display_ ;
}
