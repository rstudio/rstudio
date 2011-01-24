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

#include "stdafx.h"
#include "comutil.h"
#include "dispex.h"
#include "ExceptionCatcher.h"
#include "IESessionHandler.h"
#include "ServerMethods.h"
#include "scoped_ptr/scoped_ptr.h"
#include "IEUtils.h"
#include "Constants.h"


IESessionHandler::IESessionHandler(HostChannel* channel,
                                   IHTMLWindow2* window) : SessionData(channel, window, this), jsObjectId(1)
{
  // window->put_defaultStatus(L"GWT Developer Plugin active");
  IEUtils::resetResolver();
}

IESessionHandler::~IESessionHandler(void) {
  Debug::log(Debug::Debugging) << "Destroying session handler" << Debug::flush;
  Debug::log(Debug::Spam) << jsObjectsById.size() << " active JS object referances" << Debug::flush;
  // Put any remaining JavaObject references into zombie-mode in case
  // of lingering references
  Debug::log(Debug::Spam) << javaObjectsById.size() << " active Java object references" << Debug::flush;

  IEUtils::resetResolver();
  std::map<int, IUnknown*>::iterator it = javaObjectsById.begin();
  while (it != javaObjectsById.end()) {
    ((CJavaObject*)it->second)->shutdown();
    it++;
  }
  channel->disconnectFromHost();
}

void IESessionHandler::disconnectDetectedImpl() {
  DISPID dispId;

  HRESULT hr = IEUtils::resolveName(window, Constants::__gwt_disconnected, &dispId);
  if(FAILED(hr)) {
    Debug::log(Debug::Error) << "Unable to get dispId for __gwt_disconnected" << Debug::flush;
    return;
  }

  DISPPARAMS dispParams = {NULL, NULL, 0, 0};
  CComPtr<IDispatchEx> dispEx;
  hr = IEUtils::Invoke(getWindow(), dispId, DISPATCH_METHOD, &dispParams, NULL, NULL, NULL);
  if (FAILED(hr)) {
    Debug::log(Debug::Error) << "Unable to invoke __gwt_disconnected" << Debug::flush;
    SYSLOGERROR(L"failed to invoke __gwt_disconnected", L"hr=0x%08x", hr);
  }
}

void IESessionHandler::fatalError(HostChannel& channel,
    const std::string& message) {
  SYSLOGERROR(L"IESessionHandler::fatalError()", L"%S", message.c_str());
  Debug::log(Debug::Error) << "Fatal error: " << message << Debug::flush;
}

void IESessionHandler::freeJavaObject(unsigned int objId) {
  // Remove the now-defunct object from the lookup table
  javaObjectsById.erase(objId);

  // and add it to the set of objects to free on the server
  javaObjectsToFree.insert(objId);
}

void IESessionHandler::sendFreeValues(HostChannel& channel) {
  int idCount = javaObjectsToFree.size();
  if (idCount == 0) {
    return;
  }

  Debug::log(Debug::Debugging) << "Freeing " << idCount << " Java objects on server" << Debug::flush;
  scoped_array<int> ids(new int[idCount]);

  std::set<int>::iterator it = javaObjectsToFree.begin();
  for (int i = 0; it != javaObjectsToFree.end(); it++) {
    ids[i++] = *it;
  }

  if (!ServerMethods::freeJava(channel, this, idCount, ids.get())) {
    Debug::log(Debug::Error) << "Unable to free Java ids on server" << Debug::flush;
  }

  javaObjectsToFree.clear();
}

void IESessionHandler::freeValue(HostChannel& channel, int idCount, const int* ids) {
  for (int i = 0; i < idCount; i++) {
    int jsId = ids[i];
    std::map<int, CComPtr<IUnknown>>::iterator it = jsObjectsById.find(jsId);
    if (it == jsObjectsById.end()) {
      Debug::log(Debug::Error) << "Trying to free unknown js id " << jsId << Debug::flush;
      continue;
    }
    jsIdsByObject.erase(it->second);
    jsObjectsById.erase(it);
  }
  Debug::log(Debug::Debugging) << "Freed " << idCount << " JS objects" << Debug::flush;
}

