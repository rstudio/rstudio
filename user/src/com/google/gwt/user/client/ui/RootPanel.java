/*
 * Copyright 2006 Google Inc.
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

import java.util.HashMap;
import java.util.Iterator;

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

  private static HashMap rootPanels = new HashMap();

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
    RootPanel gwt = (RootPanel) rootPanels.get(id);
    if (gwt != null) {
      return gwt;
    }
    // Find the element that this RootPanel will wrap.
    Element elem = null;
    if (id != null) {
      if (null == (elem = DOM.getElementById(id))) {
        return null;
      }
    }

    if (rootPanels.size() == 0) {
      hookWindowClosing();
    }

    // Create the panel and put it in the map.
    rootPanels.put(id, gwt = new RootPanel(elem));
    return gwt;
  }

  /**
   * Convenience method for getting the document's body element.
   * 
   * @return the document's body element
   */
  public static native Element getBodyElement() /*-{
   return $doc.body;
   }-*/;

  private static void hookWindowClosing() {
    // Catch the window closing event.
    Window.addWindowCloseListener(new WindowCloseListener() {
      public void onWindowClosed() {
        // When the window is closing, detach all root panels. This will cause
        // all of their children's event listeners to be unhooked, which will
        // avoid potential memory leaks.
        for (Iterator it = rootPanels.values().iterator(); it.hasNext();) {
          RootPanel gwt = (RootPanel) it.next();
          gwt.onDetach();
        }
      }

      public String onWindowClosing() {
        return null;
      }
    });
  }

  private RootPanel(Element elem) {
    if (elem == null) {
      // 'null' means use document's body element.
      elem = getBodyElement();
    }

    setElement(elem);
    onAttach();
  }
}
