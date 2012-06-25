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
package com.google.gwt.sample.simplerpc.server;

import com.google.gwt.sample.simplerpc.client.SimpleRPCException;
import com.google.gwt.sample.simplerpc.client.SimpleRPCService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The implementation of the <code>SimpleRPCService</code> code. This code
 * only runs on the server.
 */
public class SimpleRPCServiceImpl extends RemoteServiceServlet implements
    SimpleRPCService {

  /**
   * The server strings used to supply the information to <code>getString</code>.
   */
  private static final String[] SERVER_STRINGS = new String[] {
      "Hello World", "Bonjour monde", "Hola Español"};

  /**
   * Gets a map of strings associated with the given indexes.
   */
  public Map<Integer, String> getMultipleStrings(List<Integer> indexes)
      throws SimpleRPCException {
    Map<Integer, String> accum = new HashMap<Integer, String>();
    for (int i = 0; i < indexes.size(); i++) {
      Integer key = indexes.get(i);
      String value = getString(key.intValue());
      accum.put(key, value);
    }
    return accum;
  }

  /**
   * Gets a string associated with a given index. In a real world application,
   * we would be consulting a database or some other server-side set of
   * information. Here we are just accessing a server side array.
   *
   * @param index index of string
   * @return the string associated with the given index
   * @throws SimpleRPCException
   */
  public String getString(int index) throws SimpleRPCException {
    try {
      return SERVER_STRINGS[index];
    } catch (RuntimeException e) {
      throw new SimpleRPCException(e.getClass().getName() + ":"
          + e.getMessage());
    }
  }
}
