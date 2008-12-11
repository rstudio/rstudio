/*
 * Copyright 2007 Google Inc.
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

/*
 * Defines the JavaScript class gwt_external_class, which interface via JNI
 * to Java objects.
 */

#include <jni.h>
#include "JsRootedValue.h"
#include "gwt-jni.h"
#include "ExternalWrapper.h"
#include "Tracer.h"
#include "JsStringWrap.h"

#ifdef ENABLE_TRACING
extern void PrintJSValue(JSContext* cx, jsval val, char* prefix="");
#else
// Include a null version just to keep from cluttering up call sites.
static inline void PrintJSValue(JSContext* cx, jsval val, char* prefix="") { }
#endif

// note that this does not use the NS_DEFINE_CID macro because it defines
// the variable as const, which by default has internal linkage.  I could
// work around it by putting extern in front of the macro, but that seems
// fragile if the macro changes.
nsCID kGwtExternalCID = GWT_EXTERNAL_FACTORY_CID;

/*
 * definition of JavaScript gwtOnLoad method which maps to the Java
 * gwtOnLoad method.
 */
static JSBool gwtOnLoad(JSContext *cx, JSObject *obj, uintN argc,
    jsval *argv, jsval *rval)
{
  Tracer tracer("gwtOnLoad");
  tracer.log("context=%08x", unsigned(cx));
  JsRootedValue::ContextManager context(cx);
  JsRootedValue::ensureRuntime(cx);
  if (argc < 3) {
    tracer.setFail("less than 3 args");
    return JS_FALSE;
  }

  JSObject* scriptWindow = 0;
  if (argv[0] != JSVAL_NULL && argv[0] != JSVAL_VOID) {
    if (!JS_ValueToObject(cx, argv[0], &scriptWindow)) {
      tracer.setFail("can't get script window object");
      return JS_FALSE;
    }
  }

  JSString* moduleName = 0;
  if (argv[1] != JSVAL_NULL && argv[1] != JSVAL_VOID) {
    moduleName = JS_ValueToString(cx, argv[1]);
  }

  JSString* version = 0;
  if (argv[2] != JSVAL_NULL && argv[2] != JSVAL_VOID) {
    version = JS_ValueToString(cx, argv[2]);
  }

  nsCOMPtr<nsIScriptGlobalObject> scriptGlobal(0);
  if (scriptWindow) {
    nsCOMPtr<nsIXPConnect> xpConnect = do_GetService(nsIXPConnect::GetCID());
    nsCOMPtr<nsIXPConnectWrappedNative> wrappedNative;
    xpConnect->GetWrappedNativeOfJSObject(cx, scriptWindow,
        getter_AddRefs(wrappedNative));
    if (wrappedNative) {
        nsCOMPtr<nsISupports> native;
        wrappedNative->GetNative(getter_AddRefs(native));
        scriptGlobal = do_QueryInterface(native);
    }
  }
  jstring jModuleName(0);
  if (moduleName) {
    jModuleName = savedJNIEnv->NewString(JS_GetStringChars(moduleName),
        JS_GetStringLength(moduleName));
    if (!jModuleName || savedJNIEnv->ExceptionCheck()) {
      tracer.setFail("can't get module name in Java string");
      return JS_FALSE;
    }
    tracer.log("module name=%s", JS_GetStringBytes(moduleName));
  } else {
    tracer.log("null module name");
  }

  jstring jVersion(0);
  if (version) {
    jVersion = savedJNIEnv->NewString(JS_GetStringChars(version),
        JS_GetStringLength(version));
    if (!jVersion || savedJNIEnv->ExceptionCheck()) {
      tracer.setFail("can't get module name in Java string");
      return JS_FALSE;
    }
    tracer.log("version=%s", JS_GetStringBytes(version));
  } else {
    tracer.log("null version");
  }

  jobject externalObject = NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, obj));
  jclass objClass = savedJNIEnv->GetObjectClass(externalObject);
  if (!objClass || savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("can't get LowLevelMoz.ExternalObject class");
    return JS_FALSE;
  }

  jmethodID methodID = savedJNIEnv->GetMethodID(objClass, "gwtOnLoad",
      "(ILjava/lang/String;Ljava/lang/String;)Z");
  if (!methodID || savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("can't get gwtOnLoad method");
    return JS_FALSE;
  }

  tracer.log("scriptGlobal=%08x", unsigned(scriptGlobal.get()));

  jboolean result = savedJNIEnv->CallBooleanMethod(externalObject, methodID,
      NS_REINTERPRET_CAST(jint, scriptGlobal.get()), jModuleName, jVersion);
  if (savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("LowLevelMoz.ExternalObject.gwtOnLoad() threw an exception");
    return JS_FALSE;
  }
  *rval = BOOLEAN_TO_JSVAL((result == JNI_FALSE) ? JS_FALSE : JS_TRUE);
  return JS_TRUE;
}

