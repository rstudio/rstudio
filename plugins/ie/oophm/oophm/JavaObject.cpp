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

// JavaObject.cpp : Implementation of CJavaObject

#include "stdafx.h"
#include "InvokeMessage.h"
#include "JavaObject.h"
#include "ReturnMessage.h"
#include "ServerMethods.h"
#include "scoped_ptr/scoped_ptr.h"
#include "IEUtils.h"
#include "Constants.h"
//#include "activscp.h"

static const DISPID DISPID_TOSTRING = 1;

// CJavaObject
STDMETHODIMP CJavaObject::GetDispID(BSTR name, DWORD options, DISPID* dispId){
  std::string nameString = BSTRToUTF8(name);

  // toString is the only non-numeric dispid we recognize
  if (nameString == "toString") {
    *dispId = DISPID_TOSTRING;
    return S_OK;
  }

  char* lastChar;
  int d = strtol(nameString.c_str(), &lastChar, 10);

  if (*lastChar != '\0' || d < 0) {
    Debug::log(Debug::Error) << "Unable to get dispatch id for " << nameString << Debug::flush;
    // Set to unknown name in the case of an error
    *dispId = DISPID_UNKNOWN;
    return DISP_E_UNKNOWNNAME;
  }
  *dispId = d;
  return S_OK;
}

STDMETHODIMP CJavaObject::DeleteMemberByName(BSTR,DWORD){
  return S_FALSE;
}

STDMETHODIMP CJavaObject::DeleteMemberByDispID(DISPID){
  return S_FALSE;
}

STDMETHODIMP CJavaObject::GetMemberProperties(DISPID dispId, DWORD options, DWORD* retVal){
  Debug::log(Debug::Error) << "Hit unimplemented GetMemberProperties" << Debug::flush;
  return DISP_E_UNKNOWNNAME;
}

STDMETHODIMP CJavaObject::GetMemberName(DISPID,BSTR *){
  Debug::log(Debug::Error) << "Hit unimplemented GetMemberName" << Debug::flush;
  return DISP_E_UNKNOWNNAME;
}

STDMETHODIMP CJavaObject::GetNextDispID(DWORD,DISPID,DISPID *){
  Debug::log(Debug::Error) << "Hit unimplemented GetNextDispID" << Debug::flush;
  return DISP_E_UNKNOWNNAME;
}

STDMETHODIMP CJavaObject::GetNameSpaceParent(IUnknown **unk){
  sessionData->getWindow()->QueryInterface(unk);
  return S_OK;
}

STDMETHODIMP CJavaObject::GetIDsOfNames(REFIID riid, LPOLESTR* rgszNames,
                                        UINT cNames, LCID lcid, DISPID* rgdispid)
{
  USES_CONVERSION;
  // Stack-allocated
  return GetDispID(OLE2BSTR(*rgszNames), 0, rgdispid);
}

STDMETHODIMP CJavaObject::Invoke(DISPID dispidMember, REFIID riid,
                                 LCID lcid, WORD wFlags, DISPPARAMS* pdispparams,
                                 VARIANT* pvarResult, EXCEPINFO* pexcepinfo,
                                 UINT* puArgErr)
{
  return InvokeEx(dispidMember, lcid, wFlags, pdispparams, pvarResult,
    pexcepinfo, NULL);
}

