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
package com.google.gwt.sample.dynatablerf.client.widgets;

import com.google.gwt.dom.client.Document;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.text.shared.Parser;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.user.client.ui.ValueBox;

import java.io.IOException;
import java.text.ParseException;

/**
 * A simple implementation of a US zip code input field.
 * <p>
 * Accepted formats are <code>ddddd</code> or <code>ddddd-dddd</code>.
 */
public class ZipPlusFourBox extends ValueBox<String> {
  private static final RegExp PATTERN = RegExp.compile("^\\d{5}(-\\d{4})?$");
  private static final Renderer<String> RENDERER = new Renderer<String>() {
    public String render(String object) {
      if (object == null) {
        return null;
      }
      StringBuilder sb = new StringBuilder(String.valueOf(object));
      if (sb.length() == 9) {
        sb.insert(5, '-');
      }
      return sb.toString();
    }

    public void render(String object, Appendable appendable) throws IOException {
      appendable.append(render(object));
    }
  };
  private static final Parser<String> PARSER = new Parser<String>() {
    public String parse(CharSequence text) throws ParseException {
      switch (text.length()) {
        case 9:
          text = text.subSequence(0, 5) + "-" + text.subSequence(5, 9);
          // Fall-though intentional
          // CHECKSTYLE_OFF
        case 5:
          // Fall-through intentional
        case 10:
          // CHECKSTYLE_ON
          if (PATTERN.test(text.toString())) {
            return text.toString();
          } else {
            throw new ParseException("Illegal value", 0);
          }
      }
      throw new ParseException("Illegal length", 0);
    }
  };

  public ZipPlusFourBox() {
    super(Document.get().createTextInputElement(), RENDERER, PARSER);
  }
}
