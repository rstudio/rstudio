/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.dev.jjs.SourceOrigin;

import java.io.Serializable;
import java.util.List;

/**
 * A Java constructor method.
 */
public class JConstructor extends JMethod {

  private static class ExternalSerializedForm implements Serializable {

    private final JClassType enclosingType;
    private final String signature;

    public ExternalSerializedForm(JConstructor ctor) {
      enclosingType = ctor.getEnclosingType();
      signature = ctor.getSignature();
    }

    private Object readResolve() {
      JConstructor result = new JConstructor(SourceOrigin.UNKNOWN, enclosingType);
      result.signature = signature;
      return result;
    }
  }

  /**
   * Caches whether or not this constructor does any work. Once true, we never
   * have to recheck, but we keep rechecking as long as it's false.
   */
  private boolean isEmpty = false;

  private boolean defaultConstructor;

  public void setDefaultConstructor() {
    defaultConstructor = true;
  }

  /**
   * True if the constructor is default, auto-synthesized.
   */
  public boolean isDefaultConstructor() {
    return defaultConstructor;
  }

  public JConstructor(SourceInfo info, JClassType enclosingType) {
    // Access only matters for virtual methods, just use public.
    super(info, enclosingType.getShortName(), enclosingType, JPrimitiveType.VOID, false, false,
        true, AccessModifier.PUBLIC);
  }

  @Override
  public boolean canBePolymorphic() {
    return false;
  }

  @Override
  public JMethodBody getBody() {
    return (JMethodBody) super.getBody();
  }

  @Override
  public JClassType getEnclosingType() {
    return (JClassType) super.getEnclosingType();
  }

  public JNonNullType getNewType() {
    return getEnclosingType().getNonNull();
  }

  @Override
  public boolean isConstructor() {
    return true;
  }

  /**
   * Returns <code>true</code> if this constructor does no real work.
   *
   * NOTE: this method does NOT account for any clinits that would be triggered
   * if this constructor is the target of a new instance operation from an
   * external class.
   *
   * TODO(scottb): make this method less expensive by computing in an external
   * visitor.
   */
  public boolean isEmpty() {
    if (isEmpty) {
      return true;
    }
    JMethodBody body = getBody();
    List<JStatement> statements = body.getStatements();
    if (statements.isEmpty()) {
      return (isEmpty = true);
    }
    if (statements.size() > 1) {
      return false;
    }
    // Only one statement. Check to see if it's an empty super() or this() call.
    JStatement stmt = statements.get(0);
    if (stmt instanceof JExpressionStatement) {
      JExpressionStatement exprStmt = (JExpressionStatement) stmt;
      JExpression expr = exprStmt.getExpr();
      if (expr instanceof JMethodCall && !(expr instanceof JNewInstance)) {
        JMethodCall call = (JMethodCall) expr;
        JMethod target = call.getTarget();
        if (target instanceof JConstructor) {
          return isEmpty = ((JConstructor) target).isEmpty();
        }
      }
    }
    return false;
  }

  @Override
  public boolean needsVtable() {
    return false;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    String before = traceBefore(visitor);
    if (visitor.visit(this, ctx)) {
      visitChildren(visitor);
    }
    visitor.endVisit(this, ctx);
    traceAfter(visitor, before);
  }

  @Override
  protected Object writeReplace() {
    if (isExternal()) {
      return new ExternalSerializedForm(this);
    } else {
      return this;
    }
  }
}
