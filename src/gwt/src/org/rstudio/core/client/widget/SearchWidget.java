/*
 * SearchWidget.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestBox.SuggestionDisplay;

import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

public class SearchWidget extends Composite implements SearchDisplay                                   
{
   interface MyUiBinder extends UiBinder<Widget, SearchWidget> {}
   private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

   class FocusSuggestBox extends SuggestBox implements HasFocusHandlers,
                                                       HasBlurHandlers
   {
      FocusSuggestBox(SuggestOracle oracle)
      {
         super(oracle);
      }
    
      FocusSuggestBox(SuggestOracle oracle, TextBoxBase textBox)
      {
         super(oracle, textBox);
      }
      
      FocusSuggestBox(SuggestOracle oracle, 
                      TextBoxBase textBox, 
                      SuggestionDisplay suggestDisplay)
      {
         super(oracle, textBox, suggestDisplay);
      }
      
    

      public HandlerRegistration addBlurHandler(BlurHandler handler)
      {
         return addDomHandler(handler, BlurEvent.getType());
      }

      public HandlerRegistration addFocusHandler(FocusHandler handler)
      {
         return addDomHandler(handler, FocusEvent.getType());
      }
   }
  

   public SearchWidget(SuggestOracle oracle)
   {
      this(oracle, null);
   }
   
   public SearchWidget(SuggestOracle oracle, 
                       SuggestionDisplay suggestDisplay)
   {
      this(oracle, new TextBox(), suggestDisplay);
   }
   
   public SearchWidget(SuggestOracle oracle, 
                       TextBoxBase textBox, 
                       SuggestionDisplay suggestDisplay)
   {
      this(oracle, textBox, suggestDisplay, true);
   }

   public SearchWidget(SuggestOracle oracle,
                       TextBoxBase textBox,
                       SuggestionDisplay suggestDisplay,
                       boolean continuousSearch)
   {
      if (suggestDisplay != null)
         suggestBox_ = new FocusSuggestBox(oracle, textBox, suggestDisplay);
      else 
         suggestBox_ = new FocusSuggestBox(oracle, textBox);
      
      initWidget(uiBinder.createAndBindUi(this));
      close_.setVisible(false);

      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();

      suggestBox_.setStylePrimaryName(styles.searchBox());
      suggestBox_.setAutoSelectEnabled(false) ;
      addKeyDownHandler(new KeyDownHandler() {
         public void onKeyDown(KeyDownEvent event)
         {
            switch (event.getNativeKeyCode())
            {
            case KeyCodes.KEY_ENTER:
               Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                  public void execute()
                  {
                     SelectionCommitEvent.fire(SearchWidget.this, 
                                               suggestBox_.getText()) ;
                  }
               }) ;
               break ;
            case KeyCodes.KEY_ESCAPE:
               
               event.preventDefault();
               event.stopPropagation();
               
               // defer the handling of ESC so that it doesn't end up
               // inside other UI (the editor) if/when the parent search
               // ui is dismissed
               Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                  @Override
                  public void execute()
                  {
                     if (getSuggestionDisplay().isSuggestionListShowing())
                     {  
                        getSuggestionDisplay().hideSuggestions();
                        setText("", true);
                     }
                     else
                     {
                        CloseEvent.fire(SearchWidget.this, SearchWidget.this);
                     }
                  }   
               });
                   
               break ;
            }
         }
      }) ;

      if (continuousSearch)
      {
         // Unlike SuggestBox's ValueChangeEvent impl, we want the
         // event to fire as soon as the value changes
         suggestBox_.addKeyUpHandler(new KeyUpHandler() {
            public void onKeyUp(KeyUpEvent event)
            {
               String value = suggestBox_.getText();
               if (!value.equals(lastValueSent_))
               {
                  updateLastValue(value);
                  ValueChangeEvent.fire(SearchWidget.this, value);
               }
            }
         });
      }
      suggestBox_.addValueChangeHandler(new ValueChangeHandler<String>()
      {
         public void onValueChange(ValueChangeEvent<String> evt)
         {
            if (!evt.getValue().equals(lastValueSent_))
            {
               updateLastValue(evt.getValue());
               delegateEvent(SearchWidget.this, evt);
            }
         }
      });

      close_.addMouseDownHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            event.preventDefault();
            
            suggestBox_.setText("");
            ValueChangeEvent.fire(suggestBox_, "");
         }
      });
   }
   
   public HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      return suggestBox_.addFocusHandler(handler);
   }

   public HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      return suggestBox_.addBlurHandler(handler);
   }

   public HandlerRegistration addValueChangeHandler(
                                           ValueChangeHandler<String> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }

   public HandlerRegistration addSelectionCommitHandler(
                                       SelectionCommitHandler<String> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType()) ;
   }

   public HandlerRegistration addKeyDownHandler(final KeyDownHandler handler)
   {
      return suggestBox_.addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            if (ignore_ = !ignore_)
               handler.onKeyDown(event);
         }

         private boolean ignore_ = false;
      });
   }

   public HandlerRegistration addSelectionHandler(
                           SelectionHandler<SuggestOracle.Suggestion> handler)
   {
      return suggestBox_.addSelectionHandler(handler);
   }
   
   public HandlerRegistration addCloseHandler(
                                          CloseHandler<SearchDisplay> handler)
   {
      return addHandler(handler, CloseEvent.getType());
   }
   
   @Override
   public void setAutoSelectEnabled(boolean selectsFirstItem)
   {
      suggestBox_.setAutoSelectEnabled(selectsFirstItem);
      
   }
   
   public String getText()
   {
      return suggestBox_.getText() ;
   }

   public void setText(String text)
   {
      suggestBox_.setText(text) ;
   }

   public void setText(String text, boolean fireEvents)
   {
      suggestBox_.setValue(text, fireEvents);
   }
   
   @Override
   public String getValue()
   {
      return suggestBox_.getValue();
   }

   @Override
   public void setValue(String value)
   {
      suggestBox_.setValue(value);
   }

   @Override
   public void setValue(String value, boolean fireEvents)
   {
      suggestBox_.setValue(value, fireEvents);
   }

   public void setIcon(ImageResource image)
   {
      icon_.setResource(image);
   }
   
   public void focus()
   {
      suggestBox_.setFocus(true);      
   }
   
   public void clear()
   {
      setText("", true);
      close_.setVisible(false);
   }
   
   // NOTE: only works if you are using the default display!
   public DefaultSuggestionDisplay getSuggestionDisplay()
   {
      return (DefaultSuggestionDisplay) suggestBox_.getSuggestionDisplay();
   }
   
   protected TextBoxBase getTextBox()
   {
      return suggestBox_.getTextBox();
   }
   
   private void updateLastValue(String value)
   {
      lastValueSent_ = value;
      close_.setVisible(lastValueSent_.length() > 0);
   }

   @UiField(provided=true)
   FocusSuggestBox suggestBox_;
   @UiField
   Image close_;
   @UiField
   Image icon_;

   private String lastValueSent_ = null;

  
}
