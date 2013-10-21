/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.impl.codesplitter;

import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;

/**
* A {@link LivenessPredicate} where nothing is alive.
*/
public class NothingAlivePredicate implements LivenessPredicate {

  @Override
  public boolean isLive(JDeclaredType type) {
    return false;
  }

  @Override
  public boolean isLive(JField field) {
    return false;
  }

  @Override
  public boolean isLive(JMethod method) {
    return false;
  }

  @Override
  public boolean isLive(String string) {
    return false;
  }

  @Override
  public boolean miscellaneousStatementsAreLive() {
    return false;
  }
}
