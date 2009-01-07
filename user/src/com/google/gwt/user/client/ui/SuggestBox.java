/*
 * Copyright 2009 Google Inc.
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

import static com.google.gwt.event.dom.client.KeyCodes.KEY_DOWN;
import static com.google.gwt.event.dom.client.KeyCodes.KEY_ENTER;
import static com.google.gwt.event.dom.client.KeyCodes.KEY_TAB;
import static com.google.gwt.event.dom.client.KeyCodes.KEY_UP;

import com.google.gwt.event.dom.client.HandlesAllKeyEvents;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel.AnimationType;
import com.google.gwt.user.client.ui.SuggestOracle.Callback;
import com.google.gwt.user.client.ui.SuggestOracle.Request;
import com.google.gwt.user.client.ui.SuggestOracle.Response;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import java.util.Collection;
import java.util.List;
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
 * Note that there is no method to retrieve the "currently selected suggestion"
 * in a SuggestBox, because there are points in time where the currently
 * selected suggestion is not defined. For example, if the user types in some
 * text that does not match any of the SuggestBox's suggestions, then the
 * SuggestBox will not have a currently selected suggestion. It is more useful
 * to know when a suggestion has been chosen from the SuggestBox's list of
 * suggestions. A SuggestBox fires {@link SuggestionEvent SuggestionEvents}
 * whenever a suggestion is chosen, and handlers for these events can be added
 * using the {@link #addValueChangeHandler(ValueChangeHandler)} method.
 * </p>
 * 
 * <p>
 * <img class='gallery' src='SuggestBox.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3> 
 * <ul class='css'> 
 * <li>.gwt-SuggestBox { the suggest
 * box itself }</li> 
 * <li>.gwt-SuggestBoxPopup { the suggestion popup }</li> 
 * <li>.gwt-SuggestBoxPopup .item { an unselected suggestion }</li> 
 * <li>.gwt-SuggestBoxPopup .item-selected { a selected suggestion }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupTopLeft { the top left cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupTopLeftInner { the inner element of the cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupTopCenter { the top center cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupTopCenterInner { the inner element of the cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupTopRight { the top right cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupTopRightInner { the inner element of the cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupMiddleLeft { the middle left cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupMiddleLeftInner { the inner element of the cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupMiddleCenter { the middle center cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupMiddleCenterInner { the inner element of the cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupMiddleRight { the middle right cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupMiddleRightInner { the inner element of the cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupBottomLeft { the bottom left cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupBottomLeftInner { the inner element of the cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupBottomCenter { the bottom center cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupBottomCenterInner { the inner element of the cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupBottomRight { the bottom right cell }</li> 
 * <li>.gwt-SuggestBoxPopup .suggestPopupBottomRightInner { the inner element of the cell }</li> </ul>
 * 
 * @see SuggestOracle
 * @see MultiWordSuggestOracle
 * @see TextBoxBase
 */
