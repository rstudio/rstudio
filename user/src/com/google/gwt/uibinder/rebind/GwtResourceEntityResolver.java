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

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Does special handling of external entities encountered by sax xml parser,
 * e.g. the uri in
 *
 * <pre>
 * &lt;!DOCTYPE gwt:UiBinder
  SYSTEM "http://google-web-toolkit.googlecode.com/files/xhtml.ent"></pre>
 * <p>
 * Specifically, if the requested uri starts with
 * <code>http://google-web-toolkit.googlecode.com/files</code>, provide
 * the contents from a built in resource rather than allowing sax to make a
 * network request.
 */
class GwtResourceEntityResolver implements EntityResolver {
  interface ResourceLoader {
    InputStream fetch(String name);
  }

  private static final String EXTERNAL_ENTITY_PREFIX = "http://google-web-toolkit.googlecode.com/files/";

  private static final String RESOURCES = "com/google/gwt/uibinder/resources/";

  private final ResourceLoader resourceLoader;

  public GwtResourceEntityResolver() {
    this(new ResourceLoader() {
      public InputStream fetch(String name) {
        return UiBinderGenerator.class.getClassLoader().getResourceAsStream(
            name);
      }
    });
  }

  /**
   * For testing.
   */
  GwtResourceEntityResolver(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public InputSource resolveEntity(String publicId, String systemId)
      throws SAXException, IOException {
    if (!systemId.startsWith(EXTERNAL_ENTITY_PREFIX)) {
      // Not our problem, return null and sax will find it
      return null;
    }

    String name = RESOURCES
        + systemId.substring(EXTERNAL_ENTITY_PREFIX.length());
    InputStream resourceStream = resourceLoader.fetch(name);

    if (resourceStream == null) {
      /*
       * Maybe, just maybe they're asking for something that has been added to
       * svn but hasn't reached their local gwt install yet. Return null to
       * allow sax to try to fetch it.
       */
      return null;
    }

    InputSource inputSource = new InputSource(resourceStream);
    inputSource.setPublicId(publicId);
    inputSource.setSystemId(systemId);
    return inputSource;
  }
}
