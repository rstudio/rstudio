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
package com.google.gwt.xml.client.impl;

import com.google.gwt.xml.client.DOMException;

/**
 * Thrown when a particular DOM item causes an exception.
 */
public class DOMNodeException extends DOMException {

  private DOMItem item;

  public DOMNodeException() {
    super((short) 0, "node exception");
  }

  public DOMNodeException(short code, Throwable e, DOMItem item) {
    // This item must be initialized during construction, and Java does not
    // allow any statements before the super, so
    // toString must be evaluated twice
    super(code, "Error during DOM manipulation of: "
        + DOMParseException.summarize(item.toString()));
    initCause(e);
    this.item = item;
  }

  public DOMItem getItem() {
    return item;
  }

}
