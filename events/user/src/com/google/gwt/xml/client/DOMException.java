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
package com.google.gwt.xml.client;

/**
 * Thrown when DOM exceptions occur. Two subclasses exist:
 * <code>DOMNodeException</code> and <code>DOMParseException</code> which
 * give more detailed information for DOM manipulation errors and parse errors,
 * respectively. All <code>DOMExceptions</code> thrown in this package will be
 * instances of one of those two classes.
 */
public class DOMException extends RuntimeException {

  public static final short INVALID_ACCESS_ERR = 15;
  public static final short INVALID_CHARACTER_ERR = 5;
  public static final short INVALID_MODIFICATION_ERR = 13;
  public static final short INVALID_STATE_ERR = 11;
  public static final short SYNTAX_ERR = 12;

  protected short code;

  public DOMException(short code, String message) {
    super(message);
    this.code = code;
  }

  /**
   * This method gets the code of this <code>DOMException</code>.
   * 
   * @return the code of this <code>DOMException</code>
   */
  public short getCode() {
    return code;
  }

}
