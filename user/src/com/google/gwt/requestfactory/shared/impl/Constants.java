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
package com.google.gwt.requestfactory.shared.impl;

/**
 * Contains a variety of contstants shared between client and server code.
 */
public interface Constants {

  /**
   * Property on a proxy JSO that holds its encoded server side data store id.
   */
  String ENCODED_ID_PROPERTY = "!id";
  /**
   * Property on a proxy JSO that holds its server side version data.
   */
  String ENCODED_VERSION_PROPERTY = "!version";
  /**
   * Id property that server entity objects are required to define.
   */
  String ENTITY_ID_PROPERTY = "id";
  /**
   * Version property that server entity objects are required to define.
   */
  Property<Integer> ENTITY_VERSION_PROPERTY = new Property<Integer>("version",
      Integer.class);
}
