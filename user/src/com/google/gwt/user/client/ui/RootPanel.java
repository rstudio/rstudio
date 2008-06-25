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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowCloseListener;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.BidiUtils;
import com.google.gwt.i18n.client.HasDirection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The panel to which all other widgets must ultimately be added. RootPanels are
 * never created directly. Rather, they are accessed via {@link RootPanel#get()}.
 * 
 * <p>
 * Most applications will add widgets to the default root panel in their
 * {@link com.google.gwt.core.client.EntryPoint#onModuleLoad} methods.
 * </p>
 */
public class RootPanel extends AbsolutePanel {

  private static HashMap<String, RootPanel> rootPanels = new HashMap<String, RootPanel>();
  private static List<Widget> widgetsToDetach = new ArrayList<Widget>();

  /**
   * Adds a widget to the list of widgets to be detached when the page unloads.
   * 
   * This method must be called for all widgets that have no parent widgets.
   * These are most commonly {@link RootPanel RootPanels}, but can also be any
   * widget used to wrap an existing element on the page. Failing to do this
   * may cause these widgets to leak memory.
   * 
   * @param widget the widget to be cleaned up when the page closes
   */
  public static void detachOnWindowClose(Widget widget) {
    widgetsToDetach.add(widget);
  }

  /**
   * Gets the default root panel. This panel wraps body of the browser's
   * document. This root panel can contain any number of widgets, which will be
   * laid out in their natural HTML ordering. Many applications, however, will
   * add a single panel to the RootPanel to provide more structure.
   * 
   * @return the default RootPanel
   */
  public static RootPanel get() {
    return get(null);
  }

  /**
   * Gets the root panel associated with a given browser element. For this to
   * work, the HTML document into which the application is loaded must have
   * specified an element with the given id.
   * 
   * @param id the id of the element to be wrapped with a root panel
   * @return the root panel, or <code>null</code> if no such element was found
   */
  public static RootPanel get(String id) {
    // See if this RootPanel is already created.
    RootPanel rp = rootPanels.get(id);
    if (rp != null) {
      return rp;
    }

    // Find the element that this RootPanel will wrap.
    Element elem = null;
    if (id != null) {
      if (null == (elem = DOM.getElementById(id))) {
        return null;
      }
    }

    // Note that the code in this if block only happens once - 
    // on the first RootPanel.get(String) or RootPanel.get() 
    // call.
    
    if (rootPanels.size() == 0) {
      hookWindowClosing();
      
      // If we're in a RTL locale, set the RTL directionality
      // on the entire document.
      if (LocaleInfo.getCurrentLocale().isRTL()) {
        BidiUtils.setDirectionOnElement(getRootElement(), HasDirection.Direction.RTL);
      }      
    }

    // Create the panel and put it in the map.
    if (elem == null) {
      // 'null' means use document's body element.
      elem = getBodyElement();
    }
    rootPanels.put(id, rp = new RootPanel(elem));
    widgetsToDetach.add(rp);
    return rp;
  }

  /**
   * Convenience method for getting the document's body element.
   * 
   * @return the document's body element
   */
  public static native Element getBodyElement() /*-{
    return $doc.body;
  }-*/;

  /**
   * Convenience method for getting the document's root 
   * (<html>) element.
   *
   * @return the document's root element
   */
  private static native Element getRootElement() /*-{
    return $doc;
  }-*/;

  private static void hookWindowClosing() {
    // Catch the window closing event.
    Window.addWindowCloseListener(new WindowCloseListener() {
      public void onWindowClosed() {
        // When the window is closing, detach all widgets that need to be
        // cleaned up. This will cause all of their event listeners
        // to be unhooked, which will avoid potential memory leaks.
        for (int i = 0; i < widgetsToDetach.size(); ++i) {
          Widget widget = widgetsToDetach.get(i);
          if (widget.isAttached()) {
            widget.onDetach();
          }
        }
      }

      public String onWindowClosing() {
        return null;
      }
    });
  }

  private RootPanel(Element elem) {
    super(elem);
    onAttach();
  }
}
