/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single completed compilation.
 * 
 * @see Compilations
 */
public class Compilation {

  private Map rebindDecisions = new HashMap();

  private Map sourceHashByGeneratedTypeName = new HashMap();

  private String strongName;

  public Compilation() {
  }

  public String[] getGeneratedTypeNames() {
    return Util.toStringArray(sourceHashByGeneratedTypeName.keySet());
  }

  public String[] getRebindInputs() {
    return Util.toStringArray(rebindDecisions.keySet());
  }

  /**
   * @return <code>null</code> if there is no answer for this cached
   *         compilation
   */
  public String getRebindOutput(String inputTypeName) {
    String out = (String) rebindDecisions.get(inputTypeName);
    return out;
  }

  public String getStrongName() {
    return strongName;
  }

  public String getTypeHash(String generatedTypeName)
      throws UnableToCompleteException {
    String hash = (String) sourceHashByGeneratedTypeName.get(generatedTypeName);
    if (hash != null) {
      return hash;
    } else {
      throw new UnableToCompleteException();
    }
  }

  public boolean recordDecision(String inputTypeName, String outputTypeName) {
    // see if we've already recorded this one
    String recodedOutputName = (String) rebindDecisions.get(inputTypeName);
    if (recodedOutputName != null) {
      // Error to try to change the existing mapping
      if (!recodedOutputName.equals(outputTypeName)) {
        String msg = "Decision '" + recodedOutputName
            + "' already recorded for '" + inputTypeName + "'";
        throw new IllegalStateException(msg);
      }
      // already recorded this one
      return false;
    }
    rebindDecisions.put(inputTypeName, outputTypeName);
    // this was a new entry
    return true;
  }

  public void recordGeneratedTypeHash(String generatedTypeName,
      String sourceHash) {
    sourceHashByGeneratedTypeName.put(generatedTypeName, sourceHash);
  }

  public void setStrongName(String strongName) {
    this.strongName = strongName;
  }
}
