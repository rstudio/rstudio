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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

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
public class FileUpload extends Widget implements HasName, HasChangeHandlers, HasEnabled {
  /**
   * Implementation class for {@link FileUpload}.
   */
  private static class FileUploadImpl {
    /**
     * Initialize the impl class.
     * 
     * @param fileUpload the {@link FileUpload} to handle
     */
    public void init(FileUpload fileUpload) {
    }

    /**
     * Handle the browser event.
     * 
     * @param event the native event
     * @return true to fire the event normally, false to ignore it
     */
    public boolean onBrowserEvent(Event event) {
      return true;
    }
  }

  /**
   * Opera fires an onChange event every time a character is typed, but we only
   * want to fire one when the input element is blurred.
   */
  @SuppressWarnings("unused")
  private static class FileUploadImplOpera extends FileUploadImpl {
    private FileUpload fileUpload;
    private boolean eventPending;
    private boolean allowEvent;

    @Override
    public void init(FileUpload fileUpload) {
      this.fileUpload = fileUpload;
      fileUpload.sinkEvents(Event.ONBLUR);
    }

    @Override
    public boolean onBrowserEvent(Event event) {
      switch (event.getTypeInt()) {
        case Event.ONCHANGE:
          // When we fire the change event onBlur, we allow it to pass to
          // Widget#onBrowserEvent().
          if (!allowEvent) {
            eventPending = true;
            return false;
          }
          break;
        case Event.ONBLUR:
          // Trigger a change event now.
          if (eventPending) {
            allowEvent = true;
            fileUpload.getElement().dispatchEvent(
                Document.get().createChangeEvent());
            allowEvent = false;
            eventPending = false;
          }
          break;
      }
      return true;
    }
  }

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

  private FileUploadImpl impl;

  /**
   * Constructs a new file upload widget.
   */
  public FileUpload() {
    setElement(Document.get().createFileInputElement());
    setStyleName("gwt-FileUpload");
    impl = GWT.create(FileUploadImpl.class);
    impl.init(this);
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

  public HandlerRegistration addChangeHandler(ChangeHandler handler) {
    return addDomHandler(handler, ChangeEvent.getType());
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

  /**
   * Gets whether this widget is enabled.
   * 
   * @return <code>true</code> if the widget is enabled
   */
  public boolean isEnabled() {
    return !getElement().getPropertyBoolean("disabled");
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (impl.onBrowserEvent(event)) {
      super.onBrowserEvent(event);
    }
  }

  /**
   * Sets whether this widget is enabled.
   * 
   * @param enabled <code>true</code> to enable the widget, <code>false</code>
   *          to disable it
   */
  public void setEnabled(boolean enabled) {
    getElement().setPropertyBoolean("disabled", !enabled);
  }

  public void setName(String name) {
    getInputElement().setName(name);
  }

  private InputElement getInputElement() {
    return getElement().cast();
  }
}
