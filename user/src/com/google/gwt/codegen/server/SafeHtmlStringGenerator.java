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

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * Helper class to produce string expressions consisting of literals and
 * computed values.
 */
class SafeHtmlStringGenerator extends StringGenerator {

  /**
   * Fully-qualified class name of the SafeHtmlBuilder class.
   */
  private static final String SAFE_HTML_BUILDER_FQCN = SafeHtmlBuilder.class.getCanonicalName();

  /**
   * Initialize the StringGenerator with an output buffer.
   *
   * @param buf output buffer
   */
  SafeHtmlStringGenerator(StringBuilder buf) {
    super(buf);
    buf.append("new " + SAFE_HTML_BUILDER_FQCN + "()");
  }

  @Override
  protected void afterExpression(Type type) {
    buf.append(')');
  }

  @Override
  protected void beforeExpression(Type type) {
    switch (type) {
      case LITERAL:
        buf.append(".appendHtmlConstant(");
        break;
      case PRIMITIVE:
      case SAFE:
        buf.append(".append(");
        break;
      case OTHER:
        buf.append(".appendEscaped(");
        break;
    }
  }

  @Override
  protected void finishOutput() {
    buf.append(".toSafeHtml()");
  }

  @Override
  protected void forceStringPrefix() {
    buf.append("String.valueOf(");
  }

  @Override
  protected void forceStringSuffix() {
    buf.append(")");
  }
}
