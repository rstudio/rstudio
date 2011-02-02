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
package com.google.gwt.dev.js;

import java.util.HashSet;
import java.util.Set;

/**
 * Determines whether or not a particular string is a JavaScript keyword or not.
 */
public class JsKeywords {

  private static Set<String> sJavaScriptKeywords = new HashSet<String>();

  static {
    initJavaScriptKeywords();
  }

  public static boolean isKeyword(String s) {
    return sJavaScriptKeywords.contains(s);
  }

  private static synchronized void initJavaScriptKeywords() {
    String[] keywords = new String[]{
        // These are current keywords
        //
        "break", "delete", "function", "return", "typeof", "case", "do", "if",
        "switch", "var", "catch", "else", "in", "this", "void", "continue",
        "false", "instanceof", "throw", "while", "debugger",
        "finally",
        "new",
        "true",
        "with",
        "default",
        "for",
        "null",
        "try",

        // These are future keywords
        //
        "abstract", "double", "goto", "native", "static", "boolean", "enum",
        "implements", "package", "super", "byte", "export", "import",
        "private", "synchronized", "char", "extends", "int", "protected",
        "throws", "class", "final", "interface", "public", "transient",
        "const", "float", "long", "short", "volatile"};

    for (int i = 0; i < keywords.length; i++) {
      sJavaScriptKeywords.add(keywords[i]);
    }
  }

  private JsKeywords() {
  }

}
