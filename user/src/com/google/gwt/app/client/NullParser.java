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
package com.google.gwt.app.client;

import com.google.gwt.text.shared.Parser;
import com.google.gwt.valuestore.shared.Record;

import java.text.ParseException;

/**
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * <p>
 * A no-op renderer, always returns null
 * @param <T> a Record type.
 */
public class NullParser<T extends Record> implements Parser<T> {

  private static NullParser INSTANCE;

  /**
   * @return the instance of the null parser
   */
  public static <T extends Record> Parser<T> instance() {
    if (INSTANCE == null) {
      INSTANCE = new NullParser<T>();
    }
    return INSTANCE;
  }

  protected NullParser() {
  }

  public T parse(CharSequence object) throws ParseException {
    return null;
  }
}
