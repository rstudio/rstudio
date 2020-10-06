/*
 * AutocompleteComboBox.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class AutocompleteComboBox extends Composite
{
   private static AutocompleteComboBoxUiBinder uiBinder = GWT
         .create(AutocompleteComboBoxUiBinder.class);

   interface AutocompleteComboBoxUiBinder extends
         UiBinder<Widget, AutocompleteComboBox>
   {
   }

   interface Style extends CssResource
   {
      String suggestionList();
   }

   public interface Resources extends ClientBundle
   {
      @Source("dropButtonNormal.png")
      ImageResource dropButtonNormal();

      @Source("dropButtonPressed.png")
      ImageResource dropButtonPressed();
   }

   public AutocompleteComboBox(SuggestOracle oracle)
   {
      TextBox textBox = new TextBox();
      final AutocompleteSuggestionDisplay display = 
            new AutocompleteSuggestionDisplay();
      display.setAnimationEnabled(true);
      suggestBox_ = new SuggestBox(oracle, textBox, display);
      initWidget(uiBinder.createAndBindUi(this));
      
      dropButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent evt)
         {
            // toggle state 
            if (display.isSuggestionListShowing())
            {
               display.hideSuggestions();
            }
            else
            {
               suggestBox_.showSuggestionList();
            }
         }
      });
      
      // move focus to the drop list when the button is clicked
      dropButton_.addDomHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent event)
         {
            suggestBox_.setFocus(true);
         }
      }, 
      FocusEvent.getType());

      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand()
      {
         @Override
         public void execute()
         {
            display.addPopupStyleName(style.suggestionList());
            display.setPopupWidth(getElement().getClientWidth() + "px");
         }
      });
      
      // start the button image in the normal state, and swap it when the
      // suggestion list is shown/hidden
      dropButton_.setResource(RES.dropButtonNormal());
      display.addShowSuggestionHandler(
            new AutocompleteSuggestionDisplay.ShowSuggestionsHandler()
      {
         @Override
         public void onSuggestionsShown(boolean shown)
         {
            dropButton_.setResource(shown ? 
               RES.dropButtonPressed() : 
               RES.dropButtonNormal());
         }
      });
   }
   
   // forward most handler registrations to the underlying suggestion box
   public HandlerRegistration addSelectionHandler(
         SelectionHandler<Suggestion> handler)
   {
      return suggestBox_.addSelectionHandler(handler);
   }
   
   public HandlerRegistration addValueChangeHandler(
         ValueChangeHandler<String> handler)
   {
      return suggestBox_.addValueChangeHandler(handler);
   }
   
   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
   {
      return suggestBox_.addKeyPressHandler(handler);
   }

   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return suggestBox_.addKeyDownHandler(handler);
   }
   
   public <H extends EventHandler> HandlerRegistration addEntryDomHandler(
         H handler, DomEvent.Type<H> type)
   {
      return suggestBox_.addDomHandler(handler, type);
   }
   
   public String getValue()
   {
      return suggestBox_.getValue();
   }
   
   public String getText()
   {
      return suggestBox_.getText();
   }
   
   public void setText(String text)
   {
      suggestBox_.setText(text);
   }
   
   public void setFocus(boolean focused)
   {
      suggestBox_.setFocus(focused);
   }

   @UiField Image dropButton_;
   @UiField(provided=true) SuggestBox suggestBox_;
   @UiField Style style;

   private static final Resources RES = GWT.<Resources>create(Resources.class); 
}
