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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.Collection;
import java.util.List;

/**
 * A low-level node representing a castable type map.
 */
public class JCastMap extends JExpression {

  private List<JExpression> canCastToTypes = Lists.newArrayList();

  @Override
  public boolean hasSideEffects() {
    return false;
  }

  @Override
  public JType getType() {
    return null;
  }

  public JCastMap(SourceInfo sourceInfo, JClassType javaLangObjectType,
      Collection<JReferenceType> canCastToTypes) {
    super(sourceInfo);
    for (JReferenceType type : canCastToTypes) {
      this.canCastToTypes.add(new JRuntimeTypeReference(sourceInfo, javaLangObjectType, type));
    }
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(canCastToTypes);
    }
    visitor.endVisit(this, ctx);
  }

  public List<JExpression> getCanCastToTypes() {
    return canCastToTypes;
  }
}
