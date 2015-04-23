/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMember;
import com.google.gwt.dev.js.ast.JsExpression;

/**
 * Generates codes to handle @JsExport.
 */
public interface JsInteropExportsGenerator {
  /**
   * Makes sure the type is exported even there are no exported constructors for the type but is
   * still marked with JsType.
   * <p>
   * This is essentially needed by Closure formatted output so that type declarations can be
   * provided for JsTypes that are not exported via a constructor.
   */
  void exportType(JDeclaredType x);

  /**
   * Exports a member to the namespace that is provided by its qualified export name.
   */
  void exportMember(JMember member, JsExpression bridgeMethodOrAlias);
}
