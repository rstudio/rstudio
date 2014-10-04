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
package com.google.gwt.resources.converter;

import com.google.gwt.resources.css.ast.Context;
import com.google.gwt.resources.css.ast.CssProperty;
import com.google.gwt.resources.css.ast.CssProperty.ListValue;
import com.google.gwt.resources.css.ast.CssVisitor;
import com.google.gwt.thirdparty.guava.common.base.Splitter;

import java.util.regex.Pattern;

/**
 * Escapes white spaces in font-family declarations, thus allowing usage of fonts that might
 * be mistaken for constants.
 */
public class FontFamilyVisitor extends CssVisitor {
  // font family name that contains white space(s) and aren't escaped yet.
  private static Pattern ESCAPE_TEST = Pattern.compile("^[^'\"].*\\s.*[^'\"]$");
  @Override
  public boolean visit(CssProperty x, Context ctx) {

    if ("font-family".equals(x.getName())) {
      ListValue values = x.getValues();
      String css = values.toCss();
      StringBuilder valueBuilder = new StringBuilder();

      boolean first = true;

      for (String subProperty : Splitter.on(",").trimResults().omitEmptyStrings().split(css)) {
        if (first) {
          first = false;
        } else {
          valueBuilder.append(",");
        }

        if (hasToBeEscaped(subProperty)) {
          valueBuilder.append("'" + subProperty + "'");
        } else {
          valueBuilder.append(subProperty);
        }
      }

      x.setValue(new SimpleValue(valueBuilder.toString()));
    }
    return false;
  }

  private boolean hasToBeEscaped(String fontFamilyName) {
    return ESCAPE_TEST.matcher(fontFamilyName).matches();
  }
}
