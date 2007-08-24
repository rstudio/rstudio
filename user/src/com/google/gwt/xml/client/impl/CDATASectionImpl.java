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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.CDATASection;

/**
 * This class implements the CDATASectionImpl interface.  
 */

class CDATASectionImpl extends TextImpl implements CDATASection {
  protected CDATASectionImpl(JavaScriptObject o) {
    super(o);
  }

  /**
   * This method returns the string representation of this 
   * <code>CDATASectionImpl</code>.
   * @return the string representation of this <code>CDATASectionImpl</code>.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer b = new StringBuffer("<![CDATA[");
    b.append(getData());
    b.append("]]>");
    return b.toString();
  }
}