/*
 * definition of JavaScript initModule method which maps to the Java
 * initModule method.
 */
static JSBool initModule(JSContext *cx, JSObject *obj, uintN argc,
    jsval *argv, jsval *rval)
{
  Tracer tracer("initModule");
  tracer.log("context=%08x", unsigned(cx));
  JsRootedValue::ContextManager context(cx);
  JsRootedValue::ensureRuntime(cx);
  if (argc < 1) {
    tracer.setFail("less than 1 args");
    return JS_FALSE;
  }

  JSString* moduleName = 0;
  if (argv[0] != JSVAL_NULL && argv[0] != JSVAL_VOID) {
    moduleName = JS_ValueToString(cx, argv[0]);
  }

  jstring jModuleName(0);
  if (moduleName) {
    jModuleName = savedJNIEnv->NewString(JS_GetStringChars(moduleName),
        JS_GetStringLength(moduleName));
    if (!jModuleName || savedJNIEnv->ExceptionCheck()) {
      tracer.setFail("can't get module name in Java string");
      return JS_FALSE;
    }
    tracer.log("module name=%s", JS_GetStringBytes(moduleName));
  } else {
    tracer.log("null module name");
  }

  jobject externalObject = NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, obj));
  jclass objClass = savedJNIEnv->GetObjectClass(externalObject);
  if (!objClass || savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("can't get LowLevelMoz.ExternalObject class");
    return JS_FALSE;
  }

  jmethodID methodID = savedJNIEnv->GetMethodID(objClass, "initModule",
      "(Ljava/lang/String;)Z");
  if (!methodID || savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("can't get initModule method");
    return JS_FALSE;
  }

  jboolean result = savedJNIEnv->CallBooleanMethod(externalObject, methodID,
      jModuleName);
  if (savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("LowLevelMoz.ExternalObject.initModule() threw an exception");
    return JS_FALSE;
  }
  *rval = BOOLEAN_TO_JSVAL((result == JNI_FALSE) ? JS_FALSE : JS_TRUE);
  return JS_TRUE;
}

/*=======================================================================*/
/* Implementation of the getProperty, setProperty, and finalize methods  */
/* for the JavaScript class gwt_external_class.                          */
/*=======================================================================*/

