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
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

/**
 * Integer & String message.
 */
public final class Message2IntString extends Message2 {

  public Message2IntString(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, int x, String s, Throwable caught) {
    Integer xi = Integer.valueOf(x);
    return branch2(logger, xi, s, getFormatter(xi), getFormatter(s), caught);
  }

  public void log(TreeLogger logger, int x, String s, Throwable caught) {
    Integer xi = Integer.valueOf(x);
    log2(logger, xi, s, getFormatter(xi), getFormatter(s), caught);
  }

}