@SuppressWarnings("deprecation")
public class SuggestBox extends Composite implements HasText, HasFocus,
    HasAnimation, SourcesClickEvents, SourcesFocusEvents, SourcesChangeEvents,
    SourcesKeyboardEvents, FiresSuggestionEvents, HasAllKeyHandlers,
    HasValue<String>, HasSelectionHandlers<Suggestion> {

  /**
   * The SuggestionMenu class is used for the display and selection of
   * suggestions in the SuggestBox widget. SuggestionMenu differs from MenuBar
   * in that it always has a vertical orientation, and it has no submenus. It
   * also allows for programmatic selection of items in the menu, and
   * programmatically performing the action associated with the selected item.
   * In the MenuBar class, items cannot be selected programatically - they can
   * only be selected when the user places the mouse over a particlar item.
   * Additional methods in SuggestionMenu provide information about the number
   * of items in the menu, and the index of the currently selected item.
   */
  private static class SuggestionMenu extends MenuBar {

    public SuggestionMenu(boolean vertical) {
      super(vertical);
      // Make sure that CSS styles specified for the default Menu classes
      // do not affect this menu
      setStyleName("");
    }

    public void doSelectedItemAction() {
      // In order to perform the action of the item that is currently
      // selected, the menu must be showing.
      MenuItem selectedItem = getSelectedItem();
      if (selectedItem != null) {
        doItemAction(selectedItem, true);
      }
    }

    public int getNumItems() {
      return getItems().size();
    }

    /**
     * Returns the index of the menu item that is currently selected.
     * 
     * @return returns the selected item
     */
    public int getSelectedItemIndex() {
      // The index of the currently selected item can only be
      // obtained if the menu is showing.
      MenuItem selectedItem = getSelectedItem();
      if (selectedItem != null) {
        return getItems().indexOf(selectedItem);
      }
      return -1;
    }

    /**
     * Selects the item at the specified index in the menu. Selecting the item
     * does not perform the item's associated action; it only changes the style
     * of the item and updates the value of SuggestionMenu.selectedItem.
     * 
     * @param index index
     */
    public void selectItem(int index) {
      List<MenuItem> items = getItems();
      if (index > -1 && index < items.size()) {
        itemOver(items.get(index), false);
      }
    }
  }

  /**
   * Class for menu items in a SuggestionMenu. A SuggestionMenuItem differs from
   * a MenuItem in that each item is backed by a Suggestion object. The text of
   * each menu item is derived from the display string of a Suggestion object,
   * and each item stores a reference to its Suggestion object.
   */
  private static class SuggestionMenuItem extends MenuItem {

    private static final String STYLENAME_DEFAULT = "item";

    private Suggestion suggestion;

    public SuggestionMenuItem(Suggestion suggestion, boolean asHTML) {
      super(suggestion.getDisplayString(), asHTML);
      // Each suggestion should be placed in a single row in the suggestion
      // menu. If the window is resized and the suggestion cannot fit on a
      // single row, it should be clipped (instead of wrapping around and
      // taking up a second row).
      DOM.setStyleAttribute(getElement(), "whiteSpace", "nowrap");
      setStyleName(STYLENAME_DEFAULT);
      setSuggestion(suggestion);
    }

    public Suggestion getSuggestion() {
      return suggestion;
    }

    public void setSuggestion(Suggestion suggestion) {
      this.suggestion = suggestion;
    }
  }

  /**
   * A PopupPanel with a SuggestionMenu as its widget. The SuggestionMenu is
   * placed in a PopupPanel so that it can be displayed at various positions
   * around the SuggestBox's text field. Moreover, the SuggestionMenu needs to
   * appear on top of any other widgets on the page, and the PopupPanel provides
   * this behavior.
   * 
   * A non-static member class is used because the popup uses the SuggestBox's
   * SuggestionMenu as its widget, and the position of the SuggestBox's TextBox
   * is needed in order to correctly position the popup.
   */
  private class SuggestionPopup extends DecoratedPopupPanel {
    private static final String STYLENAME_DEFAULT = "gwt-SuggestBoxPopup";

    public SuggestionPopup() {
      super(true, false, "suggestPopup");
      setWidget(suggestionMenu);
      setStyleName(STYLENAME_DEFAULT);
      setPreviewingAllNativeEvents(true);
    }

    /**
     * The default position of the SuggestPopup is directly below the
     * SuggestBox's text box, with its left edge aligned with the left edge of
     * the text box. Depending on the width and height of the popup and the
     * distance from the text box to the bottom and right edges of the window,
     * the popup may be displayed directly above the text box, and/or its right
     * edge may be aligned with the right edge of the text box.
     */
    public void showAlignedPopup() {

      // Set the position of the popup right before it is shown.
      setPopupPositionAndShow(new PositionCallback() {
        public void setPosition(int offsetWidth, int offsetHeight) {

          // Calculate left position for the popup. The computation for
          // the left position is bidi-sensitive.

          int textBoxOffsetWidth = box.getOffsetWidth();

          // Compute the difference between the popup's width and the
          // textbox's width
          int offsetWidthDiff = offsetWidth - textBoxOffsetWidth;

          int left;

          if (LocaleInfo.getCurrentLocale().isRTL()) { // RTL case

            int textBoxAbsoluteLeft = box.getAbsoluteLeft();

            // Right-align the popup. Note that this computation is
            // valid in the case where offsetWidthDiff is negative.
            left = textBoxAbsoluteLeft - offsetWidthDiff;

            // If the suggestion popup is not as wide as the text box, always
            // align to the right edge of the text box. Otherwise, figure out
            // whether to right-align or left-align the popup.
            if (offsetWidthDiff > 0) {

              // Make sure scrolling is taken into account, since
              // box.getAbsoluteLeft() takes scrolling into account.
              int windowRight = Window.getClientWidth()
                  + Window.getScrollLeft();
              int windowLeft = Window.getScrollLeft();

              // Compute the left value for the right edge of the textbox
              int textBoxLeftValForRightEdge = textBoxAbsoluteLeft
                  + textBoxOffsetWidth;

              // Distance from the right edge of the text box to the right edge
              // of the window
              int distanceToWindowRight = windowRight
                  - textBoxLeftValForRightEdge;

              // Distance from the right edge of the text box to the left edge
              // of the window
              int distanceFromWindowLeft = textBoxLeftValForRightEdge
                  - windowLeft;

              // If there is not enough space for the overflow of the popup's
              // width to the right of the text box and there IS enough space
              // for the overflow to the right of the text box, then left-align
              // the popup. However, if there is not enough space on either
              // side, stick with right-alignment.
              if (distanceFromWindowLeft < offsetWidth
                  && distanceToWindowRight >= offsetWidthDiff) {
                // Align with the left edge of the text box.
                left = textBoxAbsoluteLeft;
              }
            }
          } else { // LTR case

            // Left-align the popup.
            left = box.getAbsoluteLeft();

            // If the suggestion popup is not as wide as the text box, always
            // align to the left edge of the text box. Otherwise, figure out
            // whether to left-align or right-align the popup.
            if (offsetWidthDiff > 0) {
              // Make sure scrolling is taken into account, since
              // box.getAbsoluteLeft() takes scrolling into account.
              int windowRight = Window.getClientWidth()
                  + Window.getScrollLeft();
              int windowLeft = Window.getScrollLeft();

              // Distance from the left edge of the text box to the right edge
              // of the window
              int distanceToWindowRight = windowRight - left;

              // Distance from the left edge of the text box to the left edge of
              // the window
              int distanceFromWindowLeft = left - windowLeft;

              // If there is not enough space for the overflow of the popup's
              // width to the right of hte text box, and there IS enough space
              // for the overflow to the left of the text box, then right-align
              // the popup. However, if there is not enough space on either
              // side, then stick with left-alignment.
              if (distanceToWindowRight < offsetWidth
                  && distanceFromWindowLeft >= offsetWidthDiff) {
                // Align with the right edge of the text box.
                left -= offsetWidthDiff;
              }
            }
          }

          // Calculate top position for the popup

          int top = box.getAbsoluteTop();

          // Make sure scrolling is taken into account, since
          // box.getAbsoluteTop() takes scrolling into account.
          int windowTop = Window.getScrollTop();
          int windowBottom = Window.getScrollTop() + Window.getClientHeight();

          // Distance from the top edge of the window to the top edge of the
          // text box
          int distanceFromWindowTop = top - windowTop;

          // Distance from the bottom edge of the window to the bottom edge of
          // the text box
          int distanceToWindowBottom = windowBottom
              - (top + box.getOffsetHeight());

          // If there is not enough space for the popup's height below the text
          // box and there IS enough space for the popup's height above the text
          // box, then then position the popup above the text box. However, if
          // there is not enough space on either side, then stick with
          // displaying the popup below the text box.
          if (distanceToWindowBottom < offsetHeight
              && distanceFromWindowTop >= offsetHeight) {
            top -= offsetHeight;
          } else {
            // Position above the text box
            top += box.getOffsetHeight();
          }

          setPopupPosition(left, top);
        }
      });
    }
  }

  private static final String STYLENAME_DEFAULT = "gwt-SuggestBox";

  private int limit = 20;
  private boolean selectsFirstItem = false;
  private SuggestOracle oracle;
  private String currentText;
  private final SuggestionMenu suggestionMenu;
  private final SuggestionPopup suggestionPopup;
  private final TextBoxBase box;
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

    // suggestionMenu must be created before suggestionPopup, because
    // suggestionMenu is suggestionPopup's widget
    suggestionMenu = new SuggestionMenu(true);
    suggestionPopup = new SuggestionPopup();
    suggestionPopup.setAnimationType(AnimationType.ONE_WAY_CORNER);

    addEventsToTextBox();

    setOracle(oracle);
    setStyleName(STYLENAME_DEFAULT);
  }

  /**
   * 
   * Adds a listener to receive change events on the SuggestBox's text box. The
   * source Widget for these events will be the SuggestBox.
   * 
   * @param listener the listener interface to add
   * @deprecated use getTextBox().addChangeHandler instead
   */
  @Deprecated
  public void addChangeListener(final ChangeListener listener) {
    ListenerWrapper.Change legacy = new ListenerWrapper.Change(listener);
    legacy.setSource(this);
    box.addChangeHandler(legacy);
  }

  /**
   * Adds a listener to receive click events on the SuggestBox's text box. The
   * source Widget for these events will be the SuggestBox.
   * 
   * @param listener the listener interface to add
   * @deprecated use getTextBox().addClickHandler instead
   */
  @Deprecated
  public void addClickListener(final ClickListener listener) {
    ListenerWrapper.Click legacy = ListenerWrapper.Click.add(box, listener);
    legacy.setSource(this);
  }

  /**
   * Adds an event to this handler.
   * 
   * @deprecated use addSelectionHandler instead.
   */
  @Deprecated
  public void addEventHandler(final SuggestionHandler handler) {
    ListenerWrapper.Suggestion.add(this, handler);
  }

  /**
   * Adds a listener to receive focus events on the SuggestBox's text box. The
   * source Widget for these events will be the SuggestBox.
   * 
   * @param listener the listener interface to add
   * @deprecated use getTextBox().addFocusHandler/addBlurHandler() instead
   */
  @Deprecated
  public void addFocusListener(final FocusListener listener) {
    ListenerWrapper.Focus focus = ListenerWrapper.Focus.add(box, listener);
    focus.setSource(this);
  }

  @Deprecated
  public void addKeyboardListener(KeyboardListener listener) {
    ListenerWrapper.Keyboard.add(this, listener);
  }

  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return addDomHandler(handler, KeyDownEvent.getType());
  }

  public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
    return addDomHandler(handler, KeyPressEvent.getType());
  }

  public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
    return addDomHandler(handler, KeyUpEvent.getType());
  }

  public HandlerRegistration addSelectionHandler(
      SelectionHandler<Suggestion> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }

  public HandlerRegistration addValueChangeHandler(
      ValueChangeHandler<String> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  /**
   * Gets the limit for the number of suggestions that should be displayed for
   * this box. It is up to the current {@link SuggestOracle} to enforce this
   * limit.
   * 
   * @return the limit for the number of suggestions
   */
  public int getLimit() {
    return limit;
  }

  /**
   * Returns whether or not the first suggestion will be automatically
   * selected. This behavior is off by default.
   *
   * @return true if the first suggestion will be automatically selected
   */
  public boolean getSelectsFirstItem() {
    return selectsFirstItem;
  }

  /**
   * Gets the suggest box's {@link com.google.gwt.user.client.ui.SuggestOracle}.
   * 
   * @return the {@link SuggestOracle}
   */
  public SuggestOracle getSuggestOracle() {
    return oracle;
  }

  public int getTabIndex() {
    return box.getTabIndex();
  }

  public String getText() {
    return box.getText();
  }

  /**
   * Get the text box associated with this suggest box.
   * 
   * @return this suggest box's text box
   */
  public TextBoxBase getTextBox() {
    return box;
  }

  public String getValue() {
    return box.getValue();
  }

  public boolean isAnimationEnabled() {
    return suggestionPopup.isAnimationEnabled();
  }

  @Deprecated
  public void removeChangeListener(ChangeListener listener) {
    ListenerWrapper.Change.remove(box, listener);
  }

  @Deprecated
  public void removeClickListener(ClickListener listener) {
    ListenerWrapper.Click.remove(box, listener);
  }

  @Deprecated
  public void removeEventHandler(SuggestionHandler handler) {
    ListenerWrapper.Suggestion.remove(this, handler);
  }

  @Deprecated
  public void removeFocusListener(FocusListener listener) {
    ListenerWrapper.Focus.remove(this, listener);
  }

  @Deprecated
  public void removeKeyboardListener(KeyboardListener listener) {
    ListenerWrapper.Keyboard.remove(this, listener);
  }

  public void setAccessKey(char key) {
    box.setAccessKey(key);
  }

  public void setAnimationEnabled(boolean enable) {
    suggestionPopup.setAnimationEnabled(enable);
  }

  public void setFocus(boolean focused) {
    box.setFocus(focused);
  }

  /**
   * Sets the limit to the number of suggestions the oracle should provide. It
   * is up to the oracle to enforce this limit.
   * 
   * @param limit the limit to the number of suggestions provided
   */
  public void setLimit(int limit) {
    this.limit = limit;
  }

  /**
   * Sets the style name of the suggestion popup.
   * 
   * @param style the new primary style name
   * @see UIObject#setStyleName(String)
   */
  public void setPopupStyleName(String style) {
    suggestionPopup.setStyleName(style);
  }

  /**
   * Turns on or off the behavior that automatically selects the first suggested
   * item. It defaults to off.
   *
   * @param selectsFirstItem Whether or not to automatically select the first
   *          suggested
   */
  public void setSelectsFirstItem(boolean selectsFirstItem) {
    this.selectsFirstItem = selectsFirstItem;
  }

  public void setTabIndex(int index) {
    box.setTabIndex(index);
  }

  public void setText(String text) {
    box.setText(text);
  }

  public void setValue(String newValue) {
    box.setValue(newValue);
  }

  public void setValue(String value, boolean fireEvents) {
    box.setValue(value, fireEvents);
  }

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-popup = The popup that appears with suggestions.</li>
   * <li>-items-item# = The suggested item at the specified index.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
    suggestionPopup.ensureDebugId(baseID + "-popup");
    suggestionMenu.setMenuItemDebugIds(baseID);
  }

  private void addEventsToTextBox() {
    class TextBoxEvents extends HandlesAllKeyEvents implements
        ValueChangeHandler<String> {

      public void onKeyDown(KeyDownEvent event) {
        // Make sure that the menu is actually showing. These keystrokes
        // are only relevant when choosing a suggestion.
        if (suggestionPopup.isAttached()) {
          switch (event.getNativeKeyCode()) {
            case KEY_DOWN:
              suggestionMenu.selectItem(suggestionMenu.getSelectedItemIndex() + 1);
              break;
            case KEY_UP:
              suggestionMenu.selectItem(suggestionMenu.getSelectedItemIndex() - 1);
              break;
            case KEY_ENTER:
            case KEY_TAB:
              if (suggestionMenu.getSelectedItemIndex() < 0) {
                suggestionPopup.hide();
              } else {
                suggestionMenu.doSelectedItemAction();
              }
              break;
          }
        }
        fireEvent(event);
      }

      public void onKeyPress(KeyPressEvent event) {
        fireEvent(event);
      }

      public void onKeyUp(KeyUpEvent event) {
        // After every user key input, refresh the popup's suggestions.
        refreshSuggestions();
        fireEvent(event);
      }

      public void onValueChange(ValueChangeEvent<String> event) {
         fireEvent(event);
      }

      private void refreshSuggestions() {
        // Get the raw text.
        String text = box.getText();
        if (text.equals(currentText)) {
          return;
        } else {
          currentText = text;
        }

        if (text.length() == 0) {
          // Optimization to avoid calling showSuggestions with an empty
          // string
          suggestionPopup.hide();
          suggestionMenu.clearItems();
        } else {
          showSuggestions(text);
        }
      }
    }

    TextBoxEvents events = new TextBoxEvents();
    events.addKeyHandlersTo(box);
    box.addValueChangeHandler(events);
  }

  private void fireSuggestionEvent(Suggestion selectedSuggestion) {
    SelectionEvent.fire(this, selectedSuggestion);
  }

  private void setNewSelection(SuggestionMenuItem menuItem) {
    Suggestion curSuggestion = menuItem.getSuggestion();
    currentText = curSuggestion.getReplacementString();
    setText(currentText);
    suggestionPopup.hide();
    fireSuggestionEvent(curSuggestion);
  }

  /**
   * Sets the suggestion oracle used to create suggestions.
   * 
   * @param oracle the oracle
   */
  private void setOracle(SuggestOracle oracle) {
    this.oracle = oracle;
  }

  /**
   * Show the given collection of suggestions.
   * 
   * @param suggestions suggestions to show
   */
  private void showSuggestions(Collection<? extends Suggestion> suggestions) {
    if (suggestions.size() > 0) {

      // Hide the popup before we manipulate the menu within it. If we do not
      // do this, some browsers will redraw the popup as items are removed
      // and added to the menu.
      boolean isAnimationEnabled = suggestionPopup.isAnimationEnabled();
      if (suggestionPopup.isAttached()) {
        suggestionPopup.setAnimationEnabled(false);
        suggestionPopup.hide();
      }

      suggestionMenu.clearItems();

      for (Suggestion curSuggestion : suggestions) {
        final SuggestionMenuItem menuItem = new SuggestionMenuItem(
            curSuggestion, oracle.isDisplayStringHTML());
        menuItem.setCommand(new Command() {
          public void execute() {
            SuggestBox.this.setNewSelection(menuItem);
          }
        });

        suggestionMenu.addItem(menuItem);
      }

      if (selectsFirstItem) {
        // Select the first item in the suggestion menu.
        suggestionMenu.selectItem(0);
      }

      suggestionPopup.showAlignedPopup();
      suggestionPopup.setAnimationEnabled(isAnimationEnabled);
    } else {
      suggestionPopup.hide();
    }
  }

  private void showSuggestions(String query) {
    oracle.requestSuggestions(new Request(query, limit), callBack);
  }
}
