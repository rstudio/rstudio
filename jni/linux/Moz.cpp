// Copyright 2005 Google Inc.
// All Rights Reserved.

// Mozilla-specific hosted-mode methods

// Mozilla header files
#include "nsIServiceManagerUtils.h"
#include "nsComponentManagerUtils.h"
#include "nsICategoryManager.h"
#include "nsIScriptNameSpaceManager.h"
#include "nsIScriptObjectOwner.h"
#include "nsIScriptGlobalObject.h"
#include "nsIScriptContext.h"
#include "nsIDOMWindow.h"
#include "nsIXPConnect.h"
#include "nsCOMPtr.h"
#include "nsAutoPtr.h"

#include <jni.h>

static JNIEnv* gEnv = 0;
static jclass gClass = 0;

//#define FILETRACE
//#define JAVATRACE
#if defined(FILETRACE) && defined(JAVATRACE)
#define TRACE(s) filetrace(s),javatrace(s)
#elif defined(FILETRACE)
#define TRACE(s) filetrace(s)
#elif defined(JAVATRACE)
#define TRACE(s) javatrace(s)
#else
#define TRACE(s) ((void)0)
#endif

#ifdef FILETRACE
static FILE* gout = 0;
static void filetrace(const char* s)
{
    fprintf(gout, s);
    fprintf(gout, "\n");
    fflush(gout);
}
#endif // FILETRACE

#ifdef JAVATRACE
static jmethodID gTraceMethod = 0;
static void javatrace(const char* s)
{
    if (!gEnv->ExceptionCheck())
    {
        jstring out = gEnv->NewStringUTF(s);
        if (!gEnv->ExceptionCheck())
            gEnv->CallStaticVoidMethod(gClass, gTraceMethod, out);
        else
            gEnv->ExceptionClear();
    }
}
#endif // JAVATRACE

static bool InitGlobals(JNIEnv* env, jclass llClass)
{
    if (gEnv)
        return true;

#ifdef FILETRACE
    gout = fopen("gwt-ll.log", "w");
    filetrace("LOG STARTED");
#endif // FILETRACE

    gClass = static_cast<jclass>(env->NewGlobalRef(llClass));
    if (!gClass || env->ExceptionCheck())
        return false;

#ifdef JAVATRACE
    gTraceMethod = env->GetStaticMethodID(gClass, "trace", "(Ljava/lang/String;)V");
    if (!gTraceMethod || env->ExceptionCheck())
        return false;
#endif // JAVATRACE

    gEnv = env;
    return true;
}

struct JStringWrap
{
    JStringWrap(JNIEnv* env, jstring str): env(env), s(str), p(0), jp(0) { }
    ~JStringWrap() { if (p) env->ReleaseStringUTFChars(s, p); if (jp) env->ReleaseStringChars(s, jp); }
    const char* str() { if (!p) p = env->GetStringUTFChars(s, 0); return p; }
    const jchar* jstr() { if (!jp) jp = env->GetStringChars(s, 0); return jp; }
private:
    JNIEnv* env;
    jstring s;
    const char* p;
    const jchar* jp;
};

static void hextrace(int val) {
    char buf[20];
    const char* hex = "0123456789ABCDEF";
    for (int i = 7; i >= 0; --i) {
        buf[i] = hex[val & 0xF];
        val >>= 4;
    }
    buf[8] = 0;
    TRACE(buf);
}

static JSBool gwt_invoke(JSContext *cx, JSObject *obj, uintN argc, jsval *argv, jsval *rval)
{
    TRACE("ENTER gwt_invoke");

    // I kid you not; this is how XPConnect gets their function object so they can
    // multiplex dispatch the call from a common site.  See XPCDispObject.cpp(466)
    //
    // I now have a secondary confirmation that this trick is legit.  brandon@mozilla.org
    // writes:
    //
    // argv[-2] is part of the JS API, unabstracted.  Just as argv[0] is the
    // first argument (if argc != 0), argv[-1] is the |this| parameter (equal
    // to OBJECT_TO_JSVAL(obj) in a native method with the standard |obj|
    // second formal parameter name), and argv[-2] is the callee object, tagged
    // as a jsval.
    if (JS_TypeOfValue(cx, argv[-2]) != JSTYPE_FUNCTION)
        return TRACE("FAIL gwt_invoke: JSTYPE_FUNCTION"), JS_FALSE;

    JSObject* funObj = JSVAL_TO_OBJECT(argv[-2]);
    jsval jsCleanupObj;

    // Pull the wrapper object out of the funObj's reserved slot
    if (!JS_GetReservedSlot(cx, funObj, 0, &jsCleanupObj))
        return TRACE("FAIL gwt_invoke: JS_GetReservedSlot"), JS_FALSE;

    JSObject* cleanupObj = JSVAL_TO_OBJECT(jsCleanupObj);
    if (!cleanupObj)
        return TRACE("FAIL gwt_invoke: cleanupObj"), JS_FALSE;

    // Get dispMeth global ref out of the wrapper object
    jobject dispMeth = NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, cleanupObj));
    if (!dispMeth)
        return TRACE("FAIL gwt_invoke: dispMeth"), JS_FALSE;

    jclass dispClass = gEnv->GetObjectClass(dispMeth);
    if (!dispClass || gEnv->ExceptionCheck())
        return TRACE("FAIL gwt_invoke: GetObjectClass"), JS_FALSE;

    jmethodID invokeID = gEnv->GetMethodID(dispClass, "invoke", "(I[I)I");
    if (!invokeID || gEnv->ExceptionCheck())
        return TRACE("FAIL gwt_invoke: GetMethodID"), JS_FALSE;

    jintArray jsargs = gEnv->NewIntArray(argc);
    if (!jsargs || gEnv->ExceptionCheck())
        return TRACE("FAIL gwt_invoke: NewIntArray"), JS_FALSE;

    gEnv->SetIntArrayRegion(jsargs, 0, argc, (jint*)argv);
    if (gEnv->ExceptionCheck())
        return TRACE("FAIL gwt_invoke: SetIntArrayRegion"), JS_FALSE;

    *rval = gEnv->CallIntMethod(dispMeth, invokeID, argv[-1], jsargs);
    if (gEnv->ExceptionCheck())
        return TRACE("FAIL gwt_invoke: java exception is active"), JS_FALSE;

    if (JS_IsExceptionPending(cx))
        return TRACE("FAIL gwt_invoke: js exception is active"), JS_FALSE;

    TRACE("SUCCESS gwt_invoke");
    return JS_TRUE;
}

