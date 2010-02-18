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
package com.google.gwt.cells.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

/**
 * A light weight representation of a renderable object.
 * 
 * @param <C> the type that this Cell represents
 */
public abstract class Cell<C> {

  /**
   * @param parent
   * @param value
   * @param event
   * @param valueUpdater a {@link ValueUpdater}, or null
   */
  public void onBrowserEvent(Element parent, C value, NativeEvent event,
      ValueUpdater<C> valueUpdater) {
  }

  // TODO: render needs a way of assuming text by default, but allowing HTML.
  public abstract void render(C value, StringBuilder sb);

  public void setValue(Element parent, C value) {
    StringBuilder sb = new StringBuilder();
    render(value, sb);
    parent.setInnerHTML(sb.toString());
  }
}
