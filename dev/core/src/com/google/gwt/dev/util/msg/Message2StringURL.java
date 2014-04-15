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

import java.net.URL;

/**
 * String & URL message.
 */
public final class Message2StringURL extends Message {

  public Message2StringURL(Type type, String fmt) {
    super(type, fmt, 2);
  }

  public TreeLogger branch(TreeLogger logger, String s, URL u, Throwable caught) {
    return branch(logger, s, u, getFormatter(s), getFormatter(u), caught);
  }

  public void log(TreeLogger logger, String s, URL u, Throwable caught) {
    log(logger, s, u, getFormatter(s), getFormatter(u), caught);
  }

}
