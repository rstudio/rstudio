/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.dev.js.ast.JsObfuscatableName;
import com.google.gwt.dev.js.ast.JsScope;

/**
 * Implements a naming strategy that obfuscated the standard names of
 * identifiers.
 */
public class ObfuscatedNamingStrategy extends NamingStrategy {

  private static final char[] sBase64Chars = new char[] {
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
      'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B',
      'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
      'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3',
      '4', '5', '6', '7', '8', '9', '$', '_'};

  private int nextId = 0;

  // @Override
  protected String getBaseIdent(JsObfuscatableName name) {
    return name.getIdent();
  }

  // @Override
  protected String obfuscate(String name, JsScope scope, JsScope rootScope) {
    String result = "";
    int curId = nextId++;

    // Use base-32 for the first character of the identifier,
    // so that we don't use any numbers (which are illegal at
    // the beginning of an identifier).
    //
    result += sBase64Chars[curId & 0x1f];
    curId >>= 5;

    // Use base-64 for the rest of the identifier.
    //
    while (curId != 0) {
      result += sBase64Chars[curId & 0x3f];
      curId >>= 6;
    }

    return result;
  }

}
