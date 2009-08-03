#ifndef JAVAOBJECT_H_
#define JAVAOBJECT_H_
/*
 * Copyright 2008 Google Inc.
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

#include <string>
#include <vector>

#include "mozincludes.h"
#include "Plugin.h"
#include "ScriptableInstance.h"
#include "Debug.h"
#include "HashMap.h"

class JavaObject : public NPObjectWrapper<JavaObject> {
  friend class ScriptableInstance;
private:
  ScriptableInstance* plugin;
  int objectId;
  
  NPIdentifier idID;

public:
  JavaObject(NPP npp) : NPObjectWrapper<JavaObject>(npp),
      plugin(reinterpret_cast<Plugin*>(npp->pdata)->getScriptableInstance()),
      idID(NPN_GetStringIdentifier("id")) {}
  virtual ~JavaObject();
  static JavaObject* create(ScriptableInstance* plugin, int id);
  static bool isInstance(NPObject* obj);
  
  virtual bool enumeration(NPIdentifier** names, uint32_t* count);
  virtual bool hasMethod(NPIdentifier name);
  virtual bool invoke(NPIdentifier name, const NPVariant *args, uint32_t num_args,
      NPVariant *result);
  virtual bool invokeDefault(const NPVariant *args, uint32_t num_args, NPVariant *result);
  virtual bool hasProperty(NPIdentifier name);
  virtual bool getProperty(NPIdentifier name, NPVariant *result);
  virtual bool setProperty(NPIdentifier name, const NPVariant *value);
  
  void setObjectId(int objectId) {
    this->objectId = objectId;
  }

  int getObjectId() const {
    return objectId;
  }
private:
  // Called by a Plugin instance when it is about to be destroyed.
  void disconnectPlugin() {
    plugin = 0;
  }
};

#endif /*JAVAOBJECT_H_*/
