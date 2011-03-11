/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.uibinder.rebind;

/**
 * Accepts Java statements to be included in generated UiBinder implementations.
 */
public interface Statements {

  /**
   * Instance of {@link Statements} which does nothing.
   */
  Statements EMPTY = new Empty();

  /**
   * Implementation of {@link Statements} which does nothing.
   */
  class Empty implements Statements {

    public void addDetachStatement(String format, Object... args) {
    }

    public void addInitStatement(String format, Object... params) {
    }

    public void addStatement(String format, Object... args) {
    }
  };

  /**
   * Add a statement to be executed right after the current attached element is
   * detached. This is useful for doing things that might be expensive while the
   * element is attached to the DOM.
   * 
   * @param format
   * @param args
   * @see UiBinderWriter#beginAttachedSection(String)
   */
  void addDetachStatement(String format, Object... args);

  /**
   * Add a statement to be run after everything has been instantiated, in the
   * style of {@link String#format}.
   */
  void addInitStatement(String format, Object... params);

  /**
   * Adds a statement to the block run after fields are declared, in the style
   * of {@link String#format}.
   */
  void addStatement(String format, Object... args);
}
