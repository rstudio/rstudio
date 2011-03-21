/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HandlesAllFocusEvents;
import com.google.gwt.event.dom.client.HandlesAllKeyEvents;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasErrorHandlers;
import com.google.gwt.event.dom.client.HasLoadHandlers;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.dom.client.HasMouseMoveHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;
import com.google.gwt.event.dom.client.HasMouseUpHandlers;
import com.google.gwt.event.dom.client.HasMouseWheelHandlers;
import com.google.gwt.event.dom.client.HasScrollHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.user.client.BaseListenerWrapper;
import com.google.gwt.user.client.Event;

import java.util.EventListener;

/**
 * Legacy listener support hierarchy for
 * <code>com.google.gwt.user.client.ui</code>. Gathers the bulk of the legacy
 * glue code in one place, for easy deletion when Listener methods are deleted.
 * 
 * 
 * @param <T> listener type
 * @deprecated will be removed in GWT 2.0 with the handler listeners themselves
 */
@Deprecated
public abstract class ListenerWrapper<T> extends BaseListenerWrapper<T> {

  /**
   * Wrapper for a {@link LoadListener}.
   */
  public static class WrappedLoadListener extends ListenerWrapper<LoadListener>
      implements LoadHandler, ErrorHandler {

    /**
     * Adds the wrapped listener.
     * 
     * @param <S> the source of the events
     * 
     * @param source the event source
     * @param listener the listener
     * @return the wrapped listener
     * 
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static <S extends HasLoadHandlers & HasErrorHandlers> WrappedLoadListener add(
        S source, LoadListener listener) {
      WrappedLoadListener l = new WrappedLoadListener(listener);
      source.addLoadHandler(l);
      source.addErrorHandler(l);
      return l;
    }

    /**
     * Removes the wrapped listener.
     * 
     * @param eventSource the event source from which to remove the wrapped
     *          listener
     * @param listener the listener to remove
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static void remove(Widget eventSource, LoadListener listener) {
      baseRemove(eventSource, listener, LoadEvent.getType(),
          ErrorEvent.getType());
    }

    private WrappedLoadListener(LoadListener listener) {
      super(listener);
    }

    public void onError(ErrorEvent event) {
      getListener().onError(getSource(event));
    }

    public void onLoad(LoadEvent event) {
      getListener().onLoad(getSource(event));
    }
  }
  /**
   * Wrapper for a {@link ChangeListener}.
   * 
   * @deprecated will be removed in GWT 2.0 along with the listeners being
   *             wrapped
   */
  @Deprecated
  public static class WrappedChangeListener extends
      ListenerWrapper<ChangeListener> implements ChangeHandler {

    /**
     * Adds the wrapped listener.
     * 
     * @param source the event source
     * @param listener the listener
     * @return the wrapped listener
     * 
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static WrappedChangeListener add(HasChangeHandlers source,
        ChangeListener listener) {
      WrappedChangeListener rtn = new WrappedChangeListener(listener);
      source.addChangeHandler(rtn);
      return rtn;
    }

    /**
     * Removes the wrapped listener.
     * 
     * @param eventSource the event source from which to remove the wrapped
     *          listener
     * @param listener the listener to remove
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static void remove(Widget eventSource, ChangeListener listener) {
      baseRemove(eventSource, listener, ChangeEvent.getType());
    }

    WrappedChangeListener(ChangeListener listener) {
      super(listener);
    }

    public void onChange(ChangeEvent event) {
      getListener().onChange(getSource(event));
    }
  }

  /**
   * Wrapper for a {@link ClickListener}.
   * 
   * @deprecated will be removed in GWT 2.0 along with the listeners being
   *             wrapped
   */
  @Deprecated
  public static class WrappedClickListener extends
      ListenerWrapper<ClickListener> implements ClickHandler {

    /**
     * Adds the wrapped listener.
     * 
     * @param source the event source
     * @param listener the listener
     * @return the wrapped listener
     * 
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static WrappedClickListener add(HasClickHandlers source,
        ClickListener listener) {
      WrappedClickListener rtn = new WrappedClickListener(listener);
      source.addClickHandler(rtn);
      return rtn;
    }

    /**
     * Removes the wrapped listener.
     * 
     * @param eventSource the event source from which to remove the wrapped
     *          listener
     * @param listener the listener to remove
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static void remove(Widget eventSource, ClickListener listener) {
      baseRemove(eventSource, listener, ClickEvent.getType());
    }

    private WrappedClickListener(ClickListener listener) {
      super(listener);
    }

    public void onClick(ClickEvent event) {
      getListener().onClick(getSource(event));
    }
  }
  /**
   * Wrapper for a {@link FocusListener}.
   */
  public static class WrappedFocusListener extends
      ListenerWrapper<FocusListener> implements FocusHandler, BlurHandler {

    /**
     * Adds the wrapped listener.
     * 
     * @param eventSource the event source
     * @param listener the listener
     * 
     * @return the wrapped listener
     * 
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static WrappedFocusListener add(HasAllFocusHandlers eventSource,
        FocusListener listener) {
      WrappedFocusListener rtn = new WrappedFocusListener(listener);
      HandlesAllFocusEvents.handle(eventSource, rtn);
      return rtn;
    }

    /**
     * Removes the wrapped listener.
     * 
     * @param eventSource the event source from which to remove the wrapped
     *          listener
     * @param listener the listener to remove
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static void remove(Widget eventSource, FocusListener listener) {
      baseRemove(eventSource, listener, LoadEvent.getType(),
          ErrorEvent.getType());
    }

    private WrappedFocusListener(FocusListener listener) {
      super(listener);
    }

    public void onBlur(BlurEvent event) {
      getListener().onLostFocus(getSource(event));
    }

    public void onFocus(FocusEvent event) {
      getListener().onFocus(getSource(event));
    }
  }

  /**
   * Wrapper for a {@link KeyboardListener}.
   */
  public static class WrappedKeyboardListener extends
      ListenerWrapper<KeyboardListener> implements KeyDownHandler,
      KeyUpHandler, KeyPressHandler {

    /**
     * Adds the wrapped listener.
     * 
     * @param source the event source
     * @param listener the listener
     * @return the wrapped listener
     * 
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static WrappedKeyboardListener add(HasAllKeyHandlers source,
        KeyboardListener listener) {
      WrappedKeyboardListener b = new WrappedKeyboardListener(listener);
      HandlesAllKeyEvents.addHandlers(source, b);
      return b;
    }

    /**
     * Removes the wrapped listener.
     * 
     * @param eventSource the event source from which to remove the wrapped
     *          listener
     * @param listener the listener to remove
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static void remove(Widget eventSource, KeyboardListener listener) {
      ListenerWrapper.baseRemove(eventSource, listener, KeyDownEvent.getType(),
          KeyUpEvent.getType(), KeyPressEvent.getType());
    }

    private WrappedKeyboardListener(KeyboardListener listener) {
      super(listener);
    }

    public void onKeyDown(KeyDownEvent event) {
      getListener().onKeyDown(
          getSource(event),
          (char) event.getNativeKeyCode(),
          KeyboardListenerCollection.getKeyboardModifiers(Event.as(event.getNativeEvent())));
    }

    public void onKeyPress(KeyPressEvent event) {
      getListener().onKeyPress(
          getSource(event),
          (char) event.getNativeEvent().getKeyCode(),
          KeyboardListenerCollection.getKeyboardModifiers(Event.as(event.getNativeEvent())));
    }

    public void onKeyUp(KeyUpEvent event) {
      getSource(event);
      getListener().onKeyUp(
          getSource(event),
          (char) event.getNativeKeyCode(),
          KeyboardListenerCollection.getKeyboardModifiers(Event.as(event.getNativeEvent())));
    }
  }

  /**
   * Wrapper for a {@link ChangeListener} being converted to a logical
   * {@link ValueChangeHandler}.
   * 
   * @param <V> the type of the value changed
   * 
   * @deprecated will be removed in GWT 2.0 along with the listeners being
   *             wrapped
   */
  @Deprecated
  public static class WrappedLogicalChangeListener<V> extends
      ListenerWrapper<ChangeListener> implements ValueChangeHandler<V> {

    /**
     * Adds the wrapped listener.
     * 
     * @param <V> the type of value changed
     * 
     * @param source the event source
     * @param listener the listener
     * @return the wrapped listener
     * 
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static <V> WrappedLogicalChangeListener<V> add(
        HasValueChangeHandlers<V> source, ChangeListener listener) {
      WrappedLogicalChangeListener<V> rtn = new WrappedLogicalChangeListener<V>(
          listener);
      source.addValueChangeHandler(rtn);
      return rtn;
    }

    /**
     * Removes the wrapped listener.
     * 
     * @param eventSource the event source from which to remove the wrapped
     *          listener
     * @param listener the listener to remove
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static void remove(Widget eventSource, ChangeListener listener) {
      baseRemove(eventSource, listener, ValueChangeEvent.getType());
    }

    private WrappedLogicalChangeListener(ChangeListener listener) {
      super(listener);
    }

    public void onValueChange(ValueChangeEvent<V> event) {
      getListener().onChange(getSource(event));
    }
  }
  /**
   * Wrapper for a {@link MouseListener}.
   */
  public static class WrappedMouseListener extends
      ListenerWrapper<MouseListener> implements MouseDownHandler,
      MouseUpHandler, MouseOutHandler, MouseOverHandler, MouseMoveHandler {
    /**
     * Adds the wrapped listener.
     * 
     * @param source the event source
     * @param listener the listener
     * @return the wrapped listener
     * @param <E> source of the handlers
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static <E extends HasMouseDownHandlers & HasMouseUpHandlers & HasMouseOutHandlers & HasMouseOverHandlers & HasMouseMoveHandlers> WrappedMouseListener add(
        E source, MouseListener listener) {
      WrappedMouseListener handlers = new WrappedMouseListener(listener);
      source.addMouseDownHandler(handlers);
      source.addMouseUpHandler(handlers);
      source.addMouseOutHandler(handlers);
      source.addMouseOverHandler(handlers);
      source.addMouseMoveHandler(handlers);
      return handlers;
    }

    /**
     * Removes the wrapped listener.
     * 
     * @param eventSource the event source from which to remove the wrapped
     *          listener
     * @param listener the listener to remove
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static void remove(Widget eventSource, MouseListener listener) {
      baseRemove(eventSource, listener, MouseDownEvent.getType(),
          MouseUpEvent.getType(), MouseOverEvent.getType(),
          MouseOutEvent.getType());
    }

    private WrappedMouseListener(MouseListener listener) {
      super(listener);
    }

    public void onMouseDown(MouseDownEvent event) {
      Widget source = getSource(event);
      getListener().onMouseDown(source, event.getX(), event.getY());
    }

    public void onMouseMove(MouseMoveEvent event) {
      Widget source = getSource(event);
      getListener().onMouseMove(source, event.getX(), event.getY());
    }

    public void onMouseOut(MouseOutEvent event) {
      getListener().onMouseLeave(getSource(event));
    }

    public void onMouseOver(MouseOverEvent event) {
      getListener().onMouseEnter(getSource(event));
    }

    public void onMouseUp(MouseUpEvent event) {
      Widget source = getSource(event);
      getListener().onMouseUp(source, event.getX(), event.getY());
    }
  }
  /**
   * Wrapper for a {@link MouseWheelListener}.
   */
  public static class WrappedMouseWheelListener extends
      ListenerWrapper<MouseWheelListener> implements MouseWheelHandler {
    /**
     * Adds the wrapped listener.
     * 
     * @param source the event source
     * @param listener the listener
     * @return the wrapped listener
     * 
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static WrappedMouseWheelListener add(HasMouseWheelHandlers source,
        MouseWheelListener listener) {
      WrappedMouseWheelListener wrap = new WrappedMouseWheelListener(listener);
      source.addMouseWheelHandler(new WrappedMouseWheelListener(listener));
      return wrap;
    }

    /**
     * Removes the wrapped listener.
     * 
     * @param eventSource the event source from which to remove the wrapped
     *          listener
     * @param listener the listener to remove
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static void remove(Widget eventSource, MouseWheelListener listener) {
      baseRemove(eventSource, listener, MouseWheelEvent.getType());
    }

    private WrappedMouseWheelListener(MouseWheelListener listener) {
      super(listener);
    }

    public void onMouseWheel(MouseWheelEvent event) {
      getListener().onMouseWheel(getSource(event),
          new MouseWheelVelocity(Event.as(event.getNativeEvent())));
    }
  }
  /**
   * Wrapper for a {@link ScrollListener}.
   */
  public static class WrappedScrollListener extends
      ListenerWrapper<ScrollListener> implements ScrollHandler {

    /**
     * Adds the wrapped listener.
     * 
     * @param source the event source
     * @param listener the listener
     * @return the wrapped listener
     * 
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static WrappedScrollListener add(HasScrollHandlers source,
        ScrollListener listener) {
      WrappedScrollListener s = new WrappedScrollListener(listener);
      source.addScrollHandler(s);
      return s;
    }

    /**
     * Removes the wrapped listener.
     * 
     * @param eventSource the event source from which to remove the wrapped
     *          listener
     * @param listener the listener to remove
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static void remove(Widget eventSource, ScrollListener listener) {
      baseRemove(eventSource, listener, ScrollEvent.getType(),
          ErrorEvent.getType());
    }

    private WrappedScrollListener(ScrollListener listener) {
      super(listener);
    }

    public void onScroll(ScrollEvent event) {
      Widget source = getSource(event);
      Element elem = source.getElement();
      getListener().onScroll(getSource(event), elem.getScrollLeft(),
          elem.getScrollTop());
    }
  }
  static class WrappedOldDisclosureHandler extends
      ListenerWrapper<DisclosureHandler> implements
      CloseHandler<DisclosurePanel>, OpenHandler<DisclosurePanel> {

    public static void add(DisclosurePanel source, DisclosureHandler listener) {
      WrappedOldDisclosureHandler handlers = new WrappedOldDisclosureHandler(
          listener);
      source.addOpenHandler(handlers);
      source.addCloseHandler(handlers);
    }

    public static void remove(Widget eventSource, DisclosureHandler listener) {
      baseRemove(eventSource, listener, CloseEvent.getType(),
          OpenEvent.getType());
    }

    private WrappedOldDisclosureHandler(DisclosureHandler listener) {
      super(listener);
    }

    public void onClose(CloseEvent<DisclosurePanel> event) {
      getListener().onClose(
          new DisclosureEvent((DisclosurePanel) event.getSource()));
    }

    public void onOpen(OpenEvent<DisclosurePanel> event) {
      getListener().onOpen(
          new DisclosureEvent((DisclosurePanel) event.getSource()));
    }
  }

  static class WrappedOldFormHandler extends ListenerWrapper<FormHandler>
      implements FormPanel.SubmitHandler, FormPanel.SubmitCompleteHandler {

    public static void add(FormPanel source, FormHandler listener) {
      WrappedOldFormHandler handlers = new WrappedOldFormHandler(listener);
      source.addSubmitHandler(handlers);
      source.addSubmitCompleteHandler(handlers);
    }

    public static void remove(Widget eventSource, FormHandler listener) {
      baseRemove(eventSource, listener, FormPanel.SubmitEvent.getType(),
          FormPanel.SubmitCompleteEvent.getType());
    }

    private WrappedOldFormHandler(FormHandler listener) {
      super(listener);
    }

    public void onSubmit(FormPanel.SubmitEvent event) {
      FormSubmitEvent fse = new FormSubmitEvent((FormPanel) event.getSource());
      getListener().onSubmit(fse);
      if (fse.isSetCancelledCalled()) {
        event.setCanceled(fse.isCancelled());
      }
    }

    public void onSubmitComplete(FormPanel.SubmitCompleteEvent event) {
      getListener().onSubmitComplete(
          new FormSubmitCompleteEvent((FormPanel) event.getSource(),
              event.getResults()));
    }
  }

  static class WrappedOldSuggestionHandler extends
      ListenerWrapper<SuggestionHandler> implements
      SelectionHandler<SuggestOracle.Suggestion> {
    /**
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static void add(SuggestBox source, SuggestionHandler listener) {
      source.addSelectionHandler(new WrappedOldSuggestionHandler(listener));
    }

    public static void remove(Widget eventSource, SuggestionHandler listener) {
      baseRemove(eventSource, listener, SelectionEvent.getType());
    }

    private WrappedOldSuggestionHandler(SuggestionHandler listener) {
      super(listener);
    }

    public void onSelection(SelectionEvent<SuggestOracle.Suggestion> event) {
      getListener().onSuggestionSelected(
          new SuggestionEvent((SuggestBox) event.getSource(),
              event.getSelectedItem()));
    }
  }

  static class WrappedPopupListener extends ListenerWrapper<PopupListener>
      implements CloseHandler<PopupPanel> {

    public static void add(HasCloseHandlers<PopupPanel> source,
        PopupListener listener) {
      source.addCloseHandler(new WrappedPopupListener(listener));
    }

    public static void remove(Widget eventSource, PopupListener listener) {
      baseRemove(eventSource, listener, CloseEvent.getType());
    }

    private WrappedPopupListener(PopupListener listener) {
      super(listener);
    }

    public void onClose(CloseEvent<PopupPanel> event) {
      getListener().onPopupClosed((PopupPanel) event.getSource(),
          event.isAutoClosed());
    }
  }

  static class WrappedTableListener extends ListenerWrapper<TableListener>
      implements ClickHandler {
    /**
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static void add(HasClickHandlers source, TableListener listener) {
      source.addClickHandler(new WrappedTableListener(listener));
    }

    public static void remove(Widget eventSource, TableListener listener) {
      baseRemove(eventSource, listener, ClickEvent.getType());
    }

    private WrappedTableListener(TableListener listener) {
      super(listener);
    }

    public void onClick(ClickEvent event) {
      HTMLTable table = (HTMLTable) event.getSource();
      HTMLTable.Cell cell = table.getCellForEvent(event);
      if (cell != null) {
        getListener().onCellClicked(table, cell.getRowIndex(),
            cell.getCellIndex());
      }
    }
  }

  static class WrappedTabListener extends ListenerWrapper<TabListener>
      implements SelectionHandler<Integer>, BeforeSelectionHandler<Integer> {
    /**
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static void add(TabBar source, TabListener listener) {
      WrappedTabListener t = new WrappedTabListener(listener);
      source.addBeforeSelectionHandler(t);
      source.addSelectionHandler(t);
    }

    public static void add(TabPanel source, TabListener listener) {
      WrappedTabListener t = new WrappedTabListener(listener);
      source.addBeforeSelectionHandler(t);
      source.addSelectionHandler(t);
    }

    public static void remove(Widget eventSource, TabListener listener) {
      baseRemove(eventSource, listener, SelectionEvent.getType(),
          BeforeSelectionEvent.getType());
    }

    private WrappedTabListener(TabListener listener) {
      super(listener);
    }

    public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
      if (!getListener().onBeforeTabSelected(
          (SourcesTabEvents) event.getSource(), event.getItem().intValue())) {
        event.cancel();
      }
    }

    public void onSelection(SelectionEvent<Integer> event) {
      getListener().onTabSelected((SourcesTabEvents) event.getSource(),
          event.getSelectedItem().intValue());
    }
  }

  static class WrappedTreeListener extends ListenerWrapper<TreeListener>
      implements SelectionHandler<TreeItem>, CloseHandler<TreeItem>,
      OpenHandler<TreeItem> {
    /**
     * @deprecated will be removed in GWT 2.0 along with the listener classes
     */
    @Deprecated
    public static void add(com.google.gwt.user.client.ui.Tree tree,
        TreeListener listener) {
      WrappedTreeListener t = new WrappedTreeListener(listener);
      tree.addSelectionHandler(t);
      tree.addCloseHandler(t);
      tree.addOpenHandler(t);
    }

    public static void remove(Widget eventSource, TreeListener listener) {
      baseRemove(eventSource, listener, ValueChangeEvent.getType());
    }

    private WrappedTreeListener(TreeListener listener) {
      super(listener);
    }

    public void onClose(CloseEvent<TreeItem> event) {
      getListener().onTreeItemStateChanged(event.getTarget());
    }

    public void onOpen(OpenEvent<TreeItem> event) {
      getListener().onTreeItemStateChanged(event.getTarget());
    }

    public void onSelection(SelectionEvent<TreeItem> event) {
      getListener().onTreeItemSelected(event.getSelectedItem());
    }
  }

  /**
   * Convenience method to remove wrapped handlers from a widget.
   * 
   * @param <H> event handler type
   * @param eventSource the event source
   * @param listener the listener to remove
   * @param types the event types to remove it from
   */
  // This is an internal helper method with the current formulation, we have
  // lost the info needed to make it safe by this point.
  @SuppressWarnings("rawtypes")
  protected static <H extends EventHandler> void baseRemove(Widget eventSource,
      EventListener listener, Type... types) {
    HandlerManager manager = eventSource.getHandlerManager();
    if (manager != null) {
      baseRemove(manager, listener, types);
    }
  }

  protected ListenerWrapper(T listener) {
    super(listener);
  }

}
