/*
 * Copyright 2010 Google Inc.
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
package com.google.web.bindery.requestfactory.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.ExtraTypes;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.Service;
import com.google.web.bindery.requestfactory.shared.SimpleFooProxy;
import com.google.web.bindery.requestfactory.shared.TestRequestFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Tests behavior of RequestFactory when using polymorphic Request return types
 * and other non-trivial type hierarchies.
 */
public class RequestFactoryPolymorphicTest extends GWTTestCase {

  /**
   * Mandatory javadoc.
   */
  public static class A {
    private static int idCount = 0;

    public static A findA(int id) {
      A toReturn = new A();
      toReturn.id = id;
      return toReturn;
    }

    private String a = "a";

    protected int id = idCount++;

    private A nextA;

    public String getA() {
      return a;
    }

    public int getId() {
      return id;
    }

    public A getNextA() {
      if (nextA == null) {
        nextA = new A();
      }
      return nextA;
    }

    public int getVersion() {
      return 0;
    }

    public void setA(String value) {
      a = value;
    }
  }

  /**
   * Mandatory javadoc.
   */
  @ProxyFor(A.class)
  public interface AProxy extends EntityProxy, HasA {
    AProxy getNextA();

    // Mix in getA() from HasA
    void setA(String value);
  }

  /**
   * Mandatory javadoc.
   */
  public static class ASub extends A {
  }

  /**
   * Mandatory javadoc.
   */
  public static class B extends A {
    public static B findB(int id) {
      B toReturn = new B();
      toReturn.id = id;
      return toReturn;
    }

    private String b = "b";

    public String getB() {
      return b;
    }

    public void setB(String value) {
      b = value;
    }
  }

  /**
   * Mandatory javadoc.
   */
  @ProxyFor(B.class)
  public interface B1Proxy extends AProxy {
    String getB();

    void setB(String value);
  }

  /**
   * Mandatory javadoc.
   */
  @ProxyFor(B.class)
  public interface B2Proxy extends AProxy {
    String getB();

    void setB(String value);
  }

  /**
   * Mandatory javadoc.
   */
  public static class BSub extends B {
  }

  /**
   * Mandatory javadoc.
   */
  public static class C extends B {
    public static C findC(int id) {
      C toReturn = new C();
      toReturn.id = id;
      return toReturn;
    }

    private String c = "c";

    private C nextC;

    public String getC() {
      return c;
    }

    public C getNextC() {
      if (nextC == null) {
        nextC = new C();
      }

      return nextC;
    }

    public void setC(String value) {
      c = value;
    }
  }
  /**
   * Mandatory javadoc.
   */
  @ProxyFor(C.class)
  public interface C1Proxy extends B1Proxy {
    String getC();

    C1Proxy getNextC();

    void setC(String value);
  }

  /**
   * Mandatory javadoc.
   */
  @ProxyFor(C.class)
  public interface C2Proxy extends B2Proxy {
    String getC();

    void setC(String value);
  }

  /**
   * Mandatory javadoc.
   */
  public static class CSub extends C {
  }

  /**
   * Mandatory javadoc.
   */
  public static class D extends A {
    public static D findD(int id) {
      D toReturn = new D();
      toReturn.id = id;
      return toReturn;
    }

    private String d = "d";

    public String getD() {
      return d;
    }

    public void setD(String value) {
      d = value;
    }
  }

  /**
   * Mandatory javadoc.
   */
  @ExtraTypes(D2Proxy.class)
  @ProxyFor(D.class)
  public interface D1Proxy extends AProxy {
    String getD();

    void setD(String value);
  }

  /**
   * Mandatory javadoc.
   */
  @ProxyFor(D.class)
  public interface D2Proxy extends EntityProxy {
    String getD();

    void setD(String value);
  }

  /**
   * This class should not be referenced except as a superclass of
   * {@link MoreDerivedProxy}.
   */
  @ProxyFor(D.class)
  public interface D3Proxy extends EntityProxy {
    String getD();

