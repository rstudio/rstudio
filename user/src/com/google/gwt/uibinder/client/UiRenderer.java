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

import com.google.gwt.dom.client.Element;

/**
 * Marker interface for classes whose implementation is to be provided via UiBinder code
 * generation for SafeHtml rendering.
 * <p>
 * <span style='color: red'>This is experimental code in active
 * developement. It is unsupported, and its api is subject to
 * change.</span>
 */
public interface UiRenderer {

  /**
   * Checks whether {@code parent} is a valid element to use as an argument for field getters.
   * 
   * @return {@code true} if parent contains or directly points to a previously rendered element.
   *         In DevMode it also checks whether the parent is attached to the DOM
   */
  boolean isParentOrRenderer(Element parent);
}
