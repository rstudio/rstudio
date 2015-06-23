/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.core.client.interop;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests JsType with array functionality.
 */
public class JsTypeArrayTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  /* MAKE SURE EACH TYPE IS ONLY USED ONCE PER TEST CASE */

  @JsType
  interface SimpleJsTypeReturnFromNative { }

  public void testJsTypeArray_returnFromNative() {
    SimpleJsTypeReturnFromNative[] array = returnJsTypeFromNative();
    assertEquals(2, array.length);
    assertNotNull(array[0]);
  }

  private native SimpleJsTypeReturnFromNative[] returnJsTypeFromNative() /*-{
    return [{}, {}];
  }-*/;

  @JsType
  interface SimpleJsTypeReturnFromNativeWithACall {
    @JsProperty int getId();
  }

  public void testJsTypeArray_returnFromNativeWithACall() {
    SimpleJsTypeReturnFromNativeWithACall[] array = returnJsTypeWithIdsFromNative();
    assertEquals(2, array[1].getId());
  }

  private native SimpleJsTypeReturnFromNativeWithACall[] returnJsTypeWithIdsFromNative() /*-{
    return [{id:1}, {id:2}];
  }-*/;

  @JsType
  interface SimpleJsTypeAsAField { }

  @JsType
  static class SimpleJsTypeAsAFieldHolder {
    public SimpleJsTypeAsAField[] arrayField;
  }

  // TODO(rluble): Needs fixes in ImlementCastsAndTypeChecks, ArrayNormalizer and maybe type oracle.
  public void __disabled__testJsTypeArray_asAField() {
    SimpleJsTypeAsAFieldHolder holder = new SimpleJsTypeAsAFieldHolder();
    fillArrayField(holder);
    SimpleJsTypeAsAField[] array = holder.arrayField;
    assertEquals(2, array.length);
    assertNotNull(array[0]);
  }

  private native static void fillArrayField(SimpleJsTypeAsAFieldHolder holder) /*-{
    holder.arrayField = [{}, {}];
  }-*/;

  @JsType
  interface SimpleJsTypeAsAParam { }

  @JsType
  static class SimpleJsTypeAsAParamHolder {
    private SimpleJsTypeAsAParam[] theParam;

    public void setArrayParam(SimpleJsTypeAsAParam[] param) {
      theParam = param;
    }
  }

  public void testJsTypeArray_asAParam() {
    SimpleJsTypeAsAParamHolder holder = new SimpleJsTypeAsAParamHolder();
    fillArrayParam(holder);
    SimpleJsTypeAsAParam[] array = holder.theParam;
    assertEquals(2, array.length);
    assertNotNull(array[0]);
  }

  private native void fillArrayParam(SimpleJsTypeAsAParamHolder holder) /*-{
    holder.setArrayParam([{}, {}]);
  }-*/;

  @JsType
  interface SimpleJsTypeReturnForMultiDimArray {
    @JsProperty int getId();
  }

  // TODO(rluble): Needs fixes in ImlementCastsAndTypeChecks, ArrayNormalizer and maybe type oracle.
  public void __disabled__testJsType3DimArray_castedFromNativeWithACall() {
    SimpleJsTypeReturnForMultiDimArray[][][] array =
        (SimpleJsTypeReturnForMultiDimArray[][][]) returnJsType3DimFromNative();
    assertEquals(1, array.length);
    assertEquals(2, array[0].length);
    assertEquals(3, array[0][0].length);
    assertEquals(1, array[0][0][0].getId());
  }

  private native Object returnJsType3DimFromNative() /*-{
    return [ [ [{id:1}, {id:2}, {}], [] ] ];
  }-*/;

  // TODO(rluble): Needs fixes in ImlementCastsAndTypeChecks, ArrayNormalizer and maybe type oracle.
  public void __disabled__testObjectArray_castedFromNative() {
    Object[] array = (Object[]) returnObjectArrayFromNative();
    assertEquals(3, array.length);
    assertEquals("1", array[0]);
  }

  private native Object returnObjectArrayFromNative() /*-{
    return ["1", "2", "3"];
  }-*/;
}
