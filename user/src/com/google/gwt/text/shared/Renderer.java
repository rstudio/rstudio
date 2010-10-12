/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.text.shared;

import java.io.IOException;

/**
 * An object that can render other objects of a particular type into plain-text
 * form. Allows decoupling that is useful for a dependency-injection
 * architecture.
 * 
 * @param <T> the type to render
 */
public interface Renderer<T> {

  /**
   * Renders {@code object} as plain text. Should never throw any exceptions!
   */
  String render(T object);

  /**
   * Renders {@code object} as plain text, appended directly to {@code
   * appendable}. Should never throw any exceptions except if {@code appendable}
   * throws an {@code IOException}.
   */
  void render(T object, Appendable appendable) throws IOException;
}
