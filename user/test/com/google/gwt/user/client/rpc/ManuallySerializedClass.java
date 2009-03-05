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
package com.google.gwt.user.client.rpc;

/**
 * This class is defined outside of the CustomFieldSerializerTestSetFactory
 * because of a bug where custom field serializers cannot be inner classes and
 * custom field serializers must be in the same package as the class that they
 * serializer. Once we fix this bug we can move this class into the test set
 * factory.
 */
public class ManuallySerializedClass {
  private int a = 1;

  private int b = 2;

  private int c = 3;

  private String str = "hello";
  
  private StackTraceElement ste = new StackTraceElement("FakeClass",
      "fakeMethod", "FakeClass.java", 1234);

  public int getA() {
    return a;
  }

  public int getB() {
    return b;
  }

  public int getC() {
    return c;
  }

  public String getString() {
    return str;
  }
  
  public StackTraceElement getStackTraceElement() {
    return ste;
  }

  public void setA(int a) {
    this.a = a;
  }

  public void setB(int b) {
    this.b = b;
  }

  public void setC(int c) {
    this.c = c;
  }

  public void setString(String str) {
    this.str = str;
  }
  
  public void setStackTraceElement(StackTraceElement ste) {
    this.ste = ste;
  }
}