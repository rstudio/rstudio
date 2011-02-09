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
package com.google.gwt.dev.javac.rebind;

import com.google.gwt.dev.cfg.Rule;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A cache for storing {@link CachedRebindResult} entries.  Entries are keyed
 * by rebind Rule and queryTypeName.
 */
public class RebindCache implements Serializable {
 
  private final Map<String, Map<String, CachedRebindResult>> rebindResults;
  
  public RebindCache() {
    rebindResults = new HashMap<String, Map<String, CachedRebindResult>>();
  }

  public CachedRebindResult get(Rule rule, String queryTypeName) {
    Map<String, CachedRebindResult> ruleResults;
    ruleResults = rebindResults.get(rule.toString());
    if (ruleResults != null) {
      return ruleResults.get(queryTypeName);
    }

    return null;
  }
  
  public void invalidate() {
    rebindResults.clear();
  }
  
  public void put(Rule rule, String queryTypeName, CachedRebindResult results) {
    Map<String, CachedRebindResult> ruleResults = rebindResults.get(rule.toString());
    if (ruleResults == null) {
      ruleResults = new HashMap<String, CachedRebindResult>();
      rebindResults.put(rule.toString(), ruleResults);
    }
    ruleResults.put(queryTypeName, results);
  }
}