STDMETHODIMP CJavaObject::InvokeEx(DISPID dispidMember, LCID lcid, WORD wFlags,
                                   DISPPARAMS* pdispparams, VARIANT* pvarResult,
                                   EXCEPINFO* pexcepinfo,
                                   IServiceProvider* pspCaller)
{
  Debug::log(Debug::Debugging) << "Invoking " << dispidMember << " on Java object " << objId << Debug::flush;

  if (!sessionData) {
    // Prevent errors if the object is retained post-disconnect
    Debug::log(Debug::Warning) << "JavaObject retained beyound session shutdown" << Debug::flush;
    return DISP_E_MEMBERNOTFOUND;
  }

  HostChannel* channel = sessionData->getHostChannel();
  Value thisRef = Value();
  thisRef.setJavaObject(objId);

  if ((wFlags & DISPATCH_PROPERTYGET) && dispidMember == DISPID_VALUE &&
    pdispparams->cArgs - pdispparams->cNamedArgs == 0) {
      // This is an expression like ('' + obj)
      // raw toString();
      wFlags = DISPATCH_METHOD;
      dispidMember = DISPID_TOSTRING;
  }

  if (wFlags & DISPATCH_METHOD) {
    Debug::log(Debug::Spam) << "Dispatching method " << dispidMember << " on " << objId << Debug::flush;

    if (!(dispidMember == DISPID_VALUE || dispidMember == DISPID_TOSTRING)) {
      Debug::log(Debug::Error) << "Cannot dispatch for non-default id: " << dispidMember << Debug::flush;
      return E_FAIL;
    }
    scoped_array<Value> args;
    Value javaDispatchId;
    int numArgs;

    if (dispidMember == DISPID_VALUE) {
      numArgs = pdispparams->cArgs - pdispparams->cNamedArgs - 2;
      if (numArgs < 0) {
        // Indicates an error in JSNI rewriting or dispatch code
        Debug::log(Debug::Error) << "Insufficient number of arguments" << Debug::flush;
        return E_FAIL;
      }
      args.reset(new Value[numArgs]);
      // The dispatch parameters are backwards
      sessionData->makeValue(javaDispatchId, pdispparams->rgvarg[pdispparams->cArgs - 1]);
      sessionData->makeValue(thisRef, pdispparams->rgvarg[pdispparams->cArgs - 2]);
      for (int i = 0; i < numArgs; i++) {
        int index = pdispparams->cArgs - 3 - i;
        VARIANTARG element = pdispparams->rgvarg[index];
        sessionData->makeValue(args[i], element);
      }
    } else if (dispidMember == DISPID_TOSTRING) {
      // raw toString();
      numArgs = 0;
      javaDispatchId.setInt(0);
    }

    bool isException = false;
    Value returnValue;
    if (!InvokeMessage::send(*channel, thisRef, javaDispatchId.getInt(), numArgs, args.get())) {
      Debug::log(Debug::Error) << "Unable to send method invocation" << Debug::flush;
    } else {
      scoped_ptr<ReturnMessage> m(channel->reactToMessagesWhileWaitingForReturn(
      sessionData->getSessionHandler()));

      if (!m.get()) {
        Debug::log(Debug::Error) << "Did not receive ReturnMessage" << Debug::flush;
      } else {
        if (dispidMember == DISPID_TOSTRING) {
          // raw toString();
          if (pvarResult) {
            // This will be NULL when the caller doesn't care about the return value
            _variant_t returnVariant;
            sessionData->makeValueRef(returnVariant, m->getReturnValue());
            *pvarResult = returnVariant.Detach();
          }
          return m->isException() ? E_FAIL : S_OK;
        }
        isException = m->isException();
        returnValue = m->getReturnValue();
      }
    }

    DISPID dispId;

    HRESULT hr = IEUtils::resolveName(sessionData->getWindow(), Constants::__gwt_makeResult, &dispId);
    if (FAILED(hr)) {
        Debug::log(Debug::Error) << "Unable to get dispId for __gwt_makeResult" << Debug::flush;
        return E_FAIL;
    }

    // Call __gwt_makeResult(isException, returnValue)
    scoped_array<_variant_t> varArgs(new _variant_t[2]);
    // Args go backwards.
    varArgs[1] = (isException ? 1 : 0);
    sessionData->makeValueRef(varArgs[0], returnValue);
    DISPPARAMS dispParams = {varArgs.get(), NULL, 2, 0};
    CComPtr<IDispatchEx> dispEx;
    sessionData->getWindow()->QueryInterface(&dispEx);
    return dispEx->InvokeEx(dispId, LOCALE_SYSTEM_DEFAULT, DISPATCH_METHOD,
      &dispParams, pvarResult, pexcepinfo, pspCaller);

  } else if (wFlags & DISPATCH_PROPERTYGET) {
    Debug::log(Debug::Spam) << "Getting property " << dispidMember << " on " << objId << Debug::flush;

    if (dispidMember == DISPID_VALUE) {
      this->QueryInterface(IID_IDispatch, (void**)&pvarResult->pdispVal);
      pvarResult->vt = VT_DISPATCH;

    } else if (dispidMember == DISPID_TOSTRING) {
      // Asking for a tear-off of the .toString function
      Debug::log(Debug::Spam) << "Making .toString tearoff" << Debug::flush;

      // Get a reference to __gwt_makeTearOff
      DISPID tearOffDispid;
      HRESULT hr = IEUtils::resolveName(sessionData->getWindow(), Constants::__gwt_makeTearOff, &tearOffDispid);
      if (FAILED(hr)) {
        Debug::log(Debug::Error) << "Unable to find __gwt_makeTearOff" << Debug::flush;
        return E_FAIL;
      }

      scoped_array<_variant_t> tearOffArgs(new _variant_t[3]);
      // Parameters are backwards:
      // __gwt_makeTearOff(proxy, dispId, argCount);
      tearOffArgs[2] = this; // proxy
      tearOffArgs[1] = 0; // dispId
      tearOffArgs[0] = 0; // argCount
      DISPPARAMS tearOffParams = {tearOffArgs.get(), NULL, 3, 0};

      // Invoke __gwt_makeTearOff
      hr = IEUtils::Invoke(sessionData->getWindow(), tearOffDispid,DISPATCH_METHOD,
          &tearOffParams, pvarResult, NULL, 0);
      if (FAILED(hr)) {
        Debug::log(Debug::Error) << "Unable to invoke __gwt_makeTearOff" << Debug::flush;
        return E_FAIL;
      }

    } else {
      Value ret = ServerMethods::getProperty(*channel,
        sessionData->getSessionHandler(), objId, dispidMember);

      if (ret.isUndefined()) {
        Debug::log(Debug::Error) << "Undefined get from Java object" << Debug::flush;
        return E_FAIL;
      }

      _variant_t returnVariant;
      sessionData->makeValueRef(returnVariant, ret);
      *pvarResult = returnVariant.Detach();
    }

  } else if (wFlags & (DISPATCH_PROPERTYPUT | DISPATCH_PROPERTYPUTREF)) {
    Debug::log(Debug::Spam) << "Setting property " << dispidMember << " on " << objId << Debug::flush;

    Value value;
    sessionData->makeValue(value, pdispparams->rgvarg[0]);

    ServerMethods::setProperty(*channel, sessionData->getSessionHandler(),
      objId, dispidMember, value);

  } else {
    Debug::log(Debug::Error) << "Unsupported invocation " << wFlags << Debug::flush;
    return DISP_E_MEMBERNOTFOUND;
  }

  return S_OK;
}
