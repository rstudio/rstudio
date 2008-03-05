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
#ifndef DISP_WRAPPER_H
#define DISP_WRAPPER_H

#include "gwt-webkit.h"
#include <kjs/object.h>

/*
 * This class wraps Java WebKitDispatchAdapter objects.
 */
class DispWrapper : public KJS::JSObject {
public:
  // dispObj MUST be a global ref
    DispWrapper(jobject dispObj);
  virtual ~DispWrapper();
  jobject getDispObj();

public:
  // implementations of JSObject methods
  const KJS::ClassInfo *classInfo() const { return &info; }

  virtual bool getOwnPropertySlot(KJS::ExecState*, const KJS::Identifier&,
      KJS::PropertySlot&);
  virtual bool canPut(KJS::ExecState*, const KJS::Identifier&) const;
  virtual void put(KJS::ExecState*, const KJS::Identifier&, KJS::JSValue*, int);
  virtual bool deleteProperty(KJS::ExecState*, const KJS::Identifier&);
  virtual KJS::JSValue *defaultValue(KJS::ExecState*, KJS::JSType) const;
  virtual bool implementsCall() const;
  virtual KJS::JSValue *callAsFunction(KJS::ExecState*, KJS::JSObject*,
      const KJS::List&);
  
  static const KJS::ClassInfo info;

private:
  static KJS::JSValue* getter(KJS::ExecState*, KJS::JSObject*,
      const KJS::Identifier&, const KJS::PropertySlot&);

private:
  jobject dispObj;
};

inline jobject DispWrapper::getDispObj() {
  return dispObj;
}

#endif
