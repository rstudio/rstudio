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
import com.google.gwt.rpc.client.impl.SimplePayloadSink;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.impl.SerializabilityUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;

/**
 * A ClientOracle that is used for hosted-mode clients. This type only
 * implements a limited subset of the ClientOracle functionality.
 */
public final class HostedModeClientOracle extends ClientOracle {

  @Override
  public CommandSink createCommandSink(OutputStream out) throws IOException {
    final BufferedWriter buffer = new BufferedWriter(new OutputStreamWriter(
        out, "UTF-8"));

    return new SimplePayloadSink(buffer) {
      @Override
      public void finish() throws SerializationException {
        super.finish();
        try {
          buffer.flush();
        } catch (IOException e) {
          throw new SerializationException("Could not flush buffer", e);
        }
      }
    };
  }

  /**
   * Unimplemented.
   */
  @Override
  public String createUnusedIdent(String ident) {
    return unimplemented();
  }

  @Override
  public CastableTypeData getCastableTypeData(Class<?> clazz) {
    return unimplemented();
  }

  /**
   * Unimplemented.
   */
  @Override
  public String getFieldId(Class<?> clazz, String fieldName) {
    return unimplemented();
  }

  /**
   * Unimplemented.
   */
  @Override
  public String getFieldId(Enum<?> value) {
    return unimplemented();
  }

  /**
   * Unimplemented.
   */
  @Override
  public String getFieldId(String className, String fieldName) {
    return unimplemented();
  }

  @Override
  public Pair<Class<?>, String> getFieldName(Class<?> clazz, String fieldId) {
    while (clazz != null) {
      try {
        clazz.getDeclaredField(fieldId);
        return new Pair<Class<?>, String>(clazz, fieldId);
      } catch (SecurityException e) {
        // Fall through
      } catch (NoSuchFieldException e) {
        // Fall through
      }
      clazz = clazz.getSuperclass();
    }
    return null;
  }

  /**
   * Unimplemented.
   */
  @Override
  public String getMethodId(Class<?> clazz, String methodName, Class<?>... args) {
    return unimplemented();
  }

  /**
   * Unimplemented.
   */
  @Override
  public String getMethodId(String className, String methodName,
      String... jsniArgTypes) {
    return unimplemented();
  }

  /**
   * Falls back to reflectively analyzing the provided class.
   */
  @Override
  public Field[] getOperableFields(Class<?> clazz) {
    return SerializabilityUtil.applyFieldSerializationPolicy(clazz);
  }

  /**
   * Unimplemented.
   */
  @Override
  public String getRuntimeTypeId(Class<?> clazz) {
    return unimplemented();
  }

  /**
   * Unimplemented.
   */
  @Override
  public String getJsSymbolName(Class<?> clazz) {
    return unimplemented();
  }

  /**
   * Unimplemented.
   */
  @Override
  public String getTypeName(String seedName) {
    return seedName;
  }

  /**
   * Unimplemented.
   */
  @Override
  public boolean isScript() {
    return false;
  }

  private <T> T unimplemented() {
    throw new RuntimeException("Not supported in Development Mode");
  }
}