    void setD(String value);
  }

  /**
   * Mandatory javadoc.
   */
  public static class DSub extends D {
  }

  /**
   * Mandatory javadoc.
   */
  public interface HasA {
    String getA();
  }

  /**
   * Mandatory javadoc.
   */
  public static class Impl {
    public static A AasA() {
      return new A();
    }

    public static A BasA() {
      return new B();
    }

    public static B BasB() {
      return new B();
    }

    public static A CasA() {
      return new C();
    }

    public static B CasB() {
      return new C();
    }

    public static String checkA(Object obj) {
      return A.class.equals(obj.getClass()) && "A".equals(((A) obj).getA()) ? "" : "checkA";
    }

    public static String checkB(Object obj) {
      return B.class.equals(obj.getClass()) && "B".equals(((B) obj).getB()) ? "" : "checkB";
    }

    public static String checkC(Object obj) {
      return C.class.equals(obj.getClass()) && "C".equals(((C) obj).getC()) ? "" : "checkC";
    }

    public static String checkD(Object obj) {
      return D.class.equals(obj.getClass()) && "D".equals(((D) obj).getD()) ? "" : "checkD";
    }

    public static String checkList(List<Object> list) {
      if (list.size() != 4) {
        return "size";
      }
      String temp;
      temp = checkA(list.get(0));
      if (!temp.isEmpty()) {
        return temp;
      }
      temp = checkB(list.get(1));
      if (!temp.isEmpty()) {
        return temp;
      }
      temp = checkC(list.get(2));
      if (!temp.isEmpty()) {
        return temp;
      }
      temp = checkD(list.get(3));
      if (!temp.isEmpty()) {
        return temp;
      }
      return "";
    }

    public static String checkW(Object obj) {
      return W.class.equals(obj.getClass()) && "W".equals(((W) obj).getW()) ? "" : "checkW";
    }

    public static String checkZ(Object obj) {
      return Z.class.equals(obj.getClass()) && "Z".equals(((Z) obj).getZ()) ? "" : "checkZ";
    }

    public static A DasA() {
      return new D();
    }

    public static D DasD() {
      return new D();
    }

    public static List<A> testCollection() {
      return Arrays.asList(new A(), new B(), new C(), new D());
    }

    public static List<A> testCollectionSub() {
      return Arrays.asList(new ASub(), new BSub(), new CSub(), new DSub());
    }

    public static W W() {
      return new W();
    }

    public static W W2() {
      return new W();
    }

    public static Z Z() {
      return new Z();
    }

    public static Z Z2() {
      return new Z();
    }
  }

  /**
   * Check diamond inheritance.
   */
  @ProxyFor(D.class)
  public interface MoreDerivedProxy extends AProxy, D1Proxy, D2Proxy, D3Proxy {
  }

  /**
   * The W and Z types are used with the WZProxy and ZWProxy to demonstrate
   * proxy interface inheritance even when their proxy-for types aren't related.
   */
  public static class W {
    private static int idCount;

    public static W findW(int id) {
      W toReturn = new W();
      toReturn.id = id;
      return toReturn;
    }

    private int id = idCount++;

    private String w = "w";

    private String z = "z";

    public int getId() {
      return id;
    }

    public int getVersion() {
      return 0;
    }

    public String getW() {
      return w;
    }

    public String getZ() {
      return z;
    }

    public void setW(String w) {
      this.w = w;
    }

    public void setZ(String z) {
      this.z = z;
    }
  }

  /**
   * Mandatory javadoc.
   */
  @ProxyFor(W.class)
  public interface WProxy extends EntityProxy {
    String getW();

    void setW(String w);
  }
  /**
   * Mandatory javadoc.
   */
  @ProxyFor(W.class)
  public interface WZProxy extends ZProxy {
    String getW();

    void setW(String w);
  }
  /**
   * @see W
   */
  public static class Z extends A {
    private static int idCount;

