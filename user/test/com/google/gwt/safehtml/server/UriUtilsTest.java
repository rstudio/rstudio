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
package com.google.gwt.safehtml.server;

import com.google.gwt.safehtml.shared.UriUtils;

import junit.framework.TestCase;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for UriUtils.
 */
public class UriUtilsTest extends TestCase {

  /**
   * Encapsulates a URI and relevant attributes for use in tests of
   * {@link UriUtils#sanitizeUri(String)} and related methods.
   */
  private static class UriTestCaseSpec {
    private String uri;
    private String scheme;
    private boolean expectUriParseException;
    private URI parsedUri;

    /**
     * Creates a URI test case specification.
     *
     * @param uri the URI of this test vector
     * @param scheme the scheme that is expected to be parsed from {@code uri}
     *          by {@link UriUtils#extractScheme(String)}
     * @param expectUriParseException true if parsing {@code uri} into a
     *          {@link URI} object is expected to result in a
     *          {@link URISyntaxException}
     */
    public UriTestCaseSpec(
        String uri, String scheme, boolean expectUriParseException) {
      this.uri = uri;
      this.scheme = scheme;
      this.expectUriParseException = expectUriParseException;
      if (!expectUriParseException) {
        try {
          parsedUri = new URI(uri);
        } catch (URISyntaxException e) {
          throw new IllegalStateException(
              "parsing \"" + uri + "\" resulted in " + "unexpected exception: "
                  + e);
        }
      }
    }

    public UriTestCaseSpec(String uri, String scheme) {
      this(uri, scheme, false);
    }

    public String getUri() {
      return uri;
    }

    public URI getParsedUri() {
      return parsedUri;
    }

    public String getScheme() {
      return scheme;
    }

    public boolean getExpectUriParseException() {
      return expectUriParseException;
    }
  }

  private static final List<UriTestCaseSpec> GOOD_URIS;
  static {
    ArrayList<UriTestCaseSpec> goodUris = new ArrayList<UriTestCaseSpec>();

    // URIs with no scheme.
    goodUris.add(new UriTestCaseSpec("bar", null));
    goodUris.add(new UriTestCaseSpec("/foo/bar", null));
    goodUris.add(new UriTestCaseSpec("/foo/bar#baz", null));
    goodUris.add(new UriTestCaseSpec("/foo/bar:baz", null));
    goodUris.add(new UriTestCaseSpec("#baz", null));
    goodUris.add(new UriTestCaseSpec("#baz:dooz", null));
    goodUris.add(new UriTestCaseSpec("foo#baz:dooz", null));

    // URIs with http scheme.
    goodUris.add(new UriTestCaseSpec("http:foo", "http"));
    goodUris.add(new UriTestCaseSpec("http://foo.com:80/blah", "http"));
    goodUris.add(new UriTestCaseSpec("http://foo.com/bar", "http"));
    goodUris.add(new UriTestCaseSpec("http://foo.com/bar#baz", "http"));

    // URIs with https, ftp, mailto scheme.
    goodUris.add(new UriTestCaseSpec("mailto:good@good.com", "mailto"));
    goodUris.add(new UriTestCaseSpec("https://foo.com", "https"));
    goodUris.add(new UriTestCaseSpec("ftp://foo.com", "ftp"));

    GOOD_URIS = Collections.unmodifiableList(goodUris);
  }

  private static final List<UriTestCaseSpec> BAD_URIS;
  static {
    ArrayList<UriTestCaseSpec> badUris = new ArrayList<UriTestCaseSpec>();

    // URIs with defined, bad schemes.
    badUris.add(new UriTestCaseSpec("javascript:evil", "javascript"));
    badUris.add(new UriTestCaseSpec("javascript://foo()", "javascript"));
    badUris.add(new UriTestCaseSpec("javascript:evil#world", "javascript"));
    badUris.add(new UriTestCaseSpec("javascript:evil/is", "javascript"));

    // URIs with weird schemes, neither of which can be parsed as a URI.
    badUris.add(
        new UriTestCaseSpec("  mailto:good@good.com", "  mailto", true));
    badUris.add(new UriTestCaseSpec("ma&ilto:good@good.com", "ma&ilto", true));

    BAD_URIS = Collections.unmodifiableList(badUris);
  }

  public static void testExtractScheme() {
    for (UriTestCaseSpec uriSpec : GOOD_URIS) {
      assertEquals(
          uriSpec.getScheme(), UriUtils.extractScheme(uriSpec.getUri()));
      // Verify that the scheme parsed by extractScheme() is the same as
      // obtained by {@link URI}'s parser.
      assertEquals(uriSpec.getScheme(), uriSpec.getParsedUri().getScheme());
    }
    for (UriTestCaseSpec uriSpec : BAD_URIS) {
      assertEquals(
          uriSpec.getScheme(), UriUtils.extractScheme(uriSpec.getUri()));
      if (!uriSpec.getExpectUriParseException()) {
        // Verify that the scheme parsed by extractScheme() is the same as
        // obtained by {@link URI}'s parser (for those URIs that can be parsed
        // by the latter).
        assertEquals(uriSpec.getScheme(), uriSpec.getParsedUri().getScheme());
      }
    }
  }

  public static void testIsSafeUri() {
    for (UriTestCaseSpec uriSpec : GOOD_URIS) {
      assertTrue(UriUtils.isSafeUri(uriSpec.getUri()));
    }
    for (UriTestCaseSpec uriSpec : BAD_URIS) {
      assertFalse(UriUtils.isSafeUri(uriSpec.getUri()));
    }
  }

  public static void testSanitizeUri() {
    for (UriTestCaseSpec uriSpec : GOOD_URIS) {
      assertEquals(uriSpec.getUri(), UriUtils.sanitizeUri(uriSpec.getUri()));
    }
    for (UriTestCaseSpec uriSpec : BAD_URIS) {
      assertEquals("#", UriUtils.sanitizeUri(uriSpec.getUri()));
    }
  }
}
