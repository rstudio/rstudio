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
#include "DispWrapper.h"
#include "FunctionObject.h"

using namespace KJS;

const ClassInfo DispWrapper::info = {"DispWrapper", 0, 0, 0};

JSValue *DispWrapper::getter(ExecState* exec, JSObject* thisObj,
    const Identifier& propertyName, const PropertySlot& slot)
{
  TRACE("ENTER DispWrapper::getter");
  if (propertyName.ustring() == "toString") {
    return new ToStringFunction();
  }
  if (thisObj->classInfo() == &DispWrapper::info) {
    DispWrapper* dispWrap = static_cast<DispWrapper*>(thisObj);
    jobject dispObj = dispWrap->dispObj;
    jstring jpropName = gEnv->NewString((const jchar*)propertyName.data(),
        propertyName.size());
    if (!jpropName || gEnv->ExceptionCheck()) {
      gEnv->ExceptionClear();
      return jsUndefined();
    }
    jint result = gEnv->CallIntMethod(dispObj, gGetFieldMeth, jpropName);
    if (!result || gEnv->ExceptionCheck()) {
      gEnv->ExceptionClear();
      return jsUndefined();
    }
    TRACE("SUCCESS DispWrapper::getter");
    return (JSValue*)result;
  }
  return jsUndefined();
}

/*
 * Construct a JavaScript wrapper around a WebKitDispatchAdapter object.
 * 
 * dispObj a GlobalRef to the Java object to wrap
 */
DispWrapper::DispWrapper(jobject dispObj): dispObj(dispObj) { }

/*
 * Free GlobalRef on the underlying WebKitDispatchAdapter object.
 */
DispWrapper::~DispWrapper() {
  gEnv->DeleteGlobalRef(dispObj);
}

bool DispWrapper::getOwnPropertySlot(ExecState *exec,
    const Identifier& propertyName, PropertySlot& slot)
{
  slot.setCustom(this, getter);
  return true;
}

/*
 * Tells JavaScript that we can store properties into this object.
 * Note that we do not verify the property exists, so we 
 * 
 * exec JS execution state
 * proeprtyName the property to be updated
 */
bool DispWrapper::canPut(ExecState *exec, const Identifier &propertyName)
    const
{
  return true;
}

/*
 * Store a value into a field on a Java object.
 * 
 * exec JS execution state
 * propertyName the name of the field
 * value the JS value to store in the field
 * attr unused attributes of the property
 * 
 * Silently catches any Java exceptions in WebKitDispatchAdapter.setField(),
 * including undefined fields, so updates to undefined fields in Java objects
 * will be silently ignored.  TODO: is that the desired behavior?
 */
void DispWrapper::put(ExecState *exec, const Identifier &propertyName,
    JSValue *value, int attr)
{
  TRACE("ENTER DispWrapper::put");
  jstring jpropName = gEnv->NewString((const jchar*)propertyName.data(),
      propertyName.size());
  if (!jpropName || gEnv->ExceptionCheck()) {
    gEnv->ExceptionClear();
    return;
  }
  gwtGCProtect(value); // Java will take ownership of this value
  gEnv->CallVoidMethod(dispObj, gSetFieldMeth, jpropName, (jint)value);
  if (gEnv->ExceptionCheck()) {
    gEnv->ExceptionClear();
    return;
  }
  TRACE("SUCCESS DispWrapper::put");
}

/*
 * Prevent JavaScript from deleting fields from a Java object.
 */
bool DispWrapper::deleteProperty(ExecState *exec,
    const Identifier &propertyName)
{
  return false;
}

/*
 * Return a string representation of the Java object.
 * Calls obj.toString() on the Java object.
 * 
 * exec JS execution state
 * hint unused
 * 
 * Returns undefined if toString() failed, or the string returned (which may
 * be null).
 */
JSValue *DispWrapper::defaultValue(ExecState *exec, JSType hint) const {
  jstring result = (jstring)gEnv->CallObjectMethod(dispObj, gToStringMeth);
  if (gEnv->ExceptionCheck()) {
    return jsUndefined();
  } else if (!result) {
    return jsNull();
  } else {
    JStringWrap jresult(gEnv, result);
    return jsString(UString((const UChar*)jresult.jstr(), jresult.length()));
  }
}

/*
 * Tell JavaScript that this object does not implement call functionality.
 */
bool DispWrapper::implementsCall() const {
  return false;
}

/*
 * Prevent JavaScript from calling the WebKitDispatchAdapter object
 * as if it were a function.
 */
JSValue *DispWrapper::callAsFunction(ExecState *, JSObject *, const List &) {
  return jsUndefined();
}
