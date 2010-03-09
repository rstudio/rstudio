/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.jjs.impl.gflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Set of all assumptions for all edges coming from/to a subgraph.
 * 
 * @param <A> assumption type.
 */
public class SubgraphAssumptions<A extends Assumption<?>> {
  public static <A extends Assumption<?>> SubgraphAssumptions<A> 
  replaceInValues(SubgraphAssumptions<A> assumptions, A inValue) {
    ArrayList<A> inValues = new ArrayList<A>();
    for (int i = 0; i < assumptions.getInValues().size(); ++i) {
      inValues.add(inValue);
    }

    return replaceInValues(assumptions, inValues);
  }
  
  public static <A extends Assumption<?>> SubgraphAssumptions<A> 
  replaceInValues(SubgraphAssumptions<A> assumptions, ArrayList<A> inValues) {
    return new SubgraphAssumptions<A>(inValues, assumptions.getOutValues());
  }

  public static <A extends Assumption<?>> SubgraphAssumptions<A> 
  replaceOutValues(SubgraphAssumptions<A> assumptions, A outValue) {
    ArrayList<A> outValues = new ArrayList<A>();
    for (int i = 0; i < assumptions.getOutValues().size(); ++i) {
      outValues.add(outValue);
    }

    return replaceOutValues(assumptions, outValues);
  }
  
  public static <A extends Assumption<?>> SubgraphAssumptions<A> 
  replaceOutValues(SubgraphAssumptions<A> assumptions, ArrayList<A> outValues) {
    return new SubgraphAssumptions<A>(assumptions.getInValues(), outValues);
  }

  private List<A> inValues;
  private List<A> outValues;

  public SubgraphAssumptions(List<A> inValues, List<A> outValues) {
    if (inValues == null) {
      this.inValues = new ArrayList<A>(0);
    } else {
      this.inValues = inValues;
    }
    
    if (outValues == null) {
      this.outValues = new ArrayList<A>(0);
    } else {
      this.outValues = outValues;
    }
  }

  /**
   * Gets assumptions along incoming edges. 
   */
  public List<A> getInValues() {
    return inValues;
  }
  
  /**
   * Gets assumptions along outgoing edges. 
   */
  public List<A> getOutValues() {
    return outValues;
  }

  @Override
  public String toString() {
    return "[" + inValues + " => " + outValues + "]";
  }
}
