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
 * A placeholder for custom logic invocation.
 */
public class InvokeCustomFieldSerializerCommand extends IdentityValueCommand
    implements HasSetters, HasTargetClass, HasValues {
  private final Class<?> manuallySerializedType;
  private final Class<?> serializer;
  private final List<SetCommand> setters = new ArrayList<SetCommand>();
  private final Class<?> instantiatedType;
  private final List<ValueCommand> values = new ArrayList<ValueCommand>();

  public InvokeCustomFieldSerializerCommand(Class<?> instantiatedType,
      Class<?> serializer, Class<?> manuallySerializedType) {
    this.instantiatedType = instantiatedType;
    this.serializer = serializer;
    this.manuallySerializedType = manuallySerializedType;
  }

  public void addValue(ValueCommand value) {
    values.add(value);
  }

  @Override
  public void clear() {
    values.clear();
  }

  public Class<?> getManuallySerializedType() {
    return manuallySerializedType;
  }

  public Class<?> getSerializerClass() {
    return serializer;
  }

  public List<SetCommand> getSetters() {
    return setters;
  }

  public Class<?> getTargetClass() {
    return instantiatedType;
  }

  public List<ValueCommand> getValues() {
    return values;
  }

  public void set(Class<?> fieldDeclClass, String fieldName, ValueCommand value) {
    setters.add(new SetCommand(fieldDeclClass, fieldName, value));
  }

  @Override
  public void traverse(RpcCommandVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(values);
      visitor.accept(setters);
    }
    visitor.endVisit(this, ctx);
  }
}
