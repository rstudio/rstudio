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

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;

/**
 * A singleton implementation of {@link LayoutPanel} that always attaches itself
 * to the document body (i.e. {@link RootPanel#get()}).
 * 
 * <p>
 * This panel automatically calls {@link RequiresResize#onResize()} on itself
 * when initially created, and whenever the window is resized.
 * </p>
 * 
 * <p>
 * NOTE: This widget will <em>only</em> work in standards mode, which requires
 * that the HTML page in which it is run have an explicit &lt;!DOCTYPE&gt;
 * declaration.
 * </p>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.LayoutPanelExample}
 * </p>
 */
public class RootLayoutPanel extends LayoutPanel {

  private static RootLayoutPanel singleton;

  /**
   * Gets the singleton instance of RootLayoutPanel. This instance will always
   * be attached to the document body via {@link RootPanel#get()}.
   * 
   * <p>
   * Note that, unlike {@link RootPanel#get(String)}, this class provides no way
   * to get an instance for any element on the page other than the document
   * body. This is because we know of no way to get resize events for anything
   * but the window.
   * </p>
   */
  public static RootLayoutPanel get() {
    if (singleton == null) {
      singleton = new RootLayoutPanel();
      RootPanel.get().add(singleton);
    }
    return singleton;
  }

  private RootLayoutPanel() {
    Window.addResizeHandler(new ResizeHandler() {
      public void onResize(ResizeEvent event) {
        RootLayoutPanel.this.onResize();
      }
    });

    // TODO(jgw): We need notification of font-size changes as well.
    // I believe there's a hidden iframe trick that we can use to get
    // a font-size-change event (really an em-definition-change event).
  }

  @Override
  protected void onLoad() {
    getLayout().onAttach();
    getLayout().fillParent();
  }
}
