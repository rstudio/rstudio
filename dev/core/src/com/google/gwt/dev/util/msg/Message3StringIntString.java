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
 * String, Integer, & String message.
 */
public final class Message3StringIntString extends Message {

  public Message3StringIntString(Type type, String fmt) {
    super(type, fmt, 3);
  }

  public TreeLogger branch(TreeLogger logger, String s1, int x, String s2,
      Throwable caught) {
    Integer xi = Integer.valueOf(x);
    return branch(logger, s1, xi, s2, getFormatter(s1), getFormatter(xi),
        getFormatter(s2), caught);
  }

  public void log(TreeLogger logger, String s1, int x, String s2,
      Throwable caught) {
    Integer xi = Integer.valueOf(x);
    log(logger, s1, xi, s2, getFormatter(s1), getFormatter(xi),
        getFormatter(s2), caught);
  }

}
