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
import com.google.gwt.dev.jjs.ast.Mutator;

import java.util.List;

class RemoveNodeMutator/* <N extends JNode> */extends ChangeBase {

  final List/* <N> */list;
  private final Mutator/* <N> */node;

  public RemoveNodeMutator(Mutator/* <N> */node, List/* <N> */list) {
    this.node = node;
    this.list = list;
    assert (list.contains(node.get()));
  }

  public void apply() {
    boolean removed = list.remove(node.get());
    assert (removed);
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    logger.log(type, "Remove " + ChangeList.getNodeString(node.get())
        + ChangeList.getEnclosingTypeString(" from", node.get()), null);
  }

}