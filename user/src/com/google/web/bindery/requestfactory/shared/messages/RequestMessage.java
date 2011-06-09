/*
 * Copyright 2010 Google Inc.
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

import java.util.List;

/**
 * The message sent from the client to the server.
 */
public interface RequestMessage extends VersionedMessage {
  String FACTORY = "F";
  String INVOCATION = "I";
  String OPERATIONS = "O";

  @PropertyName(INVOCATION)
  List<InvocationMessage> getInvocations();

  @PropertyName(OPERATIONS)
  List<OperationMessage> getOperations();

  @PropertyName(FACTORY)
  String getRequestFactory();

  @PropertyName(INVOCATION)
  void setInvocations(List<InvocationMessage> value);

  @PropertyName(OPERATIONS)
  void setOperations(List<OperationMessage> value);

  @PropertyName(FACTORY)
  void setRequestFactory(String value);
}
