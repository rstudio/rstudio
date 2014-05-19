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
package com.google.gwt.dev.js;

import com.google.gwt.dev.cfg.ConfigProps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Holds the list of JavaScript identifiers that have special meaning, so they shouldn't be used
 * in a GWT program. (Unless used deliberately in JavaScript.)
 */
class ReservedNames {

  static final String BLACKLIST = "js.identifier.blacklist";
  static final String BLACKLIST_SUFFIXES = "js.identifier.blacklist.suffixes";

  private final Set<String> blacklistedIdents;

  private final List<String> blacklistedSuffixes;

  ReservedNames(ConfigProps config) {
    Set<String> blacklist = new HashSet<String>();
    List<String> blacklistSuffixes = new ArrayList<String>();
    if (config != null) {
      blacklist.addAll(config.getCommaSeparatedStrings(BLACKLIST));
      blacklistSuffixes.addAll(config.getCommaSeparatedStrings(BLACKLIST_SUFFIXES));
    }
    blacklistedIdents = Collections.unmodifiableSet(blacklist);
    blacklistedSuffixes = Collections.unmodifiableList(blacklistSuffixes);
  }

  final boolean isAvailable(String newIdent) {
    if (!JsProtectedNames.isLegalName(newIdent)) {
      return false;
    }
    String lcIdent = newIdent.toLowerCase(Locale.ENGLISH);
    for (String suffix : blacklistedSuffixes) {
      if (lcIdent.endsWith(suffix.toLowerCase(Locale.ENGLISH))) {
        return false;
      }
    }
    return !blacklistedIdents.contains(newIdent);
  }
}
