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
import com.google.gwt.dev.shell.BrowserChannel.SessionHandler;
import com.google.gwt.dev.shell.BrowserChannel.Value;
import com.google.gwt.dev.shell.BrowserChannel.Value.ValueType;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;
import com.gargoylesoftware.htmlunit.javascript.host.Window;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.JavaScriptException;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
import net.sourceforge.htmlunit.corejs.javascript.Undefined;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handle session tasks for HtmlUnit. TODO (amitmanjhi): refactor
 * SessionHandler.
 */
public class HtmlUnitSessionHandler extends SessionHandler {

  private class ToStringMethod extends ScriptableObject implements Function {

    private static final int EXPECTED_NUM_ARGS = 0;
    private static final long serialVersionUID = 1592865718416163348L;

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
  private Set<Integer> javaObjectsToFree;

  private JavaScriptEngine jsEngine;

  private IdentityHashMap<Scriptable, Integer> jsObjectToRef;
  private final PrintWriterTreeLogger logger = new PrintWriterTreeLogger();

  private int nextRefId = 1;
  private Map<Integer, Scriptable> refToJsObject;
  private SessionData sessionData;

  private final ToStringMethod toStringMethod = new ToStringMethod();

  private final Window window;

  HtmlUnitSessionHandler(Window window, JavaScriptEngine jsEngine) {
    this.window = window;
    logger.setMaxDetail(TreeLogger.ERROR);
    this.jsEngine = jsEngine;
    htmlPage = (HtmlPage) this.window.getWebWindow().getEnclosedPage();
    logger.log(TreeLogger.INFO, "jsEngine = " + jsEngine + ", HtmlPage = "
        + htmlPage);

    jsObjectToRef = new IdentityHashMap<Scriptable, Integer>();
    javaObjectsToFree = new HashSet<Integer>();
    nextRefId = 1;
    refToJsObject = new HashMap<Integer, Scriptable>();

    // related to JavaObject cache.
    javaObjectCache = new HashMap<Integer, JavaObject>();
  }

  @Override
  public void freeValue(BrowserChannel channel, int[] ids) {
    for (int id : ids) {
      Scriptable scriptable = refToJsObject.remove(id);
      if (scriptable != null) {
        jsObjectToRef.remove(scriptable);
      }
    }
  }

  public HtmlPage getHtmlPage() {
    return htmlPage;
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
  public ExceptionOrReturnValue getProperty(BrowserChannel channel, int refId,
      int dispId) {
    throw new UnsupportedOperationException(
        "getProperty should not be called on the client-side");
  }

  public Object getToStringTearOff(Context jsContext) {
    return toStringMethod;
  }

  public String getUserAgent() {
    return "HtmlUnit-"
        + jsEngine.getWebClient().getBrowserVersion().getUserAgent();
  }

  @Override
  public ExceptionOrReturnValue invoke(BrowserChannel channel, Value thisObj,
      int dispId, Value[] args) {
    throw new UnsupportedOperationException(
        "should not be called on the client side");
  }

  public ExceptionOrReturnValue invoke(BrowserChannel channel, Value thisObj,
      String methodName, Value[] args) {
    logger.log(TreeLogger.DEBUG, "INVOKE: thisObj: " + thisObj
        + ", methodName: " + methodName + ", args: " + args);
    /*
     * 1. lookup functions by name. 2. Find context and scope. 3. Convert
     * thisObject to ScriptableObject 4. Convert args 5. Get return value
     */
    Context jsContext = Context.getCurrentContext();
    ScriptableObject jsThis;
    if (thisObj.getType() == ValueType.NULL) {
      jsThis = window;
    } else {
      jsThis = (ScriptableObject) makeJsvalFromValue(jsContext, thisObj);
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
    logger.log(TreeLogger.SPAM, "INVOKE: jsFunction: " + jsFunction);

    Object jsArgs[] = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      jsArgs[i] = makeJsvalFromValue(jsContext, args[i]);
    }
    Object result = null;
    try {
      if (args.length == 1
          && methodName.indexOf(REPLACE_METHOD_SIGNATURE) != -1) {
        // getUrl() is not visible
        String currentUrl = window.jsxGet_location().toString();
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
      result = jsEngine.callFunction(htmlPage, jsFunction, jsContext, window,
          jsThis, jsArgs);     
    } catch (JavaScriptException ex) {
      logger.log(TreeLogger.INFO, "INVOKE: JavaScriptException " + ex
          + ", message: " + ex.getMessage() + " when invoking " + methodName);
      return new ExceptionOrReturnValue(true, makeValueFromJsval(jsContext,
          ex.getValue()));
    } catch (Exception ex) {
      logger.log(TreeLogger.INFO, "INVOKE: exception " + ex + ", message: "
          + ex.getMessage() + " when invoking " + methodName);
      return new ExceptionOrReturnValue(true, makeValueFromJsval(jsContext,
          Undefined.instance));
    }
    logger.log(TreeLogger.INFO, "INVOKE: result: " + result
        + " of jsFunction: " + jsFunction);
    return new ExceptionOrReturnValue(false, makeValueFromJsval(jsContext,
        result));
  }

  @SuppressWarnings("unused")
  public ExceptionOrReturnValue invokeSpecial(BrowserChannel channel,
      SpecialDispatchId specialDispatchId, Value[] args) {
    throw new UnsupportedOperationException(
        "InvokeSpecial must not be called on the client side");
  }

  public void loadJsni(BrowserChannel channel, String jsniString) {
    logger.log(TreeLogger.SPAM, "LOAD_JSNI: " + jsniString);
    ScriptResult scriptResult = htmlPage.executeJavaScript(jsniString);
    logger.log(TreeLogger.INFO, "LOAD_JSNI: scriptResult=" + scriptResult);
  }

  @Override
  public TreeLogger loadModule(BrowserChannel channel, String moduleName,
      String userAgent, String url, String tabKey, String sessionKey,
      byte[] userAgentIcon) {
    throw new UnsupportedOperationException("loadModule must not be called");
  }

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
    return new Value(value);
  }

  // TODO: check synchronization and multi-threading
  public void sendFreeValues(BrowserChannel channel) {
    int size = javaObjectsToFree.size();
    if (size == 0) {
      return;
    }
    int ids[] = new int[size];
    int index = 0;
    for (int id : javaObjectsToFree) {
      ids[index++] = id;
    }
    if (ServerMethods.freeJava(channel, this, ids)) {
      javaObjectsToFree.clear();
    }
  }

  @Override
  public ExceptionOrReturnValue setProperty(BrowserChannel channel, int refId,
      int dispId, Value newValue) {
    throw new UnsupportedOperationException(
        "setProperty should not be called on the client-side");
  }

  public void setSessionData(SessionData sessionData) {
    this.sessionData = sessionData;
  }

  @Override
  public void unloadModule(BrowserChannel channel, String moduleName) {
    throw new UnsupportedOperationException("unloadModule must not be called");
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
      case FLOAT:
        return new Float(value.getFloat());
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
