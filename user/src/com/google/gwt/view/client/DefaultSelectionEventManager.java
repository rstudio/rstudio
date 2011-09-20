/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.view.client;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An implementation of {@link CellPreviewEvent.Handler} that adds selection
 * support via the spacebar and mouse clicks and handles the control key.
 * 
 * <p>
 * If the {@link HasData} source of the selection event uses a
 * {@link MultiSelectionModel}, this manager additionally provides support for
 * shift key to select a range of values. For all other {@link SelectionModel}s,
 * only the control key is supported.
 * </p>
 * 
 * @param <T> the data type of records in the list
 */
public class DefaultSelectionEventManager<T> implements
    CellPreviewEvent.Handler<T> {

  /**
   * An event translator that disables selection for the specified blacklisted
   * columns.
   * 
   * @param <T> the data type
   */
  public static class BlacklistEventTranslator<T> implements EventTranslator<T> {
    private final Set<Integer> blacklist = new HashSet<Integer>();

    /**
     * Construct a new {@link BlacklistEventTranslator}.
     * 
     * @param blacklistedColumns the columns to blacklist
     */
    public BlacklistEventTranslator(int... blacklistedColumns) {
      if (blacklistedColumns != null) {
        for (int i : blacklistedColumns) {
          setColumnBlacklisted(i, true);
        }
      }
    }

    /**
     * Clear all columns from the blacklist.
     */
    public void clearBlacklist() {
      blacklist.clear();
    }

    public boolean clearCurrentSelection(CellPreviewEvent<T> event) {
      return false;
    }

    /**
     * Check if the specified column is blacklisted.
     * 
     * @param index the column index
     * @return true if blacklisted, false if not
     */
    public boolean isColumnBlacklisted(int index) {
      return blacklist.contains(index);
    }

    /**
     * Set whether or not the specified column in blacklisted.
     * 
     * @param index the column index
     * @param isBlacklisted true to blacklist, false to allow selection
     */
    public void setColumnBlacklisted(int index, boolean isBlacklisted) {
      if (isBlacklisted) {
        blacklist.add(index);
      } else {
        blacklist.remove(index);
      }
    }

    public SelectAction translateSelectionEvent(CellPreviewEvent<T> event) {
      return isColumnBlacklisted(event.getColumn()) ? SelectAction.IGNORE
          : SelectAction.DEFAULT;
    }
  }
  
  /**
   * Implementation of {@link EventTranslator} that only triggers selection when
   * any checkbox is selected.
   * 
   * @param <T> the data type
   */
  public static class CheckboxEventTranslator<T> implements EventTranslator<T> {

    /**
     * The column index of the checkbox. Other columns are ignored.
     */
    private final int column;

    /**
     * Construct a new {@link CheckboxEventTranslator} that will trigger
     * selection when any checkbox in any column is selected.
     */
    public CheckboxEventTranslator() {
      this(-1);
    }

    /**
     * Construct a new {@link CheckboxEventTranslator} that will trigger
     * selection when a checkbox in the specified column is selected.
     * 
     * @param column the column index, or -1 for all columns
     */
    public CheckboxEventTranslator(int column) {
      this.column = column;
    }

    public boolean clearCurrentSelection(CellPreviewEvent<T> event) {
      return false;
    }

    public SelectAction translateSelectionEvent(CellPreviewEvent<T> event) {
      // Handle the event.
      NativeEvent nativeEvent = event.getNativeEvent();
      if (BrowserEvents.CLICK.equals(nativeEvent.getType())) {
        // Ignore if the event didn't occur in the correct column.
        if (column > -1 && column != event.getColumn()) {
          return SelectAction.IGNORE;
        }

        // Determine if we clicked on a checkbox.
        Element target = nativeEvent.getEventTarget().cast();
        if ("input".equals(target.getTagName().toLowerCase())) {
          final InputElement input = target.cast();
          if ("checkbox".equals(input.getType().toLowerCase())) {
            // Synchronize the checkbox with the current selection state.
            input.setChecked(event.getDisplay().getSelectionModel().isSelected(
                event.getValue()));
            return SelectAction.TOGGLE;
          }
        }
        return SelectAction.IGNORE;
      }

      // For keyboard events, do the default action.
      return SelectAction.DEFAULT;
    }
  }

  /**
   * Translates {@link CellPreviewEvent}s into {@link SelectAction}s.
   */
  public static interface EventTranslator<T> {
    /**
     * Check whether a user selection event should clear all currently selected
     * values.
     * 
     * @param event the {@link CellPreviewEvent} to translate
     */
    boolean clearCurrentSelection(CellPreviewEvent<T> event);

    /**
     * Translate the user selection event into a {@link SelectAction}.
     * 
     * @param event the {@link CellPreviewEvent} to translate
     */
    SelectAction translateSelectionEvent(CellPreviewEvent<T> event);
  }

  /**
   * The action that controls how selection is handled.
   */
  public static enum SelectAction {
    DEFAULT, // Perform the default action.
    SELECT, // Select the value.
    DESELECT, // Deselect the value.
    TOGGLE, // Toggle the selected state of the value.
    IGNORE; // Ignore the event.
  }

  /**
   * An event translator that allows selection only for the specified
   * whitelisted columns.
   * 
   * @param <T> the data type
   */
  public static class WhitelistEventTranslator<T> implements EventTranslator<T> {
    private final Set<Integer> whitelist = new HashSet<Integer>();

    /**
     * Construct a new {@link WhitelistEventTranslator}.
     * 
     * @param whitelistedColumns the columns to whitelist
     */
    public WhitelistEventTranslator(int... whitelistedColumns) {
      if (whitelistedColumns != null) {
        for (int i : whitelistedColumns) {
          setColumnWhitelisted(i, true);
        }
      }
    }

    public boolean clearCurrentSelection(CellPreviewEvent<T> event) {
      return false;
    }

    /**
     * Clear all columns from the whitelist.
     */
    public void clearWhitelist() {
      whitelist.clear();
    }

    /**
     * Check if the specified column is whitelisted.
     * 
     * @param index the column index
     * @return true if whitelisted, false if not
     */
    public boolean isColumnWhitelisted(int index) {
      return whitelist.contains(index);
    }

    /**
     * Set whether or not the specified column in whitelisted.
     * 
     * @param index the column index
     * @param isWhitelisted true to whitelist, false to allow disallow selection
     */
    public void setColumnWhitelisted(int index, boolean isWhitelisted) {
      if (isWhitelisted) {
        whitelist.add(index);
      } else {
        whitelist.remove(index);
      }
    }

    public SelectAction translateSelectionEvent(CellPreviewEvent<T> event) {
      return isColumnWhitelisted(event.getColumn()) ? SelectAction.DEFAULT
          : SelectAction.IGNORE;
    }
  }

  /**
   * Construct a new {@link DefaultSelectionEventManager} that ignores selection
   * for the columns in the specified blacklist.
   * 
   * @param <T> the data type of the display
   * @param blacklistedColumns the columns to include in the blacklist
   * @return a {@link DefaultSelectionEventManager} instance
   */
  public static <T> DefaultSelectionEventManager<T> createBlacklistManager(
      int... blacklistedColumns) {
    return new DefaultSelectionEventManager<T>(new BlacklistEventTranslator<T>(
        blacklistedColumns));
  }
  
  /**
   * Construct a new {@link DefaultSelectionEventManager} that triggers
   * selection when any checkbox in any column is clicked.
   * 
   * @param <T> the data type of the display
   * @return a {@link DefaultSelectionEventManager} instance
   */
  public static <T> DefaultSelectionEventManager<T> createCheckboxManager() {
    return new DefaultSelectionEventManager<T>(new CheckboxEventTranslator<T>());
  }

  /**
   * Construct a new {@link DefaultSelectionEventManager} that triggers
   * selection when a checkbox in the specified column is clicked.
   * 
   * @param <T> the data type of the display
   * @param column the column to handle
   * @return a {@link DefaultSelectionEventManager} instance
   */
  public static <T> DefaultSelectionEventManager<T> createCheckboxManager(
      int column) {
    return new DefaultSelectionEventManager<T>(new CheckboxEventTranslator<T>(
        column));
  }

  /**
   * Create a new {@link DefaultSelectionEventManager} using the specified
   * {@link EventTranslator} to control which {@link SelectAction} to take for
   * each event.
   * 
   * @param <T> the data type of the display
   * @param translator the {@link EventTranslator} to use
   * @return a {@link DefaultSelectionEventManager} instance
   */
  public static <T> DefaultSelectionEventManager<T> createCustomManager(
      EventTranslator<T> translator) {
    return new DefaultSelectionEventManager<T>(translator);
  }

  /**
   * Create a new {@link DefaultSelectionEventManager} that handles selection
   * via user interactions.
   * 
   * @param <T> the data type of the display
   * @return a new {@link DefaultSelectionEventManager} instance
   */
  public static <T> DefaultSelectionEventManager<T> createDefaultManager() {
    return new DefaultSelectionEventManager<T>(null);
  }

  /**
   * Construct a new {@link DefaultSelectionEventManager} that allows selection
   * only for the columns in the specified whitelist.
   * 
   * @param <T> the data type of the display
   * @param whitelistedColumns the columns to include in the whitelist
   * @return a {@link DefaultSelectionEventManager} instance
   */
  public static <T> DefaultSelectionEventManager<T> createWhitelistManager(
      int... whitelistedColumns) {
    return new DefaultSelectionEventManager<T>(new WhitelistEventTranslator<T>(
        whitelistedColumns));
  }

  /**
   * The last {@link HasData} that was handled.
   */
  private HasData<T> lastDisplay;

  /**
   * The last page start.
   */
  private int lastPageStart;

  /**
   * The last selected row index.
   */
  private int lastSelectedIndex = -1;

  /**
   * A boolean indicating that the last shift selection was additive.
   */
  private boolean shiftAdditive;

  /**
   * The last place where the user clicked without holding shift. Multi
   * selections that use the shift key are rooted at the anchor.
   */
  private int shiftAnchor = -1;

  /**
   * The {@link EventTranslator} that controls how selection is handled.
   */
  private final EventTranslator<T> translator;

  /**
   * Construct a new {@link DefaultSelectionEventManager} using the specified
   * {@link EventTranslator} to control which {@link SelectAction} to take for
   * each event.
   * 
   * @param translator the {@link EventTranslator} to use
   */
  protected DefaultSelectionEventManager(EventTranslator<T> translator) {
    this.translator = translator;
  }

  /**
   * Update the selection model based on a user selection event.
   * 
   * @param selectionModel the selection model to update
   * @param row the absolute index of the selected row
   * @param rowValue the selected row value
   * @param action the {@link SelectAction} to apply
   * @param selectRange true to select the range from the last selected row
   * @param clearOthers true to clear the current selection
   */
  public void doMultiSelection(MultiSelectionModel<? super T> selectionModel,
      HasData<T> display, int row, T rowValue, SelectAction action,
      boolean selectRange, boolean clearOthers) {
    // Determine if we will add or remove selection.
    boolean addToSelection = true;
    if (action != null) {
      switch (action) {
        case IGNORE:
          // Ignore selection.
          return;
        case SELECT:
          addToSelection = true;
          break;
        case DESELECT:
          addToSelection = false;
          break;
        case TOGGLE:
          addToSelection = !selectionModel.isSelected(rowValue);
          break;
      }
    }

    // Determine which rows will be newly selected.
    int pageStart = display.getVisibleRange().getStart();
    if (selectRange && pageStart == lastPageStart && lastSelectedIndex > -1
        && shiftAnchor > -1 && display == lastDisplay) {
      /*
       * Get the new shift bounds based on the existing shift anchor and the
       * selected row.
       */
      int start = Math.min(shiftAnchor, row); // Inclusive.
      int end = Math.max(shiftAnchor, row); // Inclusive.

      if (lastSelectedIndex < start) {
        // Revert previous selection if the user reselects a smaller range.
        setRangeSelection(selectionModel, display, new Range(lastSelectedIndex,
            start - lastSelectedIndex), !shiftAdditive, false);
      } else if (lastSelectedIndex > end) {
        // Revert previous selection if the user reselects a smaller range.
        setRangeSelection(selectionModel, display, new Range(end + 1,
            lastSelectedIndex - end), !shiftAdditive, false);
      } else {
        // Remember if we are adding or removing rows.
        shiftAdditive = addToSelection;
      }

      // Update the last selected row, but do not move the shift anchor.
      lastSelectedIndex = row;

      // Select the range.
      setRangeSelection(selectionModel, display, new Range(start, end - start
          + 1), shiftAdditive, clearOthers);
    } else {
      /*
       * If we are not selecting a range, save the last row and set the shift
       * anchor.
       */
      lastDisplay = display;
      lastPageStart = pageStart;
      lastSelectedIndex = row;
      shiftAnchor = row;
      selectOne(selectionModel, rowValue, addToSelection, clearOthers);
    }
  }

  public void onCellPreview(CellPreviewEvent<T> event) {
    // Early exit if selection is already handled or we are editing.
    if (event.isCellEditing() || event.isSelectionHandled()) {
      return;
    }

    // Early exit if we do not have a SelectionModel.
    HasData<T> display = event.getDisplay();
    SelectionModel<? super T> selectionModel = display.getSelectionModel();
    if (selectionModel == null) {
      return;
    }

    // Check for user defined actions.
    SelectAction action = (translator == null) ? SelectAction.DEFAULT
        : translator.translateSelectionEvent(event);

    // Handle the event based on the SelectionModel type.
    if (selectionModel instanceof MultiSelectionModel<?>) {
      // Add shift key support for MultiSelectionModel.
      handleMultiSelectionEvent(event, action,
          (MultiSelectionModel<? super T>) selectionModel);
    } else {
      // Use the standard handler.
      handleSelectionEvent(event, action, selectionModel);
    }
  }

  /**
   * Removes all items from the selection.
   * 
   * @param selectionModel the {@link MultiSelectionModel} to clear
   */
  protected void clearSelection(MultiSelectionModel<? super T> selectionModel) {
    selectionModel.clear();
  }

  /**
   * Handle an event that could cause a value to be selected for a
   * {@link MultiSelectionModel}. This overloaded method adds support for both
   * the control and shift keys. If the shift key is held down, all rows between
   * the previous selected row and the current row are selected.
   * 
   * @param event the {@link CellPreviewEvent} that triggered selection
   * @param action the action to handle
   * @param selectionModel the {@link SelectionModel} to update
   */
  protected void handleMultiSelectionEvent(CellPreviewEvent<T> event,
      SelectAction action, MultiSelectionModel<? super T> selectionModel) {
    NativeEvent nativeEvent = event.getNativeEvent();
    String type = nativeEvent.getType();
    if (BrowserEvents.CLICK.equals(type)) {
      /*
       * Update selection on click. Selection is toggled only if the user
       * presses the ctrl key. If the user does not press the control key,
       * selection is additive.
       */
      boolean shift = nativeEvent.getShiftKey();
      boolean ctrlOrMeta = nativeEvent.getCtrlKey() || nativeEvent.getMetaKey();
      boolean clearOthers = (translator == null) ? !ctrlOrMeta
          : translator.clearCurrentSelection(event);
      if (action == null || action == SelectAction.DEFAULT) {
        action = ctrlOrMeta ? SelectAction.TOGGLE : SelectAction.SELECT;
      }
      doMultiSelection(selectionModel, event.getDisplay(), event.getIndex(),
          event.getValue(), action, shift, clearOthers);
    } else if (BrowserEvents.KEYUP.equals(type)) {
      int keyCode = nativeEvent.getKeyCode();
      if (keyCode == 32) {
        /*
         * Update selection when the space bar is pressed. The spacebar always
         * toggles selection, regardless of whether the control key is pressed.
         */
        boolean shift = nativeEvent.getShiftKey();
        boolean clearOthers = (translator == null) ? false
            : translator.clearCurrentSelection(event);
        if (action == null || action == SelectAction.DEFAULT) {
          action = SelectAction.TOGGLE;
        }
        doMultiSelection(selectionModel, event.getDisplay(), event.getIndex(),
            event.getValue(), action, shift, clearOthers);
      }
    }
  }

  /**
   * Handle an event that could cause a value to be selected. This method works
   * for any {@link SelectionModel}. Pressing the space bar or ctrl+click will
   * toggle the selection state. Clicking selects the row if it is not selected.
   * 
   * @param event the {@link CellPreviewEvent} that triggered selection
   * @param action the action to handle
   * @param selectionModel the {@link SelectionModel} to update
   */
  protected void handleSelectionEvent(CellPreviewEvent<T> event,
      SelectAction action, SelectionModel<? super T> selectionModel) {
    // Handle selection overrides.
    T value = event.getValue();
    if (action != null) {
      switch (action) {
        case IGNORE:
          return;
        case SELECT:
          selectionModel.setSelected(value, true);
          return;
        case DESELECT:
          selectionModel.setSelected(value, false);
          return;
        case TOGGLE:
          selectionModel.setSelected(value, !selectionModel.isSelected(value));
          return;
      }
    }

    // Handle default selection.
    NativeEvent nativeEvent = event.getNativeEvent();
    String type = nativeEvent.getType();
    if (BrowserEvents.CLICK.equals(type)) {
      if (nativeEvent.getCtrlKey() || nativeEvent.getMetaKey()) {
        // Toggle selection on ctrl+click.
        selectionModel.setSelected(value, !selectionModel.isSelected(value));
      } else {
        // Select on click.
        selectionModel.setSelected(value, true);
      }
    } else if (BrowserEvents.KEYUP.equals(type)) {
      // Toggle selection on space.
      int keyCode = nativeEvent.getKeyCode();
      if (keyCode == 32) {
        selectionModel.setSelected(value, !selectionModel.isSelected(value));
      }
    }
  }

  /**
   * Selects the given item, optionally clearing any prior selection.
   * 
   * @param selectionModel the {@link MultiSelectionModel} to update
   * @param target the item to select
   * @param selected true to select, false to deselect
   * @param clearOthers true to clear all other selected items
   */
  protected void selectOne(MultiSelectionModel<? super T> selectionModel,
      T target, boolean selected, boolean clearOthers) {
    if (clearOthers) {
      clearSelection(selectionModel);
    }
    selectionModel.setSelected(target, selected);
  }

  /**
   * Select or deselect a range of row indexes, optionally deselecting all other
   * values.
   * 
   * @param selectionModel the {@link MultiSelectionModel} to update
   * @param display the {@link HasData} source of the selection event
   * @param range the {@link Range} of rows to select or deselect
   * @param addToSelection true to select, false to deselect the range
   * @param clearOthers true to deselect rows not in the range
   */
  protected void setRangeSelection(
      MultiSelectionModel<? super T> selectionModel, HasData<T> display,
      Range range, boolean addToSelection, boolean clearOthers) {
    // Get the list of values to select.
    List<T> toUpdate = new ArrayList<T>();
    int itemCount = display.getVisibleItemCount();
    int relativeStart = range.getStart() - display.getVisibleRange().getStart();
    int relativeEnd = relativeStart + range.getLength();
    for (int i = relativeStart; i < relativeEnd && i < itemCount; i++) {
      toUpdate.add(display.getVisibleItem(i));
    }

    // Clear all other values.
    if (clearOthers) {
      clearSelection(selectionModel);
    }

    // Update the state of the values.
    for (T value : toUpdate) {
      selectionModel.setSelected(value, addToSelection);
    }
  }
}