static JSBool getJavaPropertyStats(JSContext *cx, JSObject *obj, jsval id, jclass& dispClass, jobject& dispObj, jstring& jident)
{
    if (!JSVAL_IS_STRING(id))
        return JS_FALSE;

    jident = gEnv->NewString(JS_GetStringChars(JSVAL_TO_STRING(id)), JS_GetStringLength(JSVAL_TO_STRING(id)));
    if (!jident || gEnv->ExceptionCheck())
        return JS_FALSE;

    dispObj = NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, obj));
    if (!dispObj)
        return JS_FALSE;

    dispClass = gEnv->GetObjectClass(dispObj);
    if (gEnv->ExceptionCheck())
        return JS_FALSE;

    return JS_TRUE;
}

static JSBool JS_DLL_CALLBACK gwt_nativewrapper_setProperty(JSContext *cx, JSObject *obj, jsval id, jsval *vp)
{
    TRACE("ENTER gwt_nativewrapper_setProperty");

    jclass dispClass;
    jobject dispObj;
    jstring ident;
    if (!getJavaPropertyStats(cx,obj,id,dispClass,dispObj,ident))
        return JS_FALSE;

    jmethodID setFieldMeth = gEnv->GetMethodID(dispClass, "setField", "(Ljava/lang/String;I)V");
    if (!setFieldMeth || gEnv->ExceptionCheck())
        return JS_FALSE;

    gEnv->CallVoidMethod(dispObj, setFieldMeth, ident, *vp);
    if (gEnv->ExceptionCheck())
        return JS_FALSE;

    TRACE("SUCCESS gwt_nativewrapper_setProperty");
    return JS_TRUE;
}

static JSBool JS_DLL_CALLBACK gwt_nativewrapper_getProperty(JSContext *cx, JSObject *obj, jsval id, jsval *vp)
{
    TRACE("ENTER gwt_nativewrapper_getProperty");

    if (*vp != JSVAL_VOID)
        return TRACE("SUCCESS, already defined"), JS_TRUE;

    jclass dispClass;
    jobject dispObj;
    jstring ident;
    if (!getJavaPropertyStats(cx,obj,id,dispClass,dispObj,ident))
        return JS_FALSE;

    jmethodID getFieldMeth = gEnv->GetMethodID(dispClass, "getField", "(Ljava/lang/String;)I");
    if (!getFieldMeth || gEnv->ExceptionCheck())
        return JS_FALSE;

    *vp = gEnv->CallIntMethod(dispObj, getFieldMeth, ident);
    if (gEnv->ExceptionCheck())
        return JS_FALSE;

    TRACE("SUCCESS gwt_nativewrapper_getProperty");
    return JS_TRUE;
}

static void JS_DLL_CALLBACK gwt_nativewrapper_finalize(JSContext *cx, JSObject *obj)
{
    jobject dispObj = NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, obj));
    if (dispObj)
        gEnv->DeleteGlobalRef(dispObj);
}

static JSClass gwt_functionwrapper_class = {
    "gwt_functionwrapper_class", JSCLASS_HAS_PRIVATE,
    JS_PropertyStub, JS_PropertyStub, JS_PropertyStub, JS_PropertyStub,
    JS_EnumerateStub, JS_ResolveStub, JS_ConvertStub, gwt_nativewrapper_finalize,
    JSCLASS_NO_OPTIONAL_MEMBERS
};

static JSClass gwt_nativewrapper_class = {
    "gwt_nativewrapper_class", JSCLASS_HAS_PRIVATE,
    JS_PropertyStub, JS_PropertyStub, gwt_nativewrapper_getProperty, gwt_nativewrapper_setProperty,
    JS_EnumerateStub, JS_ResolveStub, JS_ConvertStub, gwt_nativewrapper_finalize,
    JSCLASS_NO_OPTIONAL_MEMBERS
};

