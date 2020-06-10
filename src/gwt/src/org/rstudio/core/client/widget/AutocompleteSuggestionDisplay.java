/*
 * AutocompleteSuggestionDisplay.java
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
package org.rstudio.core.client.widget;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestBox.SuggestionCallback;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

public class AutocompleteSuggestionDisplay
             extends SuggestBox.DefaultSuggestionDisplay
{
   public interface ShowSuggestionsHandler
   {
      void onSuggestionsShown(boolean shown);
   }
   
   public AutocompleteSuggestionDisplay()
   {
      showHandlers_ = new ArrayList<ShowSuggestionsHandler>();
      PopupPanel panel = getPopupPanel();
      if (panel != null)
      {
         panel.addCloseHandler(new CloseHandler<PopupPanel>()
         {
            @Override
            public void onClose(CloseEvent<PopupPanel> arg0)
            {
               for (int i = 0; i < showHandlers_.size(); i++)
               {
                  showHandlers_.get(i).onSuggestionsShown(false);
               }
            }
         });
      }
   }

   public void setPopupWidth(String width)
   {
      PopupPanel panel = getPopupPanel();
      if (panel == null)
         return;
      panel.setWidth(width);
   }
   
   public void addPopupStyleName(String styleName)
   {
      PopupPanel panel = getPopupPanel();
      if (panel == null)
         return;
      
      panel.addStyleName(styleName);
   }
   
   public HandlerRegistration addShowSuggestionHandler(
         final ShowSuggestionsHandler handler)
   {
      showHandlers_.add(handler);
      return new HandlerRegistration()
      {
         @Override
         public void removeHandler()
         {
            showHandlers_.remove(handler);
         }
      };
   }
   
   @Override
   protected void showSuggestions(SuggestBox box, 
         Collection<? extends Suggestion> suggestions, 
         boolean a1, boolean a2, SuggestionCallback callback)
   {
      super.showSuggestions(box, suggestions, a1, a2, callback);
      for (int i = 0; i < showHandlers_.size(); i++)
      {
         showHandlers_.get(i).onSuggestionsShown(true);
      }
   }
   
   private ArrayList<ShowSuggestionsHandler> showHandlers_; 
}
