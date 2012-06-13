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
package elemental.client;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for {@link Browser}.
 */
@DoNotRunWith(Platform.HtmlUnitUnknown)
public class BrowserTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "elemental.Elemental";
  }

  /**
   * Tests the encode/decodeURI pass through correctly to the browser. This is
   * not intended to be an exhaustive test of the actual behavior (on the
   * assumption it's correct in the browser).
   */
  public void testEncodeDecodeURI() {
    String uri = "~!@#$%^&*(){}[]=:/,;?+'\"\\";
    String encodedURI = Browser.encodeURI(uri);
    assertEquals("~!@#$%25%5E&*()%7B%7D%5B%5D=:/,;?+'%22%5C", encodedURI);
    String decodedURI = Browser.decodeURI(encodedURI);
    assertEquals(uri, decodedURI);
  }

  /**
   * Tests the encode/decodeURIComponent pass through correctly to the browser.
   * This is not intended to be an exhaustive test of the actual behavior (on
   * the assumption it's correct in the browser).
   */
  public void testEncodeDecodeURIComponent() {
    String uri = "~!@#$%^&*(){}[]=:/,;?+'\"\\";
    String encodedURI = Browser.encodeURIComponent(uri);
    assertEquals("~!%40%23%24%25%5E%26*()%7B%7D%5B%5D%3D%3A%2F%2C%3B%3F%2B'%22%5C", encodedURI);
    String decodedURI = Browser.decodeURIComponent(encodedURI);
    assertEquals(uri, decodedURI);
  }
}
