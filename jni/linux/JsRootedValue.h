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

extern JSClass gwt_nativewrapper_class;

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
 * TODO(jat): handle unboxing Javascript objects like Boolean/etc.
 * TODO(jat): rewrite this to minimize the number of roots held and to
 *    improve 64-bit compatibility.
 */
class JsRootedValue
{
private:
  // the JavaScript String class
  static JSClass* stringClass;
  
  // Javascript context
  JSContext*    context_;
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
   * Make this value rooted if the current value is a pointer type
   * 
   * Returns false if an error occurred.
   */
  bool setRoot() {
    Tracer tracer("JsRootedValue::setRoot", this);
    tracer.log("context=%08x, value=%08x", context_, value_);
    if(JSVAL_IS_GCTHING(value_)) {
      tracer.log("value is GC - %s", JS_GetTypeName(context_,
          JS_TypeOfValue(context_, value_)));
      bool returnVal = JS_AddRoot(context_, &value_);
      if (!returnVal) {
        tracer.log("*** JS_AddRoot failed");
      }
      return returnVal;
    }
    return true;
  }
  /*
   * Remove this value from the roots list if it was there
   * 
   * Returns false if an error occurred (note that currently JS_RemoveRoot
   * is documented to always return true, so this is not currently possible).
   */
  bool removeRoot() {
    Tracer tracer("JsRootedValue::removeRoot", this);
    tracer.log("context=%08x, value=%08x", context_, value_);
    if(JSVAL_IS_GCTHING(value_)) {
      tracer.log("value is GC - %s", JS_GetTypeName(context_,
          JS_TypeOfValue(context_, value_)));
      bool returnVal = JS_RemoveRoot(context_, &value_);
      if (!returnVal) {
        tracer.log("*** JS_RemoveRoot failed");
      }
      return returnVal;
    }
    return true;
  }
  
public:
  /*
   * Copy constructor - make another rooted value that refers to the same
   * JavaScript object (or has the same value if a primitive)
   */
  JsRootedValue(const JsRootedValue& rooted_value)
      : context_(rooted_value.context_), value_(rooted_value.value_)
  {
    Tracer tracer("JsRootedValue copy constr", this);
    tracer.log("other jsRootedVal=%08x", reinterpret_cast<unsigned>(&rooted_value));
    if (!JS_AddRoot(context_, &value_)) {
      tracer.log("JsRootedValue copy constructor: JS_AddRoot failed");
      // TODO(jat): handle errors
    }
  }
  
  JsRootedValue(JSContext* context, jsval value) : context_(context),
      value_(value)
  {
    Tracer tracer("JsRootedValue jsval constr", this);
    tracer.log("jsval=%08x", value);
    if (!JS_AddRoot(context_, &value_)) {
      tracer.log("JsRootedValue jsval constructor: JS_AddRoot failed");
      // TODO(jat): handle errors
    }
  }
  
  /*
   * Create a void value - safe since no errors can occur
   */
  JsRootedValue(JSContext* context) : context_(context), value_(JSVAL_VOID) {  }
  
  /*
   * Destroy this object.
   */
  virtual ~JsRootedValue() {
    Tracer tracer("~JsRootedValue", this);
    // ignore error since currently it is not possible to fail
    JS_RemoveRoot(context_, &value_);
  }

  /*
   * Return the JSContext* pointer.
   */
  JSContext* getContext() const { return context_; }
  
  /*
   * Return the global object for this value's context.
   */
  JSObject* getGlobalObject() const { return JS_GetGlobalObject(context_); }
  
  /* 
   * Return the underlying JS object
   */
  jsval getValue() const { return value_; }
  
  /*
   * Sets the value of the underlying JS object and its context.
   * 
   * Returns false if an error occurred.
   */
  bool setContextValue(JSContext* new_context, jsval new_value) {
    context_ = new_context;
    value_ = new_value;
    return true;
  }
  
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
    // ignore return value -- if it failes, value will remain 0.0
    JS_ValueToNumber(context_, value_, &return_value); 
    return double(return_value);
  }
  /*
   * Set the underlying value to a double value.
   * 
   * Returns false on failure.
   */
  bool setDouble(double val) {
    jsval js_double;
    if(!JS_NewDoubleValue(context_, jsdouble(val), &js_double)) {
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
    return setValue(INT_TO_JSVAL(val));
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
    return JS_ValueToString(context_, value_);
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
   * Sets the underlying value, defined by a null-terminated array of UTF16 chars.
   * 
   * Returns false on failure.
   */
  bool setString(const wchar_t* utf16) {
    JSString* str = JS_NewUCStringCopyZ(context_, reinterpret_cast<const jschar*>(utf16));
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
   * Result is undefined if it is not actually an object.
   */
  const JSClass* getObjectClass() const {
    return isObject() ? JS_GET_CLASS(context_, getObject()) : 0;
  }
  
  /*
   * Sets the underlying value to be an object.
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
