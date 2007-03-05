/*
 * Copyright 2006 Google Inc.
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

import java.util.List;
import java.util.Map;

/**
 * The sample service.
 */
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
   * Given a list of indexes, returns a map of indexes --> string values. Uses
   * gwt.typeArgs to specify the type of both the requested list and returned
   * map.
   * <p>
   * Note that if the gwt.typeArgs annotation were not included the RPC system
   * may not be able to determine the types being returned and therefore runtime
   * errors may result.
   * </p>
   * 
   * @gwt.typeArgs indexes <java.lang.Integer>
   * @gwt.typeArgs <java.lang.String>
   * 
   * @param indexes indexes to be mapped
   * @return map of indexes --> string values
   */
  Map getMultipleStrings(List indexes) throws SimpleRPCException;
}
