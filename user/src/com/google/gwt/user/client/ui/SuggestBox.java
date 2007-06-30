/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.ui.SuggestOracle.Callback;
import com.google.gwt.user.client.ui.SuggestOracle.Request;
import com.google.gwt.user.client.ui.SuggestOracle.Response;
import com.google.gwt.user.client.ui.impl.ItemPickerDropDownImpl;
import com.google.gwt.user.client.ui.impl.SuggestPickerImpl;

import java.util.Collection;

/**
 * A {@link SuggestBox} is a text box or text area which displays a
 * pre-configured set of selections that match the user's input.
 * 
 * Each {@link SuggestBox} is associated with a single {@link SuggestOracle}.
 * The {@link SuggestOracle} is used to provide a set of selections given a
 * specific query string.
 * 
 * <p>
 * By default, the {@link SuggestBox} uses a {@link MultiWordSuggestOracle} as
 * its oracle. Below we show how a {@link MultiWordSuggestOracle} can be
 * configured:
 * </p>
 * 
 * <pre> 
 *   MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();  
 *   oracle.add("Cat");
 *   oracle.add("Dog");
 *   oracle.add("Horse");
 *   oracle.add("Canary");
 *   
 *   SuggestBox box = new SuggestBox(oracle);
 * </pre>
 * 
 * Using the example above, if the user types "C" into the text widget, the
 * oracle will configure the suggestions with the "Cat" and "Canary"
 * suggestions. Specifically, whenever the user types a key into the text
 * widget, the value is submitted to the <code>MultiWordSuggestOracle</code>.
 * 
 * <p>
 * <img class='gallery' src='SuggestBox.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-SuggestBox { the suggest box itself }</li>
 * <li>.gwt-SuggestBoxPopup { the suggestion popup }</li>
 * <li>.gwt-SuggestBoxPopup .item { an unselected suggestion }</li>
 * <li>.gwt-SuggestBoxPopup .item-selected { a selected suggestion }</li>
 * </ul>
 * 
 * @see SuggestOracle
 * @see MultiWordSuggestOracle
 * @see TextBoxBase
 */
