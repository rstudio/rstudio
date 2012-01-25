/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.useragent.rebind;

import com.google.gwt.user.rebind.StringSourceWriter;

/**
 * Represents a predicate expressed in Javascript
 * returns the given user agent if predicate evaluates
 * to true.
 */
public class UserAgentPropertyGeneratorPredicate {
  
  private final StringSourceWriter predicateWriter = new StringSourceWriter();
  private String userAgent;
  private String returnValue;
  
  public UserAgentPropertyGeneratorPredicate(String userAgent) {
    assert userAgent != null;
    assert userAgent.length() > 0;
    this.userAgent = userAgent;
  }

  public UserAgentPropertyGeneratorPredicate getPredicateBlock() {
    return this;
  }

  public String getReturnValue() {
    return returnValue;
  }
  
  public String getUserAgent() {
    return userAgent;
  }
  
  public UserAgentPropertyGeneratorPredicate indent() {
    predicateWriter.indent();
    return this;
  }

  public UserAgentPropertyGeneratorPredicate outdent() {
    predicateWriter.outdent();
    return this;
  }
  
  public UserAgentPropertyGeneratorPredicate println(String s) {
    predicateWriter.println(s);
    return this;
  }

  public UserAgentPropertyGeneratorPredicate returns(String s) {
    returnValue = s;
    return this;
  }
  
  @Override
  public String toString() {
    return predicateWriter.toString();
  }
}
