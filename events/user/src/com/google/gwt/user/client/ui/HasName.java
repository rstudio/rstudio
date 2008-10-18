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
package com.google.gwt.user.client.ui;

/**
 * A widget that implements this interface has a 'name' associated with it,
 * allowing it to be used with {@link FormPanel}. This property is the name
 * that will be associated with the widget when its form is submitted.
 */
public interface HasName {

  /**
   * Sets the widget's name.
   * 
   * @param name the widget's new name
   */
  void setName(String name);

  /**
   * Gets the widget's name.
   * 
   * @return the widget's name
   */
  String getName();
}
