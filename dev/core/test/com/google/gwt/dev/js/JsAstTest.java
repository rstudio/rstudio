/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsObjectLiteral;

import junit.framework.TestCase;

/**
 * Unit test for JS AST nodes
 */
public class JsAstTest extends TestCase {

  // TODO(rluble): flesh out the test.
  public void testEquality() {
    JsNumberLiteral one = new JsNumberLiteral(SourceOrigin.UNKNOWN, 1);
    JsNumberLiteral two = new JsNumberLiteral(SourceOrigin.UNKNOWN, 2);
    JsArrayLiteral arrayOneTwoLiteral = new JsArrayLiteral(SourceOrigin.UNKNOWN);
    arrayOneTwoLiteral.getExpressions().add(one);
    arrayOneTwoLiteral.getExpressions().add(two);

    JsArrayLiteral arrayOneTwoLiteral2 = new JsArrayLiteral(SourceOrigin.UNKNOWN);
    arrayOneTwoLiteral2.getExpressions().add(one);
    arrayOneTwoLiteral2.getExpressions().add(two);

    JsArrayLiteral arrayTwoOneLiteral = new JsArrayLiteral(SourceOrigin.UNKNOWN);
    arrayTwoOneLiteral.getExpressions().add(two);
    arrayTwoOneLiteral.getExpressions().add(one);

    JsObjectLiteral emptyObject = new JsObjectLiteral(SourceOrigin.UNKNOWN);

    assertEquals(arrayOneTwoLiteral, arrayOneTwoLiteral2);
    assertEquals(arrayOneTwoLiteral2, arrayOneTwoLiteral);
    assertFalse(arrayOneTwoLiteral.equals(arrayTwoOneLiteral));
    assertFalse(arrayTwoOneLiteral.equals(arrayOneTwoLiteral2));
    assertFalse(arrayTwoOneLiteral.equals(emptyObject));
    assertFalse(emptyObject.equals(arrayOneTwoLiteral2));
  }
}
