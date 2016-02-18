/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * Used to test static imports in UiBinder templates.
 */
public class Constants {
  /**
   * Tests enum imports.
   */
  public enum MyEnum {
    ENUM_1, ENUM_2;
  }

  /**
   * Used to test a wildcard import.
   */
  public static class Inner {
    String instance = "instance";
    public static String CONST_BAR = "Bar";
    static String CONST_BAZ = "Baz";
    protected static String PROTECTED = "protected";
    @SuppressWarnings("unused")
    private static String PRIVATE = "private";
  }

  public static String CONST_FOO = "Foo";

  public SafeHtml getSafeHtml() {
    return SafeHtmlUtils.fromSafeConstant("<b>This text should be bold!</b>");
  }
  
  public String getText() {
    return "<b>This text won't be bold!</b>";
  }

  public String getRendererText() {
    return "<b>Here's the text!</b>";
  }
}
