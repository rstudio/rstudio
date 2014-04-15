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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.msg.Message0;
import com.google.gwt.dev.util.msg.Message2IntString;
import com.google.gwt.dev.util.msg.Message3IntStringString;
import com.google.gwt.dev.util.msg.Message4IntStringStringString;

/**
 * User messages related to configuration.
 */
class Messages {
  public static final Message2IntString LINKER_NAME_INVALID = new Message2IntString(
      TreeLogger.ERROR, "Line $0: Invalid linker name '$1'");

  public static final Message2IntString NAME_INVALID = new Message2IntString(
      TreeLogger.ERROR, "Line $0: Invalid name '$1'");

  public static final Message2IntString PROPERTY_NAME_INVALID = new Message2IntString(
      TreeLogger.ERROR, "Line $0: Invalid property name '$1'");

  public static final Message2IntString PROPERTY_NOT_FOUND = new Message2IntString(
      TreeLogger.ERROR, "Line $0: Property '$1' not found");

  public static final Message2IntString PROPERTY_VALUE_INVALID = new Message2IntString(
      TreeLogger.ERROR, "Line $0: Invalid property value '$1'");

  public static final Message0 PUBLIC_PATH_LOCATIONS = new Message0(
      TreeLogger.TRACE, "Public resources found in...");

  public static final Message0 SOURCE_PATH_LOCATIONS = new Message0(
      TreeLogger.TRACE, "Translatable source found in...");

  public static final Message2IntString UNABLE_TO_LOAD_CLASS = new Message2IntString(
      TreeLogger.ERROR, "Line $0: Unable to load class '$1'");

  public static final Message3IntStringString PROPERTY_VALUE_NOT_VALID =
      new Message3IntStringString(TreeLogger.ERROR,
          "Line $0: Value '$1' in not a valid value for property '$2'");

  public static final Message3IntStringString UNDEFINED_CONFIGURATION_PROPERTY =
      new Message3IntStringString(TreeLogger.WARN,
          "Line $0: Setting configuration property named '$1' in module '$2' "
              + "that has not been previously defined");

  public static final Message2IntString CONFIGURATION_PROPERTY_REDEFINES_BINDING_PROPERTY =
      new Message2IntString(TreeLogger.ERROR,
          "Line $0: Property '$1' is already defined as a deferred-binding property");

  public static final Message4IntStringStringString CANNOT_REPLACE_PROPERTY =
      new Message4IntStringStringString(TreeLogger.ERROR,
          "Line $0: Property '$1' cannot replace property '$2' of unknown type '$3'");
}
