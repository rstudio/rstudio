// Copyright 2007, Google Inc.
//
// Redistribution and use in source and binary forms, with or without 
// modification, are permitted provided that the following conditions are met:
//
//  1. Redistributions of source code must retain the above copyright notice, 
//     this list of conditions and the following disclaimer.
//  2. Redistributions in binary form must reproduce the above copyright notice,
//     this list of conditions and the following disclaimer in the documentation
//     and/or other materials provided with the distribution.
//  3. Neither the name of Google Inc. nor the names of its contributors may be
//     used to endorse or promote products derived from this software without
//     specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
// EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
// OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
// OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// Modified from gears/base/npapi/plugin.h

#ifndef _H_NPObjectWrapper
#define _H_NPObjectWrapper

#include <cstdio>
#include "mozincludes.h"

// This is a base class for the bridge between the JavaScript engine and the plugin.
template<class Wrapper>
class NPObjectWrapper : public NPObject {
 public:
  // NPClass callbacks.
  static NPObject* Allocate(NPP npp, NPClass *npclass) {
    Wrapper* obj = new Wrapper(npp);
    return obj;
  }

  static void Deallocate(NPObject *npobj) {
    delete static_cast<Wrapper*>(npobj);
  }

  static bool Enumeration(NPObject *npobj, NPIdentifier** value,
      uint32_t *count) {
    Wrapper* obj = static_cast<Wrapper*>(npobj);
    return obj->enumeration(value, count);
  }

  static bool HasMethod(NPObject *npobj, NPIdentifier name) {
    Wrapper* obj = static_cast<Wrapper*>(npobj);
    return obj->hasMethod(name);
  }

  static bool Invoke(NPObject *npobj, NPIdentifier name, const NPVariant *args,
      uint32_t num_args, NPVariant *result) {
    Wrapper* obj = static_cast<Wrapper*>(npobj);
    return obj->invoke(name, args, num_args, result);
  }

  static bool InvokeDefault(NPObject *npobj, const NPVariant *args,
      uint32_t num_args, NPVariant *result) {
    Wrapper* obj = static_cast<Wrapper*>(npobj);
    return obj->invokeDefault(args, num_args, result);
  }

  static bool HasProperty(NPObject *npobj, NPIdentifier name) {
    Wrapper* obj = static_cast<Wrapper*>(npobj);
    return obj->hasProperty(name);
  }

  static bool GetProperty(NPObject *npobj, NPIdentifier name,
        NPVariant *result) {
    Wrapper* obj = static_cast<Wrapper*>(npobj);
    return obj->getProperty(name, result);
  }

  static bool SetProperty(NPObject *npobj, NPIdentifier name,
                          const NPVariant *value) {
    Wrapper* obj = static_cast<Wrapper*>(npobj);
    return obj->setProperty(name, value);
  }

  virtual ~NPObjectWrapper() {}

  /**
   * *value must be memory allocated with NPN_MemAlloc, as the caller will call NPN_MemFree.
   */
  virtual bool enumeration(NPIdentifier** value, uint32_t* count) {
    return false;
  }

  /**
   * Caller must release the result value when it no longer needs it,
   * so implementation must return an extra refcount.
   */
  virtual bool getProperty(NPIdentifier name, NPVariant *result) {
    return false;
  }
  
  virtual bool hasMethod(NPIdentifier name) {
    return false;
  }

  virtual bool hasProperty(NPIdentifier name) {
    return false;
  }
  
  /**
   * Caller must release the result value when it no longer needs it,
   * so implementation must return an extra refcount.
   */
  virtual bool invoke(NPIdentifier name, const NPVariant *args,
      uint32_t num_args, NPVariant *result) {
    return false;
  }

  /**
   * Caller must release the result value when it no longer needs it,
   * so implementation must return an extra refcount.
   */
  virtual bool invokeDefault(const NPVariant *args, uint32_t num_args,
      NPVariant *result) {
    return false;
  }

   virtual bool setProperty(NPIdentifier name, const NPVariant *value) {
     return false;
   }

  protected:
    NPObjectWrapper(NPP instance) : npp(instance) {}

  public:
    const NPP getNPP() const {
      return npp;
    }

  private:
    NPP npp;
    DISALLOW_EVIL_CONSTRUCTORS(NPObjectWrapper);
};

// Get the NPClass for a NPObject wrapper (the type must derive from NPObjectWrapper).
template<class Wrapper>
NPClass* GetNPClass() {
  static NPClass plugin_class = {
    NP_CLASS_STRUCT_VERSION,
    NPObjectWrapper<Wrapper>::Allocate,
    NPObjectWrapper<Wrapper>::Deallocate,
    NULL,  // Invalidate,
    NPObjectWrapper<Wrapper>::HasMethod,
    NPObjectWrapper<Wrapper>::Invoke,
    NPObjectWrapper<Wrapper>::InvokeDefault,
    NPObjectWrapper<Wrapper>::HasProperty,
    NPObjectWrapper<Wrapper>::GetProperty,
    NPObjectWrapper<Wrapper>::SetProperty,
    NULL,  // RemoveProperty,
    NPObjectWrapper<Wrapper>::Enumeration,
  };

  return &plugin_class;
}

#endif // GEARS_BASE_NPAPI_PLUGIN_H__
