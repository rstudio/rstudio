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
#ifndef JNI_LINUX_JSROOTEDVALUE_H_
#define JNI_LINUX_JSROOTEDVALUE_H_

// Mozilla header files
#include "mozilla-headers.h"

#include "Tracer.h"
#include <stack>

extern "C" JSClass gwt_nativewrapper_class;

/*
 * Holds a root for Javascript objects, so the JS interpreter knows not to
 * garbage-collect the underlying object as long as this object exists.
 * Java code will pass a pointer to this object around (as an int/long) for
 * referring to the underlying Javascript object.
 *
 * There are also convenience routines for manipulating the underlying value.
 * Note that all get* methods assume the type is correct, so the corresponding
 * is* method should be called first if you aren't sure of the type.
 *
 * See http://developer.mozilla.org/en/docs/JS_AddRoot for details.
 *
 * TODO(jat): rewrite this to minimize the number of roots held and to
 *    improve 64-bit compatibility.
 */
class JsRootedValue
{
private:
  // the JavaScript String class
  static JSClass* stringClass;
  
  // Javascript runtime
  static JSRuntime* runtime;

  // stack of Javascript contexts
  static std::stack<JSContext*> contextStack;

  // underlying Javascript value
  jsval         value_;

protected:
  /*
   * Fetch the JavaScript String class.
   * Not inlined to minimize code bloat since it should only be called once.
   */
  void fetchStringClass() const;
  
  /*
   * Make sure we have the JS code to identify String objects installed.
   */
  void ensureStringClass() const {
    if(stringClass) return;
    fetchStringClass();
  }
  
  /*
   * Helper for the various constructors
   */
  void constructorHelper(const char* ctorDesc) {
    Tracer tracer(ctorDesc, this);
    if (!JS_AddNamedRootRT(runtime, &value_, ctorDesc)) {
      tracer.log("JS_AddNamedRootRT failed");
      // TODO(jat): handle errors
    }
  }

  /*
   * Push a new JavaScript execution context.
   */
  static void pushContext(JSContext* context) {
    Tracer tracer("JsRootedValue::pushContext");
    tracer.log("pushed context=%08x", unsigned(context));
    contextStack.push(context);
  }
  
  /*
   * Pop a JavaScript execution context from the context stack.
   */
  static void popContext() {
    Tracer tracer("JsRootedValue::popContext");
    JSContext* context = currentContext();
    contextStack.pop();
    tracer.log("popped context=%08x", unsigned(context));
  }

public:
  /*
   * This is a helper class used to push/pop JSContext values automatically,
   * using RAII.  Simply create a ContextManager object on the stack and it
   * will push the context at creation time and pop it at destruction time,
   * so you don't have to worry about covering all the exit paths from a
   * function.
   */
  class ContextManager {
  public:
    explicit ContextManager(JSContext* context) {
      JsRootedValue::pushContext(context);
    }
    ~ContextManager() {
      JsRootedValue::popContext();
    }
  };

  /*
   * This is a helper class to manage short-lived roots on the stack, using
   * RAII to free the roots when no longer needed.
   */
  class Temp {
  private:
    void* ptr_;
  public:
    explicit Temp(void* ptr) : ptr_(ptr) {
      JS_AddNamedRootRT(runtime, ptr_, "temporary root");
    }
    ~Temp() {
      JS_RemoveRootRT(runtime, ptr_);
    }
  };
  
  /*
   * Copy constructor - make another rooted value that refers to the same
   * JavaScript object (or has the same value if a primitive)
   */
  JsRootedValue(const JsRootedValue& rooted_value) : value_(rooted_value.value_)
  {
    constructorHelper("JsRootedValue copy ctor");
  }
  
  /*
   * Create a value with a given jsval value
   */
  JsRootedValue(jsval value) : value_(value)
  {
    constructorHelper("JsRootedValue jsval ctor");
  }
  
  /*
   * Create a void value
   */
  JsRootedValue() : value_(JSVAL_VOID) {
    constructorHelper("JsRootedValue void ctor");
  }
  
  /*
   * Destroy this object.
   */
  ~JsRootedValue() {
    Tracer tracer("~JsRootedValue", this);
    // ignore error since currently it is not possible to fail
    JS_RemoveRootRT(runtime, &value_);
  }
  
  /*
   * Save a pointer to the JSRuntime if we don't have it yet
   */
  static void ensureRuntime(JSContext* context) {
    if(!runtime) runtime = JS_GetRuntime(context);
  }
  
  /*
   * Return the current JavaScript execution context.
   */
  static JSContext* currentContext() {
    Tracer tracer("JsRootedValue::currentContext");
    if (contextStack.empty()) {
      // TODO(jat): better error handling?
      fprintf(stderr, "JsRootedValue::currentContext - context stack empty\n");
      ::abort();
    }
    JSContext* context = contextStack.top();
    tracer.log("context=%08x", unsigned(context));
    return context;
  }

  /* 
   * Return the underlying JS object
   */
  jsval getValue() const { return value_; }
  
  /*
   * Sets the value of the underlying JS object.
   * 
   * Returns false if an error occurred.
   */
  bool setValue(jsval new_value) {
    value_ = new_value;
    return true;
  }
  
