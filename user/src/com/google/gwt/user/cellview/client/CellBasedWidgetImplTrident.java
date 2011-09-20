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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashSet;
import java.util.Set;

/**
 * IE specified Impl used by cell based widgets.
 */
class CellBasedWidgetImplTrident extends CellBasedWidgetImpl {

  /**
   * The method used to dispatch non-bubbling events.
   */
  private static JavaScriptObject dispatchFocusEvent;

  /**
   * The currently focused input element, select box, or text area.
   */
  private static Element focusedInput;

  /**
   * If true, only synthesize change events when the focused input is blurred.
   */
  private static boolean focusedInputChangesOnBlurOnly;

  /**
   * The last value of the focused input element.
   */
  private static Object focusedInputValue;

  /**
   * The set of input types that can receive change events.
   */
  private static Set<String> inputTypes;

  /**
   * Dispatch an event to the cell, ensuring that the widget will catch it.
   *
   * @param widget the widget that will handle the event
   * @param target the cell element
   * @param eventBits the event bits to sink
   * @param event the event to fire, or null not to fire an event
   */
  private static void dispatchCellEvent(Widget widget,
      com.google.gwt.user.client.Element target, int eventBits, Event event) {
    // Make sure that the target is still a child of the widget. We defer the
    // firing of some events, so its possible that the DOM structure has
    // changed before we fire the event.
    if (!widget.getElement().isOrHasChild(target)) {
      return;
    }

    // Temporary listen for events from the cell. The event listener will be
    // removed in onBrowserEvent().
    DOM.setEventListener(target, widget);
    DOM.sinkEvents(target, eventBits | DOM.getEventsSunk(target));

    // Dispatch the event to the cell.
    if (event != null) {
      target.dispatchEvent(event);
    }
  }

  /**
   * Dispatch an event through the normal GWT mechanism.
   */
  private static native void dispatchEvent(Event evt, Element elem,
      EventListener listener) /*-{
    @com.google.gwt.user.client.DOM::dispatchEvent(Lcom/google/gwt/user/client/Event;Lcom/google/gwt/user/client/Element;Lcom/google/gwt/user/client/EventListener;)(evt, elem, listener);
  }-*/;

  /**
   * Get the value of an element that has a value or checked state.
   *
   * @param elem the input element
   * @return the value of the input
   */
  private static Object getInputValue(Element elem) {
    if (isCheckbox(elem)) {
      return InputElement.as(elem).isChecked();
    }
    return getInputValueImpl(elem);
  }

  /**
   * Get the value of an element that has a value, such as an input element,
   * textarea, or select box.
   *
   * @param elem the input element
   * @return the value of the input
   */
  private static native String getInputValueImpl(Element elem) /*-{
    return elem.value;
  }-*/;

  /**
   * Used by {@link #initFocusEventSystem()} and {@link #initLoadEvents(String)}
   * to handle non bubbling events .
   *
   * @param event
   */
  @SuppressWarnings("unused")
  private static void handleNonBubblingEvent(Event event) {
    // Get the event target.
    EventTarget eventTarget = event.getEventTarget();
    if (!Element.is(eventTarget)) {
      return;
    }
    final com.google.gwt.user.client.Element target = eventTarget.cast();

    // Get the event listener.
    com.google.gwt.user.client.Element curElem = target;
    EventListener listener = DOM.getEventListener(curElem);
    while (curElem != null && listener == null) {
      curElem = curElem.getParentElement().cast();
      listener = (curElem == null) ? null : DOM.getEventListener(curElem);
    }

    // Get the Widget from the event listener.
    if (!(listener instanceof Widget)) {
      return;
    }
    Widget widget = (Widget) listener;

    // Do not special case events that occur on the widget itself.
    if (target == widget.getElement()) {
      return;
    }

    String type = event.getType();
    if (BrowserEvents.FOCUSIN.equals(type)) {
      // If this is an input element, remember that we focused it.
      String tagName = target.getTagName().toLowerCase();
      if (inputTypes.contains(tagName)) {
        focusedInput = target;
        focusedInputValue = getInputValue(target);
        focusedInputChangesOnBlurOnly = !"select".equals(tagName)
            && !isCheckbox(target);
      }

      // The focus event has not fired yet, so we just need to set the
      // CellTable as the event listener and wait for it.
      dispatchCellEvent(widget, target, Event.ONFOCUS, null);
    } else if (BrowserEvents.FOCUSOUT.equals(type)) {
      // Fire a change event on the input element if the value changed.
      maybeFireChangeEvent(widget);
      focusedInput = null;

      // The blur event has already fired, so we need to synthesize one.
      Event blurEvent = Document.get().createFocusEvent().cast();
      dispatchCellEvent(widget, target, Event.ONBLUR, null);
    } else if (BrowserEvents.LOAD.equals(type) || BrowserEvents.ERROR.equals(type)) {
      dispatchEvent(event, widget.getElement(), listener);
    }
  }

