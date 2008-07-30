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
package com.google.gwt.dev;

import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.jdt.RebindOracle;

/**
 * Represents the state of a single permutation for compile.
 * 
 * @see PermutationCompiler
 */
public final class Permutation {
  private final int number;
  private final StaticPropertyOracle propertyOracle;
  private final RebindOracle rebindOracle;

  public Permutation(int number, RebindOracle rebindOracle,
      StaticPropertyOracle propertyOracle) {
    this.number = number;
    this.rebindOracle = rebindOracle;
    this.propertyOracle = propertyOracle;
  }

  public int getNumber() {
    return number;
  }

  public StaticPropertyOracle getPropertyOracle() {
    return propertyOracle;
  }

  public RebindOracle getRebindOracle() {
    return rebindOracle;
  }
}
