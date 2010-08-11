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
package com.google.gwt.dev.generator.ast;

/**
 * A {@link Node} that represents a Java expression. An <code>Expression</code>
 * is a parsable value that is a subset of a {@link Statement}. For example,
 *
 * <ul> <li>foo( a, b )</li> <li>14</li> <li>11 / 3</li> <li>x</li> </ul>
 *
 * are all <code>Expressions</code>.
 */
public class Expression extends BaseNode {

  protected String code;

  public Expression() {
    code = "";
  }

  public Expression(String code) {
    this.code = code;
  }

  @Override
  public String toCode() {
    return code;
  }
}
