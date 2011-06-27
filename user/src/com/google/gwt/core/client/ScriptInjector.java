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

package com.google.gwt.core.client;

/*
 * Design note: This class intentionally does not use the GWT DOM wrappers so
 * that this code can pull in as few dependencies as possible and live in the
 * Core module.
 */

/**
 * Dynamically create a script tag and attach it to the DOM.
 * 
 * Usage with script as local string:
 * <p>
 * 
 * <pre>
 *   String scriptBody = "var foo = ...";
 *   ScriptInjector.fromString(scriptBody).inject();
 * </pre>
 * <p>
 * Usage with script loaded as URL:
 * <p>
 * 
 * <pre>
 *   ScriptInjector.fromUrl("http://example.com/foo.js").setCallback(
 *     new Callback<Void, Exception>() {
 *        public void onFailure(Exception reason) {
 *          Window.alert("Script load failed.");
 *        }
 *        public void onSuccess(Void result) {
 *          Window.alert("Script load success.");
 *        }
 *     }).inject();
 * </pre>
 * 
 * 
 */
public class ScriptInjector {

  /**
   * Builder for directly injecting a script body into the DOM.
   */
  public static class FromString {
    private boolean removeTag = true;
    private final String scriptBody;
    private JavaScriptObject window;

    /**
     * @param scriptBody The script text to install into the document.
     */
    public FromString(String scriptBody) {
      this.scriptBody = scriptBody;
    }

    /**
     * Injects a script into the DOM. The JavaScript is evaluated and will be
     * available immediately when this call returns.
     * 
     * By default, the script is installed in the same window that the GWT code
     * is installed in.
     * 
     * @return the script element created for the injection. Note that it may be
     *         removed from the DOM.
     */
    public JavaScriptObject inject() {
      JavaScriptObject wnd = (window == null) ? nativeDefaultWindow() : window;
      assert wnd != null;
      JavaScriptObject doc = nativeGetDocument(wnd);
      assert doc != null;
      JavaScriptObject scriptElement = nativeMakeScriptElement(doc);
      assert scriptElement != null;
      nativeSetText(scriptElement, scriptBody);
      nativeAttachToHead(doc, scriptElement);
      if (removeTag) {
        nativeRemove(scriptElement);
      }
      return scriptElement;
    }

    /**
     * @param removeTag If true, remove the tag immediately after injecting the
     *          source. This shrinks the DOM, possibly at the expense of
     *          readability if you are debugging javaScript.
     * 
     *          Default value is {@code true}.
     */
    public FromString setRemoveTag(boolean removeTag) {
      this.removeTag = removeTag;
      return this;
    }

    /**
     * @param window Specify which window to use to install the script. If not
     *          specified, the top current window GWT is loaded in is used.
     */
    public FromString setWindow(JavaScriptObject window) {
      this.window = window;
      return this;
    }
  }

  /**
   * Build an injection call for adding a script by URL.
   */
  public static class FromUrl {
    private Callback<Void, Exception> callback;
    private final String scriptUrl;
    private JavaScriptObject window;

    private FromUrl(String scriptUrl) {
      this.scriptUrl = scriptUrl;
    }

    /**
     * Injects an external JavaScript reference into the document and optionally
     * calls a callback when it finishes loading.
     * 
     * @return the script element created for the injection.
     */
    public JavaScriptObject inject() {
      JavaScriptObject wnd = (window == null) ? nativeDefaultWindow() : window;
      assert wnd != null;
      JavaScriptObject doc = nativeGetDocument(wnd);
      assert doc != null;
      JavaScriptObject scriptElement = nativeMakeScriptElement(doc);
      assert scriptElement != null;
      if (callback != null) {
        attachListeners(scriptElement, callback);
      }
      nativeSetSrc(scriptElement, scriptUrl);
      nativeAttachToHead(doc, scriptElement);
      return scriptElement;
    }

    /**
     * Specify a callback to be invoked when the script is loaded or loading
     * encounters an error.
     * <p>
     * <b>Warning:</b> This class <b>does not</b> control whether or not a URL
     * has already been injected into the document. The client of this class has
     * the responsibility of keeping score of the injected JavaScript files.
     * <p>
     * <b>Known bugs:</b>  This class uses the script tag's <code>onerror()
     * </code> callback to attempt to invoke onFailure() if the 
     * browser detects a load failure.  This is not reliable on all browsers 
     * (Doesn't work on IE or Safari 3 or less).
     * <p>
     * On Safari version 3 and prior, the onSuccess() callback may be invoked
     * even when the load of a page fails.  
     * <p>
     * To support failure notification on IE and older browsers, you should 
     * check some side effect of the script (such as a defined function)
     * to see if loading the script worked and include timeout logic.
     * 
     * @param callback callback that gets invoked asynchronously.
     */
    public FromUrl setCallback(Callback<Void, Exception> callback) {
      this.callback = callback;
      return this;
    }