static JSBool JS_DLL_CALLBACK gwt_external_getProperty(JSContext *cx,
    JSObject *obj, jsval id, jsval *vp)
{
  Tracer tracer("gwt_external_getProperty");
  JsRootedValue::ContextManager context(cx);
  if (*vp != JSVAL_VOID) {
    // we setup the gwtOnLoad function as a property in GetScriptObject and
    // do not maintain a copy of it anywhere.  So, if there is a cached
    // value for a property we must return it.  As we never redefine any
    // property values, this is safe.  If we were going to keep this code
    // around and add Toby's profiling code we would need to revisit this.
    return JS_TRUE;
  }

  if (!JSVAL_IS_STRING(id)) {
    tracer.setFail("not a string");
    return JS_FALSE;
  }
  JsStringWrap jsStr(cx, id);
  tracer.log("obj=%08x, property=%.*s", obj, jsStr.length(), jsStr.bytes());

// All this is for calling resolveReference which only returns void.  So,
// just replace this code to return void for now.  TODO(jat): revisit when
// merging in Toby's code.
#if 0
  jstring jident = savedJNIEnv->NewString(jsStr.chars(), jsStr.length());
  if (!jident || savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("can't create Java string ");
    return JS_FALSE;
  }
  jobject externalObject = NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, obj));
  jclass objClass = savedJNIEnv->GetObjectClass(externalObject);
  if (!objClass || savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("can't find Java class for JS object");
    return JS_FALSE;
  }

  jmethodID methodID = savedJNIEnv->GetMethodID(objClass, "resolveReference",
      "(Ljava/lang/String;)I");
  if (!methodID || savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("exception getting method for int resolveReference(String)");
    return JS_FALSE;
  }
  int retval = savedJNIEnv->CallIntMethod(externalObject, methodID, jident);
  if (savedJNIEnv->ExceptionCheck()) {
    tracer.setFail("exception calling int resolveReference(String)");
    return JS_FALSE;
  }
  *vp = retval;
#else
  *vp = JSVAL_VOID;
#endif
  return JS_TRUE;
}

static void JS_DLL_CALLBACK gwt_external_finalize(JSContext *cx, JSObject *obj)
{
  // We don't need to push a context if all we do is DeleteGlobalRef
  Tracer tracer("gwt_external_finalize", obj);
  jobject externalObject = NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, obj));
  if (externalObject) {
    savedJNIEnv->DeleteGlobalRef(externalObject);
  }
  JS_FinalizeStub(cx, obj);
}

static JSBool JS_DLL_CALLBACK gwt_external_setProperty(JSContext *cx,
    JSObject *obj, jsval id, jsval *vp)
{
  return JS_FALSE;
}
 
static JSClass gwt_external_class = {
    "gwt_external_class", JSCLASS_HAS_PRIVATE,
    JS_PropertyStub, JS_PropertyStub, gwt_external_getProperty,
    gwt_external_setProperty, JS_EnumerateStub, JS_ResolveStub, JS_ConvertStub,
    gwt_external_finalize,
    JSCLASS_NO_OPTIONAL_MEMBERS
};

NS_IMPL_ISUPPORTS1(ExternalWrapper, nsIScriptObjectOwner)

/*
 * Create a new LowLevelMoz.ExternalObject instance and a JavaScript object
 * that refers to it, mapping the gwtOnLoad member function mapping to
 * a Javascript method.
 */
