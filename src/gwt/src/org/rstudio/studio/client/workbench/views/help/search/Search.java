/*
 * Search.java
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
package org.rstudio.studio.client.workbench.views.help.search;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.user.client.ui.HasText;
import com.google.inject.Inject;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;

public class Search
{
   public interface Display extends HasValueChangeHandlers<String>,
                                    HasSelectionCommitHandlers<String>,
                                    HasText
   {
   }
   
   @Inject
   public Search(Display display,
                 HelpServerOperations server,
                 EventBus eventBus)
   {
      view_ = display ;
      eventBus_ = eventBus ;
      server_ = server ;
      
      view_.addSelectionCommitHandler(new SelectionCommitHandler<String>() {
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

   public Display getDisplay()
   {
      return view_ ;
   }
   
   private final HelpServerOperations server_ ;
   private final EventBus eventBus_ ;
   private final Display view_ ;
}
