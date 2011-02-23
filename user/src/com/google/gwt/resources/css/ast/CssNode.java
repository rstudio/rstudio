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
package com.google.gwt.resources.css.ast;

import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.resources.css.CssGenerationVisitor;

/**
 * The basic type that composes a CSS tree.
 */
public abstract class CssNode implements CssVisitable {
  
  /**
   * Indicates whether or not the CssNode requires runtime evaluation. This
   * method should not include any information from child nodes.
   */
  public abstract boolean isStatic();
  
  @Override
  public String toString() {
    DefaultTextOutput out = new DefaultTextOutput(false);
    CssGenerationVisitor v = new CssGenerationVisitor(out, true);
    v.accept(this);
    return out.toString();
  }
}
