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
package com.google.gwt.tools.cldr;

import org.unicode.cldr.util.Factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wraps a CLDR factory to ensure that its output is deterministic.
 */
class InputFactory {
  private final Factory delegate;

  InputFactory(String sourceDir) {
    delegate = Factory.make(sourceDir, ".*");
  }

  InputFile load(String localeId) {
    return new InputFile(delegate.make(localeId, true));
  }

  InputFile getSupplementalData() {
    return new InputFile(delegate.getSupplementalData());
  }

  /**
   * Returns the list of locales to generate.
   * @param restrictLocales a comma-separated list of locales to include, or null for all locales.
   */
  List<String> chooseLocales(String restrictLocales) {
    Set<String> locales = delegate.getAvailable();
    if (restrictLocales != null) {
      Set<String> newLocales = new HashSet<String>();
      newLocales.add("root");  // always include root or things break
      for (String locale : restrictLocales.split(",")) {
        if (!locales.contains(locale)) {
          System.err.println("Ignoring non-existent locale " + locale);
          continue;
        }
        newLocales.add(locale);
      }
      locales = newLocales;
    }

    List<String> out = new ArrayList<String>(locales);
    Collections.sort(out);
    return out;
  }

  List<String> getAvailableLanguages() {
    List<String> out = new ArrayList<String>(delegate.getAvailableLanguages());
    Collections.sort(out);
    return out;
  }
}
