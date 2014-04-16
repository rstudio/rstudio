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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.BrowserChannel.JavaObjectRef;
import com.google.gwt.dev.shell.BrowserChannel.JsObjectRef;
import com.google.gwt.dev.shell.BrowserChannel.Value;
import com.google.gwt.dev.shell.BrowserChannel.Value.ValueType;
import com.google.gwt.dev.shell.BrowserChannelClient.SessionHandlerClient;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;
import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;
import com.gargoylesoftware.htmlunit.javascript.SimpleScriptableProxy;
import com.gargoylesoftware.htmlunit.javascript.host.Window;

import net.sourceforge.htmlunit.corejs.javascript.ConsString;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.JavaScriptException;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
import net.sourceforge.htmlunit.corejs.javascript.Undefined;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Handle session tasks for HtmlUnit.
 */
public class HtmlUnitSessionHandler extends SessionHandlerClient {

  private class ToStringMethod extends ScriptableObject implements Function {

    private static final int EXPECTED_NUM_ARGS = 0;
    private static final long serialVersionUID = 1592865718416163348L;

    @Override
    public Object call(Context context, Scriptable scope, Scriptable thisObj,
        Object[] args) {
      // Allow extra arguments for forward compatibility
      if (args.length < EXPECTED_NUM_ARGS) {
        throw Context.reportRuntimeError("Bad number of parameters for function"
            + " toString: expected "
            + EXPECTED_NUM_ARGS
            + ", got "
            + args.length);
      }
      // thisObj is the javaObject.
      Value thisValue = makeValueFromJsval(context, thisObj);
      ExceptionOrReturnValue returnValue = JavaObject.getReturnFromJavaMethod(
          context, HtmlUnitSessionHandler.this, sessionData.getChannel(),
          TO_STRING_DISPATCH_ID, thisValue, EMPTY_VALUES);
      return HtmlUnitSessionHandler.this.makeJsvalFromValue(context,
          returnValue.getReturnValue());
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
      throw Context.reportRuntimeError("Function connect can't be used as a "
          + "constructor");
    }

    @Override
    public String getClassName() {
      return "function toString";
    }
  }

  private static final Value EMPTY_VALUES[] = new Value[0];
  private static final String REPLACE_METHOD_SIGNATURE = "@com.google.gwt.user.client.Window$Location::replace(Ljava/lang/String;)";
  private static final int TO_STRING_DISPATCH_ID = 0;

  Map<Integer, JavaObject> javaObjectCache;

  /**
   * The htmlPage is also used to synchronize calls to Java code.
   */
  private HtmlPage htmlPage;
  private JavaScriptEngine jsEngine;
  private IdentityHashMap<Scriptable, Integer> jsObjectToRef;
  private int nextRefId;
  private Map<Integer, Scriptable> refToJsObject;
  private SessionData sessionData;

  private final PrintWriterTreeLogger logger = new PrintWriterTreeLogger();

  private final ToStringMethod toStringMethod = new ToStringMethod();

  private final Window window;

  HtmlUnitSessionHandler(Window window, JavaScriptEngine jsEngine) {
    this.window = window;
    logger.setMaxDetail(TreeLogger.ERROR);
    this.jsEngine = jsEngine;
    htmlPage = (HtmlPage) this.window.getWebWindow().getEnclosedPage();
    if (logger.isLoggable(TreeLogger.INFO)) {
      logger.log(TreeLogger.INFO, "jsEngine = " + jsEngine + ", HtmlPage = "
          + htmlPage);
    }

    jsObjectToRef = new IdentityHashMap<Scriptable, Integer>();
    nextRefId = 1;
    refToJsObject = new HashMap<Integer, Scriptable>();

    // related to JavaObject cache.
    javaObjectCache = new HashMap<Integer, JavaObject>();
  }

  @Override
  public void freeValue(BrowserChannelClient channel, int[] ids) {
    for (int id : ids) {
      Scriptable scriptable = refToJsObject.remove(id);
      if (scriptable != null) {
        jsObjectToRef.remove(scriptable);
      }
    }
  }

  public JavaObject getOrCreateJavaObject(int refId, Context context) {
    JavaObject javaObject = javaObjectCache.get(refId);
    if (javaObject == null) {
      javaObject = new JavaObject(context, sessionData, refId);
      javaObjectCache.put(refId, javaObject);
    }
    return javaObject;
  }

