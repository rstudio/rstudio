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

import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.Sets;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Makes the sax xml parser use the {@link ResourceOracle}.
 * <p>
 * Does special case handling of GWT specific DTDs to be fetched from our
 * download site. If the requested uri starts with
 * <code>http://dl.google.com/gwt/DTD/</code> (or one or two others), provides
 * the contents from a built in resource rather than allowing sax to make a
 * network request.
 */
class GwtResourceEntityResolver implements EntityResolver {
  private static final Set<String> EXTERNAL_PREFIXES = Collections.unmodifiableSet(Sets.create(new String[] {
      "http://google-web-toolkit.googlecode.com/files/",
      "http://dl.google.com/gwt/DTD/", "https://dl-ssl.google.com/gwt/DTD/"}));

  private static final String RESOURCES = "com/google/gwt/uibinder/resources/";

  private String pathBase;

  private final ResourceOracle resourceOracle;

  public GwtResourceEntityResolver(ResourceOracle resourceOracle,
      String pathBase) {
    this.resourceOracle = resourceOracle;
    this.pathBase = pathBase;
  }

  public InputSource resolveEntity(String publicId, String systemId) {
    String matchingPrefix = findMatchingPrefix(systemId);

    Resource resource = null;
    Map<String, Resource> map = resourceOracle.getResourceMap();
    if (matchingPrefix != null) {
      resource = map.get(RESOURCES
          + systemId.substring(matchingPrefix.length()));
    }

    if (resource == null) {
      resource = map.get(pathBase + systemId);
    }

    if (resource != null) {
      String content = Util.readStreamAsString(resource.openContents());
      InputSource inputSource = new InputSource(new StringReader(content));
      inputSource.setPublicId(publicId);
      inputSource.setSystemId(resource.getPath());
      return inputSource;
    }
    /*
     * Let Sax find it on the interweb.
     */
    return null;
  }

  private String findMatchingPrefix(String systemId) {
    for (String prefix : EXTERNAL_PREFIXES) {
      if (systemId.startsWith(prefix)) {
        return prefix;
      }
    }
    return null;
  }
}
