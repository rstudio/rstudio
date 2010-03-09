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
package com.google.gwt.dev.jjs.impl.gflow.cfg;

import com.google.gwt.dev.jjs.ast.JMethodCall;

/**
 * Node which might throw exception.
 */
public class CfgOptionalThrowNode extends CfgNode<JMethodCall> {
  /**
   * Edge role for normal, no-throwing execution.
   */
  public static final String NO_THROW = "NOTHROW";
  /**
   * Edge role for throwing RuntimeException.
   */
  public static final String RUNTIME_EXCEPTION = "RE";
  /**
   * Edge role for throwing generic Error.
   */
  public static final String ERROR = "E";
  
  public CfgOptionalThrowNode(CfgNode<?> parent, JMethodCall node) {
    super(parent, node);
  }

  @Override
  public void accept(CfgVisitor visitor) {
    visitor.visitOptionalThrowNode(this);
  }

  @Override
  public String toDebugString() {
    return "OPTTHROW(" + getJNode().getTarget().getName() + "())";
  }

  @Override
  protected CfgNode<?> cloneImpl() {
    return new CfgOptionalThrowNode(getParent(), getJNode());
  }
}
