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
 * Encapsulates information about a remote client. This type is not intended to
 * be implemented by end-users although the behavior of a concrete
 * implementation may be modified via the {@link DelegatingClientOracle} type.
 */
public abstract class ClientOracle {

  /**
   * Not a generally-extensible class.
   */
  ClientOracle() {
  }

  /**
   * Create a CommandSink that can encode a payload for the client.
   * 
   * @param out the OutputStream to which the output will be written
   * @return a CommandSink
   * @throws IOException if the CommandSink cannot write to the OutputStream
   */
  public abstract CommandSink createCommandSink(OutputStream out)
      throws IOException;

  /**
   * Returns an identifier that does not conflict with any symbols defined in
   * the client. This method does not accumulate any state.
   */
  public abstract String createUnusedIdent(String ident);

  /**
   * Returns the Json castableType data for a given type.
   */
  public abstract CastableTypeData getCastableTypeData(Class<?> clazz);

  /**
   * Given a base type and the unobfuscated field name, find the obfuscated name
   * for the field in the client. This will search superclasses as well for the
   * first matching field.
   */
  public abstract String getFieldId(Class<?> clazz, String fieldName);

  /**
   * Return the field name for a given enum value.
   */
  public abstract String getFieldId(Enum<?> value);

  /**
   * This is similar to {@link #getFieldId(Class, String)} but does not search
   * supertypes. It is intended to be used to access "magic" GWT types.
   */
  public abstract String getFieldId(String className, String fieldName);

  /**
   * Return the name of a field from a client-side id. This will search
   * superclasses for the first instance of the named field.
   * 
   * @return The field's declaring class and the name of the field
   */
  public abstract Pair<Class<?>, String> getFieldName(Class<?> clazz,
      String fieldId);

  /**
   * Returns the name of the top-level function which implements the named
   * method that takes the exact arguments specified. This will search in the
   * given class as well as its supertypes.
   */
  public abstract String getMethodId(Class<?> clazz, String methodName,
      Class<?>... args);

  /**
   * This is similar to {@link #getMethodId(Class, String, Class...)} but does
   * not search supertypes. It is intended to be used to access "magic" GWT
   * types.
   */
  public abstract String getMethodId(String className, String methodName,
      String... jsniArgTypes);

  /**
   * Returns the fields of a given class that should be serialized. This method
   * does not crawl supertypes.
   */
  public abstract Field[] getOperableFields(Class<?> clazz);

  /**
   * Returns the assigned castability queryId of a given type.
   */
  public abstract int getQueryId(Class<?> clazz);

  /**
   * Returns the name of the top-level function that is used as the seed
   * function for a given type.
   */
  public abstract String getSeedName(Class<?> clazz);

  /**
   * Returns the deobfuscated name of a type based on the name of the type's
   * seed function.
   */
  public abstract String getTypeName(String seedName);

  /**
   * Indicates whether or not the remote client is running as compiled script.
   * This may be used to optimize the payload based on assumptions that can be
   * mode about web-mode or hosted-mode clients.
   */
  public abstract boolean isScript();
}
