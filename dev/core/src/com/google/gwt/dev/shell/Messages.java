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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.msg.Message0;
import com.google.gwt.dev.util.msg.Message1String;
import com.google.gwt.dev.util.msg.Message1StringArray;
import com.google.gwt.dev.util.msg.Message1ToString;

/**
 * End-user messages related to the shell.
 */
public final class Messages {

  public static final Message1ToString TRACE_CHECKING_RULE = new Message1ToString(
      TreeLogger.DEBUG, "Checking rule $0");

  public static final Message0 TRACE_CONDITION_DID_NOT_MATCH = new Message0(
      TreeLogger.DEBUG, "Condition was not satisfied");

  public static final Message0 TRACE_CONDITION_MATCHED = new Message0(
      TreeLogger.DEBUG, "Condition was satisfied");

  public static final Message0 TRACE_RULE_DID_NOT_MATCH = new Message0(
      TreeLogger.DEBUG, "Rule did not match");

  public static final Message0 TRACE_RULE_MATCHED = new Message0(
      TreeLogger.DEBUG, "Rule was a match and will be used");

  public static final Message1String TRACE_TOPLEVEL_REBIND = new Message1String(
      TreeLogger.DEBUG, "Rebinding $0");

  public static final Message1String TRACE_TOPLEVEL_REBIND_RESULT = new Message1String(
      TreeLogger.DEBUG, "Rebind result was $0");

  public static final Message1StringArray UNABLE_TO_REBIND_DUE_TO_CYCLE_IN_RULES = new Message1StringArray(
      TreeLogger.WARN,
      "The deferred binding request failed due to a cycle in the applicable rules: $0");

  // no instances
  private Messages() {
  }
}
