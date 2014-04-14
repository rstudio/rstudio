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
package com.google.gwt.dev.jjs.impl.gflow.unreachable;

import com.google.gwt.dev.jjs.impl.gflow.Assumption;

/**
 *
 */
public class UnreachableAssumptions implements Assumption<UnreachableAssumptions> {
  public static final UnreachableAssumptions REACHABLE = new UnreachableAssumptions();
  public static final UnreachableAssumptions UNREACHABLE = new UnreachableAssumptions();

  public static boolean isReachable(UnreachableAssumptions in) {
    return in == REACHABLE;
  }

  @Override
  public UnreachableAssumptions join(UnreachableAssumptions value) {
    if (this == REACHABLE || value == REACHABLE) {
      return REACHABLE;
    }
    return UNREACHABLE;
  }

  @Override
  public String toString() {
    return this == REACHABLE ? "T" : "F";
  }
}
