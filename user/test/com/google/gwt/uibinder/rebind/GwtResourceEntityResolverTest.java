/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.javac.testing.impl.MockResource;
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;
import com.google.gwt.dev.resource.Resource;

import junit.framework.TestCase;

import org.xml.sax.InputSource;

/**
 * Test of GwtResourceEntityResolver.
 */
public class GwtResourceEntityResolverTest extends TestCase {

  private static final String LEGACY_SYSTEM_ID = "http://google-web-toolkit.googlecode.com/files/xhtml.ent";
  private static final String VERSIONED_SYSTEM_ID = "http://dl.google.com/gwt/DTD/2.0.0/xhtml.ent";
  private static final String REDIRECT_SYSTEM_ID = "http://dl.google.com/gwt/DTD/xhtml.ent";
  private static final String SSL_VERSIONED_SYSTEM_ID = "https://dl-ssl.google.com/gwt/DTD/2.0.0/xhtml.ent";
  private static final String SSL_REDIRECT_SYSTEM_ID = "https://dl-ssl.google.com/gwt/DTD/xhtml.ent";

  private static final Resource xhtmlEntResource = new MockResource(
      "com/google/gwt/uibinder/resources/xhtml.ent") {
    @Override
    public CharSequence getContent() {
      return "";
    }
  };

  private MockResourceOracle oracle = new MockResourceOracle(xhtmlEntResource);

  private GwtResourceEntityResolver resolver = new GwtResourceEntityResolver(TreeLogger.NULL,
      oracle, "");

  public void testAlmostCorrectAndOnceWorked() {
    doExpectNull(LEGACY_SYSTEM_ID.replace("files", "filesss"));
    doExpectNull(VERSIONED_SYSTEM_ID.replace("DTD", "DTDdddd"));
    doExpectNull(REDIRECT_SYSTEM_ID.replace("DTD", "DTDdddd"));
  }

  public void testNotOurProblem() {
    doExpectNull("http://arbitrary");
  }

  public void testHappy() {
    doGood(REDIRECT_SYSTEM_ID);
    doGood(SSL_REDIRECT_SYSTEM_ID);
    doGood(LEGACY_SYSTEM_ID);
  }

  public void testDrillThrough() {
    doExpectNull(VERSIONED_SYSTEM_ID);
    doExpectNull(SSL_VERSIONED_SYSTEM_ID);
  }

  private void doExpectNull(String url) {
    assertNull(resolver.resolveEntity(null, url));
    assertNull(resolver.resolveEntity("meaningless", url));
  }

  private void doGood(String url) {
    String publicId = "some old public thing";
    InputSource s = resolver.resolveEntity(publicId, url);
    assertNotNull(s);
    assertEquals(publicId, s.getPublicId());
  }
}
