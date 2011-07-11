#ifndef H_LocalObjectTable
#define H_LocalObjectTable
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

#include <map>

#include "Debug.h"

#include "mozincludes.h"
#include "NPVariantUtil.h"

class LocalObjectTable {
private:
  /* The host expects Value objects to have int's for JSO id's, hence the
   * dual mapping.  ObjectMap is for host communication (Value.getJsObjectId)
   * and the IdMap is for browser communication (NPObject to ID).
   */
  typedef std::map<int, NPObject*> ObjectMap;
  typedef std::map<NPObject*,int> IdMap;

  NPP npp;

  int nextId;
  ObjectMap objects;
  IdMap ids;
  bool dontFree;

  bool jsIdentitySafe;

  const NPIdentifier gwtId;

  void setFree(int id) {
    NPObject *obj = getById(id);
    if(!obj) {
      Debug::log(Debug::Error) << "setFree(id=" << id << "): object not in table"
        << Debug::flush;
      return;
    }
    ids.erase(obj);
    objects.erase(id);
  }

public:
  LocalObjectTable(NPP npp, bool jsIdentitySafe):
    nextId(0), dontFree(false), jsIdentitySafe(jsIdentitySafe),
    gwtId(NPN_GetStringIdentifier("__gwt_ObjectId")) {
  }

  virtual ~LocalObjectTable();

  /**
   * Add a new object, which must not be in the table, and return a new id for it.
   */
  int add(NPObject* obj) {
    int id = nextId++;
    set(id, obj);

    if (!jsIdentitySafe) {
      NPVariant idVariant;
      Debug::log(Debug::Debugging) << "LocalObjectTable::set(): setting expando("
          << id << ")" << Debug::flush;
      INT32_TO_NPVARIANT(id,idVariant);
      if (!NPN_SetProperty(npp, obj, gwtId, &idVariant)) {
        Debug::log(Debug::Error) << "Setting GWT id on object failed" << Debug::flush;
      }
    }

    return id;
  }

  void set(int id, NPObject* obj) {
    Debug::log(Debug::Debugging) << "LocalObjectTable::set(id=" << id << ",obj=" << (void*)obj
        << ")" << Debug::flush;
    if (!jsIdentitySafe) {
      ObjectMap::iterator it;
      it = objects.find(id);
      if( it != objects.end() ) {
        if (it->second != obj) {
          //The JSO has changed and we need to update the map, releasing
          //the old and remembering the new object.
          ids.erase(it->second);
          NPN_ReleaseObject(it->second);
          NPN_RetainObject(obj);
        } else {
          //do nothing; object exists and is already mapped
          return;
        }
      } else {
        //New insertion, retain the object in the table
        NPN_RetainObject(obj);
      }
    } else {
      //Not dealing with identity hack, retain
      NPN_RetainObject(obj);
    }
    objects[id] = obj;
    ids[obj] = id;

    // keep track that we hold a reference in the table
  }

  void free(int id) {
    Debug::log(Debug::Debugging) << "LocalObjectTable::free(id=" << id << ")" << Debug::flush;
    ObjectMap::iterator it = objects.find(id);
    if (it == objects.end()) {
      Debug::log(Debug::Error) << "Freeing freed object slot " << id << Debug::flush;
      return;
    }
    if (!jsIdentitySafe) {
      Debug::log(Debug::Debugging) << "removing expando!" << Debug::flush;
      NPN_RemoveProperty(npp, it->second, gwtId);
    }
    setFree(id);
    if (!dontFree) {
      NPObject* obj = it->second;
      NPN_ReleaseObject(obj);
    }
  }

  void freeAll() {
    Debug::log(Debug::Info) << "LocalObjectTable::freeAll()" << Debug::flush;
    for (ObjectMap::const_iterator it = objects.begin(); it != objects.end(); ++it) {
      NPObject* obj = it->second;
      if (!dontFree) {
        NPN_ReleaseObject(obj);
      }
    }
    objects.clear();
  }

  NPObject* getById(int id) {
    ObjectMap::iterator it = objects.find(id);
    if (it == objects.end()) {
      Debug::log(Debug::Error) << "LocalObjectTable::get(id=" << id
          << "): no object found" << Debug::flush;
    }
    return it->second;
  }

  int getObjectId(NPObject* jsObject) {
    int id = -1;
    if(!jsIdentitySafe) {
      NPVariant idVariant;
      VOID_TO_NPVARIANT(idVariant);
      Debug::log(Debug::Debugging) << "LocalObjectTable::get(): expando test"
          << Debug::flush;
      if (NPN_GetProperty(npp, jsObject, gwtId, &idVariant) &&
          NPVariantUtil::isInt(idVariant)) {
        id = NPVariantUtil::getAsInt(idVariant);
        Debug::log(Debug::Debugging) << "LocalObjectTable::get(): expando: "
            << id << Debug::flush;
        set(id, jsObject);
      }
      NPN_ReleaseVariantValue(&idVariant);
    } else {
      IdMap::iterator it = ids.find(jsObject);
      if (it != ids.end()) {
        id = it->second;
      }
    }
    return id;
  }

  void setDontFree(bool dontFree) {
    this->dontFree = dontFree;
  }
};

#endif
