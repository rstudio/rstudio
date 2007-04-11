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
#include "FunctionObject.h"
#include <kjs/array_object.h>

using namespace KJS;

const ClassInfo FunctionObject::info = {"Function", 0, 0, 0};

class CallFunction : public FunctionObject {
public:
  CallFunction(): FunctionObject("call") {
  }

  virtual JSValue *callAsFunction(ExecState *exec, JSObject *thisObj,
        const List &args)
  {
    // Copied from FunctionProtoFunc::callAsFunction()
    JSValue *thisArg = args[0];
    JSObject *func = thisObj;

    if (!func->implementsCall()) {
      return throwError(exec, TypeError);
    }

    JSObject *callThis;
    if (thisArg->isUndefinedOrNull()) {
      callThis = exec->dynamicInterpreter()->globalObject();
    } else {
      callThis = thisArg->toObject(exec);
    }

    return func->call(exec, callThis, args.copyTail());
  }
};

class ApplyFunction  : public FunctionObject {
public:
  ApplyFunction(): FunctionObject("apply") {
  }

  virtual JSValue *callAsFunction(ExecState *exec, JSObject *thisObj,
      const List &args)
  {
    // Copied from FunctionProtoFunc::callAsFunction()
    JSObject *func = thisObj;
    if (!func->implementsCall()) {
      return throwError(exec, TypeError);
    }

    JSValue *thisArg = args[0];
    JSObject *applyThis;
    if (thisArg->isUndefinedOrNull()) {
      applyThis = exec->dynamicInterpreter()->globalObject();
    } else {
      applyThis = thisArg->toObject(exec);
    }

    JSValue *argArray = args[1];
    List applyArgs;
    if (!argArray->isUndefinedOrNull()) {
      if (!argArray->isObject(&ArrayInstance::info)) {
        return throwError(exec, TypeError);
      }

      JSObject *argArrayObj = static_cast<JSObject *>(argArray);
      unsigned int length = argArrayObj->get(exec, lengthPropertyName)
          ->toUInt32(exec);
      for (unsigned int i = 0; i < length; ++i) {
        applyArgs.append(argArrayObj->get(exec,i));
      }
    }
    return func->call(exec, applyThis, applyArgs);
  }
};


static UString makeFunctionString(const UString& name) {
  return "\nfunction " + name + "() {\n    [native code]\n}\n";
}

JSValue *FunctionObject::getter(ExecState* exec, JSObject* obj,
    const Identifier& propertyName, const PropertySlot& slot)
{
  if (propertyName.ustring() == "toString") {
    return new ToStringFunction();
  } else if (propertyName.ustring() == "call") {
    return new CallFunction();
  } else if (propertyName.ustring() == "apply") {
    return new ApplyFunction();
  }
  return jsUndefined();
}

FunctionObject::FunctionObject(const UString& name): name(name) {
}

bool FunctionObject::getOwnPropertySlot(ExecState *exec,
    const Identifier& propertyName, PropertySlot& slot)
{
  if (propertyName.ustring() == "toString") {
    slot.setCustom(this, getter);
    return true;
  }
  if (propertyName.ustring() == "call") {
    slot.setCustom(this, getter);
    return true;
  }
  if (propertyName.ustring() == "apply") {
    slot.setCustom(this, getter);
    return true;
  }
  return false;
}

bool FunctionObject::canPut(ExecState *exec, const Identifier &propertyName)
    const
{
  return false;
}

void FunctionObject::put(ExecState *exec, const Identifier &propertyName,
    JSValue *value, int attr)
{
}

bool FunctionObject::deleteProperty(ExecState *exec,
    const Identifier &propertyName)
{
  return false;
}

JSValue *FunctionObject::defaultValue(ExecState *exec, JSType hint) const {
  return jsString(makeFunctionString(name));
}

bool FunctionObject::implementsCall() const {
  return true;
}

// ToStringFunction
ToStringFunction::ToStringFunction(): FunctionObject("toString") {
}

JSValue *ToStringFunction::callAsFunction(ExecState *exec, JSObject *thisObj,
    const List &args)
{
  if (!thisObj) {
    return throwError(exec, TypeError);
  }
  return jsString(thisObj->toString(exec));
}
