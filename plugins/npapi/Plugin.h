#ifndef _H_Plugin
#define _H_Plugin
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

#include "Debug.h"

#include "mozincludes.h"
#include "HostChannel.h"
#include "LoadModuleMessage.h"
#include "LocalObjectTable.h"
#include "SessionHandler.h"
#include "HashMap.h"

using std::vector;

class ScriptableInstance;

class Plugin {
  friend class JavaObject;
public:
  Plugin(NPP);
  Plugin(NPP, ScriptableInstance*);
  ~Plugin();
  
  const NPNetscapeFuncs* getBrowser() { return &GetNPNFuncs(); }
  ScriptableInstance* getScriptableInstance() { return scriptableInstance; }
  NPP getNPP() { return npp; }

  static const uint32_t VERSION = 1;
private:  
  
  NPP npp;
  ScriptableInstance* scriptableInstance;
  
  NPObject* window;

};

#endif
