/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.impl.codesplitter;

import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;

/**
 * <p>
 * A predicate on whether statements and variables should be considered live.
 *
 * <p>
 * Any supplied predicate must satisfy load-order dependencies. For any atom
 * considered live, the atoms it depends on at load time should also be live.
 * The following load-order dependencies exist:
 *
 * <ul>
 * <li>A class literal depends on the strings contained in its instantiation
 * instruction.</li>
 *
 * <li>Types depend on their supertype.</li>
 *
 * <li>Instance methods depend on their enclosing type.</li>
 *
 * <li>Static fields that are initialized to strings depend on the string they
 * are initialized to.</li>
 * </ul>
 */
public interface LivenessPredicate {
  /**
   * Subclasses should return true if {@code type} is deemed live and false otherwise.
   */
  boolean isLive(JDeclaredType type);

  /**
   * Subclasses should return true if {@code field} is deemed live and false otherwise.
   */
  boolean isLive(JField field);

  /**
   * Subclasses should return true if {@code method} is deemed live and false otherwise.
   */
  boolean isLive(JMethod method);

  /**
   * Subclasses should return true if {@code stringLiteral} is deemed live and false otherwise.
   */
  boolean isLive(String stringLiteral);

  /**
   * Whether miscellaneous statements should be considered live.
   * Miscellaneous statements are any that the fragment extractor does not
   * recognize as being in any particular category. This method should almost
   * always return <code>true</code>, but does return <code>false</code> for
   * {@link NothingAlivePredicate}.
   */
  boolean miscellaneousStatementsAreLive();
}
