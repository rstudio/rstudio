/*
 * Copyright 2007 Google Inc.
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
import java.util.HashMap;
import java.util.Map;

/**
 * API for loading a {@link SerializationPolicy}.
 */
public final class SerializationPolicyLoader {
  /**
   * Default encoding for serialization policy files.
   */
  public static final String SERIALIZATION_POLICY_FILE_ENCODING = "UTF-8";

  private static final String FORMAT_ERROR_MESSAGE = "Expected: className, [true | false]";

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
   */
  public static SerializationPolicy loadFromStream(InputStream inputStream)
      throws IOException, ParseException, ClassNotFoundException {
    if (inputStream == null) {
      throw new NullPointerException("inputStream");
    }

    Map /* <Class, Boolean> */whitelist = new HashMap();

    InputStreamReader isr = new InputStreamReader(inputStream,
        SERIALIZATION_POLICY_FILE_ENCODING);
    BufferedReader br = new BufferedReader(isr);

    String line = br.readLine();
    int lineNum = 1;
    while (line != null) {
      line = line.trim();
      if (line.length() > 0) {
        String[] components = line.split(",");

        if (components.length != 2) {
          throw new ParseException(FORMAT_ERROR_MESSAGE, lineNum);
        }

        String binaryTypeName = components[0].trim();
        String instantiable = components[1].trim();

        if (binaryTypeName.length() == 0 || instantiable.length() == 0) {
          throw new ParseException(FORMAT_ERROR_MESSAGE, lineNum);
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Class clazz = Class.forName(binaryTypeName, false, contextClassLoader);
        // TODO: Validate the instantiable string better.
        whitelist.put(clazz, Boolean.valueOf(instantiable));
      }

      line = br.readLine();
      lineNum++;
    }

    return new StandardSerializationPolicy(whitelist);
  }

  private SerializationPolicyLoader() {
  }
}
