/*
 * SearchWidget.java
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
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestBox.SuggestionDisplay;

import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.ValueBoxBase;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

public class SearchWidget extends Composite implements SearchDisplay
{
   interface MyUiBinder extends UiBinder<Widget, SearchWidget> {}
   private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

   class FocusSuggestBox extends SuggestBox implements HasAllFocusHandlers
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

   public SearchWidget(String label)
   {
      this(label, new SuggestOracle()
      {
         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            // no-op
         }
      });
   }

   public SearchWidget(String label, SuggestOracle oracle)
   {
      this(label, oracle, null);
   }

   public SearchWidget(String label,
                       SuggestOracle oracle,
                       SuggestionDisplay suggestDisplay)
   {
      this(label, oracle, new TextBox(), suggestDisplay);
   }

   public SearchWidget(String label,
                       SuggestOracle oracle,
                       TextBoxBase textBox,
                       SuggestionDisplay suggestDisplay)
   {
      this(label, oracle, textBox, suggestDisplay, true);
   }

   public SearchWidget(String label,
                       SuggestOracle oracle,
                       TextBoxBase textBox,
                       SuggestionDisplay suggestDisplay,
                       boolean continuousSearch)
   {
      DomUtils.disableSpellcheck(textBox);

      if (suggestDisplay != null)
         suggestBox_ = new FocusSuggestBox(oracle, textBox, suggestDisplay);
      else
         suggestBox_ = new FocusSuggestBox(oracle, textBox);

      initWidget(uiBinder.createAndBindUi(this));
      clearFilter_.setVisible(false);
      clearFilter_.setDescription("Clear text");
      if (!StringUtil.isNullOrEmpty(label))
      {
         hiddenLabel_.setInnerText(label);
         hiddenLabel_.setHtmlFor(DomUtils.ensureHasId(textBox.getElement()));
      }
      else
      {
         A11y.setARIAHidden(hiddenLabel_);
      }
      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();

      suggestBox_.setStylePrimaryName(styles.searchBox());
      suggestBox_.setAutoSelectEnabled(false);
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
                                               suggestBox_.getText());
                  }
               });
               break;
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

               break;
            }
         }
      });

      if (continuousSearch)
      {
         // Unlike SuggestBox's ValueChangeEvent impl, we want the
         // event to fire as soon as the value changes
         suggestBox_.addKeyUpHandler(new KeyUpHandler() {
            public void onKeyUp(KeyUpEvent event)
            {
               String value = suggestBox_.getText();
               if (value != lastValueSent_)
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
            if (evt.getValue() != lastValueSent_)
            {
               updateLastValue(evt.getValue());
               delegateEvent(SearchWidget.this, evt);
            }
         }
      });

      clearFilter_.addClickHandler(event -> {
         suggestBox_.setText("");
         ValueChangeEvent.fire(suggestBox_, "");
         focus();
      });

      focusTracker_ = new FocusTracker(suggestBox_);
   }

   public HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      return suggestBox_.addFocusHandler(handler);
   }

   public HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      return suggestBox_.addBlurHandler(handler);
   }

   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }

   public HandlerRegistration addSelectionCommitHandler(SelectionCommitEvent.Handler<String> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }

   public HandlerRegistration addKeyDownHandler(final KeyDownHandler handler)
   {
      return suggestBox_.addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            handler.onKeyDown(event);
         }
      });
   }

   public HandlerRegistration addSelectionHandler(SelectionHandler<SuggestOracle.Suggestion> handler)
   {
      return suggestBox_.addSelectionHandler(handler);
   }

   public HandlerRegistration addCloseHandler(CloseHandler<SearchDisplay> handler)
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
      return suggestBox_.getText();
   }

   public void setText(String text)
   {
      suggestBox_.setText(text);
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
      clearFilter_.setVisible(false);
   }

   // NOTE: only works if you are using the default display!
   public DefaultSuggestionDisplay getSuggestionDisplay()
   {
      return (DefaultSuggestionDisplay) suggestBox_.getSuggestionDisplay();
   }

   protected ValueBoxBase<String> getTextBox()
   {
      return suggestBox_.getValueBox();
   }

   private void updateLastValue(String value)
   {
      lastValueSent_ = value;
      clearFilter_.setVisible(lastValueSent_.length() > 0);
   }

   public String getLastValue()
   {
      return lastValueSent_;
   }

   public void setPlaceholderText(String value)
   {
      DomUtils.setPlaceholder(suggestBox_.getElement(), value);
   }

   public boolean isFocused()
   {
      return focusTracker_.isFocused();
   }

   public Element getInputElement()
   {
      Element searchEl = getElement();
      NodeList<Element> inputEls = searchEl.getElementsByTagName("input");
      Element inputEl = inputEls.getItem(0);
      return inputEl;
   }

   @UiField(provided=true)
   FocusSuggestBox suggestBox_;
   @UiField
   ImageButton clearFilter_;
   @UiField
   DecorativeImage icon_;
   @UiField
   LabelElement hiddenLabel_;

   private String lastValueSent_ = null;
   private final FocusTracker focusTracker_;
}