public final class SuggestBox extends Composite implements HasText, HasFocus,
    SourcesClickEvents, SourcesFocusEvents, SourcesChangeEvents,
    SourcesKeyboardEvents {

  private static final String STYLENAME_DEFAULT = "gwt-SuggestBox";

  private int limit = 20;
  private int selectStart;
  private int selectEnd;
  private SuggestOracle oracle;
  private char[] separators;
  private String currentValue;
  private final PopupPanel popup;
  private final SuggestPickerImpl picker;
  private final TextBoxBase box;
  private DelegatingClickListenerCollection clickListeners;
  private DelegatingChangeListenerCollection changeListeners;
  private DelegatingFocusListenerCollection focusListeners;
  private DelegatingKeyboardListenerCollection keyboardListeners;
  private String separatorPadding = "";

  private final Callback callBack = new Callback() {
    public void onSuggestionsReady(Request request, Response response) {
      showSuggestions(response.getSuggestions());
    }
  };

  /**
   * Constructor for {@link SuggestBox}. Creates a
   * {@link MultiWordSuggestOracle} and {@link TextBox} to use with this
   * {@link SuggestBox}.
   */
  public SuggestBox() {
    this(new MultiWordSuggestOracle());
  }

  /**
   * Constructor for {@link SuggestBox}. Creates a {@link TextBox} to use with
   * this {@link SuggestBox}.
   * 
   * @param oracle the oracle for this <code>SuggestBox</code>
   */
  public SuggestBox(SuggestOracle oracle) {
    this(oracle, new TextBox());
  }

  /**
   * Constructor for {@link SuggestBox}. The text box will be removed from it's
   * current location and wrapped by the {@link SuggestBox}.
   * 
   * @param oracle supplies suggestions based upon the current contents of the
   *          text widget
   * @param box the text widget
   */
  public SuggestBox(SuggestOracle oracle, TextBoxBase box) {
    this.box = box;
    initWidget(box);
    this.picker = new SuggestPickerImpl(oracle.isDisplayStringHTML());
    this.popup = new ItemPickerDropDownImpl(this, picker);
    addPopupChangeListener();
    addKeyboardSupport();
    setOracle(oracle);
    setStyleName(STYLENAME_DEFAULT);
  }

  public final void addChangeListener(ChangeListener listener) {
    if (changeListeners == null) {
      changeListeners = new DelegatingChangeListenerCollection(this, box);
    }
    changeListeners.add(listener);
  }

  public final void addClickListener(ClickListener listener) {
    if (clickListeners == null) {
      clickListeners = new DelegatingClickListenerCollection(this, box);
    }
    clickListeners.add(listener);
  }

  public final void addFocusListener(FocusListener listener) {
    if (focusListeners == null) {
      focusListeners = new DelegatingFocusListenerCollection(this, box);
    }
    focusListeners.add(listener);
  }

  public final void addKeyboardListener(KeyboardListener listener) {
    if (keyboardListeners == null) {
      keyboardListeners = new DelegatingKeyboardListenerCollection(this, box);
    }
    keyboardListeners.add(listener);
  }

  /**
   * Gets the limit for the number of suggestions that should be displayed for
   * this box. It is up to the current {@link SuggestOracle} to enforce this
   * limit.
   * 
   * @return the limit for the number of suggestions
   */
  public final int getLimit() {
    return limit;
  }

  /**
   * Gets the suggest box's {@link com.google.gwt.user.client.ui.SuggestOracle}.
   * 
   * @return the {@link SuggestOracle}
   */
  public final SuggestOracle getSuggestOracle() {
    return oracle;
  }

  public final int getTabIndex() {
    return box.getTabIndex();
  }

  public final String getText() {
    return box.getText();
  }

  public final void removeChangeListener(ChangeListener listener) {
    if (clickListeners != null) {
      clickListeners.remove(listener);
    }
  }

  public final void removeClickListener(ClickListener listener) {
    if (clickListeners != null) {
      clickListeners.remove(listener);
    }
  }

  public final void removeFocusListener(FocusListener listener) {
    if (focusListeners != null) {
      focusListeners.remove(listener);
    }
  }

  public final void removeKeyboardListener(KeyboardListener listener) {
    if (keyboardListeners != null) {
      keyboardListeners.remove(listener);
    }
  }

  public final void setAccessKey(char key) {
    box.setAccessKey(key);
  }

  public final void setFocus(boolean focused) {
    box.setFocus(focused);
  }

  /**
   * Sets the limit to the number of suggestions the oracle should provide. It
   * is up to the oracle to enforce this limit.
   * 
   * @param limit the limit to the number of suggestions provided
   */
  public final void setLimit(int limit) {
    this.limit = limit;
  }

  public final void setTabIndex(int index) {
    box.setTabIndex(index);
  }

  public final void setText(String text) {
    box.setText(text);
  }

  /**
   * Show the given collection of suggestions.
   * 
   * @param suggestions suggestions to show
   */
  private void showSuggestions(Collection suggestions) {
    if (suggestions.size() > 0) {
      picker.setItems(suggestions);
      popup.show();
    } else {
      popup.hide();
    }
  }

  private void addKeyboardSupport() {
    box.addKeyboardListener(new KeyboardListenerAdapter() {
      private boolean pendingCancel;

      public void onKeyDown(Widget sender, char keyCode, int modifiers) {
        pendingCancel = picker.delegateKeyDown(keyCode);
      }

      public void onKeyPress(Widget sender, char keyCode, int modifiers) {
        if (pendingCancel) {
          // IE does not allow cancel key on key down, so we have delayed the
          // cancellation of the key until the associated key press.
          box.cancelKey();
          pendingCancel = false;
        } else if (popup.isAttached()) {
          if (separators != null && isSeparator(keyCode)) {
            // onKeyDown/onKeyUps's keyCode for ',' comes back '1/4', so unlike
            // navigation, we use key press events to determine when the user
            // wants to simulate clicking on the popup.
            picker.commitSelection();

            // The separator will be added after the popup is activated, so the
            // popup will have already added a new separator. Therefore, the
            // original separator should not be added as well.
            box.cancelKey();
          }
        }
      }

      public void onKeyUp(Widget sender, char keyCode, int modifiers) {
        // After every user key input, refresh the popup's suggestions.
        refreshSuggestions();
      }

      /**
       * In the presence of separators, returns the active search selection.
       */
      private String getActiveSelection(String text) {
        selectEnd = box.getCursorPos();

        // Find the last instance of a separator.
        selectStart = -1;
        for (int i = 0; i < separators.length; i++) {
          selectStart = Math.max(
              text.lastIndexOf(separators[i], selectEnd - 1), selectStart);
        }
        ++selectStart;

        return text.substring(selectStart, selectEnd).trim();
      }

      private void refreshSuggestions() {
        // Get the raw text.
        String text = box.getText();
        if (text.equals(currentValue)) {
          return;
        } else {
          currentValue = text;
        }

        // Find selection to replace.
        String selection;
        if (separators == null) {
          selection = text;
        } else {
          selection = getActiveSelection(text);
        }
        // If we have no text, let's not show the suggestions.
        if (selection.length() == 0) {
          popup.hide();
        } else {
          showSuggestions(selection);
        }
      }
    });
  }

  /**
   * Adds a standard popup listener to the suggest box's popup.
   */
  private void addPopupChangeListener() {
    picker.addChangeListener(new ChangeListener() {
      public void onChange(Widget sender) {
        if (separators != null) {
          onChangeWithSeparators();
        } else {
          currentValue = picker.getSelectedValue().toString();
          box.setText(currentValue);
        }
        if (changeListeners != null) {
          changeListeners.fireChange(SuggestBox.this);
        }
      }

      private void onChangeWithSeparators() {
        String newValue = (String) picker.getSelectedValue();

        StringBuffer accum = new StringBuffer();
        String text = box.getText();

        // Add all text up to the selection start.
        accum.append(text.substring(0, selectStart));

        // Add one space if not at start.
        if (selectStart > 0) {
          accum.append(separatorPadding);
        }
        // Add the new value.
        accum.append(newValue);

        // Find correct cursor position.
        int savedCursorPos = accum.length();

        // Add all text after the selection end
        String ender = text.substring(selectEnd).trim();
        if (ender.length() == 0 || !isSeparator(ender.charAt(0))) {
          // Add a separator if the first char of the ender is not already a
          // separator.
          accum.append(separators[0]).append(separatorPadding);
          savedCursorPos = accum.length();
        }
        accum.append(ender);

        // Set the text and cursor pos to correct location.
        String replacement = accum.toString();
        currentValue = replacement.trim();
        box.setText(replacement);
        box.setCursorPos(savedCursorPos);
      }
    });
  }

  /**
   * Convenience method for identifying if a character is a separator.
   */
  private boolean isSeparator(char candidate) {
    // An int map would be very handy right here...
    for (int i = 0; i < separators.length; i++) {
      if (candidate == separators[i]) {
        return true;
      }
    }
    return false;
  }

  /**
   * Sets the suggestion oracle used to create suggestions.
   * 
   * @param oracle the oracle
   */
  private void setOracle(SuggestOracle oracle) {
    this.oracle = oracle;
  }

  private void showSuggestions(String query) {
    oracle.requestSuggestions(new Request(query, limit), callBack);
  }
}
