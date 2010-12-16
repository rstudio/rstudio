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
package com.google.gwt.user.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.AsyncProxy.AllowNonVoid;
import com.google.gwt.user.client.AsyncProxy.ConcreteType;
import com.google.gwt.user.client.AsyncProxy.DefaultValue;
import com.google.gwt.user.client.AsyncProxy.ProxyCallback;
import com.google.gwt.user.client.impl.AsyncProxyBase;

/**
 * Tests the AsyncProxy type.
 */
public class AsyncProxyTest extends GWTTestCase {
  interface Test {
    boolean defaultBool();

    byte defaultByte();

    char defaultChar();

    double defaultDouble();

    float defaultFloat();

    int defaultInt();

    long defaultLong();

    Object defaultObject();

    short defaultShort();

    String defaultString();

    void one();

    void three();

    void two();
  }

  static class TestImpl implements Test {
    @AllowNonVoid
    @ConcreteType(TestImpl.class)
    @DefaultValue(intValue = 42, longValue = 42)
    interface Proxy extends AsyncProxy<Test>, Test {
    }

    private int value = 0;

    public boolean defaultBool() {
      return true;
    }

    public byte defaultByte() {
      return 1;
    }

    public char defaultChar() {
      return 1;
    }

    public double defaultDouble() {
      return 1;
    }

    public float defaultFloat() {
      return 1;
    }

    public int defaultInt() {
      return 1;
    }

    public long defaultLong() {
      return 1;
    }

    public Object defaultObject() {
      return this;
    }

    public short defaultShort() {
      return 1;
    }

    public String defaultString() {
      return "";
    }

    public void one() {
      GWTTestCase.assertEquals(0, value);
      value = 1;
    }

    public void three() {
      GWTTestCase.assertEquals(2, value);
      testInstance.finishTest();
    }

    public void two() {
      GWTTestCase.assertEquals(1, value);
      value = 2;
    }
  }

  @AllowNonVoid
  @ConcreteType(AsyncProxyTestTopLevelImpl.class)
  @DefaultValue(intValue = 42, longValue = 42)
  interface TopLevelProxy extends AsyncProxy<Test>, Test {
  }

  private static final int TEST_FINISH_DELAY_MILLIS = 10000;

  private static AsyncProxyTest testInstance;

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testProxy() {
    // Disable in Production Mode for now
    // TODO Make sure runAsync and JUnit play nicely together
    if (GWT.isScript()) {
      return;
    }

    testInstance = this;
    final Test proxy = GWT.create(TestImpl.Proxy.class);
    assertTrue(proxy instanceof AsyncProxy);

    final TestImpl.Proxy asHidden = (TestImpl.Proxy) proxy;
    assertNull(asHidden.getProxiedInstance());

    asHidden.setProxyCallback(new ProxyCallback<Test>() {

      @Override
      public void onComplete(Test instance) {
        // Check that the proxy is returning the correct values now
        assertEquals(true, proxy.defaultBool());
        assertEquals(1, proxy.defaultByte());
        assertEquals(1, proxy.defaultChar());
        assertEquals(1D, proxy.defaultDouble());
        assertEquals(1F, proxy.defaultFloat());
        assertEquals(1, proxy.defaultInt());
        assertEquals(1L, proxy.defaultLong());
        assertEquals(1, proxy.defaultShort());
        assertEquals("", proxy.defaultString());
        assertSame(instance, proxy.defaultObject());

        instance.three();
      }

      @Override
      public void onFailure(Throwable t) {
        t.printStackTrace();
        fail(t.getMessage());
      }

      @Override
      public void onInit(Test instance) {
        assertTrue(instance instanceof TestImpl);
        assertSame(asHidden.getProxiedInstance(), instance);
        instance.one();
      }
    });

    // Cast to AsyncProxyBase to fiddle with internals
    AsyncProxyBase<?> asBase = (AsyncProxyBase<?>) proxy;
    asBase.suppressLoadForTest0();
    assertEquals(false, proxy.defaultBool());
    assertEquals(0, proxy.defaultByte());
    assertEquals(0, proxy.defaultChar());
    assertEquals(0D, proxy.defaultDouble());
    assertEquals(0F, proxy.defaultFloat());
    assertEquals(42, proxy.defaultInt());
    assertEquals(42L, proxy.defaultLong());
    assertEquals(0, proxy.defaultShort());
    assertNull(proxy.defaultString());
    assertNull(proxy.defaultObject());
    asBase.enableLoadForTest0();

    delayTestFinish(TEST_FINISH_DELAY_MILLIS);
    proxy.two();
  }

  public void testProxyOfTopLevel() {
    Test proxy = GWT.create(TopLevelProxy.class);
    assertTrue(proxy instanceof AsyncProxy);
  }
}
