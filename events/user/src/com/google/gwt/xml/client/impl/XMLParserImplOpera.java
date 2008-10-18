/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.xml.client.impl;

/**
 * This class is the Opera implementation of the XMLParser interface.
 * 
 * <p>
 * Opera does not support {@link com.google.gwt.xml.client.CDATASection}. In
 * addition, Opera does not support {@link com.google.gwt.xml.client.Text} nodes
 * of more than 32k character.
 * </p>
 */
class XMLParserImplOpera extends XMLParserImplStandard {

  /**
   * Opera does not support <code>CDATASection</code>.
   */
  @Override
  public boolean supportsCDATASection() {
    return false;
  }

}
