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
package com.google.gwt.sample.simplerpc.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.List;
import java.util.Map;

/**
 * The sample service.
 */
@RemoteServiceRelativePath("simpleRPC")
public interface SimpleRPCService extends RemoteService {
  /**
   * Returns a string from the server associated with the given index.
   * 
   * @param index index of string
   * @return the string associated with the given index
   * @throws SimpleRPCException
   */
  String getString(int index) throws SimpleRPCException;

  /**
   * Given a list of indexes, returns a map of indexes --> string values.
   * 
   * @param indexes indexes to be mapped
   * @return map of indexes --> string values
   */
  Map<Integer, String> getMultipleStrings(List<Integer> indexes)
      throws SimpleRPCException;
}
