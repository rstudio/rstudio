/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.typedarrays;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.typedarrays.client.ClientSupportTest;
import com.google.gwt.typedarrays.client.GwtDataViewTest;
import com.google.gwt.typedarrays.client.GwtFloat32ArrayTest;
import com.google.gwt.typedarrays.client.GwtFloat64ArrayTest;
import com.google.gwt.typedarrays.client.GwtInt16ArrayTest;
import com.google.gwt.typedarrays.client.GwtInt32ArrayTest;
import com.google.gwt.typedarrays.client.GwtInt8ArrayTest;
import com.google.gwt.typedarrays.client.GwtUint16ArrayTest;
import com.google.gwt.typedarrays.client.GwtUint32ArrayTest;
import com.google.gwt.typedarrays.client.GwtUint8ArrayTest;
import com.google.gwt.typedarrays.client.GwtUint8ClampedArrayTest;
import com.google.gwt.typedarrays.client.StringArrayBufferTest;

import junit.framework.Test;

/**
 * TypedArray tests for client code running as a GWT test.
 */
public class TypedArraysSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("TypedArray client tests");

    // $JUnit-BEGIN$
    suite.addTestSuite(ClientSupportTest.class);
    suite.addTestSuite(GwtDataViewTest.class);
    suite.addTestSuite(GwtFloat32ArrayTest.class);
    suite.addTestSuite(GwtFloat64ArrayTest.class);
    suite.addTestSuite(GwtInt16ArrayTest.class);
    suite.addTestSuite(GwtInt32ArrayTest.class);
    suite.addTestSuite(GwtInt8ArrayTest.class);
    suite.addTestSuite(GwtUint16ArrayTest.class);
    suite.addTestSuite(GwtUint32ArrayTest.class);
    suite.addTestSuite(GwtUint8ArrayTest.class);
    suite.addTestSuite(GwtUint8ClampedArrayTest.class);
    suite.addTestSuite(StringArrayBufferTest.class);
    // $JUnit-END$

    return suite;
  }
}