bool IESessionHandler::invoke(HostChannel& channel, const Value& thisObj,
                               const std::string& methodName, int numArgs,
                               const Value* const args, Value* returnValue)
{
  Debug::log(Debug::Debugging) << "Executing method " << methodName <<
      " on object " << thisObj.toString() << Debug::flush;

  DISPID methodDispId;
  HRESULT hr = IEUtils::resolveName(window, methodName, &methodDispId);
  if (FAILED(hr)) {
    SYSLOGERROR(L"Failed to resolve name to DISPID",
        L"IESessionHandler::invoke(thisObj=%S, methodName=%S)",
        thisObj.toString().c_str(), methodName.c_str());
    Debug::log(Debug::Error) << "Unable to find method " << methodName
        << " on the window object" <<Debug::flush;
    makeExceptionValue(*returnValue, "Unable to find named method on window");
    return true;
  }

  // Get the JS Function object as an IDispatch
  // TODO try PROPERTYGET|EXECUTE instead?
  _variant_t functionObject;
  DISPPARAMS disparamsNoArgs = {NULL, NULL, 0, 0};
  hr = IEUtils::Invoke(window, methodDispId, DISPATCH_PROPERTYGET, &disparamsNoArgs,
      functionObject.GetAddress(), NULL, NULL);
  if (FAILED(hr)) {
    Debug::log(Debug::Error) << "Unable to get method " << methodName
        << Debug::flush;
    makeExceptionValue(*returnValue, "Unable to get method from window");
    return true;
  } else if (functionObject.vt != VT_DISPATCH) {
    Debug::log(Debug::Error) << "Did not get a VT_DISPATCH, got " <<
        functionObject.vt << Debug::flush;
    makeExceptionValue(*returnValue, "Did not get a VT_DISPATCH");
    return true;
  }

  // See if it's an IDispatchEx
  CComPtr<IDispatchEx> ex;
  if (functionObject.pdispVal->QueryInterface(&ex)) {
    // Probably not a function
    Debug::log(Debug::Error) << "Failed to invoke " << methodName <<
        " which is not an IDispatchEx" << Debug::flush;
    makeExceptionValue(*returnValue, "Unable to invoke method");
    return true;
  }

  // Convert the function arguments
  // The parameters in the DISPARAMS are backwards
  // Named parameters are first
  int jsArgsLen = numArgs + 1;
  scoped_array<_variant_t> jsargs(new _variant_t[jsArgsLen]);
  DISPID thisId[] = {DISPID_THIS};
  makeValueRef(jsargs[0], thisObj);
  for (int i = 0; i < numArgs; i++) {
    makeValueRef(jsargs[jsArgsLen - 1 - i], args[i]);
  }
  DISPPARAMS callDispParams = {jsargs.get(), thisId, numArgs + 1, 1};
  EXCEPINFO excepInfo;
  _variant_t retVal;
  CComPtr<IExceptionCatcher> catcher;
  CExceptionCatcher::CreateInstance(&catcher);

  CComPtr<IServiceProvider> serviceProvider;
  catcher->QueryInterface(&serviceProvider);
  hr = ex->InvokeEx(DISPID_VALUE, LOCALE_SYSTEM_DEFAULT, DISPATCH_METHOD,
    &callDispParams, retVal.GetAddress(), &excepInfo, serviceProvider);

  // There are cases where an exception was thrown and we've caught it, but
  // the return value from InvokeEx is still S_OK.  Thus, we check our
  // ExceptionCatcher before using the res value to determine failure.
  BOOL exceptionFlag = false;
  catcher->hasSeenException(&exceptionFlag);
  if (exceptionFlag) {
    VARIANT exceptionVariant;
    catcher->getException(&exceptionVariant);
    _variant_t exception(exceptionVariant);

    makeValue(*returnValue, exception);
    exceptionFlag = true;

  } else if (!SUCCEEDED(hr)) {
    makeExceptionValue(*returnValue, "Unknown failure");
    exceptionFlag = true;

  } else {
    // Success
    makeValue(*returnValue, retVal);
  }
  return exceptionFlag != 0;
}

bool IESessionHandler::invokeSpecial(HostChannel& channel, SpecialMethodId method, int numArgs,
                                     const Value* const args, Value* returnValue)
{
  Debug::log(Debug::Error) << "InvokeSpecial is currently unimplemented" << Debug::flush;
  makeExceptionValue(*returnValue, "InvokeSpecial is currently unimplemented");
  return true;
}