  @Override
  public HtmlPage getSynchronizationObject() {
    return htmlPage;
  }

  /**
   * @param jsContext the Context
   */
  public Object getToStringTearOff(Context jsContext) {
    return toStringMethod;
  }

  @Override
  public String getUserAgent() {
    return "HtmlUnit-"
        + jsEngine.getWebClient().getBrowserVersion().getUserAgent();
  }

  @SuppressWarnings("unchecked")
  @Override
  public ExceptionOrReturnValue invoke(BrowserChannelClient channel, Value thisObj,
      String methodName, Value[] args) {
    if (logger.isLoggable(TreeLogger.DEBUG)) {
      logger.log(TreeLogger.DEBUG, "INVOKE: thisObj: " + thisObj + ", methodName: " + methodName
          + ", args: " + Arrays.toString(args));
    }
    /*
     * 1. lookup functions by name. 2. Find context and scope. 3. Convert
     * thisObject to ScriptableObject 4. Convert args 5. Get return value
     */
    Context jsContext = Context.getCurrentContext();
    ScriptableObject jsThis = null;
    if (thisObj.getType() == ValueType.NULL) {
      jsThis = window;
    } else {
      Object obj = makeJsvalFromValue(jsContext, thisObj);
      if (obj instanceof ScriptableObject) {
        jsThis = (ScriptableObject) obj;
      } else if (obj instanceof SimpleScriptableProxy<?>) {
        jsThis = ((SimpleScriptableProxy<SimpleScriptable>) obj).getDelegee();
      } else {
        logger.log(TreeLogger.ERROR, "Unable to convert " + obj + " to either "
            + " ScriptableObject or SimpleScriptableProxy");
        return new ExceptionOrReturnValue(true, new Value(null));
      }
    }
    Object functionObject = ScriptableObject.getProperty(
        window, methodName);
    if (functionObject == ScriptableObject.NOT_FOUND) {
      logger.log(TreeLogger.ERROR, "function " + methodName
          + " NOT FOUND, thisObj: " + jsThis + ", methodName: " + methodName);
      // TODO: see if this maps to QUIT
      return new ExceptionOrReturnValue(true, new Value(null));
    }
    Function jsFunction = (Function) functionObject;
    if (logger.isLoggable(TreeLogger.SPAM)) {
      logger.log(TreeLogger.SPAM, "INVOKE: jsFunction: " + jsFunction);
    }

    Object jsArgs[] = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      jsArgs[i] = makeJsvalFromValue(jsContext, args[i]);
    }
    Object result = null;
    try {
      if (args.length == 1
          && methodName.indexOf(REPLACE_METHOD_SIGNATURE) != -1) {
        // getUrl() is not visible
        String currentUrl = window.getLocation().toString();
        currentUrl = getUrlBeforeHash(currentUrl);
        String newUrl = getUrlBeforeHash((String) args[0].getValue());
        if (!newUrl.equals(currentUrl)) {
          WebWindow webWindow = window.getWebWindow();
          do {
            webWindow.getJobManager().removeAllJobs();
            webWindow = webWindow.getParentWindow();
          } while (webWindow != webWindow.getTopWindow());
        }
      }
      result = jsEngine.callFunction(htmlPage, jsFunction, window,
          jsThis, jsArgs);
    } catch (ScriptException se) {
      if (se.getCause() instanceof JavaScriptException) {
        JavaScriptException ex = (JavaScriptException) se.getCause();
        if (logger.isLoggable(TreeLogger.INFO)) {
          logger.log(TreeLogger.INFO, "INVOKE: JavaScriptException " + ex
              + ", message: " + ex.getMessage() + " when invoking "
              + methodName);
        }
        return new ExceptionOrReturnValue(true, makeValueFromJsval(jsContext,
            ex.getValue()));
      } else {
        if (logger.isLoggable(TreeLogger.INFO)) {
          logger.log(TreeLogger.INFO, "INVOKE: exception " + se + ", message: "
            + se.getMessage() + " when invoking " + methodName);
        }
        return new ExceptionOrReturnValue(true, makeValueFromJsval(jsContext,
          Undefined.instance));
      }
    } catch (Exception ex) {
      if (logger.isLoggable(TreeLogger.INFO)) {
        logger.log(TreeLogger.INFO, "INVOKE: exception " + ex + ", message: "
            + ex.getMessage() + " when invoking " + methodName);
      }
      return new ExceptionOrReturnValue(true, makeValueFromJsval(jsContext,
          Undefined.instance));
    }
    if (logger.isLoggable(TreeLogger.INFO)) {
      logger.log(TreeLogger.INFO, "INVOKE: result: " + result
          + " of jsFunction: " + jsFunction);
    }
    return new ExceptionOrReturnValue(false, makeValueFromJsval(jsContext,
        result));
  }

  @Override
  public void loadJsni(BrowserChannelClient channel, String jsniString) {
    if (logger.isLoggable(TreeLogger.SPAM)) {
      logger.log(TreeLogger.SPAM, "LOAD_JSNI: " + jsniString);
    }
    ScriptResult scriptResult = htmlPage.executeJavaScript(jsniString);
    if (logger.isLoggable(TreeLogger.INFO)) {
      logger.log(TreeLogger.INFO, "LOAD_JSNI: scriptResult=" + scriptResult);
    }
  }

  /**
   * @param jsContext the Context
   */
  public Value makeValueFromJsval(Context jsContext, Object value) {
    if (value == Undefined.instance) {
      return new Value();
    }
    if (value instanceof JavaObject) {
      Value returnVal = new Value();
      int refId = ((JavaObject) value).getRefId();
      returnVal.setJavaObject(new JavaObjectRef(refId));
      return returnVal;
    }
    if (value instanceof Scriptable) {
      if (value instanceof ScriptableObject) {
        /*
         * HACK: check for native types like NativeString. NativeString is
         * package-protected. What other types do we need to check?
         */
        ScriptableObject scriptableValue = (ScriptableObject) value;
        String className = scriptableValue.getClassName();
        if (className.equals("String")) {
          return new Value(scriptableValue.toString());
        }
      }
      Integer refId = jsObjectToRef.get(value);
      if (refId == null) {
        refId = nextRefId++;
        jsObjectToRef.put((Scriptable) value, refId);
        refToJsObject.put(refId, (Scriptable) value);
      }
      Value returnVal = new Value();
      returnVal.setJsObject(new JsObjectRef(refId));
      return returnVal;
    }
    if (value instanceof ConsString) {
      return new Value(value.toString());
    }
    if (value instanceof Number) {
      return new Value(convertNumberFromJsval(((Number) value)));
    }
    return new Value(value);
  }

  /**
   * HtmlUnit can return non-js types for numerical values e.g. long for TypedArrays. This method
   * mimics the type conversion for numeric values in other devmode plugins so that we will have
   * similar behavior in HtmlUnit as well.
   */
  private Object convertNumberFromJsval(Number number) {
    if (number instanceof Byte || number instanceof Short || number instanceof Integer) {
      return number.intValue();
    }
    return number.doubleValue();
  }

  public void setSessionData(SessionData sessionData) {
    this.sessionData = sessionData;
  }

  /*
   * Returning java objects works. No need to return NativeNumber, NativeString,
   * NativeBoolean, or Undefined.
   */
  Object makeJsvalFromValue(Context jsContext, Value value) {
    switch (value.getType()) {
      case NULL:
        return null;
      case BOOLEAN:
        if (value.getBoolean()) {
          return Boolean.TRUE;
        }
        return Boolean.FALSE;
      case BYTE:
        return new Byte(value.getByte());
      case CHAR:
        return new Character(value.getChar());
      case SHORT:
        return new Short(value.getShort());
      case INT:
        return new Integer(value.getInt());
      case DOUBLE:
        return new Double(value.getDouble());
      case STRING:
        return value.getString();
      case JAVA_OBJECT:
        JavaObjectRef javaRef = value.getJavaObject();
        return JavaObject.getOrCreateJavaObject(javaRef, sessionData, jsContext);
      case JS_OBJECT:
        Scriptable scriptable = refToJsObject.get(value.getJsObject().getRefid());
        assert scriptable != null;
        return scriptable;
      case UNDEFINED:
        return Undefined.instance;
    }
    return null;
  }

  private String getUrlBeforeHash(String currentUrl) {
    int hashIndex = -1;
    if ((hashIndex = currentUrl.indexOf("#")) != -1) {
      currentUrl = currentUrl.substring(0, hashIndex);
    }
    return currentUrl;
  }
}