    public static Z findZ(int id) {
      Z toReturn = new Z();
      toReturn.id = id;
      return toReturn;
    }

    private int id = idCount++;

    private String w = "w";

    private String z = "z";

    public int getId() {
      return id;
    }

    public int getVersion() {
      return 0;
    }

    public String getW() {
      return w;
    }

    public String getZ() {
      return z;
    }

    public void setW(String w) {
      this.w = w;
    }

    public void setZ(String z) {
      this.z = z;
    }
  }
  /**
   * Mandatory javadoc.
   */
  @ProxyFor(Z.class)
  public interface ZProxy extends EntityProxy {
    String getZ();

    void setZ(String z);
  }

  /**
   * Mandatory javadoc.
   */
  @ProxyFor(Z.class)
  public interface ZWProxy extends WProxy {
    String getZ();

    void setZ(String z);
  }

  /**
   * Mandatory javadoc.
   */
  @ExtraTypes(B2Proxy.class)
  protected interface Factory extends RequestFactory {
    Context ctx();
  }

  /**
   * Verifies that the received proxy's proxy type is exactly {@code clazz} and
   * checks the values of the properties.
   */
  static class CastAndCheckReceiver extends Receiver<EntityProxy> {
    public static CastAndCheckReceiver of(Class<?> clazz) {
      return new CastAndCheckReceiver(clazz);
    }

    private final Class<?> clazz;

    public CastAndCheckReceiver(Class<?> clazz) {
      this.clazz = clazz;
    }

    @Override
    public void onSuccess(EntityProxy response) {
      assertNotNull(response);
      assertEquals(clazz, response.stableId().getProxyClass());
      if (response instanceof HasA) {
        assertEquals("a", ((HasA) response).getA());
      }
      if (response instanceof B1Proxy) {
        assertEquals("b", ((B1Proxy) response).getB());
      }
      if (response instanceof B2Proxy) {
        assertEquals("b", ((B2Proxy) response).getB());
      }
      if (response instanceof C1Proxy) {
        assertEquals("c", ((C1Proxy) response).getC());
      }
      if (response instanceof C2Proxy) {
        assertEquals("c", ((C2Proxy) response).getC());
      }
      if (response instanceof D1Proxy) {
        assertEquals("d", ((D1Proxy) response).getD());
      }
      if (response instanceof D2Proxy) {
        assertEquals("d", ((D2Proxy) response).getD());
      }
    }
  }

  @ExtraTypes({C1Proxy.class, C2Proxy.class, MoreDerivedProxy.class})
  @Service(Impl.class)
  interface Context extends RequestContext {
    Request<AProxy> AasA();

    Request<AProxy> BasA();

    Request<B1Proxy> BasB();

    Request<AProxy> CasA();

    Request<B1Proxy> CasB();

    Request<String> checkA(EntityProxy proxy);

    Request<String> checkB(EntityProxy proxy);

    Request<String> checkC(EntityProxy proxy);

    Request<String> checkD(EntityProxy proxy);

    Request<String> checkList(List<EntityProxy> list);

    Request<String> checkW(EntityProxy proxy);

    Request<String> checkZ(EntityProxy proxy);

    Request<AProxy> DasA();

    Request<D1Proxy> DasD();

    Request<List<AProxy>> testCollection();

    Request<List<AProxy>> testCollectionSub();

    Request<WProxy> W();

    Request<WZProxy> W2();

    Request<ZProxy> Z();

    Request<ZWProxy> Z2();
  }

  /**
   * Checks that the incoming list is {@code [ A, B, C, D ]}.
   */
  static class ListChecker extends Receiver<List<AProxy>> {
    @Override
    public void onSuccess(List<AProxy> response) {
      new CastAndCheckReceiver(AProxy.class).onSuccess(response.get(0));
      new CastAndCheckReceiver(B1Proxy.class).onSuccess(response.get(1));
      new CastAndCheckReceiver(C1Proxy.class).onSuccess(response.get(2));
      new CastAndCheckReceiver(MoreDerivedProxy.class).onSuccess(response.get(3));
    }
  }

