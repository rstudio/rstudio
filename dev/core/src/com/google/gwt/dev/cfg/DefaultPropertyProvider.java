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
package com.google.gwt.dev.cfg;

import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * A property provider that reports property values specified literally in a
 * host HTML page.
 */
public class DefaultPropertyProvider extends PropertyProvider {

  /*
   * TODO: this references 'parent' literally, which could be a problem if you
   * were to include the selector script in the host page itself rather than in
   * an iframe.
   */
  public DefaultPropertyProvider(ModuleDef module, Property property) {
    super(module, property);
    String src = "function () {";
    src += "return __gwt_getMetaProperty(\"";
    src += property.getName();
    src += "\"); }";
    setBody(parseFunction(src));
  }

  private JsBlock parseFunction(String jsniSrc) {
    Throwable caught = null;
    try {
      JsProgram jsPgm = new JsProgram();
      JsParser jsParser = new JsParser();
      StringReader r = new StringReader(jsniSrc);
      jsParser.setSourceInfo(jsPgm.createSourceInfoSynthetic(
          DefaultPropertyProvider.class, "Default property provider for "
              + getProperty().getName()));
      List<JsStatement> stmts = jsParser.parse(jsPgm.getScope(), r, 1);
      JsFunction fn = (JsFunction) ((JsExprStmt) stmts.get(0)).getExpression();
      return fn.getBody();
    } catch (IOException e) {
      caught = e;
    } catch (JsParserException e) {
      caught = e;
    }
    throw new RuntimeException(
        "Internal error parsing source for default property provider", caught);
  }
}
