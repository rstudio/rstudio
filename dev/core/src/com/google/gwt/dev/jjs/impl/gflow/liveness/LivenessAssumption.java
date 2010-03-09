/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.jjs.impl.gflow.liveness;

import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.impl.gflow.Assumption;
import com.google.gwt.dev.util.collect.IdentityHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Assumption for LivenessAnalysis. Contains set of all live (=used after) 
 * variables.
 */
public class LivenessAssumption implements Assumption<LivenessAssumption> {
  /**
   * Updates the assumption by copying it on first write.
   */
  public static class Updater {
    private LivenessAssumption assumption;
    private boolean copied = false;
    
    public Updater(LivenessAssumption assumption) {
      this.assumption = assumption;
    }

    public void kill(JVariable target) {
      if (assumption == null || !assumption.isLive(target)) {
        return;
      }
      copyIfNeeded();
      assumption.kill(target);
    }

    public LivenessAssumption unwrap() {
      if (assumption != null && assumption.liveVariables.isEmpty()) {
        return null;
      }
      return assumption;
    }

    public void use(JVariable target) {
      copyIfNeeded();
      assumption.use(target);
    }

    private void copyIfNeeded() {
      if (!copied) {
        assumption = new LivenessAssumption(assumption);
        copied = true;
      }
    }
  }
  
  /**
   * Set of all live variables.
   */
  private final Set<JVariable> liveVariables = new IdentityHashSet<JVariable>();
  
  public LivenessAssumption() {
    super();
  }

  public LivenessAssumption(LivenessAssumption assumptions) {
    if (assumptions != null) {
      this.liveVariables.addAll(assumptions.liveVariables);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    LivenessAssumption other = (LivenessAssumption) obj;
    return liveVariables.equals(other.liveVariables);
  }

  @Override
  public int hashCode() {
    return liveVariables.hashCode();
  }

  public boolean isLive(JVariable variable) {
    return liveVariables.contains(variable);
  }

  /**
   * Computes union of all live variables.
   */
  public LivenessAssumption join(LivenessAssumption value) {
    if (value == null || value.liveVariables.isEmpty()) {
      return this;
    }
    if (liveVariables.isEmpty()) {
      return value;
    }
    LivenessAssumption result = new LivenessAssumption(this);
    result.liveVariables.addAll(value.liveVariables);
    return result;
  }

  public String toDebugString() {
    StringBuffer result = new StringBuffer();
    
    result.append("{");
    List<JVariable> vars = new ArrayList<JVariable>(liveVariables);
    Collections.sort(vars, new Comparator<JVariable>() {
      public int compare(JVariable o1, JVariable o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    for (JVariable variable : vars) {
      if (result.length() > 1) {
        result.append(", ");
      }
      result.append(variable.getName());
    }
    result.append("}");
    
    return result.toString();
  }

  @Override
  public String toString() {
    return toDebugString();
  }

  private void kill(JVariable variable) {
    liveVariables.remove(variable);
  }

  private void use(JVariable variable) {
    liveVariables.add(variable);
  }
}