void IESessionHandler::loadJsni(HostChannel& channel, const std::string& js) {
    Debug::log(Debug::Spam) << ">>> loadJsni\n" << js << "\n<<< loadJsni" << Debug::flush;

    _variant_t retVal;
    HRESULT hr = window->execScript(UTF8ToBSTR(js.length(), js.c_str()),
        Constants::JavaScript, retVal.GetAddress());
    if (FAILED(hr)) {
        Debug::log(Debug::Error) << "Unable to evaluate JSNI code" << Debug::flush;
    }
}

void IESessionHandler::makeException(_variant_t& in, const char* message) {
  Debug::log(Debug::Debugging) << "Creating exception variant " <<
      std::string(message) << Debug::flush;

  SYSLOGERROR(L"IESessionHandler::makeException()", L"exception: %S", message);
  DISPID dispId;
  HRESULT hr = IEUtils::resolveName(window, Constants::Error, &dispId);
  if (FAILED(hr)) {
      SYSLOGERROR(L"failed to resolve Error object", L"hr=0x%08x", hr);
      return;
  }

  DISPPARAMS emptyParams = {NULL, NULL, 0, 0};
  _variant_t errorConstructor;
  hr = IEUtils::Invoke(window, dispId, DISPATCH_PROPERTYGET, &emptyParams,
      errorConstructor.GetAddress(), NULL, NULL);
  if (FAILED(hr)) {
    Debug::log(Debug::Error) << "Unable to get Error constructor" << Debug::flush;
    in.SetString("Unable to get Error constructor");
  }

  CComPtr<IDispatchEx> ex;
  hr = errorConstructor.pdispVal->QueryInterface(&ex);
  if (FAILED(hr)) {
    Debug::log(Debug::Error) << "Error constructor not IDispatchEx" << Debug::flush;
    in.SetString("Error constructor not IDispatchEx");
  }

  _variant_t param = _variant_t(message);
  DISPPARAMS dispParams = {&param, NULL, 1, 0};

  hr = ex->InvokeEx(DISPID_VALUE, LOCALE_SYSTEM_DEFAULT, DISPATCH_CONSTRUCT,
    &dispParams, in.GetAddress(), NULL, NULL);

  if (FAILED(hr)) {
    Debug::log(Debug::Error) << "Unable to invoke Error constructor" << Debug::flush;
    in.SetString("Unable to invoke Error constructor");
  }
}

void IESessionHandler::makeExceptionValue(Value& in, const char* message) {
  Debug::log(Debug::Debugging) << "Creating exception value " << std::string(message) << Debug::flush;
  _variant_t exception;
  makeException(exception, message);
  makeValue(in, exception);
}

void IESessionHandler::makeValue(Value& retVal, const _variant_t& value) {
  CComPtr<IDispatch> dispObj;
  CComPtr<IJavaObject> javaObject;

  switch (value.vt) {
    case VT_EMPTY:
      retVal.setUndefined();
      break;

    case VT_NULL:
      retVal.setNull();
      break;

    case VT_BOOL:
      retVal.setBoolean(VARIANT_TRUE == value.boolVal);
      break;

    case VT_BSTR:
      retVal.setString(BSTRToUTF8(value.bstrVal));
      break;

    case VT_I4:
      retVal.setInt(value.lVal);
      break;

    case VT_I8:
      retVal.setLong(value.llVal);
      break;

    case VT_R4:
      retVal.setFloat(value.fltVal);
      break;

    case VT_R8:
      retVal.setDouble(value.dblVal);
      break;

    case VT_DISPATCH:
      dispObj = value.pdispVal;

      if (!dispObj) {
        // XXX Determine if this is normal operation
        retVal.setUndefined();

      } else if (!dispObj->QueryInterface(&javaObject)) {
        // It's one of our Java Object proxies
        // XXX This casting is a hack
        retVal.setJavaObject(((CJavaObject*)javaObject.p)->getObjectId());

      } else {
        _variant_t stringValue;
        DISPPARAMS emptyParams = {NULL, NULL, 0, 0};
        DISPID valueOfDispId = -1;
        // See if it's a wrapped String object by invoking valueOf()
        HRESULT hr = dispObj->GetIDsOfNames(IID_NULL, (LPOLESTR*)&Constants::valueOf, 1,
            LOCALE_SYSTEM_DEFAULT, &valueOfDispId);
        if ((valueOfDispId != -1) &&
            SUCCEEDED(dispObj->Invoke(valueOfDispId, IID_NULL, LOCALE_SYSTEM_DEFAULT,
              DISPATCH_METHOD, &emptyParams, stringValue.GetAddress(),
              NULL, NULL)) &&
            stringValue.vt == VT_BSTR) {
          retVal.setString(BSTRToUTF8(stringValue.bstrVal));

        } else {
          // It's a plain-old JavaScript Object

          // We ask for the IUnknown interface since that's the only
          // COM interface guaranteed to have object-identity semantics
          CComPtr<IUnknown> asUnknown;
          dispObj->QueryInterface(&asUnknown);

          // See if we already know about this object
          std::map<IUnknown*, int>::iterator it = jsIdsByObject.find(asUnknown);
          if (it != jsIdsByObject.end()) {
            retVal.setJsObjectId(it->second);

          } else {
            // Allocate a new id
            int objId = ++jsObjectId;
            jsObjectsById[objId] = asUnknown;
            jsIdsByObject[asUnknown] = objId;
            retVal.setJsObjectId(objId);
          }
        }
      }
      break;

    default:
      Debug::log(Debug::Error) << "Unhandled variant type " << value.vt << Debug::flush;
      retVal.setString("Unhandled variant type");
  }
}

