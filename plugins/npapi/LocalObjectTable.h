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

class LocalObjectTable {
private:
  typedef std::map<int, NPObject*> ObjectMap;

  int nextId;
  ObjectMap objects;
  bool dontFree;

  void setFree(int id) {
    if (objects.erase(id) != 1) {
      Debug::log(Debug::Error) << "setFree(id=" << id << "): object not in table"
        << Debug::flush;
    }
  }

public:
  LocalObjectTable(): nextId(0), dontFree(false) {
  }

  virtual ~LocalObjectTable();

  /**
   * Add a new object, which must not be in the table, and return a new id for it.
   */
  int add(NPObject* obj) {
    int id = nextId++;
    set(id, obj);
    return id;
  }

  void set(int id, NPObject* obj) {
    Debug::log(Debug::Info) << "LocalObjectTable::set(id=" << id << ",obj=" << (void*)obj
        << ")" << Debug::flush;
    objects[id] = obj;
    // keep track that we hold a reference in the table
    NPN_RetainObject(obj);
  }

  void free(int id) {
    Debug::log(Debug::Info) << "LocalObjectTable::free(id=" << id << ")" << Debug::flush;
    ObjectMap::iterator it = objects.find(id);
    if (it == objects.end()) {
      Debug::log(Debug::Error) << "Freeing freed object slot " << id << Debug::flush;
      return;
    }
    if (!dontFree) {
      NPObject* obj = it->second;
      NPN_ReleaseObject(obj);
    }
    setFree(id);
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

  NPObject* get(int id) {
    ObjectMap::iterator it = objects.find(id);
    if (it == objects.end()) {
      Debug::log(Debug::Error) << "LocalObjectTable::get(id=" << id
          << "): no object found" << Debug::flush;
    }
    return it->second;
  }

  void setDontFree(bool dontFree) {
    this->dontFree = dontFree;
  }
};

#endif
