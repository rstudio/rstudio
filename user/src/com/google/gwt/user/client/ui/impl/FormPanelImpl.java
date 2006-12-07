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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.user.client.Element;

/**
 * Implementation class used by {@link com.google.gwt.user.client.ui.FormPanel}.
 */
public class FormPanelImpl {

  /**
   * Gets the form element's encoding.
   * 
   * @param form the form whose encoding is to be retrieved
   * @return the form's encoding type
   */
  public native String getEncoding(Element form) /*-{
    return form.enctype;
  }-*/;

  /**
   * Gets the response text from the loaded iframe.
   * 
   * @param iframe the iframe from which the response text is to be extracted
   * @return the response text
   */
  public native String getTextContents(Element iframe) /*-{
    try {
      // Make sure the iframe's document is loaded.
      if (!iframe.contentWindow.document)
        return null;

      // 'Normal' browsers just put the raw text in the body.
      return iframe.contentWindow.document.body.innerHTML;
    } catch (e) {
      return null;
    }
  }-*/;

  /**
   * Hooks the iframe's onLoad event and the form's onSubmit event.
   * 
   * @param iframe the iframe whose onLoad event is to be hooked
   * @param form the form whose onSubmit event is to be hooked
   * @param listener the listener to receive notification
   */
  public native void hookEvents(Element iframe, Element form,
      FormPanelImplHost listener) /*-{

    if (iframe) {
      iframe.onload = function() {
        // If there is no __formAction yet, this is a spurious onload
        // generated when the iframe is first added to the DOM.
        if (!iframe.__formAction)
          return;

        listener.@com.google.gwt.user.client.ui.impl.FormPanelImplHost::onFrameLoad()();
      };
    }

    form.onsubmit = function() {
      // Hang on to the form's action url, needed in the
      // onload/onreadystatechange handler.
      if (iframe)
        iframe.__formAction = form.action;
      return listener.@com.google.gwt.user.client.ui.impl.FormPanelImplHost::onFormSubmit()();
    };
  }-*/;

  /**
   * Sets the form element's encoding.
   * 
   * @param form the form whose encoding is to be set
   * @param encoding the new encoding type
   */
  // To be safe, setting both.
  public native void setEncoding(Element form, String encoding) /*-{
    form.enctype = encoding;
    form.encoding = encoding;
  }-*/;

  /**
   * Submits a form.
   * 
   * @param form the form to be submitted
   * @param iframe the iframe that is targetted, or <code>null</code>
   */
  public native void submit(Element form, Element iframe) /*-{
    // Hang on to the form's action url, needed in the
    // onload/onreadystatechange handler.
    if (iframe)
      iframe.__formAction = form.action;
    form.submit();
  }-*/;

  /**
   * Unhooks the iframe's onLoad event.
   * 
   * @param iframe the iframe whose onLoad event is to be unhooked
   * @param form the form whose onSubmit event is to be unhooked
   */
  public native void unhookEvents(Element iframe, Element form) /*-{
    if (iframe)
      iframe.onload = null;
    form.onsubmit = null;
  }-*/;
}