  private final Receiver<String> checkReceiver = new Receiver<String>() {

    @Override
    public void onSuccess(String response) {
      assertEquals("", response);
    }
  };

  private static final int TEST_DELAY = 5000;

  protected Factory factory;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  public void testChain() {
    delayTestFinish(TEST_DELAY);
    Context ctx = factory.ctx();
    ctx.CasA().with("nextA.nextA").fire(new Receiver<AProxy>() {
      @Override
      public void onSuccess(AProxy response) {
        new CastAndCheckReceiver(AProxy.class).onSuccess(response.getNextA().getNextA());
        assertNull(response.getNextA().getNextA().getNextA());
        assertNull(((C1Proxy) response).getNextC());
        finishTest();
      }
    });
  }

  public void testChainWithExtras() {
    delayTestFinish(TEST_DELAY);
    Context ctx = factory.ctx();
    ctx.CasA().with("nextC.nextC").fire(new Receiver<AProxy>() {
      @Override
      public void onSuccess(AProxy response) {
        assertNull(response.getNextA());
        C1Proxy cast = (C1Proxy) response;
        new CastAndCheckReceiver(C1Proxy.class).onSuccess(cast.getNextC().getNextC());
        assertNull(cast.getNextC().getNextC().getNextC());
        finishTest();
      }
    });
  }

  public void testChainWithWildcards() {
    delayTestFinish(TEST_DELAY);
    Context ctx = factory.ctx();
    ctx.CasA().with("*.*").fire(new Receiver<AProxy>() {
      @Override
      public void onSuccess(AProxy response) {
        new CastAndCheckReceiver(AProxy.class).onSuccess(response.getNextA().getNextA());
        C1Proxy cast = (C1Proxy) response;
        new CastAndCheckReceiver(C1Proxy.class).onSuccess(cast.getNextC().getNextC());
        assertNull(cast.getNextC().getNextC().getNextC());
        finishTest();
      }
    });
  }

