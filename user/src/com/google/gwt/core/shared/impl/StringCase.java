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
package com.google.gwt.core.shared.impl;

import java.util.Locale;

/**
 * Provides {@link Locale} independent {@link String#toLowerCase()} and {@link String#toLowerCase()}
 * functions.
 * <p>
 * For client-only code, even the emulated version of {@link String} already works locale
 * independent, as dev-mode uses the one from JDK this class should still be used in place of
 * regular String methods.
 * <p>
 * This class has a translated version to deal with the missing {@link String#toLowerCase(Locale)}
 * method in client code.
 *
 * @see <a href="http://mattryall.net/blog/2009/02/the-infamous-turkish-locale-bug">Infamus Turkish
 *      locale bug</a>
 */
public class StringCase {
  public static String toLower(String string) {
    return string.toLowerCase(Locale.ENGLISH);
  }

  public static String toUpper(String string) {
    return string.toUpperCase(Locale.ENGLISH);
  }
}
