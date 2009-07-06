/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.rpc.client.ast;

import com.google.gwt.rpc.client.ast.RpcCommandVisitor.Context;

/**
 * A command to set the value of an object in the command stream.
 */
public class SetCommand extends RpcCommand {
  /*
   * NB: Not using Field due to lack of GWT compatibility. Consider adding the
   * Field type to the GWT JRE code.
   */
  private String field;
  /**
   * The class that defines the field. Used to account for field shadowing.
   */
  private Class<?> fieldDeclClass;
  private ValueCommand value;

  public SetCommand(Class<?> fieldDeclClass, String field, ValueCommand value) {
    this.fieldDeclClass = fieldDeclClass;
    this.field = field;
    this.value = value;
  }

  @Override
  public void clear() {
    field = null;
    value = null;
  }

  public String getField() {
    return field;
  }

  public Class<?> getFieldDeclClass() {
    return fieldDeclClass;
  }

  public ValueCommand getValue() {
    return value;
  }

  @Override
  public void traverse(RpcCommandVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      value.traverse(visitor, ctx);
    }
    visitor.endVisit(this, ctx);
  }
}