static JSBool gwtOnLoad(JSContext *cx, JSObject *obj, uintN argc, jsval *argv, jsval *rval)
{
    TRACE("ENTER gwtOnLoad");

    if (argc < 2)
        return JS_FALSE;

    JSObject* scriptWindow = 0;
    if (argv[0] != JSVAL_NULL && argv[0] != JSVAL_VOID) {
        if (!JS_ValueToObject(cx, argv[0], &scriptWindow))
            return JS_FALSE;
    }

    JSString* moduleName = 0;
    if (argv[1] != JSVAL_NULL && argv[1] != JSVAL_VOID) {
        moduleName = JS_ValueToString(cx, argv[1]);
    }

    nsCOMPtr<nsIScriptGlobalObject> scriptGlobal(0);
    if (scriptWindow) {
        nsCOMPtr<nsIXPConnect> xpConnect = do_GetService(nsIXPConnect::GetCID());
        nsCOMPtr<nsIXPConnectWrappedNative> wrappedNative;
        xpConnect->GetWrappedNativeOfJSObject(cx, scriptWindow, getter_AddRefs(wrappedNative));
        if (wrappedNative) {
            nsCOMPtr<nsISupports> native;
            wrappedNative->GetNative(getter_AddRefs(native));
            scriptGlobal = do_QueryInterface(native);
        }
    }

    jstring jModuleName(0);
    if (moduleName) {
        jModuleName = gEnv->NewString(JS_GetStringChars(moduleName), JS_GetStringLength(moduleName));
        if (!jModuleName || gEnv->ExceptionCheck())
            return JS_FALSE;
    }
    
    jobject externalObject = NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, obj));
    jclass objClass = gEnv->GetObjectClass(externalObject);
    if (!objClass || gEnv->ExceptionCheck())
        return JS_FALSE;
    
    jmethodID methodID = gEnv->GetMethodID(objClass, "gwtOnLoad", "(ILjava/lang/String;)Z");
    if (!methodID || gEnv->ExceptionCheck())
        return JS_FALSE;
    
    jboolean result = gEnv->CallBooleanMethod(externalObject, methodID, NS_REINTERPRET_CAST(jint, scriptGlobal.get()), jModuleName);
    if (gEnv->ExceptionCheck())
        return JS_FALSE;

    *rval = BOOLEAN_TO_JSVAL((result == JNI_FALSE) ? JS_FALSE : JS_TRUE);
    TRACE("SUCCESS gwtOnLoad");
    return JS_TRUE;
}

class ExternalWrapper : public nsIScriptObjectOwner
{
public:
    NS_DECL_ISUPPORTS
    NS_IMETHOD GetScriptObject(nsIScriptContext *aContext, void** aScriptObject);
    NS_IMETHOD SetScriptObject(void* aScriptObject);
    ExternalWrapper(): mScriptObject(0) { }
private:
    ~ExternalWrapper() { }
    void *mScriptObject;
};

NS_IMPL_ISUPPORTS1(ExternalWrapper, nsIScriptObjectOwner)

static JSBool JS_DLL_CALLBACK gwt_external_setProperty(JSContext *cx, JSObject *obj, jsval id, jsval *vp)
{
    return JS_FALSE;
}

static JSBool JS_DLL_CALLBACK gwt_external_getProperty(JSContext *cx, JSObject *obj, jsval id, jsval *vp)
{
    TRACE("ENTER gwt_external_getProperty");

    if (*vp != JSVAL_VOID)
        return TRACE("SUCCESS, already defined"), JS_TRUE;

    if (!JSVAL_IS_STRING(id))
        return TRACE("FAIL 1"), JS_FALSE;

    jstring jident = gEnv->NewString(JS_GetStringChars(JSVAL_TO_STRING(id)), JS_GetStringLength(JSVAL_TO_STRING(id)));
    if (!jident || gEnv->ExceptionCheck())
        return TRACE("FAIL 2"), JS_FALSE;

    jobject externalObject = NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, obj));
    jclass objClass = gEnv->GetObjectClass(externalObject);
    if (!objClass || gEnv->ExceptionCheck())
        return TRACE("FAIL 4"), JS_FALSE;

    jmethodID methodID = gEnv->GetMethodID(objClass, "resolveReference", "(Ljava/lang/String;)I");
    if (!methodID || gEnv->ExceptionCheck())
        return TRACE("FAIL 5"), JS_FALSE;
    int retval = gEnv->CallIntMethod(externalObject, methodID, jident);
    if (gEnv->ExceptionCheck())
        return TRACE("FAIL 6"), JS_FALSE;
    *vp = retval;

    TRACE("SUCCESS gwt_external_getProperty");
    return JS_TRUE;
}

static void JS_DLL_CALLBACK gwt_external_finalize(JSContext *cx, JSObject *obj)
{
    jobject externalObject = NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, obj));
    if (externalObject)
        gEnv->DeleteGlobalRef(externalObject);
    JS_FinalizeStub(cx,obj);
}

