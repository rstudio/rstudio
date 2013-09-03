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
package com.google.gwt.user.client;

/**
 * An opaque handle to a native DOM Element. An <code>Element</code> cannot be
 * created directly. Instead, use the <code>Element</code> type when returning
 * a native DOM element from JSNI methods. An <code>Element</code> passed back
 * into JSNI becomes the original DOM element the <code>Element</code> was
 * created from, and can be accessed in JavaScript code as expected. This is
 * typically done by calling methods in the
 * {@link com.google.gwt.user.client.DOM} class.
 *
 * As of GWT 2.6, users should use {@link com.google.gwt.dom.client.Element}
 * instead.  As an exception, some methods still return a <code>Element</code>
 * object for backwards compatibility (though this will change in a future
 * release), so overriding them will require returning an <code>Element</code>
 * object too.
 */
@Deprecated
public class Element extends com.google.gwt.dom.client.Element {

  /**
   * Not directly instantiable. Subclasses should also define a protected no-arg
   * constructor to prevent client code from directly instantiating the class.
   */
  protected Element() {
  }
}
