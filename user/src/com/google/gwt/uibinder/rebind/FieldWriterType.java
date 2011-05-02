/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.uibinder.rebind;

/**
 * A simple enum holding all FieldWriter types.
 */
enum FieldWriterType {

  GENERATED_BUNDLE(4),
  GENERATED_CSS(3),
  IMPORTED(2),  // ui:with clauses.
  DEFAULT(1);

  /**
   * Holds the build precedence for this type. This is used when sorting the
   * field builders in the Widgets constructor.
   * {@see com.google.gwt.uibinder.rebind.initializeWidgetsInnerClass}
   */
  private int buildPrecedence;

  private FieldWriterType(int precedence) {
    this.buildPrecedence = precedence;
  }

  public int getBuildPrecedence() {
    return buildPrecedence;
  }
}
