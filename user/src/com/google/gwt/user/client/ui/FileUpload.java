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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;

/**
 * A widget that wraps the HTML &lt;input type='file'&gt; element. This widget
 * must be used with {@link com.google.gwt.user.client.ui.FormPanel} if it is to
 * be submitted to a server.
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.FormPanelExample}
 * </p>
 */
public class FileUpload extends Widget implements HasName {

  /**
   * Creates a FileUpload widget that wraps an existing &lt;input
   * type='file'&gt; element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * @param element the element to be wrapped
   */
  public static FileUpload wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    FileUpload fileUpload = new FileUpload(element);

    // Mark it attached and remember it for cleanup.
    fileUpload.onAttach();
    RootPanel.detachOnWindowClose(fileUpload);

    return fileUpload;
  }

  /**
   * Constructs a new file upload widget.
   */
  public FileUpload() {
    setElement(Document.get().createFileInputElement());
    setStyleName("gwt-FileUpload");
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be an &lt;input&gt; element whose type is
   * 'file'.
   * 
   * @param element the element to be used
   */
  protected FileUpload(Element element) {
    assert InputElement.as(element).getType().equalsIgnoreCase("file");
    setElement(element);
  }

  /**
   * Gets the filename selected by the user. This property has no mutator, as
   * browser security restrictions preclude setting it.
   * 
   * @return the widget's filename
   */
  public String getFilename() {
    return getInputElement().getValue();
  }

  public String getName() {
    return getInputElement().getName();
  }

  public void setName(String name) {
    getInputElement().setName(name);
  }

  private InputElement getInputElement() {
    return getElement().cast();
  }
}
