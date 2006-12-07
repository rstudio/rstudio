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
import com.google.gwt.dev.jjs.ast.JBlock;

class MoveBlock implements Change {
  final JBlock source;
  final JBlock target;

  public MoveBlock(JBlock source, JBlock target) {
    super();
    this.source = source;
    this.target = target;
  }

  public void apply() {
    assert (target.statements.size() == 0);
    target.statements.addAll(source.statements);
    source.statements.clear();
  }

  public void describe(TreeLogger logger, TreeLogger.Type type) {
    logger.log(type, "Move the body of " + ChangeList.getNodeString(source)
        + " to " + ChangeList.getNodeString(target), null);
  }
}