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
package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Test transfer of value types over RPC.
 */
public class ValueTypesTest extends RpcTestBase {

  private ValueTypesTestServiceAsync primitiveTypeTestService;

  public void testBigDecimal_base() {
    assertEcho(new BigDecimal("42"));
  }

  public void testBigDecimal_exponential() {
    assertEcho(new BigDecimal("0.00000000000000000001"));
  }

  public void testBigDecimal_negative() {
    assertEcho(new BigDecimal("-42"));
  }

  public void testBigDecimal_zero() {
    assertEcho(new BigDecimal("0.0"));
  }

  public void testBigInteger_base() {
    assertEcho(new BigInteger("42"));
  }

  public void testBigInteger_exponential() {
    assertEcho(new BigInteger("100000000000000000000"));
  }

  public void testBigInteger_negative() {
    assertEcho(new BigInteger("-42"));
  }

  public void testBigInteger_zero() {
    assertEcho(new BigInteger("0"));
  }

  public void testBoolean_FALSE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_FALSE(false, new AsyncCallback<Boolean>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Boolean result) {
        assertNotNull("Was null", result);
        assertFalse("Should have been false", result.booleanValue());
        finishTest();
      }
    });
  }

  public void testBoolean_TRUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_TRUE(true, new AsyncCallback<Boolean>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Boolean result) {
        assertNotNull(result);
        assertTrue(result.booleanValue());
        finishTest();
      }
    });
  }

  public void testByte() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo((byte) (Byte.MAX_VALUE / (byte) 2), new AsyncCallback<Byte>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Byte result) {
        assertNotNull(result);
        assertEquals(Byte.MAX_VALUE / 2, result.byteValue());
        finishTest();
      }
    });
  }

  public void testByte_MAX_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MAX_VALUE(Byte.MAX_VALUE, new AsyncCallback<Byte>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Byte result) {
        assertNotNull(result);
        assertEquals(Byte.MAX_VALUE, result.byteValue());
        finishTest();
      }
    });
  }

  public void testByte_MIN_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MIN_VALUE(Byte.MIN_VALUE, new AsyncCallback<Byte>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Byte result) {
        assertNotNull(result);
        assertEquals(Byte.MIN_VALUE, result.byteValue());
        finishTest();
      }
    });
  }

  public void testChar() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    final char value = (char) (Character.MAX_VALUE / (char) 2);
    service.echo(value, new AsyncCallback<Character>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Character result) {
        assertNotNull(result);
        assertEquals(value, result.charValue());
        finishTest();
      }
    });
  }

  public void testChar_MAX_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MAX_VALUE(Character.MAX_VALUE, new AsyncCallback<Character>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Character result) {
        assertNotNull(result);
        assertEquals(Character.MAX_VALUE, result.charValue());
        finishTest();
      }
    });
  }

  public void testChar_MIN_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MIN_VALUE(Character.MIN_VALUE, new AsyncCallback<Character>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Character result) {
        assertNotNull(result);
        assertEquals(Character.MIN_VALUE, result.charValue());
        finishTest();
      }
    });
  }

  public void testDouble() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(Double.MAX_VALUE / 2, new AsyncCallback<Double>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Double result) {
        assertNotNull(result);
        assertEquals(Double.MAX_VALUE / 2, result.doubleValue(), 0.0);
        finishTest();
      }
    });
  }

  public void testDouble_MAX_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MAX_VALUE(Double.MAX_VALUE, new AsyncCallback<Double>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Double result) {
        assertNotNull(result);
        assertEquals(Double.MAX_VALUE, result.doubleValue(), 0.0);
        finishTest();
      }
    });
  }

  public void testDouble_MIN_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MIN_VALUE(Double.MIN_VALUE, new AsyncCallback<Double>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Double result) {
        assertNotNull(result);
        assertEquals(Double.MIN_VALUE, result.doubleValue(), 0.0);
        finishTest();
      }
    });
  }

  /**
   * Validate that NaNs (not-a-number, such as 0/0) propagate properly via RPC.
   */
  public void testDouble_NaN() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(Double.NaN, new AsyncCallback<Double>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Double result) {
        assertNotNull(result);
        assertTrue(Double.isNaN(result.doubleValue()));
        finishTest();
      }
    });
  }

  /**
   * Validate that negative infinity propagates properly via RPC.
   */
  public void testDouble_NegInfinity() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(Double.NEGATIVE_INFINITY, new AsyncCallback<Double>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Double result) {
        assertNotNull(result);
        double doubleValue = result.doubleValue();
        assertTrue(Double.isInfinite(doubleValue) && doubleValue < 0);
        finishTest();
      }
    });
  }

  /**
   * Validate that positive infinity propagates properly via RPC.
   */
  public void testDouble_PosInfinity() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(Double.POSITIVE_INFINITY, new AsyncCallback<Double>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Double result) {
        assertNotNull(result);
        double doubleValue = result.doubleValue();
        assertTrue(Double.isInfinite(doubleValue) && doubleValue > 0);
        finishTest();
      }
    });
  }

  public void testFloat() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(Float.MAX_VALUE / 2, new AsyncCallback<Float>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Float result) {
        assertNotNull(result);
        assertEquals(Float.MAX_VALUE / 2, result.floatValue(), 0.0);
        finishTest();
      }
    });
  }

  public void testFloat_MAX_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MAX_VALUE(Float.MAX_VALUE, new AsyncCallback<Float>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Float result) {
        assertNotNull(result);
        assertEquals(Float.MAX_VALUE, result.floatValue(), 0.0);
        finishTest();
      }
    });
  }

  public void testFloat_MIN_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MIN_VALUE(Float.MIN_VALUE, new AsyncCallback<Float>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Float result) {
        assertNotNull(result);
        assertEquals(Float.MIN_VALUE, result.floatValue(), 0.0);
        finishTest();
      }
    });
  }

  /**
   * Validate that NaNs (not-a-number, such as 0/0) propagate properly via RPC.
   */
  public void testFloat_NaN() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(Float.NaN, new AsyncCallback<Float>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Float result) {
        assertNotNull(result);
        assertTrue(Float.isNaN(result.floatValue()));
        finishTest();
      }
    });
  }

  /**
   * Validate that negative infinity propagates properly via RPC.
   */
  public void testFloat_NegInfinity() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(Float.NEGATIVE_INFINITY, new AsyncCallback<Float>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Float result) {
        assertNotNull(result);
        float floatValue = result.floatValue();
        assertTrue(Float.isInfinite(floatValue) && floatValue < 0);
        finishTest();
      }
    });
  }

  /**
   * Validate that positive infinity propagates properly via RPC.
   */
  public void testFloat_PosInfinity() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(Float.POSITIVE_INFINITY, new AsyncCallback<Float>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Float result) {
        assertNotNull(result);
        float floatValue = result.floatValue();
        assertTrue(Float.isInfinite(floatValue) && floatValue > 0);
        finishTest();
      }
    });
  }

  public void testInteger() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(Integer.MAX_VALUE / 2, new AsyncCallback<Integer>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Integer result) {
        assertNotNull(result);
        assertEquals(Integer.MAX_VALUE / 2, result.intValue());
        finishTest();
      }
    });
  }

  public void testInteger_MAX_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MAX_VALUE(Integer.MAX_VALUE, new AsyncCallback<Integer>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Integer result) {
        assertNotNull(result);
        assertEquals(Integer.MAX_VALUE, result.intValue());
        finishTest();
      }
    });
  }

  public void testInteger_MIN_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MIN_VALUE(Integer.MIN_VALUE, new AsyncCallback<Integer>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Integer result) {
        assertNotNull(result);
        assertEquals(Integer.MIN_VALUE, result.intValue());
        finishTest();
      }
    });
  }

  public void testLong() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(Long.MAX_VALUE / 2, new AsyncCallback<Long>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Long result) {
        assertNotNull(result);
        long expected = Long.MAX_VALUE / 2;
        assertEquals(expected, result.longValue());
        finishTest();
      }
    });
  }

  public void testLong_MAX_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MAX_VALUE(Long.MAX_VALUE, new AsyncCallback<Long>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Long result) {
        assertNotNull(result);
        assertEquals(Long.MAX_VALUE, result.longValue());
        finishTest();
      }
    });
  }

  public void testLong_MIN_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MIN_VALUE(Long.MIN_VALUE, new AsyncCallback<Long>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Long result) {
        assertNotNull(result);
        assertEquals(Long.MIN_VALUE, result.longValue());
        finishTest();
      }
    });
  }

  public void testShort() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    final short value = (short) (Short.MAX_VALUE / 2);
    service.echo(value , new AsyncCallback<Short>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Short result) {
        assertNotNull(result);
        assertEquals(value, result.shortValue());
        finishTest();
      }
    });
  }

  public void testShort_MAX_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MAX_VALUE(Short.MAX_VALUE, new AsyncCallback<Short>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Short result) {
        assertNotNull(result);
        assertEquals(Short.MAX_VALUE, result.shortValue());
        finishTest();
      }
    });
  }

  public void testShort_MIN_VALUE() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo_MIN_VALUE(Short.MIN_VALUE, new AsyncCallback<Short>() {

      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Short result) {
        assertNotNull(result);
        assertEquals(Short.MIN_VALUE, result.shortValue());
        finishTest();
      }
    });
  }

  public void testVoidParameterizedType() {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(new SerializableGenericWrapperType<Void>(),
        new AsyncCallback<SerializableGenericWrapperType<Void>>() {

          public void onFailure(Throwable caught) {
            TestSetValidator.rethrowException(caught);
          }

          public void onSuccess(SerializableGenericWrapperType<Void> result) {
            assertNotNull(result);
            assertNull(result.getValue());
            finishTest();
          }
        });
  }

  public void testString() {
    assertEcho("test");
  }

  public void testString_empty() {
    assertEcho("");
  }

  public void testString_over64KB() {
    // Test a string over 64KB of a-z characters repeated.
    String testString = "";
    int totalChars = 0xFFFF + 0xFF;
    for (int i = 0; i < totalChars; i++) {
      testString += (char) ('a' + (i % 26));
    }
    assertEcho(testString);
  }

  public void testString_over64KBWithUnicode() {
    // Test a string over64KB string that requires unicode escaping.
    String testString = "";
    int totalChars = 0xFFFF + 0xFF;
    for (int i = 0; i < totalChars; i += 2) {
      testString += '\u2011';
      testString += (char) 0x08;
    }
    assertEcho(testString);
  }

  private void assertEcho(final BigDecimal value) {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(value, new AsyncCallback<BigDecimal>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(BigDecimal result) {
        assertEquals(value, result);
        finishTest();
      }
    });
  }

  private void assertEcho(final BigInteger value) {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(value, new AsyncCallback<BigInteger>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(BigInteger result) {
        assertEquals(value, result);
        finishTest();
      }
    });
  }

  private void assertEcho(final String value) {
    ValueTypesTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
    service.echo(value, new AsyncCallback<String>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(String result) {
        assertEquals(value, result);
        finishTest();
      }
    });
  }

  private ValueTypesTestServiceAsync getServiceAsync() {
    if (primitiveTypeTestService == null) {
      primitiveTypeTestService = (ValueTypesTestServiceAsync) GWT.create(ValueTypesTestService.class);
      ((ServiceDefTarget) primitiveTypeTestService).setServiceEntryPoint(GWT.getModuleBaseURL()
          + "valuetypes");
    }
    return primitiveTypeTestService;
  }
}