   /*
   * Returns true if the underlying value is of some number type.
   */
  bool isNumber() const {
    return JSVAL_IS_NUMBER(value_);
  }
  /*
   * Returns the underlying value as a double.
   * Result is 0.0 if the underlying value is not a number
   * type.
   */
  double getDouble() const {
    jsdouble return_value=0.0;
    // ignore return value -- if it fails, value will remain 0.0
    JS_ValueToNumber(currentContext(), value_, &return_value); 
    return double(return_value);
  }
  /*
   * Set the underlying value to a double value.
   * 
   * Returns false on failure.
   */
  bool setDouble(double val) {
    jsval js_double;
    if(!JS_NewDoubleValue(currentContext(), jsdouble(val), &js_double)) {
      return false;
    }
    return setValue(js_double);
  }

  /*
   * Returns the underlying value as an integer value.  Note that the result
   * is undefined if isInt() does not return true.
   */
  int getInt() {
    return JSVAL_TO_INT(value_);
  }
  
  /*
   * Set the underlying value to an integer value.
   * 
   * Returns false on failure.
   */
  bool setInt(int val) {
    // check if it fits in 31 bits (ie, top two bits are equal).
    // if not, store it as a double
    if ((val & 0x80000000) != ((val << 1) & 0x80000000)) {
      return setDouble(val);
    } else {
      return setValue(INT_TO_JSVAL(val));
    }
  }
  
  /*
   * Returns true if the underlying value is a boolean.
   */
  bool isBoolean() const {
    return JSVAL_IS_BOOLEAN(value_);
  }
  /*
   * Returns the underlying value as a boolean.
   * Result is undefined if the value is not actually
   * a boolean.
   */
  bool getBoolean() const {
    return value_ != JSVAL_FALSE;
  }
  /*
   * Set the underlying value to a boolean value.
   * 
   * Returns false on failure (impossible?).
   */
  bool setBoolean(bool val) {
    return setValue(val ? JSVAL_TRUE : JSVAL_FALSE);
  }

  /*
   * Returns true if the underlying value is a string.
   */
  bool isInt() const {
    return JSVAL_IS_INT(value_);
  }
  
  /*
   * Returns true if the underlying value is a string.
   */
  bool isString() const {
    return JSVAL_IS_STRING(value_);
  }

  /*
   * Check if the value is a JavaScript String object.
   */
  bool isJavaScriptStringObject() const {
    if (!isObject()) return false;
    ensureStringClass();
    return getObjectClass() == stringClass;
  }
  
  /*
   * Return this value as a string, converting as necessary.
   */
  JSString* asString() const {
    return JS_ValueToString(currentContext(), value_);
  }
  
  /* Returns the string as a JSString pointer.
   * Result is undefined if the value is not actually a string or String object.
   */
  const JSString* getString() const {
    if (JSVAL_IS_STRING(value_)) {
      return JSVAL_TO_STRING(value_);
    }
    return asString();
  }
  /*
   * Returns the string as a zero-terminated array of UTF16 characters.
   * Note that this pointer may become invalid when JS performs GC, so it
   * may only be used without calling other JS functions.  Result is
   * undefined if the value is not actually a string.
   */
  const wchar_t* getStringChars() const {
    return reinterpret_cast<const wchar_t*>(JS_GetStringChars(
        const_cast<JSString*>(getString())));
  }
  
  /*
   * Returns the length of the underlying string.  Result is undefined
   * if the value is not actually a string.
   */
  int getStringLength() const {
    return JS_GetStringLength(const_cast<JSString*>(getString()));
  } 
  
  /*
   * Sets the underlying value, defined by a null-terminated array of UTF16
   * chars.
   * 
   * Returns false on failure.
   */
  bool setString(const wchar_t* utf16) {
    JSString* str = JS_NewUCStringCopyZ(currentContext(),
        reinterpret_cast<const jschar*>(utf16));
    return setValue(STRING_TO_JSVAL(str));
  }
  
  /*
   * Sets the underlying value, defined by a counted array of UTF16 chars.
   * 
   * Returns false on failure.
   */
  bool setString(const wchar_t* utf16, size_t len) {
    JSString* str = JS_NewUCStringCopyN(currentContext(),
        reinterpret_cast<const jschar*>(utf16), len);
    return setValue(STRING_TO_JSVAL(str));
  }
  
  /*
   * Returns true if the underlying value is an object.
   */
  bool isObject() const {
    return JSVAL_IS_OBJECT(value_);
  }
  
  /*
   * Returns the underlying value as an object.
   * Result is undefined if it is not actually an object.
   */
  JSObject* getObject() const {
    return isObject() ? JSVAL_TO_OBJECT(value_) : 0;
  }
  
  /*
   * Returns the class name of the underlying value.
   *
   * Result is undefined if it is not actually an object.
   */
  const JSClass* getObjectClass() const {
    return isObject() ? JS_GET_CLASS(currentContext(), getObject()) : 0;
  }
  
  /*
   * Sets the underlying value to be an object.
   *
   * Returns false on failure.
   */
  bool setObject(JSObject* obj) {
    return setValue(OBJECT_TO_JSVAL(obj));
  }

  /*
   * Returns true if the underlying value is undefined (void).
   */
  bool isUndefined() const {
    return JSVAL_IS_VOID(value_);
  }
  
  /*
   * Sets the underlying value to be undefined (void).
   * 
   * Returns false on failure (impossible?)
   */
  bool setUndefined() {
    return setValue(JSVAL_VOID);
  }
  
  /*
   * Returns true if the underlying value is null.
   */
  bool isNull() const {
    return JSVAL_IS_NULL(value_);
  }
  /*
   * Sets the underlying value to be null.
   * 
   * Returns false on failure (impossible?)
   */
  bool setNull() {
    return setValue(JSVAL_NULL);
  }
};

#endif /*JNI_LINUX_JSROOTEDVALUE_H_*/
