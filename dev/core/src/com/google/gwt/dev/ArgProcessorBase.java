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
package com.google.gwt.dev;

import com.google.gwt.util.tools.ToolBase;

/**
 * Base class for new-style argument processors.
 */
public abstract class ArgProcessorBase extends ToolBase {
  /*
   * Configures contained options by parsing the given args. Multiple args can
   * set a value for the same option and the last setting wins.
   */
  @Override
  public final boolean processArgs(String... args) {
    return super.processArgs(args);
  }

  /*
   * Made abstract to force override.
   */
  @Override
  protected abstract String getName();
}
