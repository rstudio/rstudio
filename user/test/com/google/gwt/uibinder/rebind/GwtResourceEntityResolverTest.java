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

import junit.framework.TestCase;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Text of GwtResourceEntityResolver.
 */
public class GwtResourceEntityResolverTest extends TestCase {
  private static class MockResourceLoader implements
      GwtResourceEntityResolver.ResourceLoader {
    InputStream stream;

    public InputStream fetch(String name) {
      return stream;
    }
  }
  private static final String LEGACY_SYSTEM_ID = "http://google-web-toolkit.googlecode.com/files/xhtml.ent";
  private static final String VERSIONED_SYSTEM_ID = "http://dl.google.com/gwt/DTD/2.0.0/xhtml.ent";
  private static final String REDIRECT_SYSTEM_ID = "http://dl.google.com/gwt/DTD/xhtml.ent";
  private static final String SSL_VERSIONED_SYSTEM_ID = "https://dl-ssl.google.com/gwt/DTD/2.0.0/xhtml.ent";
  private static final String SSL_REDIRECT_SYSTEM_ID = "https://dl-ssl.google.com/gwt/DTD/xhtml.ent";

  private GwtResourceEntityResolver resolver;
  private MockResourceLoader loader;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    loader = new MockResourceLoader();
    resolver = new GwtResourceEntityResolver(loader);

    loader.stream = new InputStream() {
      @Override
      public int read() throws IOException {
        throw new UnsupportedOperationException();
      }
    };
  }

  public void testAlmostCorrectAndOnceWorked() throws SAXException, IOException {
    doBad(LEGACY_SYSTEM_ID.replace("files", "filesss"));
    doBad(VERSIONED_SYSTEM_ID.replace("DTD", "DTDdddd"));
    doBad(REDIRECT_SYSTEM_ID.replace("DTD", "DTDdddd"));
  }
  
  public void testNotOurProblem() throws SAXException, IOException {
    doBad("http://arbitrary");
  }

  public void testVersionedGood() throws SAXException, IOException {
    doGood(VERSIONED_SYSTEM_ID);
    doGood(REDIRECT_SYSTEM_ID);
    doGood(SSL_VERSIONED_SYSTEM_ID);
    doGood(SSL_REDIRECT_SYSTEM_ID);
    doGood(LEGACY_SYSTEM_ID);
  }

  private void doBad(String url) throws SAXException, IOException {
    assertNull(resolver.resolveEntity(null, url));
    assertNull(resolver.resolveEntity("meaningless", url));
  }

  private void doGood(String url) throws SAXException, IOException {
    String publicId = "some old public thing";
    InputSource s = resolver.resolveEntity(publicId, url);
    assertNotNull(s);
    assertEquals(publicId, s.getPublicId());
  }
 }
