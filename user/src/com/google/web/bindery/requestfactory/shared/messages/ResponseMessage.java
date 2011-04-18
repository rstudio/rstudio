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
import com.google.web.bindery.autobean.shared.Splittable;

import java.util.List;

/**
 * The result of fulfilling a request on the server.
 */
public interface ResponseMessage extends VersionedMessage {
  String GENERAL_FAILURE = "F";
  String INVOCATION_RESULTS = "I";
  String OPERATIONS = "O";
  String STATUS_CODES = "S";
  // V would conflict with versionedMessage
  String VIOLATIONS = "X";

  @PropertyName(GENERAL_FAILURE)
  ServerFailureMessage getGeneralFailure();

  @PropertyName(INVOCATION_RESULTS)
  List<Splittable> getInvocationResults();

  @PropertyName(OPERATIONS)
  List<OperationMessage> getOperations();

  @PropertyName(STATUS_CODES)
  List<Boolean> getStatusCodes();

  @PropertyName(VIOLATIONS)
  List<ViolationMessage> getViolations();

  @PropertyName(GENERAL_FAILURE)
  void setGeneralFailure(ServerFailureMessage failure);

  @PropertyName(INVOCATION_RESULTS)
  void setInvocationResults(List<Splittable> value);

  @PropertyName(OPERATIONS)
  void setOperations(List<OperationMessage> value);

  @PropertyName(STATUS_CODES)
  void setStatusCodes(List<Boolean> value);

  @PropertyName(VIOLATIONS)
  void setViolations(List<ViolationMessage> value);
}
