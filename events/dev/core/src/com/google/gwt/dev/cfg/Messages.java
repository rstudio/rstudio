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
import com.google.gwt.dev.util.msg.Message1String;
import com.google.gwt.dev.util.msg.Message2ClassClass;

/**
 * User messages related to configuration.
 */
class Messages {

  public static final Message2ClassClass INVALID_CLASS_DERIVATION = new Message2ClassClass(
      TreeLogger.ERROR, "Class '$0' must derive from '$1'");

  public static final Message1String LINKER_NAME_INVALID = new Message1String(
      TreeLogger.ERROR, "Invalid linker name '$0'");

  public static final Message1String NAME_INVALID = new Message1String(
      TreeLogger.ERROR, "Invalid name '$0'");

  public static final Message1String PROPERTY_NAME_INVALID = new Message1String(
      TreeLogger.ERROR, "Invalid property name '$0'");

  public static final Message1String PROPERTY_NOT_FOUND = new Message1String(
      TreeLogger.ERROR, "Property '$0' not found");

  public static final Message1String PROPERTY_VALUE_INVALID = new Message1String(
      TreeLogger.ERROR, "Invalid property value '$0'");

  public static final Message0 PUBLIC_PATH_LOCATIONS = new Message0(
      TreeLogger.TRACE, "Public resources found in...");

  public static final Message0 SOURCE_PATH_LOCATIONS = new Message0(
      TreeLogger.TRACE, "Translatable source found in...");

  public static final Message1String UNABLE_TO_CREATE_OBJECT = new Message1String(
      TreeLogger.ERROR, "Unable to create an instance of '$0'");

  public static final Message1String UNABLE_TO_LOAD_CLASS = new Message1String(
      TreeLogger.ERROR, "Unable to load class '$0'");
}
