/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.util.arg;

/**
 * Option to report warnings when modules are missing dependencies necessary to satisfy type
 * references in their provided source.
 */
public interface OptionWarnMissingDeps {

  /**
   * Whether or not to warn on when modules are missing dependencies necessary to satisfy type
   * references in their provided source.
   */
  boolean warnMissingDeps();

  /**
   * Sets whether or not to warn when modules are missing dependencies necessary to satisfy type
   * references in their provided source.
   */
  void setWarnMissingDeps(boolean warnMissingDeps);
}
