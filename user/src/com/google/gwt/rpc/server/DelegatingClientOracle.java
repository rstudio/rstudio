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
package com.google.gwt.rpc.server;

import com.google.gwt.rpc.client.ast.CommandSink;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;

/**
 * A delegate-pattern ClientOracle that can be used to introduce custom
 * behavior.
 */
public class DelegatingClientOracle extends ClientOracle {

  private final ClientOracle delegate;

  public DelegatingClientOracle(ClientOracle delegate) {
    this.delegate = delegate;
  }

  @Override
  public CommandSink createCommandSink(OutputStream out) throws IOException {
    return delegate.createCommandSink(out);
  }

  @Override
  public String createUnusedIdent(String ident) {
    return delegate.createUnusedIdent(ident);
  }

  @Override
  public CastableTypeData getCastableTypeData(Class<?> clazz) {
    return delegate.getCastableTypeData(clazz);
  }

  @Override
  public String getFieldId(Class<?> clazz, String fieldName) {
    return delegate.getFieldId(clazz, fieldName);
  }

  @Override
  public String getFieldId(Enum<?> value) {
    return delegate.getFieldId(value);
  }

  @Override
  public String getFieldId(String className, String fieldName) {
    return delegate.getFieldId(className, fieldName);
  }

  @Override
  public Pair<Class<?>, String> getFieldName(Class<?> clazz, String fieldId) {
    return delegate.getFieldName(clazz, fieldId);
  }

  @Override
  public String getMethodId(Class<?> clazz, String methodName, Class<?>... args) {
    return delegate.getMethodId(clazz, methodName, args);
  }

  @Override
  public String getMethodId(String className, String methodName,
      String... jsniArgTypes) {
    return delegate.getMethodId(className, methodName, jsniArgTypes);
  }

  @Override
  public Field[] getOperableFields(Class<?> clazz) {
    return delegate.getOperableFields(clazz);
  }

  @Override
  public String getRuntimeTypeId(Class<?> clazz) {
    return delegate.getRuntimeTypeId(clazz);
  }

  @Override
  public String getJsSymbolName(Class<?> clazz) {
    return delegate.getJsSymbolName(clazz);
  }

  @Override
  public String getTypeName(String seedName) {
    return delegate.getTypeName(seedName);
  }

  @Override
  public boolean isScript() {
    return delegate.isScript();
  }
}
