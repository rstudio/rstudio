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
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A single column list of cells.
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
    @Template("<div onclick=\"\" __idx=\"{0}\" class=\"{1}\">{2}</div>")
    SafeHtml div(int idx, String classes, SafeHtml cellContents);
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
    this(cell, getDefaultResources());
  }

  /**
   * Construct a new {@link CellList} with the specified {@link Resources}.
   * 
   * @param cell the cell used to render each item
   * @param resources the resources used for this widget
   */
  public CellList(final Cell<T> cell, Resources resources) {
    super(Document.get().createDivElement(), DEFAULT_PAGE_SIZE);
    this.cell = cell;
    this.style = resources.cellListStyle();
    this.style.ensureInjected();
    addStyleName(this.style.cellListWidget());

    // Create the DOM hierarchy.
    childContainer = Document.get().createDivElement();

    emptyMessageElem = Document.get().createDivElement();
    showOrHide(emptyMessageElem, false);

    DivElement outerDiv = getElement().cast();
    outerDiv.appendChild(childContainer);
    outerDiv.appendChild(emptyMessageElem);

    // Sink events that the cell consumes.
    Set<String> eventsToSink = new HashSet<String>();
    eventsToSink.add("click");
    Set<String> consumedEvents = cell.getConsumedEvents();
    if (consumedEvents != null) {
      eventsToSink.addAll(consumedEvents);
    }
    CellBasedWidgetImpl.get().sinkEvents(this, eventsToSink);
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

  @Override
  public void onBrowserEvent(Event event) {
    CellBasedWidgetImpl.get().onBrowserEvent(this, event);
    super.onBrowserEvent(event);

    // Forward the event to the cell.
    Element target = event.getEventTarget().cast();
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
      int idx = Integer.parseInt(idxString);
      T value = getDisplayedItem(idx - getPageStart());
      SelectionModel<? super T> selectionModel = getSelectionModel();
      if (selectionModel != null && "click".equals(eventType)
          && !cell.handlesSelection()) {
        selectionModel.setSelected(value, true);
      }

      // Fire the event to the cell.
      Set<String> consumedEvents = cell.getConsumedEvents();
      if (consumedEvents != null && consumedEvents.contains(eventType)) {
        cell.onBrowserEvent(target, value, getKey(value), event, valueUpdater);
      }
    }
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
  protected void renderRowValues(SafeHtmlBuilder sb, List<T> values, int start,
      SelectionModel<? super T> selectionModel) {
    int length = values.size();
    int end = start + length;
    for (int i = start; i < end; i++) {
      T value = values.get(i - start);
      boolean isSelected = selectionModel == null ? false
          : selectionModel.isSelected(value);
      // TODO(jlabanca): Factor out __idx because rows can move.
      StringBuilder classesBuilder = new StringBuilder();
      classesBuilder.append(i % 2 == 0 ? style.cellListEvenItem()
          : style.cellListOddItem());
      if (isSelected) {
        classesBuilder.append(" ").append(style.cellListSelectedItem());
      }

      SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
      cell.render(value, null, cellBuilder);

      sb.append(TEMPLATE.div(i, classesBuilder.toString(),
          cellBuilder.toSafeHtml()));
    }
  }

  @Override
  boolean dependsOnSelection() {
    return cell.dependsOnSelection();
  }

  @Override
  Element getChildContainer() {
    return childContainer;
  }

  @Override
  void setLoadingState(LoadingState state) {
    showOrHide(emptyMessageElem, state == LoadingState.EMPTY);
    // TODO(jlabanca): Add a loading icon.
  }

  @Override
  void setSelected(Element elem, boolean selected) {
    setStyleName(elem, style.cellListSelectedItem(), selected);
  }

  /**
   * Get the key for a given value. Defaults to the value if new
   * {@link ProvidesKey} is specified.
   * 
   * @param value the value
   * @return the key for the value
   */
  private Object getKey(T value) {
    ProvidesKey<T> keyProvider = getKeyProvider();
    return keyProvider == null ? value : keyProvider.getKey(value);
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
