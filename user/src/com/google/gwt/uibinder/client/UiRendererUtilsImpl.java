/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.uibinder.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * UiRenderer utilities.
 */
public class UiRendererUtilsImpl {

  /**
   * Build id strings used to identify DOM elements related to ui:fields.
   * 
   * @param fieldName name of the field that identifies the element
   * @param uiId common part of the identifier for all elements in the rendered DOM structure
   */
  public static String buildInnerId(String fieldName, String uiId) {
    return uiId + ":" + fieldName;
  }

  /**
   * Retrieves a specific element within a previously rendered element.
   * 
   * @param parent parent element containing the element of interest
   * @param fieldName name of the field to retrieve
   * @param attribute that identifies the root element as such
   * @return the element identified by {@code fieldName}
   *
   * @throws IllegalArgumentException if the {@code parent} does not point to or contains
   *         a previously rendered element. In DevMode also when the root element is not
   *         attached to the DOM
   * @throws IllegalStateException parent does not contain an element matching
   *         {@code filedName}
   *
   * @throws RuntimeException if the root element is not attached to the DOM and not running in
   *         DevMode
   * 
   * @throws NullPointerException if {@code parent} == null
   */
  public static Element findInnerField(Element parent, String fieldName, String attribute) {
    Element root = findRootElement(parent, attribute);

    if (parent != root && !isRenderedElementSingleChild(root)) {
      throw new IllegalArgumentException(
          "Parent Element of previously rendered element contains more than one child"
          + " while getting \"" + fieldName + "\"");
    }

    String uiId = root.getAttribute(attribute);
    String renderedId = buildInnerId(fieldName, uiId);

    Element elementById = Document.get().getElementById(renderedId);
    if (elementById == null) {
      if (!isAttachedToDom(root)) {
        throw new RuntimeException("UiRendered element is not attached to DOM while getting \""
            + fieldName + "\"");
      } else if (!GWT.isProdMode()) {
        throw new IllegalStateException("\"" + fieldName
            + "\" not found within rendered element");
      } else {
        // In prod mode we do not distinguish between being unattached or not finding the element
        throw new IllegalArgumentException("UiRendered element is not attached to DOM, or \""
            + fieldName + "\" not found within rendered element");
      }
    }
    return elementById;
  }

  /**
   * Retrieves the root of a previously rendered element contained within the {@code parent}.
   * The {@code parent} must either contain the previously rendered DOM structure as its only child,
   * or point directly to the rendered element root.
   * 
   * @param parent element containing, or pointing to, a previously rendered DOM structure
   * @param attribute attribute name that identifies the root of the DOM structure
   * @return the root element of the previously rendered DOM structure
   * 
   * @throws NullPointerException if {@code parent} == null
   * @throws IllegalArgumentException if {@code parent} does not contain a previously rendered
   *         element
   */
  public static Element findRootElement(Element parent, String attribute) {
    if (parent == null) {
      throw new NullPointerException("parent argument is null");
    }

    Element rendered;
    if (parent.hasAttribute(attribute)) {
      // The parent is the root
      return parent;
    } else if ((rendered = parent.getFirstChildElement()) != null
        && rendered.hasAttribute(attribute)) {
      // The first child is the root
      return rendered;
    } else {
      throw new IllegalArgumentException(
          "Parent element does not contain a previously rendered element");
    }
  }

  /**
   * Walks up the parents of the {@code rendered} element to ascertain that it is attached to the
   * document.
   */
  public static boolean isAttachedToDom(Element rendered) {
    if (GWT.isProdMode()) {
      return true;
    }

    Element body = Document.get().getBody();

    while (rendered != null && rendered.hasParentElement() && !body.equals(rendered)) {
      rendered = rendered.getParentElement();
    }
    return body.equals(rendered);
  };

  /**
   * Checks that the parent of {@code rendered} has a single child.
   */
  public static boolean isRenderedElementSingleChild(Element rendered) {
    return GWT.isProdMode() || rendered.getParentElement().getChildCount() == 1;
  }

  /**
   * Inserts an attribute into the first tag found in a {@code safeHtml} template.
   * This method assumes that the {@code safeHtml} template begins with an open HTML tag.
   * {@code SafeHtml} templates produced by UiBinder always meet these conditions.
   * <p>
   * This method does not attempt to ensure {@code atributeName} and {@code attributeValue}
   * contain safe values.
   *
   * @returns the {@code safeHtml} template with "{@code attributeName}={@code attributeValue}"
   *          inserted as an attribute of the first tag found
   */
  public static SafeHtml stampUiRendererAttribute(SafeHtml safeHtml, String attributeName,
      String attributeValue) {
    String html = safeHtml.asString();
    int endOfFirstTag = html.indexOf(">");

    assert endOfFirstTag > 1 : "Safe html template does not start with an HTML open tag";
    
    if (html.charAt(endOfFirstTag - 1) == '/') {
      endOfFirstTag--;
    }

    html = html.substring(0, endOfFirstTag) + " " + attributeName + "=\"" + attributeValue + "\""
            + html.substring(endOfFirstTag);
    return SafeHtmlUtils.fromTrustedString(html);
  }
}
