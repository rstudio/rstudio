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

/**
 * A widget that wraps the HTML &lt;input type='file'&gt; element. This widget
 * must be used with {@link com.google.gwt.user.client.ui.FormPanel} if it is
 * to be submitted to a server.
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.FormPanelExample}
 * </p>
 */
public class FileUpload extends Widget implements HasName {

  /**
   * Constructs a new file upload widget.
   */
  public FileUpload() {
    setElement(DOM.createElement("input"));
    DOM.setAttribute(getElement(), "type", "file");
    setStyleName("gwt-FileUpload");
  }

  /**
   * Gets the filename selected by the user. This property has no mutator, as
   * browser security restrictions preclude setting it.
   * 
   * @return the widget's filename
   */
  public String getFilename() {
    return DOM.getAttribute(getElement(), "value");
  }

  public String getName() {
    return DOM.getAttribute(getElement(), "name");
  }

  public void setName(String name) {
    DOM.setAttribute(getElement(), "name", name);
  }
}
