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
#ifndef FUNCTION_OBJECT_H
#define FUNCTION_OBJECT_H

#include "gwt-webkit.h"
#include <kjs/object.h>

class FunctionObject : public KJS::JSObject {
protected:
  FunctionObject(const KJS::UString& name);

public:
  const KJS::ClassInfo *classInfo() const { return &info; }

  // shared implementations of JSObject methods
  virtual bool getOwnPropertySlot(KJS::ExecState*, const KJS::Identifier&,
      KJS::PropertySlot&);
  virtual bool canPut(KJS::ExecState*, const KJS::Identifier&) const;
  virtual void put(KJS::ExecState*, const KJS::Identifier&, KJS::JSValue*, int);
  virtual bool deleteProperty(KJS::ExecState*, const KJS::Identifier&);
  virtual KJS::JSValue *defaultValue(KJS::ExecState*, KJS::JSType) const;
  virtual bool implementsCall() const;
  
  // subclasses must implement
  virtual KJS::JSValue *callAsFunction(KJS::ExecState*, KJS::JSObject*,
      const KJS::List&) = 0;
    
  static const KJS::ClassInfo info;

private:
  static KJS::JSValue* getter(KJS::ExecState*, KJS::JSObject*,
      const KJS::Identifier&, const KJS::PropertySlot&);

private:
  KJS::UString name;
};

class ToStringFunction : public FunctionObject {
public:
  ToStringFunction();
  virtual KJS::JSValue *callAsFunction(KJS::ExecState*, KJS::JSObject*,
      const KJS::List&);
};

#endif
