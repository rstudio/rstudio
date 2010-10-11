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
package com.google.gwt.user.cellview.client;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.ImportedWithPrefix;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.HasDataPresenter.LoadingState;
import com.google.gwt.user.client.Event;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionModel;

import java.util.List;
import java.util.Set;

/**
 * A single column list of cells.
 *
 * <p>
 * <h3>Examples</h3>
 * <dl>
 * <dt>Trivial example</dt>
 * <dd>{@example com.google.gwt.examples.cellview.CellListExample}</dd>
 * <dt>ValueUpdater example</dt>
 * <dd>{@example com.google.gwt.examples.cellview.CellListValueUpdaterExample}</dd>
 * <dt>Key provider example</dt>
 * <dd>{@example com.google.gwt.examples.view.KeyProviderExample}</dd>
 * </dl>
 * </p>
 *
 * @param <T> the data type of list items
 */
public class CellList<T> extends AbstractHasData<T> {

  /**
   * A ClientBundle that provides images for this widget.
   */
  public interface Resources extends ClientBundle {
    /**
     * The background used for selected items.
     */
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal, flipRtl = true)
    ImageResource cellListSelectedBackground();

    /**
     * The styles used in this widget.
     */
    @Source(Style.DEFAULT_CSS)
    Style cellListStyle();
  }

  /**
   * Styles used by this widget.
   */
  @ImportedWithPrefix("gwt-CellList")
  public interface Style extends CssResource {
    /**
     * The path to the default CSS styles used by this resource.
     */
    String DEFAULT_CSS = "com/google/gwt/user/cellview/client/CellList.css";

    /**
     * Applied to even items.
     */
    String cellListEvenItem();

    /**
     * Applied to the keyboard selected item.
     */
    String cellListKeyboardSelectedItem();

    /**
     * Applied to odd items.
     */
    String cellListOddItem();

    /**
     * Applied to selected items.
     */
    String cellListSelectedItem();

    /**
     * Applied to the widget.
     */
    String cellListWidget();
  }

  interface Template extends SafeHtmlTemplates {
    @Template("<div onclick=\"\" __idx=\"{0}\" class=\"{1}\" style=\"outline:none;\" >{2}</div>")
    SafeHtml div(int idx, String classes, SafeHtml cellContents);

    @Template("<div onclick=\"\" __idx=\"{0}\" class=\"{1}\" style=\"outline:none;\" tabindex=\"{2}\">{3}</div>")
    SafeHtml divFocusable(int idx, String classes, int tabIndex,
        SafeHtml cellContents);

    @Template("<div onclick=\"\" __idx=\"{0}\" class=\"{1}\" style=\"outline:none;\" tabindex=\"{2}\" accesskey=\"{3}\">{4}</div>")
    SafeHtml divFocusableWithKey(int idx, String classes, int tabIndex,
        char accessKey, SafeHtml cellContents);
  }

  /**
   * The default page size.
   */
  private static final int DEFAULT_PAGE_SIZE = 25;

  private static Resources DEFAULT_RESOURCES;

  private static final Template TEMPLATE = GWT.create(Template.class);

  private static Resources getDefaultResources() {
    if (DEFAULT_RESOURCES == null) {
      DEFAULT_RESOURCES = GWT.create(Resources.class);
    }
    return DEFAULT_RESOURCES;
  }

  private final Cell<T> cell;
  private boolean cellIsEditing;
  private final Element childContainer;

  private SafeHtml emptyListMessage = SafeHtmlUtils.fromSafeConstant("");
  private final Element emptyMessageElem;

  private final Style style;

  private ValueUpdater<T> valueUpdater;

  /**
   * Construct a new {@link CellList}.
   *
   * @param cell the cell used to render each item
   */
  public CellList(final Cell<T> cell) {
    this(cell, getDefaultResources(), null);
  }

  /**
   * Construct a new {@link CellList} with the specified {@link Resources}.
   *
   * @param cell the cell used to render each item
   * @param resources the resources used for this widget
   */
  public CellList(final Cell<T> cell, Resources resources) {
    this(cell, resources, null);
  }

  /**
   * Construct a new {@link CellList} with the specified {@link ProvidesKey key provider}.
   *
   * @param cell the cell used to render each item
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *        object should act as its own key
   */
  public CellList(final Cell<T> cell, ProvidesKey<T> keyProvider) {
    this(cell, getDefaultResources(), keyProvider);
  }

  /**
   * Construct a new {@link CellList} with the specified {@link Resources}
   * and {@link ProvidesKey key provider}.
   *
   * @param cell the cell used to render each item
   * @param resources the resources used for this widget
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *        object should act as its own key
   */
  public CellList(final Cell<T> cell, Resources resources, ProvidesKey<T> keyProvider) {
    super(Document.get().createDivElement(), DEFAULT_PAGE_SIZE, keyProvider);
    this.cell = cell;
    this.style = resources.cellListStyle();
    this.style.ensureInjected();

    String widgetStyle = this.style.cellListWidget();
    if (widgetStyle != null) {
      // The widget style is null when used in CellBrowser.
      addStyleName(widgetStyle);
    }

    // Create the DOM hierarchy.
    childContainer = Document.get().createDivElement();

    emptyMessageElem = Document.get().createDivElement();
    showOrHide(emptyMessageElem, false);

    DivElement outerDiv = getElement().cast();
    outerDiv.appendChild(childContainer);
    outerDiv.appendChild(emptyMessageElem);

    // Sink events that the cell consumes.
    CellBasedWidgetImpl.get().sinkEvents(this, cell.getConsumedEvents());
  }

  /**
   * Get the message that is displayed when there is no data.
   *
   * @return the empty message
   */
  public SafeHtml getEmptyListMessage() {
    return emptyListMessage;
  }

  /**
   * Get the {@link Element} for the specified index. If the element has not
   * been created, null is returned.
   *
   * @param indexOnPage the index on the page
   * @return the element, or null if it doesn't exists
   * @throws IndexOutOfBoundsException if the index is outside of the current
   *           page
   */
  public Element getRowElement(int indexOnPage) {
    checkRowBounds(indexOnPage);
    if (childContainer.getChildCount() > indexOnPage) {
      return childContainer.getChild(indexOnPage).cast();
    }
    return null;
  }

  /**
   * Set the message to display when there is no data.
   *
   * @param html the message to display when there are no results
   */
  public void setEmptyListMessage(SafeHtml html) {
    this.emptyListMessage = html;
    emptyMessageElem.setInnerHTML(html.asString());
  }

  /**
   * Set the value updater to use when cells modify items.
   *
   * @param valueUpdater the {@link ValueUpdater}
   */
  public void setValueUpdater(ValueUpdater<T> valueUpdater) {
    this.valueUpdater = valueUpdater;
  }

  @Override
  protected boolean dependsOnSelection() {
    return cell.dependsOnSelection();
  }

  /**
   * Called when a user action triggers selection.
   *
   * @param event the event that triggered selection
   * @param value the value that was selected
   * @param indexOnPage the index of the value on the page
   */
  protected void doSelection(Event event, T value, int indexOnPage) {
    // TODO(jlabanca): Defer to a user provided SelectionManager.
    SelectionModel<? super T> selectionModel = getSelectionModel();
    if (selectionModel != null) {
      selectionModel.setSelected(value, true);
    }
  }

  /**
   * Fire an event to the cell.
   *
   * @param event the event that was fired
   * @param parent the parent of the cell
   * @param value the value of the cell
   */
  protected void fireEventToCell(Event event, Element parent, T value) {
    Set<String> consumedEvents = cell.getConsumedEvents();
    if (consumedEvents != null && consumedEvents.contains(event.getType())) {
      Object key = getValueKey(value);
      boolean cellWasEditing = cell.isEditing(parent, value, key);
      cell.onBrowserEvent(parent, value, key, event, valueUpdater);
      cellIsEditing = cell.isEditing(parent, value, key);
      if (cellWasEditing && !cellIsEditing) {
        CellBasedWidgetImpl.get().resetFocus(new Scheduler.ScheduledCommand() {
          public void execute() {
            setFocus(true);
          }
        });
      }
    }
  }

  /**
   * Return the cell used to render each item.
   */
  protected Cell<T> getCell() {
    return cell;
  }

  /**
   * Get the parent element that wraps the cell from the list item. Override
   * this method if you add structure to the element.
   *
   * @param item the row element that wraps the list item
   * @return the parent element of the cell
   */
  protected Element getCellParent(Element item) {
    return item;
  }

  @Override
  protected Element getChildContainer() {
    return childContainer;
  }

  @Override
  protected Element getKeyboardSelectedElement() {
    int rowIndex = getKeyboardSelectedRow();
    return isRowWithinBounds(rowIndex) ? getRowElement(rowIndex) : null;
  }

  @Override
  protected boolean isKeyboardNavigationSuppressed() {
    return cellIsEditing;
  }

  @Override
  protected void onBlur() {
    // Remove the keyboard selection style.
    Element elem = getKeyboardSelectedElement();
    if (elem != null) {
      elem.removeClassName(style.cellListKeyboardSelectedItem());
    }
  }

  @Override
  protected void onBrowserEvent2(Event event) {
    // Get the event target.
    EventTarget eventTarget = event.getEventTarget();
    if (!Element.is(eventTarget)) {
      return;
    }
    Element target = event.getEventTarget().cast();

    // Forward the event to the cell.
    String idxString = "";
    while ((target != null)
        && ((idxString = target.getAttribute("__idx")).length() == 0)) {
      target = target.getParentElement();
    }
    if (idxString.length() > 0) {
      // Select the item if the cell does not consume events. Selection occurs
      // before firing the event to the cell in case the cell operates on the
      // currently selected item.
      String eventType = event.getType();
      boolean isMouseDown = "mousedown".equals(eventType);
      int idx = Integer.parseInt(idxString);
      int indexOnPage = idx - getPageStart();
      if (!isRowWithinBounds(indexOnPage)) {
        // If the event causes us to page, then the index will be out of bounds.
        return;
      }
      T value = getDisplayedItem(indexOnPage);
      if (isMouseDown && !cell.handlesSelection()) {
        doSelection(event, value, indexOnPage);
      }

      // Focus on the cell.
      if ("focus".equals(eventType) || isMouseDown) {
        isFocused = true;
        doKeyboardSelection(event, value, indexOnPage);
      }

      // Fire the event to the cell if the list has not been refreshed.
      fireEventToCell(event, getCellParent(target), value);
    }
  }

  @Override
  protected void onFocus() {
    // Add the keyboard selection style.
    Element elem = getKeyboardSelectedElement();
    if (elem != null) {
      elem.addClassName(style.cellListKeyboardSelectedItem());
    }
  }

  @Override
  protected void renderRowValues(SafeHtmlBuilder sb, List<T> values, int start,
      SelectionModel<? super T> selectionModel) {
    String keyboardSelectedItem = " " + style.cellListKeyboardSelectedItem();
    String selectedItem = " " + style.cellListSelectedItem();
    String evenItem = style.cellListEvenItem();
    String oddItem = style.cellListOddItem();
    int keyboardSelectedRow = getKeyboardSelectedRow() + getPageStart();
    int length = values.size();
    int end = start + length;
    for (int i = start; i < end; i++) {
      T value = values.get(i - start);
      boolean isSelected = selectionModel == null ? false
          : selectionModel.isSelected(value);

      StringBuilder classesBuilder = new StringBuilder();
      classesBuilder.append(i % 2 == 0 ? evenItem : oddItem);
      if (isSelected) {
        classesBuilder.append(selectedItem);
      }

      SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
      cell.render(value, null, cellBuilder);

      if (i == keyboardSelectedRow) {
        // This is the focused item.
        if (isFocused) {
          classesBuilder.append(keyboardSelectedItem);
        }
        char accessKey = getAccessKey();
        if (accessKey != 0) {
          sb.append(TEMPLATE.divFocusableWithKey(i, classesBuilder.toString(),
              getTabIndex(), accessKey, cellBuilder.toSafeHtml()));
        } else {
          sb.append(TEMPLATE.divFocusable(i, classesBuilder.toString(),
              getTabIndex(), cellBuilder.toSafeHtml()));
        }
      } else {
        sb.append(TEMPLATE.div(i, classesBuilder.toString(),
            cellBuilder.toSafeHtml()));
      }
    }
  }

  @Override
  protected boolean resetFocusOnCell() {
    int row = getKeyboardSelectedRow();
    if (isRowWithinBounds(row)) {
      Element rowElem = getKeyboardSelectedElement();
      Element cellParent = getCellParent(rowElem);
      T value = getDisplayedItem(row);
      Object key = getValueKey(value);
      return cell.resetFocus(cellParent, value, key);
    }
    return false;
  }

  @Override
  protected void setKeyboardSelected(int index, boolean selected,
      boolean stealFocus) {
    if (!isRowWithinBounds(index)) {
      return;
    }

    Element elem = getRowElement(index);
    if (!selected || isFocused || stealFocus) {
      setStyleName(elem, style.cellListKeyboardSelectedItem(), selected);
    }
    setFocusable(elem, selected);
    if (selected && stealFocus) {
      elem.focus();
      onFocus();
    }
  }

  @Override
  protected void setSelected(Element elem, boolean selected) {
    setStyleName(elem, style.cellListSelectedItem(), selected);
  }

  /**
   * Called when the user selects a cell with the mouse or tab key.
   *
   * @param event the event
   * @param value the value that is selected
   * @param indexOnPage the index on the page
   */
  void doKeyboardSelection(Event event, T value, int indexOnPage) {
    if (getPresenter().getKeyboardSelectedRow() != indexOnPage) {
      getPresenter().setKeyboardSelectedRow(indexOnPage, false);
    }
  }

  @Override
  void setLoadingState(LoadingState state) {
    showOrHide(emptyMessageElem, state == LoadingState.EMPTY);
    // TODO(jlabanca): Add a loading icon.
  }

  /**
   * Show or hide an element.
   *
   * @param element the element
   * @param show true to show, false to hide
   */
  private void showOrHide(Element element, boolean show) {
    if (show) {
      element.getStyle().clearDisplay();
    } else {
      element.getStyle().setDisplay(Display.NONE);
    }
  }
}
