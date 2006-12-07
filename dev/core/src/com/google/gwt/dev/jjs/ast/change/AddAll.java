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
package com.google.gwt.dev.jjs.ast.change;

import com.google.gwt.core.ext.TreeLogger;

import java.util.List;

class AddAll/* <N extends JNode> */extends ChangeBase {

  final List/* <N> */source;
  final List/* <N> */list;
  private final int index;

  public AddAll(List/* <N> */source, int index, List/* <N> */list) {
    this.source = source;
    this.index = index;
    this.list = list;
  }

  public void apply() {
    if (index < 0) {
      list.addAll(source);
    } else {
      list.addAll(index, source);
    }
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    if (index < 0) {
      logger.log(type, "Add a list to a list", null);
    } else {
      logger.log(type, "Add a list to a list at index " + index, null);
    }
  }

}