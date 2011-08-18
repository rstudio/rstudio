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
package com.google.web.bindery.requestfactory.server;

import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryPolymorphicTest;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.vm.impl.Deobfuscator;

import java.util.List;

/**
 * A JRE version of {@link RequestFactoryPolymorphicTest} that includes
 * additional tests for the RequestFactoryInterfaceValidator that are specific
 * to type-hierarchy mapping.
 */
public class RequestFactoryPolymorphicJreTest extends RequestFactoryPolymorphicTest {
  private Deobfuscator deobfuscator;

  @Override
  public String getModuleName() {
    return null;
  }

  /**
   * Disabling, since this is a test of the code Generator.
   */
  @Override
  public void testGenericRequest() {
  }

  /**
   * Check related proxy types with unrelated domain types.
   * */
  public void testUnrelatedDomainTypes() throws ClassNotFoundException {
    // Simple mappings
    check(W.class, WProxy.class, WProxy.class);
    check(W.class, WZProxy.class, WZProxy.class);
    check(Z.class, ZProxy.class, ZProxy.class);
    check(Z.class, ZWProxy.class, ZWProxy.class);

    // Look for derived proxy types that map to the domain type
    check(Z.class, WProxy.class, ZWProxy.class);
    check(W.class, ZProxy.class, WZProxy.class);

    /*
     * This test is included to verify that the validator will fail gracefully
     * when asked for a nonsensical assignment. For these two tests, the
     * requested proxy type isn't mapped to the domain type, nor are there any
     * *sub*-types of the requested proxy class that are assignable to the
     * domaintype. The requested proxy type's supertype is assignable to the
     * domain type, however the supertype isn't assignable to its subtype, so
     * it's not a valid choice. Normally, the RequestFactoryInterfaceValidator
     * would detect the mismatch between the RequestContext and the service
     * method return types, so this shouldn't be a problem in practice. It would
     * only ever crop up when the SkipInterfaceValidation annotation has been
     * used.
     */
    check(Z.class, WZProxy.class, null);
    check(W.class, ZWProxy.class, null);
  }

  /**
   * Tests that the RequestFactoryInterfaceValidator is producing the correct
   * mappings from domain types back to client types.
   */
  public void testValidator() throws ClassNotFoundException {
    /*
     * Check explicit mappings. Not all of the types are directly referenced in
     * the method declarations, so this also tests the ExtraTypes annotation.
     */
    check(A.class, AProxy.class, AProxy.class);
    check(B.class, B1Proxy.class, B1Proxy.class);
    check(B.class, B2Proxy.class, B2Proxy.class);
    check(C.class, C1Proxy.class, C1Proxy.class);
    check(C.class, C2Proxy.class, C2Proxy.class);

    // Check types without explicit mappings.
    check(ASub.class, AProxy.class, AProxy.class);
    check(BSub.class, B1Proxy.class, B1Proxy.class);
    check(BSub.class, B2Proxy.class, B2Proxy.class);
    check(CSub.class, C1Proxy.class, C1Proxy.class);
    check(CSub.class, C2Proxy.class, C2Proxy.class);

    // Check assignments with proxies extending proxies
    check(C.class, B1Proxy.class, C1Proxy.class);
    check(C.class, B2Proxy.class, C2Proxy.class);

    // Should prefer more-derived interfaces when possible
    check(D.class, AProxy.class, MoreDerivedProxy.class);
    check(D.class, D1Proxy.class, MoreDerivedProxy.class);
    check(D.class, D2Proxy.class, MoreDerivedProxy.class);
    check(D.class, D3Proxy.class, MoreDerivedProxy.class);
    check(D.class, MoreDerivedProxy.class, MoreDerivedProxy.class);
    check(DSub.class, AProxy.class, MoreDerivedProxy.class);
    check(DSub.class, D1Proxy.class, MoreDerivedProxy.class);
    check(DSub.class, D2Proxy.class, MoreDerivedProxy.class);
    check(DSub.class, D3Proxy.class, MoreDerivedProxy.class);
    check(DSub.class, MoreDerivedProxy.class, MoreDerivedProxy.class);
  }

  @Override
  protected Factory createFactory() {
    return RequestFactoryJreTest.createInProcess(Factory.class);
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    deobfuscator = Deobfuscator.Builder.load(Factory.class, getClass().getClassLoader()).build();
  }

  private void check(Class<?> domainType, Class<? extends BaseProxy> declaredReturnType,
      Class<? extends BaseProxy> expectedClientType) throws ClassNotFoundException {
    while (domainType != null) {
      List<String> types = deobfuscator.getClientProxies(domainType.getName());
      if (types != null) {
        for (String type : types) {
          Class<?> clientType = Class.forName(type);
          if (declaredReturnType.isAssignableFrom(clientType)) {
            if (expectedClientType == null) {
              fail("Should not have found any matches");
            }
            assertEquals(expectedClientType, clientType);
            return;
          }
        }
      }
      domainType = domainType.getSuperclass();
    }
    if (expectedClientType != null) {
      fail("Expecting to find " + expectedClientType.getCanonicalName());
    }
  }
}
