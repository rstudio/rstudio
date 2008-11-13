/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.server.rpc.impl.StandardSerializationPolicy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API for loading a {@link SerializationPolicy}.
 */
public final class SerializationPolicyLoader {
  /**
   * Default encoding for serialization policy files.
   */
  public static final String SERIALIZATION_POLICY_FILE_ENCODING = "UTF-8";

  private static final String FORMAT_ERROR_MESSAGE = "Expected: className, [true | false], [true | false], [true | false], [true | false]";

  /**
   * Returns the serialization policy file name from the from the serialization
   * policy strong name.
   * 
   * @param serializationPolicyStrongName the serialization policy strong name
   * @return the serialization policy file name from the from the serialization
   *         policy strong name
   */
  public static String getSerializationPolicyFileName(
      String serializationPolicyStrongName) {
    return serializationPolicyStrongName + ".gwt.rpc";
  }

  /**
   * Loads a SerializationPolicy from an input stream.
   * 
   * @param inputStream stream to load from
   * @return a {@link SerializationPolicy} loaded from the input stream
   * 
   * @throws IOException if an error occurs while reading the stream
   * @throws ParseException if the input stream is not properly formatted
   * @throws ClassNotFoundException if a class specified in the serialization
   *           policy cannot be loaded
   * 
   * @deprecated see {@link #loadFromStream(InputStream, List)}
   */
  @Deprecated
  public static SerializationPolicy loadFromStream(InputStream inputStream)
      throws IOException, ParseException, ClassNotFoundException {
    List<ClassNotFoundException> classNotFoundExceptions = new ArrayList<ClassNotFoundException>();
    SerializationPolicy serializationPolicy = loadFromStream(inputStream,
        classNotFoundExceptions);
    if (!classNotFoundExceptions.isEmpty()) {
      // Just report the first failure.
      throw classNotFoundExceptions.get(0);
    }

    return serializationPolicy;
  }

  /**
   * Loads a SerializationPolicy from an input stream and optionally record any
   * {@link ClassNotFoundException}s.
   * 
   * @param inputStream stream to load the SerializationPolicy from.
   * @param classNotFoundExceptions if not <code>null</code>, all of the
   *          {@link ClassNotFoundException}s thrown while loading this
   *          serialization policy will be added to this list
   * @return a {@link SerializationPolicy} loaded from the input stream.
   * 
   * @throws IOException if an error occurs while reading the stream
   * @throws ParseException if the input stream is not properly formatted
   */
  public static SerializationPolicy loadFromStream(InputStream inputStream,
      List<ClassNotFoundException> classNotFoundExceptions) throws IOException,
      ParseException {

    if (inputStream == null) {
      throw new NullPointerException("inputStream");
    }

    Map<Class<?>, Boolean> whitelistSer = new HashMap<Class<?>, Boolean>();
    Map<Class<?>, Boolean> whitelistDeser = new HashMap<Class<?>, Boolean>();

    InputStreamReader isr = new InputStreamReader(inputStream,
        SERIALIZATION_POLICY_FILE_ENCODING);
    BufferedReader br = new BufferedReader(isr);

    String line = br.readLine();
    int lineNum = 1;
    while (line != null) {
      line = line.trim();
      if (line.length() > 0) {
        String[] components = line.split(",");

        if (components.length != 2 && components.length != 5) {
          throw new ParseException(FORMAT_ERROR_MESSAGE, lineNum);
        }

        for (int i = 0; i < components.length; i++) {
          components[i] = components[i].trim();
          if (components[i].length() == 0) {
            throw new ParseException(FORMAT_ERROR_MESSAGE, lineNum);
          }
        }
        String[] fields = new String[components.length];

        String binaryTypeName = components[0].trim();
        boolean fieldSer;
        boolean instantSer;
        boolean fieldDeser;
        boolean instantDeser;
        if (components.length == 2) {
          fieldSer = fieldDeser = true;
          instantSer = instantDeser = Boolean.valueOf(components[1]);
        } else {
          int idx = 1;
          // TODO: Validate the instantiable string better.
          fieldSer = Boolean.valueOf(components[idx++]);
          instantSer = Boolean.valueOf(components[idx++]);
          fieldDeser = Boolean.valueOf(components[idx++]);
          instantDeser = Boolean.valueOf(components[idx++]);

          if (!fieldSer && !fieldDeser) {
            throw new ParseException("Type " + binaryTypeName
                + " is neither field serializable nor field deserializable",
                lineNum);
          }
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
          Class<?> clazz = Class.forName(binaryTypeName, false,
              contextClassLoader);
          if (fieldSer) {
            whitelistSer.put(clazz, instantSer);
          }
          if (fieldDeser) {
            whitelistDeser.put(clazz, instantDeser);
          }
        } catch (ClassNotFoundException ex) {
          // Ignore the error, but add it to the list of errors if one was
          // provided.
          if (classNotFoundExceptions != null) {
            classNotFoundExceptions.add(ex);
          }
        }
      }

      line = br.readLine();
      lineNum++;
    }

    return new StandardSerializationPolicy(whitelistSer, whitelistDeser);
  }

  private SerializationPolicyLoader() {
  }
}