NS_IMETHODIMP ExternalWrapper::GetScriptObject(nsIScriptContext *aContext,
    void** aScriptObject)
{
  Tracer tracer("ExternalWrapper::GetScriptObject"); 
  if (!aScriptObject) {
    tracer.setFail("null script object pointer");
    return NS_ERROR_INVALID_POINTER;
  }
  if (!jsWindowExternalObject) {
    JSContext* cx = NS_REINTERPRET_CAST(JSContext*,
        aContext->GetNativeContext());
    if (!cx) {
      tracer.setFail("can't get JSContext");
      return NS_ERROR_UNEXPECTED;
    }
    JsRootedValue::ContextManager context(cx);
    *aScriptObject = 0;

    nsIScriptGlobalObject* globalObject = aContext->GetGlobalObject();
    nsCOMPtr<nsIDOMWindow> domWindow(do_QueryInterface(globalObject));
    if (!domWindow) {
      tracer.setFail("can't get DOM window");
      return NS_ERROR_UNEXPECTED;
    }
    nsCOMPtr<nsIDOMWindow> topWindow;
    domWindow->GetTop(getter_AddRefs(topWindow));
    if (!topWindow) {
      tracer.setFail("Can't get top window");
      return NS_ERROR_UNEXPECTED;
    }

    tracer.log("savedJNIEnv=%08x, llClass=%08x", unsigned(savedJNIEnv),
        lowLevelMozClass);
    
    jmethodID methodID = savedJNIEnv->GetStaticMethodID(lowLevelMozClass,
        "createExternalObjectForDOMWindow",
        "(I)Lcom/google/gwt/dev/shell/moz/LowLevelMoz$ExternalObject;");
    if (!methodID || savedJNIEnv->ExceptionCheck()) {
      tracer.setFail("Can't get createExternalObjectForDOMWindow method");
      return NS_ERROR_UNEXPECTED;
    }

    jobject externalObject = savedJNIEnv->CallStaticObjectMethod(
        lowLevelMozClass, methodID, NS_REINTERPRET_CAST(jint, topWindow.get()));
    if (!externalObject || savedJNIEnv->ExceptionCheck()) {
      tracer.setFail("createExternalObjectForDOMWindow failed");
      return NS_ERROR_UNEXPECTED;
    }
    externalObject = savedJNIEnv->NewGlobalRef(externalObject);
    if (!externalObject || savedJNIEnv->ExceptionCheck()) {
      tracer.setFail("can't get GlobalRef for external object");
      return NS_ERROR_UNEXPECTED;
    }
    JSObject* newObj = JS_NewObject(cx, &gwt_external_class, 0,
        globalObject->GetGlobalJSObject());
    if (!newObj) {
      tracer.setFail("can't create new gwt_external_class object");
      return NS_ERROR_OUT_OF_MEMORY;
    }
    if (!JS_SetPrivate(cx, newObj, externalObject)) {
      savedJNIEnv->DeleteGlobalRef(externalObject);
      tracer.setFail("can't store external object private reference");
      return NS_ERROR_UNEXPECTED;
    }
  
    if (!JS_DefineFunction(cx, newObj, "gwtOnLoad", gwtOnLoad, 3,
        JSPROP_ENUMERATE | JSPROP_READONLY | JSPROP_PERMANENT)) {
      tracer.setFail("can't define gwtOnLoad function on JavaScript object");
      return NS_ERROR_UNEXPECTED;
    }
    if (!JS_DefineFunction(cx, newObj, "initModule", initModule, 1,
        JSPROP_ENUMERATE | JSPROP_READONLY | JSPROP_PERMANENT)) {
      tracer.setFail("can't define initModule function on JavaScript object");
      return NS_ERROR_UNEXPECTED;
    }
    jsWindowExternalObject = newObj;
  }

  *aScriptObject = jsWindowExternalObject;
  return NS_OK;
}

NS_IMETHODIMP ExternalWrapper::SetScriptObject(void* aScriptObject)
{
    jsWindowExternalObject = aScriptObject;
    return NS_OK;
}

NS_IMPL_ISUPPORTS1(nsRpExternalFactory, nsIFactory)

NS_IMETHODIMP nsRpExternalFactory::CreateInstance(nsISupports *aOuter,
    const nsIID& aIID, void** aResult)
{
  Tracer tracer("nsRpExternalFactory::CreateInstance");
  if (!aResult) {
    tracer.setFail("null pointer for return value");
    return NS_ERROR_INVALID_POINTER;
  }
  *aResult  = NULL;

  if (aOuter) {
    tracer.setFail("aOuter is not null");
    return NS_ERROR_NO_AGGREGATION;
  }

  nsISupports* object = new ExternalWrapper();
  if (!object) {
    tracer.setFail("can't create a new ExternalWrapper");
    return NS_ERROR_OUT_OF_MEMORY;
  }

  nsresult result = object->QueryInterface(aIID, aResult);
  if (!*aResult || NS_FAILED(result)) {
    tracer.setFail("ExternalWrapper::QueryInterface failed");
    delete object;
  }
  return result;
}

NS_IMETHODIMP nsRpExternalFactory::LockFactory(PRBool lock)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}
