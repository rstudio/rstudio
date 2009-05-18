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
 * Long message.
 */
public final class Message1Long extends Message1 {

  public Message1Long(Type type, String fmt) {
    super(type, fmt);
  }

  public TreeLogger branch(TreeLogger logger, long x, Throwable caught) {
    Long xl = Long.valueOf(x);
    return branch1(logger, xl, getFormatter(xl), caught);
  }

  public void log(TreeLogger logger, long x, Throwable caught) {
    Long xl = Long.valueOf(x);
    log1(logger, xl, getFormatter(xl), caught);
  }

}
