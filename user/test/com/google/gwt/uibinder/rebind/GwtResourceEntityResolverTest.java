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
  private static final String SYSTEM_ID = "http://google-web-toolkit.googlecode.com/files/xhtml.ent";

  private static class MockResourceLoader implements
      GwtResourceEntityResolver.ResourceLoader {
    String fetched;
    InputStream stream;

    public InputStream fetch(String name) {
      return stream;
    }
  }

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

  public void testNotOurProblem() throws SAXException, IOException {
    assertNull(resolver.resolveEntity(null, "http://arbitrary"));
    assertNull(resolver.resolveEntity("meaningless", "http://arbitrary"));
    assertNull(resolver.resolveEntity(null, "arbitrary/relative"));

    String almostCorrectAndOnceWorked = SYSTEM_ID.replace("files", "filesss");
    assertNull(resolver.resolveEntity("meaningless", almostCorrectAndOnceWorked));
    assertNull(resolver.resolveEntity(null, almostCorrectAndOnceWorked));
  }

  public void testOursGood() throws SAXException, IOException {
    String publicId = "some old public thing";

    InputSource s = resolver.resolveEntity(publicId, SYSTEM_ID);
    assertEquals(publicId, s.getPublicId());
    assertEquals(SYSTEM_ID, s.getSystemId());
    assertEquals(loader.stream, s.getByteStream());
  }
}