static JSClass gwt_external_class = {
    "gwt_external_class", JSCLASS_HAS_PRIVATE,
    JS_PropertyStub, JS_PropertyStub, gwt_external_getProperty, gwt_external_setProperty,
    JS_EnumerateStub, JS_ResolveStub, JS_ConvertStub, gwt_external_finalize,
    JSCLASS_NO_OPTIONAL_MEMBERS
};

class nsJSObjectLocker : public nsISupports
{
public:
    NS_DECL_ISUPPORTS
    nsJSObjectLocker(JSContext* cx, jsval val): mCx(cx), mVal(val) { if (mCx && mVal) JSVAL_LOCK(mCx,mVal); }

    JSContext* const mCx;
    const jsval mVal;

    // Major hack; compare other object's vtable ptrs to this one to do crude RTTI
    static nsJSObjectLocker sJSObjectLocker;

private:
    ~nsJSObjectLocker() { if (mCx && mVal) JSVAL_UNLOCK(mCx,mVal); }
};
NS_IMPL_ISUPPORTS0(nsJSObjectLocker)

// Major hack; compare other object's vtable ptrs to this one to do crude RTTI
nsJSObjectLocker nsJSObjectLocker::sJSObjectLocker(0,0);

NS_IMETHODIMP ExternalWrapper::GetScriptObject(nsIScriptContext *aContext, void** aScriptObject)
{
    TRACE("ENTER ExternalWrapper::GetScriptObject");

    if (!aScriptObject)
        return TRACE("FAIL 0"), NS_ERROR_INVALID_POINTER;

    if (!mScriptObject)
    {
        *aScriptObject = 0;

        nsIScriptGlobalObject* globalObject = aContext->GetGlobalObject();
        nsCOMPtr<nsIDOMWindow> domWindow(do_QueryInterface(globalObject));
        if (!domWindow)
            return TRACE("FAIL 1"), NS_ERROR_UNEXPECTED;
        
        nsCOMPtr<nsIDOMWindow> topWindow;
        domWindow->GetTop(getter_AddRefs(topWindow));
        if (!topWindow)
            return TRACE("FAIL 2"), NS_ERROR_UNEXPECTED;

        jmethodID methodID = gEnv->GetStaticMethodID(gClass, "createExternalObjectForDOMWindow", "(I)Lcom/google/gwt/dev/shell/moz/LowLevelMoz$ExternalObject;");
        if (!methodID || gEnv->ExceptionCheck())
            return TRACE("FAIL 3"), NS_ERROR_UNEXPECTED;

        jobject externalObject = gEnv->CallStaticObjectMethod(gClass, methodID, NS_REINTERPRET_CAST(jint, topWindow.get()));
        if (!externalObject || gEnv->ExceptionCheck())
            return TRACE("FAIL 4"), NS_ERROR_UNEXPECTED;
        externalObject = gEnv->NewGlobalRef(externalObject);
        if (!externalObject || gEnv->ExceptionCheck())
            return TRACE("FAIL 5"), NS_ERROR_UNEXPECTED;

        JSContext* cx = NS_REINTERPRET_CAST(JSContext*,aContext->GetNativeContext());
        if (!cx)
            return TRACE("FAIL 6"), NS_ERROR_UNEXPECTED;
        JSObject* newObj = JS_NewObject(cx, &gwt_external_class, 0, globalObject->GetGlobalJSObject());
        if (!newObj)
            return TRACE("FAIL 7"), NS_ERROR_OUT_OF_MEMORY;
        if (!JS_SetPrivate(cx, newObj, externalObject)) {
            gEnv->DeleteGlobalRef(externalObject);
            return TRACE("FAIL 8"), NS_ERROR_UNEXPECTED;
        }
        if (!JS_DefineFunction(cx, newObj, "gwtOnLoad", gwtOnLoad, 3,
                JSPROP_ENUMERATE | JSPROP_READONLY | JSPROP_PERMANENT))
            return TRACE("FAIL 9"), NS_ERROR_UNEXPECTED;

        mScriptObject = newObj;
    }

    *aScriptObject = mScriptObject;
    TRACE("SUCCESS ExternalWrapper::GetScriptObject");
    return NS_OK;
}

NS_IMETHODIMP ExternalWrapper::SetScriptObject(void* aScriptObject)
{
    mScriptObject = aScriptObject;
    return NS_OK;
}

class nsRpExternalFactory : public nsIFactory
{
public:
    NS_DECL_ISUPPORTS
    NS_DECL_NSIFACTORY
    nsRpExternalFactory() { }
private:
    ~nsRpExternalFactory() { }
};

NS_IMPL_ISUPPORTS1(nsRpExternalFactory, nsIFactory)

