/*
 * Copyright 2011 Google Inc.
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
package com.google.web.bindery.requestfactory.shared.messages;

import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;
import com.google.web.bindery.autobean.shared.Splittable;

import java.util.Map;

/**
 * A JSON-RPC request payload.
 */
public interface JsonRpcRequest {
  String getApiVersion();

  int getId();

  String getMethod();

  Map<String, Splittable> getParams();

  @PropertyName("jsonrpc")
  String getVersion();

  void setApiVersion(String version);

  void setId(int id);

  void setMethod(String method);

  void setParams(Map<String, Splittable> params);

  @PropertyName("jsonrpc")
  void setVersion(String version);
}
