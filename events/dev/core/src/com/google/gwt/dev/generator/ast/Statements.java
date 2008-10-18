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
package com.google.gwt.dev.generator.ast;

import java.util.List;

/**
 * Represents one or more groups of {@link Statement}s. Can optionally be
 * added to.
 */
public interface Statements extends Node {

  /**
   * Returns a list of {@link Statements}.
   *
   * @return a non <code>null</code> list of {@link Statements}.
   */
  List<Statements> getStatements();
}
