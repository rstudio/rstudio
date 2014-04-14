/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.jjs.SourceInfo;

/**
 * Abstract base class for JavaScript statement objects.
 */
public abstract class JsStatement extends JsNode {

  protected JsStatement(SourceInfo sourceInfo) {
    super(sourceInfo);
  }

  /**
   * Returns true if this statement definitely causes an abrupt change in flow control.
   */
  public boolean unconditionalControlBreak() {
    return false;
  }
}