NS_IMETHODIMP nsRpExternalFactory::CreateInstance(nsISupports *aOuter, const nsIID & aIID, void** aResult)
{
    TRACE("ENTER nsRpExternalFactory::CreateInstance");

    if (!aResult)
        return NS_ERROR_INVALID_POINTER;

    *aResult  = NULL;

    if (aOuter)
        return NS_ERROR_NO_AGGREGATION;

    nsISupports* object = new ExternalWrapper();
    if (!object)
        return NS_ERROR_OUT_OF_MEMORY;
        
    nsresult result = object->QueryInterface(aIID, aResult);
    if (!*aResult || NS_FAILED(result))
        delete object;
    else
        TRACE("SUCCESS nsRpExternalFactory::CreateInstance");
    return result;
}

NS_IMETHODIMP nsRpExternalFactory::LockFactory(PRBool lock)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

#define GWT_EXTERNAL_FACTORY_CID \
{ 0xF56E23F8, 0x5D06, 0x47F9, \
{ 0x88, 0x5A, 0xD9, 0xCA, 0xC3, 0x38, 0x41, 0x7F } }
#define GWT_EXTERNAL_CONTRACTID "@com.google/GWT/external;1"

static NS_DEFINE_CID(kGwtExternalCID, GWT_EXTERNAL_FACTORY_CID);