    /**
     * This call allows you to specify which DOM window object to install the
     * script tag in. To install into the Top level window call
     * 
     * <code>
     *   builder.setWindow(ScriptInjector.TOP_WINDOW);
     * </code>
     * 
     * @param window Specifies which window to install in.
     */
    public FromUrl setWindow(JavaScriptObject window) {
      this.window = window;
      return this;
    }
  }

  /**
   * Returns the top level window object. Use this to inject a script so that
   * global variable references are available under <code>$wnd</code> in JSNI
   * access.
   * <p>
   * Note that if your GWT app is loaded from a different domain than the top
   * window, you may not be able to add a script element to the top window.
   */
  public static final JavaScriptObject TOP_WINDOW = nativeTopWindow();

  /**
   * Build an injection call for directly setting the script text in the DOM.
   * 
   * @param scriptBody the script text to be injected and immediately executed.
   */
  public static FromString fromString(String scriptBody) {
    return new FromString(scriptBody);
  }

  /**
   * Build an injection call for adding a script by URL.
   * 
   * @param scriptUrl URL of the JavaScript to be injected.
   */
  public static FromUrl fromUrl(String scriptUrl) {
    return new FromUrl(scriptUrl);
  }

  /**
   * Attaches event handlers to a script DOM element that will run just once a
   * callback when it gets successfully loaded.
   * <p>
   * <b>IE Notes:</b> Internet Explorer calls {@code onreadystatechanged}
   * several times while varying the {@code readyState} property: in theory,
   * {@code "complete"} means the content is loaded, parsed and ready to be
   * used, but in practice, {@code "complete"} happens when the JS file was
   * already cached, and {@code "loaded"} happens when it was transferred over
   * the network. Other browsers just call the {@code onload} event handler. To
   * ensure the callback will be called at most once, we clear out both event
   * handlers when the callback runs for the first time. More info at the <a
   * href="http://www.phpied.com/javascript-include-ready-onload/">phpied.com
   * blog</a>.
   * <p>
   * In IE, do not trust the "order" of {@code readyState} values. For instance,
   * in IE 8 running in Vista, if the JS file is cached, only {@code "complete"}
   * will happen, but if the file has to be downloaded, {@code "loaded"} can
   * fire in parallel with {@code "loading"}.
   * 
   * 
   * @param scriptElement element to which the event handlers will be attached
   * @param callback callback that runs when the script is loaded and parsed.
   */
  private static native void attachListeners(JavaScriptObject scriptElement,
      Callback<Void, Exception> callback) /*-{
    function clearCallbacks() {
      scriptElement.onerror = scriptElement.onreadystatechange = scriptElement.onload = function() {
      };
    }
    scriptElement.onload = $entry(function() {
      clearCallbacks();
      callback.@com.google.gwt.core.client.Callback::onSuccess(Ljava/lang/Object;)(null);
    });
    // or possibly more portable script_tag.addEventListener('error', function(){...}, true); 
    scriptElement.onerror = $entry(function() {
      clearCallbacks();
      var ex = @com.google.gwt.core.client.CodeDownloadException::new(Ljava/lang/String;)("onerror() called.");
      callback.@com.google.gwt.core.client.Callback::onFailure(Ljava/lang/Object;)(ex)
    });
    scriptElement.onreadystatechange = $entry(function() {
      if (scriptElement.readyState == 'complete' || scriptElement.readyState == 'loaded') {
        scriptElement.onload();
      }
    });
  }-*/;

  private static native void nativeAttachToHead(JavaScriptObject doc, JavaScriptObject scriptElement) /*-{
    doc.getElementsByTagName("head")[0].appendChild(scriptElement);
  }-*/;

  private static native JavaScriptObject nativeDefaultWindow() /*-{
    return window;
  }-*/;

  private static native JavaScriptObject nativeGetDocument(JavaScriptObject wnd) /*-{
    return wnd.document;
  }-*/;

  private static native JavaScriptObject nativeMakeScriptElement(JavaScriptObject doc) /*-{
    var element = doc.createElement("script");
    element.type = "text/javascript";
    return element;
  }-*/;

  private static native void nativeRemove(JavaScriptObject scriptElement) /*-{
    var p = scriptElement.parentNode;
    p.removeChild(scriptElement);
  }-*/;

  private static native void nativeSetSrc(JavaScriptObject element, String url) /*-{
    element.src = url;
  }-*/;

  private static native void nativeSetText(JavaScriptObject element, String scriptBody) /*-{
    element.text = scriptBody;
  }-*/;

  private static native JavaScriptObject nativeTopWindow() /*-{
    return $wnd;
  }-*/;

  /**
   * Utility class - do not instantiate
   */
  private ScriptInjector() {
  }
}
