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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.dom.client.Element;

/**
 * IE6 implementation of {@link com.google.gwt.user.client.ui.impl.FormPanelImpl}.
 */
public class FormPanelImplIE6 extends FormPanelImpl {

  @Override
  public native void hookEvents(Element iframe, Element form, FormPanelImplHost listener) /*-{
    if (iframe) {
      iframe.onreadystatechange = $entry(function() {
        // If there is no __formAction yet, this is a spurious onreadystatechange
        // generated when the iframe is first added to the DOM.
        if (!iframe.__formAction)
          return;

        if (iframe.readyState == 'complete') {
          // If the iframe's contentWindow has not navigated to the expected action
          // url, then it must be an error, so we ignore it.
          listener.@com.google.gwt.user.client.ui.impl.FormPanelImplHost::onFrameLoad()();
        }
      });
    }

    form.onsubmit = $entry(function() {
      // Hang on to the form's action url, needed in the
      // onload/onreadystatechange handler.
      if (iframe)
        iframe.__formAction = form.action;
      return listener.@com.google.gwt.user.client.ui.impl.FormPanelImplHost::onFormSubmit()();
    });
  }-*/;

  @Override
  public native void unhookEvents(Element iframe, Element form) /*-{
    if (iframe)
      iframe.onreadystatechange = null;
    form.onsubmit = null;
  }-*/;
}
