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

#include <vector>
#include <algorithm>

#include "Debug.h"

#include "mozincludes.h"

class LocalObjectTable {
private:
  static const int INITIAL_OBJECT_TABLE_SIZE = 300;

  int nextFree;
  std::vector<NPObject*> objects;
  bool dontFree;

  bool isFree(int id) {
    // low bit is set for free pointers, object pointers can't be odd
    NPObject* obj = objects[id];
    return !obj || (reinterpret_cast<long long>(obj) & 1);
  }

  void setFree(int id) {
    objects[id] = reinterpret_cast<NPObject*>((nextFree << 1) | 1LL);
    nextFree = id;
  }

public:
  LocalObjectTable() {
    nextFree = -1;
    objects.reserve(INITIAL_OBJECT_TABLE_SIZE);
    dontFree = false;
  }

  virtual ~LocalObjectTable();

  /**
   * Add a new object, which must not be in the table, and return a new id for it.
   */
  int add(NPObject* obj) {
    int id;
    if (nextFree >= 0) {
      id = nextFree;
      nextFree = int(reinterpret_cast<long long>(objects[nextFree])) >> 1;
      objects[id] = obj;
    } else {
      id = static_cast<int>(objects.size());
      objects.push_back(obj);
    }
    Debug::log(Debug::Spam) << "LocalObjectTable::add(obj=" << obj << "): id=" << id
        << Debug::flush;
    // keep track that we hold a reference in the table
    NPN_RetainObject(obj);
    return id;
  }

  void free(int id) {
    Debug::log(Debug::Spam) << "LocalObjectTable::free(id=" << id << ")" << Debug::flush;
    if (unsigned(id) >= objects.size()) {
      Debug::log(Debug::Error) << "LocalObjectTable::free(id=" << id << "): invalid index (size="
          << objects.size() << Debug::flush;
      return;
    }
    if (isFree(id)) {
      Debug::log(Debug::Error) << "Freeing freed object slot " << id << Debug::flush;
      return;
    }
    NPObject* obj = objects[id];
    setFree(id);
    if (!dontFree) {
      NPN_ReleaseObject(obj);
    }
  }

  void freeAll() {
    Debug::log(Debug::Spam) << "LocalObjectTable::freeAll()" << Debug::flush;
    for (unsigned i = 0; i < objects.size(); ++i) {
      if (!isFree(i)) {
        NPObject* obj = objects[i];
        setFree(i);
        if (!dontFree) {
          NPN_ReleaseObject(obj);
        }
      }
    }
  }

  NPObject* get(int id) {
    if (unsigned(id) >= objects.size()) {
      Debug::log(Debug::Error) << "LocalObjectTable::get(id=" << id << "): invalid index (size="
          << objects.size() << Debug::flush;
      return 0;
    }
    if (isFree(id)) {
      Debug::log(Debug::Error) << "Getting freed object slot " << id << Debug::flush;
      return 0;
    }
    return objects[id];
  }

  void setDontFree(bool dontFree) {
    this->dontFree = dontFree;
  }
};

#endif