  /**
   * Check whether or not an element is a checkbox or radio button.
   *
   * @param elem the element to check
   * @return true if a checkbox, false if not
   */
  private static boolean isCheckbox(Element elem) {
    if (elem == null || !"input".equalsIgnoreCase(elem.getTagName())) {
      return false;
    }
    String inputType = InputElement.as(elem).getType().toLowerCase();
    return "checkbox".equals(inputType) || "radio".equals(inputType);
  }

  /**
   * Synthesize a change event on the focused input element if the value has
   * changed.
   *
   * @param widget the {@link Widget} containing the element
   */
  private static void maybeFireChangeEvent(Widget widget) {
    if (focusedInput == null) {
      return;
    }

    Object newValue = getInputValue(focusedInput);
    if (!newValue.equals(focusedInputValue)) {
      // Save the new value in case it changes again.
      focusedInputValue = newValue;

      // Fire a synthetic event to the input element.
      com.google.gwt.user.client.Element target = focusedInput.cast();
      Event changeEvent = Document.get().createChangeEvent().cast();
      dispatchCellEvent(widget, target, Event.ONCHANGE, changeEvent);
    }
  }

  /**
   * The set of event types that can trigger a change event.
   */
  private final Set<String> changeEventTriggers;

  /**
   * If true, load events have been initialized.
   */
  private boolean loadEventsInitialized;

  public CellBasedWidgetImplTrident() {
    // Initialize the input types.
    if (inputTypes == null) {
      inputTypes = new HashSet<String>();
      inputTypes.add("select");
      inputTypes.add("input");
      inputTypes.add("textarea");
    }

    // Initialize the change event triggers.
    changeEventTriggers = new HashSet<String>();
    changeEventTriggers.add(BrowserEvents.MOUSEUP);
    changeEventTriggers.add(BrowserEvents.MOUSEWHEEL);
  }

  @Override
  public boolean isFocusable(Element elem) {
    return focusableTypes.contains(elem.getTagName().toLowerCase())
        || getTabIndexIfSpecified(elem) >= 0;
  }

