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

import java.util.ArrayList;
import java.util.List;

/**
 * Triggers the instantiation of an object.
 */
public class InstantiateCommand extends IdentityValueCommand implements
    HasSetters, HasTargetClass {
  private final Class<?> clazz;
  private final List<SetCommand> setters = new ArrayList<SetCommand>();

  public InstantiateCommand(Class<?> clazz) {
    this.clazz = clazz;
  }

  @Override
  public void clear() {
    setters.clear();
  }

  public List<SetCommand> getSetters() {
    return setters;
  }

  public Class<?> getTargetClass() {
    return clazz;
  }

  public void set(Class<?> fieldDeclClass, String fieldName, ValueCommand value) {
    setters.add(new SetCommand(fieldDeclClass, fieldName, value));
  }

  @Override
  public void traverse(RpcCommandVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(setters);
    }
    visitor.endVisit(this, ctx);
  }
}
