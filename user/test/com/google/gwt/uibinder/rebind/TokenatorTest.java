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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.uibinder.rebind.Tokenator.ValueAndInfo;

import junit.framework.TestCase;

/**
 * Having a style tool that forces one to create javadoc that communicates no
 * more information than the class name itself is the kind of foolishness that
 * gives Java development a bad name.
 */
public class TokenatorTest extends TestCase {
  private static final String FORMAT = "Token %d replaces \"%s\"";
  private static final String BEGIN = "This is a bunch of text.";
  private static final String EXPECTED =
      "Token 1 replaces \"This\" is a " + "Token 2 replaces \"bunch\" of "
          + "Token 3 replaces \"text.\"";

  private int serial = 0;
  private Tokenator tokenator;
  private String betokened;

  @Override
  public void setUp() {
    tokenator = new Tokenator();
    betokened =
      BEGIN.replace("This", betoken("This")).replace("bunch",
          betoken("bunch")).replace("text.", betoken("text."));
    serial = 0;
  };

  public void testHasToken() {
    String noTokens = BEGIN;
    assertFalse(Tokenator.hasToken(noTokens));
    assertTrue(Tokenator.hasToken(betokened));
  }

  public void testSimple() {
    assertEquals(EXPECTED, tokenator.detokenate(betokened));
  }
  
  public void testInfo() {
    int i = 0;
    for (ValueAndInfo valueAndInfo : tokenator.getOrderedValues(betokened)) {
      assertEquals(++i, valueAndInfo.info);
    }
  }

  public void testStatic() {
    assertEquals("0 is a 1 of 2", Tokenator.detokenate(betokened,
        new Tokenator.Resolver() {
          int i = 0;

          public String resolveToken(String token) {
            return String.format("%d", i++);
          }
        }));
  }

  private String betoken(String in) {
    int id = ++serial;
    return tokenator.nextToken(id, String.format(FORMAT, id, in));
  }
}
