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

// ExceptionCatcher.cpp : Implementation of CExceptionCatcher

#include "stdafx.h"
#include "Debug.h"
#include "ExceptionCatcher.h"

// CExceptionCatcher


STDMETHODIMP CExceptionCatcher::getException(VARIANT* retVal)
{
  *retVal = caughtException.GetVARIANT();
  return S_OK;
}

STDMETHODIMP CExceptionCatcher::hasSeenException(BOOL* retVal) {
  *retVal = hasCaughtException;
  return S_OK;
}

STDMETHODIMP CExceptionCatcher::CanHandleException(EXCEPINFO* exInfo, VARIANT* value) {
  Debug::log(Debug::Debugging) << "Caught an exception from JS function" << Debug::flush;
  if (hasCaughtException) {
    Debug::log(Debug::Spam) << "Double-catching exception" << Debug::flush;
    // We see this if a COM object that called a JavaObject doesn't recognize the
    // throwing-exception return code; just keep the first exception that we've
    // seen.
    return S_OK;
  }
  caughtException = value;
  hasCaughtException = true;
  return S_OK;
}

STDMETHODIMP CExceptionCatcher::QueryService(const GUID& guidService, const IID& riid, void** ret) {
  Debug::log(Debug::Spam) << "QueryService not supported by ExceptionCatcher" << Debug::flush;
  return E_NOTIMPL;
}
