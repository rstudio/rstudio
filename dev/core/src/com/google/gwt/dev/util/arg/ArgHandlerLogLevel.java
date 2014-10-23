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
package com.google.gwt.dev.util.arg;

import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.util.tools.ArgHandlerEnum;

/**
 * Argument handler for processing the log level flag.
 */
public class ArgHandlerLogLevel extends ArgHandlerEnum<Type> {

  private final OptionLogLevel options;

  public ArgHandlerLogLevel(OptionLogLevel options) {
    this(options, options.getLogLevel() == null ? Type.INFO : options.getLogLevel());
  }

  public ArgHandlerLogLevel(OptionLogLevel options, Type defaultLogLevel) {
    super(Type.class, defaultLogLevel, false);
    this.options = options;
  }

  @Override
  public String getPurpose() {
    return getPurposeString("The level of logging detail:");
  }

  @Override
  public String getTag() {
    return "-logLevel";
  }

  @Override
  public void setValue(Type logLevel) {
    if (options.getLogLevel() != logLevel) {
      options.setLogLevel(logLevel);
    }
  }
}