void IESessionHandler::makeValueRef(_variant_t& retVal, const Value& value) {
  switch (value.getType()) {
    case Value::NULL_TYPE:
      retVal.ChangeType(VT_NULL);
      break;

    case Value::BOOLEAN:
      retVal = value.getBoolean();
      break;

    case Value::BYTE:
      retVal = value.getByte();
      break;

    case Value::CHAR:
      retVal = value.getChar();
      break;

    case Value::SHORT:
      retVal = value.getShort();
      break;

    case Value::INT:
      retVal = value.getInt();
      break;

    case Value::LONG:
      retVal = value.getLong();
      break;

    case Value::FLOAT:
      retVal = value.getFloat();
      break;

    case Value::DOUBLE:
      retVal = value.getDouble();
      break;

    case Value::STRING:
      // The copy-constructor does not correctly handle embedded nulls
      retVal.bstrVal = UTF8ToBSTR(value.getString().length(),
        value.getString().c_str()).Detach();
      retVal.vt = VT_BSTR;
      break;

    case Value::JAVA_OBJECT:
      {
        int javaId = value.getJavaObjectId();

        std::map<int, IUnknown*>::iterator i = javaObjectsById.find(javaId);
        if (i == javaObjectsById.end()) {
          CComPtr<IUnknown> target;

          // Create a new instance of the Java object proxy type
          CJavaObject::CreateInstance(&target);

          // Because we used CreateInstance, we can cast it back to the concrete type
          // which allows us to pass pointers around, since we're guaranteed that
          // it is in the same process space
          ((CJavaObject*)target.p)->initialize(javaId, this);
          target->QueryInterface(&retVal.pdispVal);

          // Don't artificially increase the lifetime of a Java object proxy by
          // calling Detach; we want Release to be called.
          javaObjectsById[javaId] = target;

          // We may have previously released the proxy for the same object id,
          // but have not yet sent a free message back to the server.
          javaObjectsToFree.erase(javaId);

        } else {
          i->second->QueryInterface(&retVal.pdispVal);
        }
        retVal.vt = VT_DISPATCH;
      }
      break;

    case Value::JS_OBJECT:
      {
        int jsId = value.getJsObjectId();

        std::map<int, CComPtr<IUnknown>>::iterator i = jsObjectsById.find(jsId);
        if (i == jsObjectsById.end()) {
          Debug::log(Debug::Error) << "Missing jsObject with id " << jsId << Debug::flush;

        } else {
          i->second->QueryInterface(&retVal.pdispVal);
          retVal.vt = VT_DISPATCH;
        }
      }
      break;

    case Value::UNDEFINED:
      retVal.ChangeType(VT_EMPTY);
      break;

    default:
      Debug::log(Debug::Error) << "Unknown Value type " << value.toString() << Debug::flush;
  }
}
