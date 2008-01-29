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
package com.google.gwt.xml.client;

/**
 * This interface documents the ProcessingInstruction node type. For example,
 * <pre>&lt;?xml-stylesheet href="mystyle.css" type="text/css"?&gt;</pre>
 */
public interface ProcessingInstruction extends Node {
  /**
   * This method retrieves the data.
   * 
   * @return the data of this <code>ProcessingInstruction</code>
   */
  String getData();

  /**
   * This method retrieves the target.
   * 
   * @return the target of this <code>ProcessingInstruction</code>
   */
  String getTarget();

  /**
   * This method sets the data to <code>data</code>.
   * 
   * @param data the new data
   */
  void setData(String data);
}