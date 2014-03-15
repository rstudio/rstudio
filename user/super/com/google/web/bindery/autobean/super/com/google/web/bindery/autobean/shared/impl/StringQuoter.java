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
package com.google.web.bindery.autobean.shared.impl;

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JsDate;
import com.google.gwt.core.client.JsonUtils;
import com.google.web.bindery.autobean.gwt.client.impl.JsoSplittable;
import com.google.web.bindery.autobean.shared.Splittable;

import java.util.Date;

/**
 * This a super-source version with a client-only implementation.
 */
@GwtScriptOnly
public class StringQuoter {
  public static Splittable create(boolean value) {
    return JsoSplittable.create(value);
  }

  public static Splittable create(double value) {
    return JsoSplittable.create(value);
  }

  public static Splittable create(String value) {
    return JsoSplittable.create(value);
  }

  public static Splittable createIndexed() {
    return JsoSplittable.createIndexed();
  }

  public static Splittable createSplittable() {
    return JsoSplittable.create();
  }

  public static Splittable nullValue() {
    return JsoSplittable.nullValue();
  }

  public static String quote(String raw) {
    return JsonUtils.escapeValue(raw);
  }

  public static Splittable split(String payload) {
    char c = payload.charAt(0);
    boolean isSimple = c != '{' && c != '[';
    if (isSimple) {
      payload = "[" + payload + "]";
    }
    Splittable toReturn = JsonUtils.safeEval(payload).<JsoSplittable> cast();
    if (isSimple) {
      toReturn = toReturn.get(0);
    }
    return toReturn;
  }

  public static Date tryParseDate(String date) {
    try {
      return new Date(Long.parseLong(date));
    } catch (NumberFormatException ignored) {
    }
    try {
      JsDate js = JsDate.create(date);
      return new Date((long) js.getTime());
    } catch (JavaScriptException ignored) {
    }
    return null;
  }
}
