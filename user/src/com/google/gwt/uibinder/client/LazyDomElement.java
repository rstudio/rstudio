/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.uibinder.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

/**
 * Wraps a call to a DOM element. LazyDomElement can boost performance of html
 * elements and delay calls to getElementById() to when the element is actually
 * used. But note that it will throw a RuntimeException in case the element is
 * accessed but not yet attached in the DOM tree.
 * <p>
 * Usage example:
 * <p>
 * <b>Template:</b>
 * <pre>
 *   &lt;gwt:HTMLPanel&gt;
 *      &lt;div ui:field="myDiv" /&gt;
 *   &lt;/gwt:HTMLPanel&gt;
 * </pre>
 * <p>
 * <b>Class:</b>
 * <pre>
 *   {@literal @}UiField LazyDomElement&lt;DivElement&gt; myDiv;
 *
 *   public setText(String text) {
 *     myDiv.get().setInnerHtml(text);
 *   }
 * </pre>
 *
 * @param <T> the Element type associated
 */
public class LazyDomElement<T extends Element> {

  private T element;
  private final String domId;

 /**
  * Creates an instance to fetch the element with the given id.
  */
  public LazyDomElement(String domId) {
    this.domId = domId;
  }

 /**
  * Returns the dom element.
  *
  * @return the dom element
  * @throws RuntimeException if the element cannot be found
  */
  public T get() {
    if (element == null) {
      element = Document.get().getElementById(domId).<T>cast();
      if (element == null) {
        throw new RuntimeException("Cannot find element with id \"" + domId
            + "\". Perhaps it is not attached to the document body.");
      }
      element.removeAttribute("id");
    }
    return element;
  }
}