extern "C" {

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _coerceTo31Bits
 * Signature: (II[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1coerceTo31Bits
  (JNIEnv* env, jclass, jint scriptObjInt, jint v, jintArray rval)
{
    nsIScriptGlobalObject* scriptObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
    nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
    if (!scriptContext)
       return JNI_FALSE;
    JSContext* cx = (JSContext*)scriptContext->GetNativeContext();

    jint r;
    if (!JS_ValueToECMAInt32(cx, v, &r))
        return JNI_FALSE;
    env->SetIntArrayRegion(rval, 0, 1, &r);
    if (env->ExceptionCheck())
       return JNI_FALSE;
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _coerceToBoolean
 * Signature: (II[Z)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1coerceToBoolean
  (JNIEnv* env, jclass, jint scriptObjInt, jint v, jbooleanArray rval)
{
    nsIScriptGlobalObject* scriptObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
    nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
    if (!scriptContext)
       return JNI_FALSE;
    JSContext* cx = (JSContext*)scriptContext->GetNativeContext();

    JSBool r;
    if (!JS_ValueToBoolean(cx, v, &r))
        return JNI_FALSE;
    jboolean jr = (r == JS_FALSE) ? JNI_FALSE : JNI_TRUE;
    env->SetBooleanArrayRegion(rval, 0, 1, &jr);
    if (env->ExceptionCheck())
       return JNI_FALSE;
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _coerceToDouble
 * Signature: (II[D)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1coerceToDouble
  (JNIEnv* env, jclass, jint scriptObjInt, jint v, jdoubleArray rval)
{
    nsIScriptGlobalObject* scriptObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
    nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
    if (!scriptContext)
       return JNI_FALSE;
    JSContext* cx = (JSContext*)scriptContext->GetNativeContext();

    jdouble r;
    if (!JS_ValueToNumber(cx, v, &r))
        return JNI_FALSE;
    env->SetDoubleArrayRegion(rval, 0, 1, &r);
    if (env->ExceptionCheck())
       return JNI_FALSE;
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _coerceToString
 * Signature: (II[Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1coerceToString
  (JNIEnv* env, jclass, jint scriptObjInt, jint v, jobjectArray rval)
{
    jstring jr(0);
    if (v != JSVAL_NULL && v != JSVAL_VOID) {
        nsIScriptGlobalObject* scriptObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
        nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
        if (!scriptContext)
            return JNI_FALSE;
        JSContext* cx = (JSContext*)scriptContext->GetNativeContext();

        JSString* str = JS_ValueToString(cx, v);
        if (!str)
            return JNI_FALSE;
        jr = env->NewString(JS_GetStringChars(str), JS_GetStringLength(str));
        if (env->ExceptionCheck())
            return JNI_FALSE;
    }
    env->SetObjectArrayElement(rval, 0, jr);
    if (env->ExceptionCheck())
       return JNI_FALSE;
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _convert31Bits
 * Signature: (II[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1convert31Bits
  (JNIEnv* env, jclass, int scriptObjInt, jint v, jintArray rval)
{
    jint r = INT_TO_JSVAL(v);
    env->SetIntArrayRegion(rval, 0, 1, &r);
    if (env->ExceptionCheck())
       return JNI_FALSE;
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _convertBoolean
 * Signature: (IZ[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1convertBoolean
  (JNIEnv* env, jclass, int scriptObjInt, jboolean v, jintArray rval)
{
    jint r = BOOLEAN_TO_JSVAL((v == JNI_FALSE) ? JS_FALSE : JS_TRUE);
    env->SetIntArrayRegion(rval, 0, 1, &r);
    if (env->ExceptionCheck())
       return JNI_FALSE;
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _convertDouble
 * Signature: (ID[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1convertDouble
  (JNIEnv* env, jclass, int scriptObjInt, jdouble v, jintArray rval)
{
    nsIScriptGlobalObject* scriptObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
    nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
    if (!scriptContext)
       return JNI_FALSE;
    JSContext* cx = (JSContext*)scriptContext->GetNativeContext();

    jsval rv;
    if (!JS_NewDoubleValue(cx, jsdouble(v), &rv))
       return JNI_FALSE;
    jint r = rv;
    env->SetIntArrayRegion(rval, 0, 1, &r);
    if (env->ExceptionCheck())
       return JNI_FALSE;
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _convertString
 * Signature: (ILjava/lang/String;[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1convertString
  (JNIEnv* env, jclass, int scriptObjInt, jstring v, jintArray rval)
{
    jint r = 0;
    if (v) {
        JStringWrap jv(env, v);
        if (!jv.jstr())
            return JNI_FALSE;

        nsIScriptGlobalObject* scriptObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
        nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
        if (!scriptContext)
           return JNI_FALSE;
        JSContext* cx = (JSContext*)scriptContext->GetNativeContext();

        JSString* str = JS_NewUCStringCopyZ(cx, jv.jstr());
        if (!str)
           return JNI_FALSE;

        r = STRING_TO_JSVAL(str);
    }

    env->SetIntArrayRegion(rval, 0, 1, &r);
    if (env->ExceptionCheck())
       return JNI_FALSE;
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _executeScript
 * Signature: (ILjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1executeScript
  (JNIEnv* env, jclass llClass, jint scriptObject, jstring code)
{
    JStringWrap jcode(env, code);
    if (!jcode.jstr())
        return JNI_FALSE;

    nsIScriptGlobalObject* globalObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObject);
    nsCOMPtr<nsIScriptContext> scriptContext(globalObject->GetContext());
    nsXPIDLString scriptString;
    scriptString = jcode.jstr();

    nsXPIDLString aRetValue;
    PRBool aIsUndefined;
    if (NS_FAILED(scriptContext->EvaluateString(scriptString, globalObject->GetGlobalJSObject(),
            0, __FILE__, __LINE__, 0, aRetValue, &aIsUndefined)))
        return JNI_FALSE;
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _executeScriptWithInfo
 * Signature: (ILjava/lang/String;Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1executeScriptWithInfo
  (JNIEnv* env, jclass llClass, jint scriptObject, jstring code, jstring file, jint line)
{
    JStringWrap jcode(env, code);
    if (!jcode.jstr())
        return JNI_FALSE;

    JStringWrap jfile(env, file);
    if (!jfile.str())
        return JNI_FALSE;

    nsIScriptGlobalObject* globalObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObject);
    nsCOMPtr<nsIScriptContext> scriptContext(globalObject->GetContext());
    nsXPIDLString scriptString;
    scriptString = jcode.jstr();

    nsXPIDLString aRetValue;
    PRBool aIsUndefined;
    if (NS_FAILED(scriptContext->EvaluateString(scriptString, globalObject->GetGlobalJSObject(),
            0, jfile.str(), line, 0, aRetValue, &aIsUndefined)))
        return JNI_FALSE;
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _invoke
 * Signature: (ILjava/lang/String;II[I[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1invoke
  (JNIEnv* env, jclass, int scriptObjInt, jstring methodName, jint jsthisval, jint jsargc, jintArray jsargs, jintArray rval)
{
    TRACE("ENTER Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1invoke");

    JStringWrap methodStr(env,methodName);
    if (!methodStr.str())
       return TRACE("FAIL 1"), JNI_FALSE;

    nsIScriptGlobalObject* scriptObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
    nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
    if (!scriptContext)
       return TRACE("FAIL 2"), JNI_FALSE;
    JSContext* cx = (JSContext*)scriptContext->GetNativeContext();
    JSObject* scriptWindow = (JSObject*)scriptObject->GetGlobalJSObject();

    jsval fval;
    if (!JS_GetProperty(cx, scriptWindow, methodStr.str(), &fval))
       return TRACE("FAIL 3"), JNI_FALSE;
    if (!JS_ValueToFunction(cx, fval))
       return TRACE("FAIL 4"), JNI_FALSE;

    nsAutoArrayPtr<jint> jsargvals(new jint[jsargc]);
    if (!jsargvals)
       return TRACE("FAIL 5"), JNI_FALSE;

    env->GetIntArrayRegion(jsargs, 0, jsargc, jsargvals);
    if (env->ExceptionCheck())
       return TRACE("FAIL 6"), JNI_FALSE;

    jsval jsrval;
    JSObject* jsthis = (jsthisval == JSVAL_NULL) ? scriptWindow : JSVAL_TO_OBJECT(jsthisval);
    if (!JS_CallFunctionValue(cx, jsthis, fval, jsargc, (jsval*)jsargvals.get(), &jsrval))
       return TRACE("FAIL 7"), JNI_FALSE;

    env->SetIntArrayRegion(rval, 0, 1, (jint*)&jsrval);
    if (env->ExceptionCheck())
       return TRACE("FAIL 8"), JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1invoke");
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _isWrappedDispatch
 * Signature: (II[Z)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1isWrappedDispatch
  (JNIEnv* env, jclass, jint scriptObjInt, jint jsobjval, jbooleanArray rval)
{
    TRACE("ENTER Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1isWrappedDispatch");

    nsIScriptGlobalObject* scriptObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
    nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
    if (!scriptContext)
       return JNI_FALSE;
    JSContext* cx = (JSContext*)scriptContext->GetNativeContext();

    jboolean r = JNI_FALSE;
    if (JSVAL_IS_OBJECT(jsobjval))
    {
        JSObject* jsobj = JSVAL_TO_OBJECT(jsobjval);
        if (JS_InstanceOf(cx, jsobj, &gwt_nativewrapper_class, 0))
            r = JNI_TRUE;
    }

    env->SetBooleanArrayRegion(rval, 0, 1, &r);
    if (env->ExceptionCheck())
       return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1isWrappedDispatch");
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _raiseJavaScriptException
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1raiseJavaScriptException
  (JNIEnv* env, jclass, jint scriptObjInt, jint jsarg)
{
    TRACE("ENTER Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1raiseJavaScriptException");

    nsIScriptGlobalObject* scriptObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
    nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
    if (!scriptContext)
       return JNI_FALSE;
    JSContext* cx = (JSContext*)scriptContext->GetNativeContext();
    JS_SetPendingException(cx, jsarg);

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1raiseJavaScriptException");
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _registerExternalFactoryHandler
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1registerExternalFactoryHandler
  (JNIEnv* env, jclass llClass)
{
    if (!InitGlobals(env, llClass))
        return JNI_FALSE;

    TRACE("ENTER Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1registerExternalFactoryHandler");

    // Register "window.external" as our own class
    if (NS_FAILED(nsComponentManager::RegisterFactory(
            kGwtExternalCID, "externalFactory", GWT_EXTERNAL_CONTRACTID,
            new nsRpExternalFactory(), PR_TRUE)))
        return JNI_FALSE;

    nsCOMPtr<nsICategoryManager> categoryManager = do_GetService(NS_CATEGORYMANAGER_CONTRACTID);
    if (!categoryManager)
        return JNI_FALSE;

    nsXPIDLCString previous;
    if (NS_FAILED(categoryManager->AddCategoryEntry(JAVASCRIPT_GLOBAL_PROPERTY_CATEGORY,
                            "external", GWT_EXTERNAL_CONTRACTID,
                            PR_TRUE, PR_TRUE, getter_Copies(previous))))
        return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1registerExternalFactoryHandler");
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _unwrapDispatch
 * Signature: (II[Lcom/google/gwt/dev/shell/moz/LowLevelMoz/DispatchObject;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1unwrapDispatch
  (JNIEnv* env, jclass, jint scriptObjInt, jint jsobjval, jobjectArray rval)
{
    TRACE("ENTER Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1unwrapDispatch");

    nsIScriptGlobalObject* scriptObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
    nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
    if (!scriptContext)
       return JNI_FALSE;
    JSContext* cx = (JSContext*)scriptContext->GetNativeContext();

    if (!JSVAL_IS_OBJECT(jsobjval))
        return JNI_FALSE;

    JSObject* jsobj = JSVAL_TO_OBJECT(jsobjval);
    if (!JS_InstanceOf(cx, jsobj, &gwt_nativewrapper_class, 0))
        return JNI_FALSE;

    jobject dispObj = NS_REINTERPRET_CAST(jobject, JS_GetPrivate(cx, jsobj));
    if (!dispObj)
        return JNI_FALSE;

    env->SetObjectArrayElement(rval, 0, dispObj);
    if (env->ExceptionCheck())
       return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1unwrapDispatch");
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _unwrapJSObject
 * Signature: (I[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1unwrapJSObject
  (JNIEnv* env, jclass, jint nsISupportsPtr, jintArray rval)
{
    TRACE("ENTER Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1unwrapJSObject");

    // MAJOR HACK: check the vtable ptr against a known "good" as a very crude RTTI
    long *vt1 = NS_REINTERPRET_CAST(long*,NS_STATIC_CAST(nsISupports*, &nsJSObjectLocker::sJSObjectLocker));
    long *vt2 = NS_REINTERPRET_CAST(long*,nsISupportsPtr);
    if (*vt1 != *vt2)
        return JNI_FALSE;

    // probably safe
    nsJSObjectLocker* jsObjectLocker = NS_STATIC_CAST(nsJSObjectLocker*, NS_REINTERPRET_CAST(nsISupports*,nsISupportsPtr));
    jsval r = jsObjectLocker->mVal;
    if (!JSVAL_IS_OBJECT(r))
        return JNI_FALSE;

    env->SetIntArrayRegion(rval, 0, 1, (jint*)&r);
    if (env->ExceptionCheck())
        return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1unwrapJSObject");
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _wrapDispatch
 * Signature: (ILcom/google/gwt/dev/shell/moz/LowLevelMoz/DispatchObject;[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1wrapDispatch
  (JNIEnv* env, jclass, jint scriptObjInt, jobject dispObj, jintArray rval)
{
    TRACE("ENTER Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1wrapDispatch");

    nsIScriptGlobalObject* scriptObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
    nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
    if (!scriptContext)
       return JNI_FALSE;

    JSContext* cx = (JSContext*)scriptContext->GetNativeContext();
    JSObject* scriptWindow = (JSObject*)scriptObject->GetGlobalJSObject();

    JSObject* newObj = JS_NewObject(cx, &gwt_nativewrapper_class, 0, scriptWindow);
    if (!newObj)
        return JNI_FALSE;

    jobject dispObjRef = env->NewGlobalRef(dispObj);
    if (!dispObjRef || env->ExceptionCheck())
        return JNI_FALSE;

    if (!JS_SetPrivate(cx, newObj, dispObjRef))
    {
        env->DeleteGlobalRef(dispObjRef);
        return JNI_FALSE;
    }

    // forcibly setup a "toString" method to override the default
    jclass dispClass = env->GetObjectClass(dispObj);
    if (env->ExceptionCheck())
        return JS_FALSE;

    jmethodID getFieldMeth = env->GetMethodID(dispClass, "getField", "(Ljava/lang/String;)I");
    if (!getFieldMeth || env->ExceptionCheck())
        return JS_FALSE;

    jstring ident = env->NewStringUTF("@java.lang.Object::toString()");
    if (!ident || env->ExceptionCheck())
        return JS_FALSE;

    jsval toStringFunc = env->CallIntMethod(dispObj, getFieldMeth, ident);
    if (env->ExceptionCheck())
        return JS_FALSE;

    if (!JS_DefineProperty(cx, newObj, "toString", toStringFunc, JS_PropertyStub, JS_PropertyStub, JSPROP_READONLY | JSPROP_PERMANENT))
        return JNI_FALSE;

    env->SetIntArrayRegion(rval, 0, 1, (jint*)&newObj);
    if (env->ExceptionCheck())
        return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1wrapDispatch");
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _wrapFunction
 * Signature: (ILjava/lang/String;Lcom/google/gwt/dev/shell/moz/LowLevelMoz/DispatchMethod;[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1wrapFunction
  (JNIEnv* env, jclass, jint scriptObjInt, jstring name, jobject dispMeth, jintArray rval)
{
    TRACE("ENTER Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1wrapFunction");

    nsIScriptGlobalObject* scriptObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
    nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
    if (!scriptContext)
        return JNI_FALSE;
    JSContext* cx = (JSContext*)scriptContext->GetNativeContext();
    JSObject* scriptWindow = (JSObject*)scriptObject->GetGlobalJSObject();

    JStringWrap nameStr(env, name);
    if (!nameStr.str())
        return JNI_FALSE;

    JSFunction* function = JS_NewFunction(cx, gwt_invoke, 0, JSFUN_LAMBDA, 0, nameStr.str());
    if (!function)
        return JNI_FALSE;

    JSObject* funObj = JS_GetFunctionObject(function);
    if (!funObj)
        return JNI_FALSE;

    // Create a wrapper object to hold and clean up dispMeth
    JSObject* cleanupObj = JS_NewObject(cx, &gwt_functionwrapper_class, 0, scriptWindow);
    if (!cleanupObj)
        return JNI_FALSE;

    jobject dispMethRef = env->NewGlobalRef(dispMeth);
    if (!dispMethRef || env->ExceptionCheck())
        return JNI_FALSE;

    // Store our global ref in the wrapper object
    if (!JS_SetPrivate(cx, cleanupObj, dispMethRef))
    {
        env->DeleteGlobalRef(dispMethRef);
        return JNI_FALSE;
    }

    // Store the wrapper object in funObj's reserved slot
    if(!JS_SetReservedSlot(cx, funObj, 0, OBJECT_TO_JSVAL(cleanupObj)))
        return JS_FALSE;

    env->SetIntArrayRegion(rval, 0, 1, (jint*)&funObj);
    if (env->ExceptionCheck())
        return JNI_FALSE;

    TRACE("SUCCESS Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1wrapFunction");
    return JNI_TRUE;
}

/*
 * Class:     com_google_gwt_dev_shell_moz_LowLevelMoz
 * Method:    _wrapJSObject
 * Signature: (II[I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1wrapJSObject
  (JNIEnv* env, jclass, jint scriptObjInt, jint jsobjval, jintArray rval)
{
    TRACE("ENTER Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1wrapJSObject");

    if (!JSVAL_IS_OBJECT(jsobjval))
        return JNI_FALSE;

    nsIScriptGlobalObject* scriptObject = NS_REINTERPRET_CAST(nsIScriptGlobalObject*, scriptObjInt);
    nsCOMPtr<nsIScriptContext> scriptContext(scriptObject->GetContext());
    if (!scriptContext)
        return JNI_FALSE;
    JSContext* cx = (JSContext*)scriptContext->GetNativeContext();

    nsISupports* objLocker = new nsJSObjectLocker(cx, jsobjval);
    if (!objLocker)
        return JNI_FALSE;

    jint r = (jint)objLocker;
    env->SetIntArrayRegion(rval, 0, 1, &r);
    if (env->ExceptionCheck())
        return JNI_FALSE;

    objLocker->AddRef();
    TRACE("SUCCESS Java_com_google_gwt_dev_shell_moz_LowLevelMoz__1wrapJSObject");
    return JNI_TRUE;
}

} // extern "C"
