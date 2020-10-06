/*
 * HelpSearch.java
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
package org.rstudio.studio.client.workbench.views.help.search;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.inject.Inject;

import org.rstudio.core.client.events.SelectionCommitEvent;
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
      display_ = display;
      eventBus_ = eventBus;
      server_ = server;

      display_.getSearchDisplay().addSelectionHandler((SelectionEvent<Suggestion> event) ->
      {
         fireShowHelpEvent(event.getSelectedItem().getDisplayString());
      });

      display_.getSearchDisplay().addSelectionCommitHandler((SelectionCommitEvent<String> event) ->
      {
         fireShowHelpEvent(event.getSelectedItem());
      });
   }

   public SearchDisplay getSearchWidget()
   {
      return display_.getSearchDisplay();
   }

   private void fireShowHelpEvent(String topic)
   {
      server_.search(topic, new SimpleRequestCallback<JsArrayString>() {
         public void onResponseReceived(JsArrayString url)
         {
            if (url != null && url.length() > 0)
               eventBus_.fireEvent(new ShowHelpEvent(url.get(0)));
         }
         });
   }

   private final HelpServerOperations server_;
   private final EventBus eventBus_;
   private final Display display_;
}
