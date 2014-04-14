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
package com.google.gwt.dev;

import com.google.gwt.core.ext.CachedGeneratorResult;
import com.google.gwt.dev.cfg.Rule;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A cache for storing {@link CachedGeneratorResult} entries.  Entries are keyed
 * by rebind Rule and queryTypeName.
 */
public class RebindCache implements Serializable {

  private final Map<String, Map<String, CachedGeneratorResult>> rebindResults;

  public RebindCache() {
    rebindResults = new HashMap<String, Map<String, CachedGeneratorResult>>();
  }

  public CachedGeneratorResult get(Rule rule, String queryTypeName) {
    Map<String, CachedGeneratorResult> ruleResults;
    ruleResults = rebindResults.get(rule.toString());
    if (ruleResults != null) {
      return ruleResults.get(queryTypeName);
    }

    return null;
  }

  public void put(Rule rule, String queryTypeName, CachedGeneratorResult results) {
    Map<String, CachedGeneratorResult> ruleResults = rebindResults.get(rule.toString());
    if (ruleResults == null) {
      ruleResults = new HashMap<String, CachedGeneratorResult>();
      rebindResults.put(rule.toString(), ruleResults);
    }
    ruleResults.put(queryTypeName, results);
  }
}