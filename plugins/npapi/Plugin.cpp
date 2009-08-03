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

#include <cstring>

#include "Plugin.h"
#include "ScriptableInstance.h"
#include "InvokeMessage.h"
#include "ReturnMessage.h"
#include "ServerMethods.h"
#include "mozincludes.h"
#include "scoped_ptr/scoped_ptr.h"
#include "NPVariantWrapper.h"

using std::string;
using std::endl;

Plugin::Plugin(NPP npp) : npp(npp) {
  this->scriptableInstance = static_cast<ScriptableInstance*>(NPN_CreateObject(npp,
	  GetNPClass<ScriptableInstance>()));
}

Plugin::Plugin(NPP npp, ScriptableInstance* scriptableInstance) : npp(npp),
    scriptableInstance(scriptableInstance) {
}

Plugin::~Plugin() {
  Debug::log(Debug::Debugging) << "Plugin destroyed" << Debug::flush;
  if (scriptableInstance) {
    scriptableInstance->pluginDeath();
    NPN_ReleaseObject(scriptableInstance);
    scriptableInstance = 0;
  }
}
