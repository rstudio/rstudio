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
package java.util;

/**
 * A very simple emulation of Locale for shared-code patterns like
 * {@code String.toUpperCase(Locale.US)}.
 * <p>
 * Note: Any changes to this class should put into account the assumption that was made in rest of
 * the JRE emulation.
 */
public class Locale {

  public static final Locale ROOT = new Locale() {
    @Override
    public String toString() {
      return "";
    }
  };

  public static final Locale ENGLISH = new Locale() {
    @Override
    public String toString() {
      return "en";
    }
  };

  public static final Locale US = new Locale() {
    @Override
    public String toString() {
      return "en_US";
    }
  };

  private static Locale defaultLocale = new Locale() {
    @Override
    public String toString() {
      return "unknown";
    }
  };

  /**
   * Returns an instance that represents the browser's default locale (not necessarily the one
   * defined by 'gwt.locale').
   */
  public static Locale getDefault() {
    return defaultLocale;
  }

  // Hidden as we don't support manual creation of Locales.
  private Locale() { }
}