  @Override
  public void onBrowserEvent(final Widget widget, Event event) {
    // We need to remove the event listener from the cell now that the event
    // has fired.
    String type = event.getType().toLowerCase();
    if (BrowserEvents.FOCUS.equals(type) || BrowserEvents.BLUR.equals(type) || BrowserEvents.CHANGE.equals(type)) {
      EventTarget eventTarget = event.getEventTarget();
      if (Element.is(eventTarget)) {
        com.google.gwt.user.client.Element target = eventTarget.cast();
        if (target != widget.getElement()) {
          DOM.setEventListener(target, null);
        }
      }
    }

    // Update the value of the focused input box.
    if (focusedInput != null && BrowserEvents.CHANGE.equals(type)) {
      focusedInputValue = getInputValue(focusedInput);
    }

    // We might need to fire a synthetic change event on the input element.
    if (focusedInput != null && !focusedInputChangesOnBlurOnly
        && changeEventTriggers.contains(type)) {
      // Defer the change event because the change does not occur until after
      // the events specified above.
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        public void execute() {
          maybeFireChangeEvent(widget);
        }
      });
    }
  }

  @Override
  public SafeHtml processHtml(SafeHtml html) {
    // If the widget is listening for load events, we modify the HTML to add the
    // load/error listeners.
    if (loadEventsInitialized && html != null) {
      String moduleName = GWT.getModuleName();
      String listener = "__gwt_CellBasedWidgetImplLoadListeners[\""
          + moduleName + "\"]();";

      String htmlString = html.asString();
      htmlString = htmlString.replaceAll("(<img)([\\s/>])", "<img onload='"
          + listener + "' onerror='" + listener + "'$2");

      // We assert that the resulting string is safe
      html = SafeHtmlUtils.fromTrustedString(htmlString);
    }
    return html;
  }

  @Override
  public void resetFocus(ScheduledCommand command) {
    // IE will not focus an element that was created in this event loop.
    Scheduler.get().scheduleDeferred(command);
  }

  @Override
  protected int sinkEvent(Widget widget, String typeName) {
    if (BrowserEvents.CHANGE.equals(typeName) || BrowserEvents.FOCUS.equals(typeName)
        || BrowserEvents.BLUR.equals(typeName)) {
      // Initialize the focus events.
      if (dispatchFocusEvent == null) {
        initFocusEventSystem();
      }

      // Sink the events required for focus. We use an attribute on the widget
      // to remember whether or not we've sunk the events.
      int eventsToSink = 0;
      Element elem = widget.getElement();
      String attr = "__gwtCellBasedWidgetImplDispatchingFocus";
      if (!"true".equals(elem.getAttribute(attr))) {
        elem.setAttribute(attr, "true");
        sinkFocusEvents(elem);

        // Sink the events that could trigger a change event. Change events
        // are also triggered on blur if the value changes.
        for (String trigger : changeEventTriggers) {
          eventsToSink |= Event.getTypeInt(trigger);
        }
      }
      return eventsToSink;
    } else if (BrowserEvents.LOAD.equals(typeName) || BrowserEvents.ERROR.equals(typeName)) {
      // Initialize the load listener.
      if (!loadEventsInitialized) {
        loadEventsInitialized = true;
        initLoadEvents(GWT.getModuleName());
      }
      return -1;
    } else {
      return super.sinkEvent(widget, typeName);
    }
  }

  /**
   * Get the tab index of an element if the tab index is specified.
   * 
   * @param elem the Element
   * @return the tab index, or -1 if not specified
   */
  private native int getTabIndexIfSpecified(Element elem) /*-{
    var attrNode = elem.getAttributeNode('tabIndex');
    return (attrNode != null && attrNode.specified) ? elem.tabIndex : -1;
  }-*/;

  /**
   * Initialize the focus event listener.
   */
  private native void initFocusEventSystem() /*-{
    @com.google.gwt.user.cellview.client.CellBasedWidgetImplTrident::dispatchFocusEvent = $entry(function() {
      @com.google.gwt.user.cellview.client.CellBasedWidgetImplTrident::handleNonBubblingEvent(Lcom/google/gwt/user/client/Event;)($wnd.event);
    });
  }-*/;

  /**
   * Initialize load events. We hang a method off of $wnd so we can reference it
   * in the HTML generated for img tags.
   *
   * @param moduleName the module name of the current module
   */
  private native void initLoadEvents(String moduleName) /*-{
    // Initialize an array of listeners. Each module gets its own entry in the
    // array to prevent conflicts on pages with multiple modules.
    if (!$wnd.__gwt_CellBasedWidgetImplLoadListeners) {
      $wnd.__gwt_CellBasedWidgetImplLoadListeners = new Array();
    }

    // Add an entry for the specified module.
    $wnd.__gwt_CellBasedWidgetImplLoadListeners[moduleName] = $entry(function() {
      @com.google.gwt.user.cellview.client.CellBasedWidgetImplTrident::handleNonBubblingEvent(Lcom/google/gwt/user/client/Event;)($wnd.event);
    });
  }-*/;

  /**
   * Sink focus events for the specified element.
   *
   * @param elem the element that will receive the events
   */
  private native void sinkFocusEvents(Element elem) /*-{
    elem.attachEvent('onfocusin',
        @com.google.gwt.user.cellview.client.CellBasedWidgetImplTrident::dispatchFocusEvent);
    elem.attachEvent('onfocusout',
        @com.google.gwt.user.cellview.client.CellBasedWidgetImplTrident::dispatchFocusEvent);
  }-*/;
}
