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
package elemental.js;

import com.google.gwt.core.client.GWT;

import elemental.client.Browser;
import elemental.html.Navigator;
import elemental.js.dom.JsDocument;
import elemental.js.html.JsWindow;

/**
 * JavaScript native implementation of {@link elemental.client.Browser}.
 */
public class JsBrowser {
  /**
   * A {@link Browser.Info} implementation for when the browser is known to be
   * Gecko at compile time.
   */
  @SuppressWarnings("unused")
  private static class InfoWhenKnownGecko extends InfoWhenUnknown {
    @Override
    public boolean isGecko() {
      return true;
    }

    @Override
    public boolean isSupported() {
      return true;
    }

    @Override
    public boolean isWebKit() {
      return false;
    }
  }

  /**
   * A {@link Browser.Info} implementation for when the browser is known to be
   * Unsupported at compile time.
   */
  @SuppressWarnings("unused")
  private static class InfoWhenKnownUnsupported extends InfoWhenUnknown {
    @Override
    public boolean isGecko() {
      return false;
    }

    @Override
    public boolean isSupported() {
      return false;
    }

    @Override
    public boolean isWebKit() {
      return false;
    }
  }

  /**
   * A {@link Browser.Info} implementation for when the browser is known to be
   * WebKit at compile time.
   */
  @SuppressWarnings("unused")
  private static class InfoWhenKnownWebKit extends InfoWhenUnknown {
    @Override
    public boolean isGecko() {
      return false;
    }

    @Override
    public boolean isSupported() {
      return true;
    }

    @Override
    public boolean isWebKit() {
      return true;
    }
  }

  /**
   * A {@link Browser.Info} implementation for when the browser is not known
   * until runtime.
   *
   * <p>
   * Careful Captain! All those static fields are intentional. In order to
   * ensure good dead stripping of the entire InfoWhen class hierarchy, the
   * instance returned by {@link JsBrowser#getInfo()} must not be aliased, so it
   * instead returns a fly-weight instance referencing static fields.
   * </p>
   */
  private static class InfoWhenUnknown implements Browser.Info {
    private native static double getGeckoVersion(String userAgent) /*-{
      var r = / rv\:(\d+\.\d+)/.exec(userAgent);
      return r ? parseFloat(r[1]) : 0.0;
    }-*/;

    private native static String getProduct(Navigator nav) /*-{
      return nav.product;
    }-*/;

    private native static double getWebKitVersion(String userAgent) /*-{
      var r = /WebKit\/(\d+\.\d+)/.exec(userAgent);
      return r ? parseFloat(r[1]) : 0.0;
    }-*/;

    private static boolean browserDetected;

    private static boolean platformDetected;

    private static boolean isWebKit;

    private static boolean isGecko;

    private static boolean isMac;

    private static boolean isWindows;

    private static boolean isLinux;

    private void ensurePlatformDetected() {
      if (!platformDetected) {
        platformDetected = true;
        final String userAgent = getWindow().getNavigator().getUserAgent();
        isWindows = userAgent.indexOf("Win") >= 0;
        if (isWindows) {
          return;
        }

        isMac = userAgent.indexOf("Mac") >= 0;
        if (isMac) {
          return;
        }

        isLinux = userAgent.indexOf("Linux") >= 0;
      }
    }

    private void ensureBrowserDetected() {
      if (!browserDetected) {
        browserDetected = true;
        final Navigator nav = getWindow().getNavigator();
        final String ua = nav.getUserAgent();
        boolean isWebKitBased = ua.indexOf("WebKit") >= 0;
        if (isWebKitBased) {
          isWebKit = getWebKitVersion(ua) >= SUPPORTED_WEBKIT_VERSION;
          return;
        }

        assert !isWebKitBased;
        isGecko = getProduct(nav).equals("Gecko") && getGeckoVersion(ua) >= SUPPORTED_GECKO_VERSION;
      }
    }

    @Override
    public boolean isGecko() {
      ensureBrowserDetected();
      return isGecko;
    }

    @Override
    public boolean isSupported() {
      ensureBrowserDetected();
      return isGecko || isWebKit;
    }

    @Override
    public boolean isWebKit() {
      ensureBrowserDetected();
      return isWebKit;
    }

    @Override
    public boolean isLinux() {
      ensurePlatformDetected();
      return isLinux;
    }

    @Override
    public boolean isMac() {
      ensurePlatformDetected();
      return isMac;
    }

    public boolean isWindows() {
      ensurePlatformDetected();
      return isWindows;
    }
  }

  /**
   * The minimum version of WebKit that is supported.
   *
   * This equates to >= Safari 5.0.2
   */
  private static final double SUPPORTED_WEBKIT_VERSION = 533.18;

  /**
   * The minimum version of Gecko that is supported.
   *
   * This equates to >= Firefox 4.0
   */
  private static final double SUPPORTED_GECKO_VERSION = 2.0;

  /**
   * Gets the document within which this script is running.
   */
  public static native JsDocument getDocument() /*-{
    return $doc;
  }-*/;

  public static Browser.Info getInfo() {
    return GWT.create(InfoWhenUnknown.class);
  }

  /**
   * Gets the window within which this script is running.
   */
  public static native JsWindow getWindow() /*-{
    return $wnd;
  }-*/;

  // Non-instantiable.
  private JsBrowser() {
  }
}
