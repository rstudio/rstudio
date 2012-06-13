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

import elemental.dom.Document;
import elemental.html.Window;
import elemental.js.JsBrowser;

/**
 * Entry-point for getting the access to browser globals.
 */
public class Browser {

  /**
   * Provides limited user agent information for the current browser. The API is
   * structured such that info can either be determined at runtime or
   * constrained at compile time through GWT permutations.
   * 
   * TODO(knorton): Add the gwt.xml file that enables permutations.
   */
  public interface Info {
    
    /**
     * Indicates whether the browser is a supported version of Gecko.
     */
    boolean isGecko();

    /**
     * Indicates whether the platform is Linux.
     */
    boolean isLinux();

    /**
     * Indicates whether the platform is Macintosh.
     */
    boolean isMac();
    
    /**
     * Indicates whether the browser is one of the supported browsers.
     */
    boolean isSupported();
    
    /**
     * Indicates whether the browser is a supported version of WebKit.
     */
    boolean isWebKit();
    
    /**
     * Indicates whether the platform is Windows.
     */
    boolean isWindows();
  }

  /**
   * Decodes a URI as specified in the ECMA-262, 5th edition specification,
   * section 15.1.3.1.
   * 
   * @see "http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-262.pdf"
   */
  public static native String decodeURI(String encodedURI) /*-{
    return decodeURI(encodedURI);
  }-*/;

  /**
   * Encodes a URI compoment as specified in the ECMA-262, 5th edition specification,
   * section 15.1.3.1.
   * 
   * @see "http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-262.pdf"
   */
  public static native String decodeURIComponent(String encodedUriComponent) /*-{
    return decodeURIComponent(encodedUriComponent);
  }-*/;

  /**
   * Encodes a URI as specified in the ECMA-262, 5th edition specification,
   * section 15.1.3.1.
   * 
   * @see "http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-262.pdf"
   */
  public static native String encodeURI(String uri) /*-{
    return encodeURI(uri);
  }-*/;

  /**
   * Encodes a URI component as specified in the ECMA-262, 5th edition specification,
   * section 15.1.3.1.
   * 
   * @see "http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-262.pdf"
   */
  public static native String encodeURIComponent(String uriComponent) /*-{
    return encodeURIComponent(uriComponent);
  }-*/;

  /**
   * Gets the document within which this script is running.
   */
  public static Document getDocument() {
    return JsBrowser.getDocument();
  }

  /**
   * Gets information about the current browser.
   */
  public static Info getInfo() {
    return JsBrowser.getInfo();
  }

  /**
   * Gets the window within which this script is running.
   */
  public static Window getWindow() {
    return JsBrowser.getWindow();
  }

  // Non-instantiable.
  private Browser() {
  }
}
