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
package com.google.gwt.user.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Element;

/**
 * Native implementation used by {@link WindowImpl} and {@link DOMImpl} to
 * access the appropriate documentRoot element, which varies based on the render
 * mode of the browser.
 * 
 * @deprecated use the direct methods provided in {@link Document} instead
 */
@Deprecated
public class DocumentRootImpl {
  protected static Element documentRoot =
    ((DocumentRootImpl) GWT.create(DocumentRootImpl.class)).getDocumentRoot();

  protected Element getDocumentRoot() {
    Document doc = Document.get();
    return (doc.isCSS1Compat() ? doc.getDocumentElement() : doc.getBody()).cast();
  }
}
