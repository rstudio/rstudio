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

/**
 * A {@link com.google.gwt.user.client.ui.Frame} that has a 'name' associated
 * with it. This allows the frame to be the target of a
 * {@link com.google.gwt.user.client.ui.FormPanel}
 */
public class NamedFrame extends Frame {

  /**
   * Constructs a frame with the given name.
   * 
   * @param name the name of the frame
   */
  public NamedFrame(String name) {
    // Use innerHTML to implicitly create the <iframe>. This is necessary
    // because most browsers will not respect a dynamically-set iframe name.
    Element div = DOM.createDiv();
    DOM.setInnerHTML(div, "<iframe name='" + name + "'>");

    Element iframe = DOM.getFirstChild(div);
    setElement(iframe);
  }

  /**
   * Gets the name associated with this frame.
   * 
   * @return the frame's name
   */
  public String getName() {
    return DOM.getAttribute(getElement(), "name");
  }
}