  public void testCreation() {
    delayTestFinish(TEST_DELAY);
    Context ctx = factory.ctx();
    checkA(ctx, AProxy.class);
    checkB(ctx, B1Proxy.class);
    checkB(ctx, B2Proxy.class);
    checkC(ctx, C1Proxy.class);
    checkC(ctx, C2Proxy.class);
    checkD(ctx, D1Proxy.class);
    checkD(ctx, D2Proxy.class);
    // D3Proxy is a proxy supertype, assignable to BaseProxy and has @ProxyFor
    checkD(ctx, D3Proxy.class);
    checkD(ctx, MoreDerivedProxy.class);
    checkW(ctx, WProxy.class);
    checkW(ctx, WZProxy.class);
    checkZ(ctx, ZProxy.class);
    checkZ(ctx, ZWProxy.class);
    ctx.fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        finishTest();
      }
    });
  }

  /**
   * Ensure heterogeneous collections work.
   */
  public void testCreationList() {
    Context ctx = factory.ctx();
    ctx.checkList(
        Arrays.asList(create(ctx, AProxy.class), create(ctx, B2Proxy.class), create(ctx,
            C2Proxy.class), create(ctx, D2Proxy.class))).to(checkReceiver);
    ctx.fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        finishTest();
      }
    });
  }

  public void testGenericRequest() {
    TestRequestFactory rf = GWT.create(TestRequestFactory.class);
    EventBus eventBus = new SimpleEventBus();
    rf.initialize(eventBus);
    SimpleFooProxy simpleFoo = rf.testGenericRequest().create(SimpleFooProxy.class);
    assertNull(simpleFoo.getUserName());
  }

  public void testRetrieval() {
    delayTestFinish(TEST_DELAY);
    Context ctx = factory.ctx();
    ctx.AasA().to(CastAndCheckReceiver.of(AProxy.class));
    ctx.BasA().to(CastAndCheckReceiver.of(B1Proxy.class));
    ctx.BasB().to(CastAndCheckReceiver.of(B1Proxy.class));
    ctx.CasA().to(CastAndCheckReceiver.of(C1Proxy.class));
    ctx.CasB().to(CastAndCheckReceiver.of(C1Proxy.class));
    ctx.DasA().to(CastAndCheckReceiver.of(MoreDerivedProxy.class));
    ctx.DasD().to(CastAndCheckReceiver.of(MoreDerivedProxy.class));
    ctx.W().to(CastAndCheckReceiver.of(WProxy.class));
    ctx.W2().to(CastAndCheckReceiver.of(WZProxy.class));
    ctx.Z().to(CastAndCheckReceiver.of(ZProxy.class));
    ctx.Z2().to(CastAndCheckReceiver.of(ZWProxy.class));
    ctx.fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        finishTest();
      }
    });
  }

  public void testRetrievalCollection() {
    delayTestFinish(TEST_DELAY);
    Context ctx = factory.ctx();
    ctx.testCollection().to(new ListChecker());
    ctx.testCollectionSub().to(new ListChecker());
    ctx.fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void response) {
        finishTest();
      }
    });
  }

  protected Factory createFactory() {
    Factory f = GWT.create(Factory.class);
    f.initialize(new SimpleEventBus());
    return f;
  }

  @Override
  protected void gwtSetUp() throws Exception {
    factory = createFactory();
  }

  private void checkA(Context ctx, Class<? extends AProxy> clazz) {
    ctx.checkA(create(ctx, clazz)).to(checkReceiver);
  }

  private void checkB(Context ctx, Class<? extends EntityProxy> clazz) {
    ctx.checkB(create(ctx, clazz)).to(checkReceiver);
  }

  private void checkC(Context ctx, Class<? extends EntityProxy> clazz) {
    ctx.checkC(create(ctx, clazz)).to(checkReceiver);
  }

  private void checkD(Context ctx, Class<? extends EntityProxy> clazz) {
    ctx.checkD(create(ctx, clazz)).to(checkReceiver);
  }

  private void checkW(Context ctx, Class<? extends EntityProxy> clazz) {
    ctx.checkW(create(ctx, clazz)).to(checkReceiver);
  }

  private void checkZ(Context ctx, Class<? extends EntityProxy> clazz) {
    ctx.checkZ(create(ctx, clazz)).to(checkReceiver);
  }

  private <T extends EntityProxy> T create(Context ctx, Class<T> clazz) {
    T obj = ctx.create(clazz);
    if (obj instanceof AProxy) {
      ((AProxy) obj).setA("A");
    }
    if (obj instanceof B1Proxy) {
      ((B1Proxy) obj).setB("B");
    }
    if (obj instanceof B2Proxy) {
      ((B2Proxy) obj).setB("B");
    }
    if (obj instanceof C1Proxy) {
      ((C1Proxy) obj).setC("C");
    }
    if (obj instanceof C2Proxy) {
      ((C2Proxy) obj).setC("C");
    }
    if (obj instanceof D1Proxy) {
      ((D1Proxy) obj).setD("D");
    }
    if (obj instanceof D2Proxy) {
      ((D2Proxy) obj).setD("D");
    }
    if (obj instanceof D3Proxy) {
      ((D3Proxy) obj).setD("D");
    }
    if (obj instanceof WProxy) {
      ((WProxy) obj).setW("W");
    }
    if (obj instanceof ZProxy) {
      ((ZProxy) obj).setZ("Z");
    }
    if (obj instanceof WZProxy) {
      ((WZProxy) obj).setW("W");
    }
    if (obj instanceof ZWProxy) {
      ((ZWProxy) obj).setZ("Z");
    }
    return obj;
  }
}
