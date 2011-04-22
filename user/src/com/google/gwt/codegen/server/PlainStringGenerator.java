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
package com.google.gwt.codegen.server;

/**
 * Helper class to produce string expressions consisting of literals and
 * computed values.
 */
class PlainStringGenerator extends StringGenerator {

  private boolean firstExpression = true;

  /**
   * Initialize the StringGenerator with an output buffer.
   *
   * @param buf output buffer
   */
  PlainStringGenerator(StringBuilder buf) {
    super(buf);
  }

  @Override
  protected void afterExpression(Type type) {
    firstExpression = false;
  }

  @Override
  protected void beforeExpression(Type type) {
    if (firstExpression) {
      if (type == Type.PRIMITIVE) {
        buf.append("\"\" + ");
      }
    } else {
      buf.append(" + ");
    }
  }

  @Override
  protected void finishOutput() {
    if (firstExpression) {
      buf.append("\"\"");
    }
  }

  @Override
  protected void forceStringPrefix() {
    if (firstExpression) {
      buf.append("\"\" + ");
    }
  }

  @Override
  protected void forceStringSuffix() {
  }
}
