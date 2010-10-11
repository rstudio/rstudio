/* ***** BEGIN LICENSE BLOCK *****
 * Version: NPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Netscape Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is 
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1998
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or 
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the NPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the NPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

////////////////////////////////////////////////////////////
//
// Implementation of Netscape entry points (NPN_*), which are the functions
// the plugin calls to talk to the browser.
//

#include "Debug.h"

// Sun's cstring doesn't declare memcpy
#include <string.h>
//#include <cstring>

#include "mozincludes.h"
//#include "gears/base/common/base_class.h"
//#include "gears/base/common/thread_locals.h"
//#include "gears/base/npapi/module.h"

#ifndef HIBYTE
#define HIBYTE(x) ((((uint32)(x)) & 0xff00) >> 8)
#endif

#ifndef LOBYTE
#define LOBYTE(W) ((W) & 0xFF)
#endif

static NPNetscapeFuncs npn_funcs;

void SetNPNFuncs(NPNetscapeFuncs* npnFuncs) {
  // Since we can't rely on the pointer remaining valid, we need to
  // copy the function pointers.
  int size = sizeof(NPNetscapeFuncs);
  if (size > npnFuncs->size) {
    Debug::log(Debug::Warning) << "*** Warning: NPNetscapeFuncs supplied by "
        "browser is smaller than expected: " << npnFuncs->size << " vs " << size
        << Debug::flush;
    size = npnFuncs->size;
  }
  memcpy(&npn_funcs, npnFuncs, size);
}

const NPNetscapeFuncs &GetNPNFuncs() {
  return npn_funcs;
}

void NPN_Version(int* plugin_major, int* plugin_minor,
                 int* netscape_major, int* netscape_minor)
{
  const NPNetscapeFuncs &funcs = GetNPNFuncs();
  *plugin_major   = NP_VERSION_MAJOR;
  *plugin_minor   = NP_VERSION_MINOR;
  *netscape_major = HIBYTE(funcs.version);
  *netscape_minor = LOBYTE(funcs.version);
}

NPError NPN_GetURLNotify(NPP instance, const char *url, const char *target,
                         void* notifyData)
{
  const NPNetscapeFuncs &funcs = GetNPNFuncs();
  int navMinorVers = funcs.version & 0xFF;
  NPError rv = NPERR_NO_ERROR;

  if (navMinorVers >= NPVERS_HAS_NOTIFICATION)
    rv = funcs.geturlnotify(instance, url, target, notifyData);
  else
    rv = NPERR_INCOMPATIBLE_VERSION_ERROR;

  return rv;
}

NPError NPN_GetURL(NPP instance, const char *url, const char *target)
{
  NPError rv = GetNPNFuncs().geturl(instance, url, target);
  return rv;
}

NPError NPN_PostURLNotify(NPP instance, const char* url, const char* window,
                          uint32 len, const char* buf, NPBool file,
                          void* notifyData)
{
  const NPNetscapeFuncs &funcs = GetNPNFuncs();
  int navMinorVers = funcs.version & 0xFF;
  NPError rv = NPERR_NO_ERROR;

  if (navMinorVers >= NPVERS_HAS_NOTIFICATION) {
    rv = funcs.posturlnotify(instance, url, window, len, buf, file, notifyData);
  } else {
    rv = NPERR_INCOMPATIBLE_VERSION_ERROR;
  }

  return rv;
}

NPError NPN_PostURL(NPP instance, const char* url, const char* window,
                    uint32 len, const char* buf, NPBool file)
{
  NPError rv = GetNPNFuncs().posturl(instance, url, window, len, buf, file);
  return rv;
} 

NPError NPN_RequestRead(NPStream* stream, NPByteRange* rangeList)
{
  NPError rv = GetNPNFuncs().requestread(stream, rangeList);
  return rv;
}

NPError NPN_NewStream(NPP instance, NPMIMEType type, const char* target,
                      NPStream** stream)
{
  const NPNetscapeFuncs &funcs = GetNPNFuncs();
  int navMinorVersion = funcs.version & 0xFF;

  NPError rv = NPERR_NO_ERROR;

  if ( navMinorVersion >= NPVERS_HAS_STREAMOUTPUT )
    rv = funcs.newstream(instance, type, target, stream);
  else
    rv = NPERR_INCOMPATIBLE_VERSION_ERROR;

  return rv;
}

int32 NPN_Write(NPP instance, NPStream *stream, int32 len, void *buffer)
{
  const NPNetscapeFuncs &funcs = GetNPNFuncs();
  int navMinorVersion = funcs.version & 0xFF;
  int32 rv = 0;

  if ( navMinorVersion >= NPVERS_HAS_STREAMOUTPUT )
    rv = funcs.write(instance, stream, len, buffer);
  else
    rv = -1;

  return rv;
}

NPError NPN_DestroyStream(NPP instance, NPStream* stream, NPError reason)
{
  const NPNetscapeFuncs &funcs = GetNPNFuncs();
  int navMinorVersion = funcs.version & 0xFF;
  NPError rv = NPERR_NO_ERROR;

  if ( navMinorVersion >= NPVERS_HAS_STREAMOUTPUT )
    rv = funcs.destroystream(instance, stream, reason);
  else
    rv = NPERR_INCOMPATIBLE_VERSION_ERROR;

  return rv;
}

void NPN_Status(NPP instance, const char *message)
{
  GetNPNFuncs().status(instance, message);
}

const char* NPN_UserAgent(NPP instance)
{
  const char * rv = NULL;
  rv = GetNPNFuncs().uagent(instance);
  return rv;
}

void* NPN_MemAlloc(uint32 size)
{
  void * rv = NULL;
  rv = GetNPNFuncs().memalloc(size);
  return rv;
}

void NPN_MemFree(void* ptr)
{
  GetNPNFuncs().memfree(ptr);
}

uint32 NPN_MemFlush(uint32 size)
{
  uint32 rv = GetNPNFuncs().memflush(size);
  return rv;
}

void NPN_ReloadPlugins(NPBool reloadPages)
{
  GetNPNFuncs().reloadplugins(reloadPages);
}

NPError NPN_GetValue(NPP instance, NPNVariable variable, void *value)
{
  NPError rv = GetNPNFuncs().getvalue(instance, variable, value);
  return rv;
}

NPError NPN_SetValue(NPP instance, NPPVariable variable, void *value)
{
  NPError rv = GetNPNFuncs().setvalue(instance, variable, value);
  return rv;
}

void NPN_InvalidateRect(NPP instance, NPRect *invalidRect)
{
  GetNPNFuncs().invalidaterect(instance, invalidRect);
}

void NPN_InvalidateRegion(NPP instance, NPRegion invalidRegion)
{
  GetNPNFuncs().invalidateregion(instance, invalidRegion);
}

void NPN_ForceRedraw(NPP instance)
{
  GetNPNFuncs().forceredraw(instance);
}

NPIdentifier NPN_GetStringIdentifier(const NPUTF8 *name)
{
  return GetNPNFuncs().getstringidentifier(name);
}

void NPN_GetStringIdentifiers(const NPUTF8 **names, int32_t nameCount,
                              NPIdentifier *identifiers)
{
  return GetNPNFuncs().getstringidentifiers(names, nameCount, identifiers);
}

NPIdentifier NPN_GetIntIdentifier(int32_t intid)
{
  return GetNPNFuncs().getintidentifier(intid);
}

bool NPN_IdentifierIsString(NPIdentifier identifier)
{
  return GetNPNFuncs().identifierisstring(identifier);
}

NPUTF8 *NPN_UTF8FromIdentifier(NPIdentifier identifier)
{
  return GetNPNFuncs().utf8fromidentifier(identifier);
}

// On WebKit under OSX, the intfromidentifier field of the structure isn't
// filled in (see WebNetscapePluginPackage.m#526 in WebKit source tree).
// At this time this function isn't called from our code, so for now comment it
// out.
//
int32_t NPN_IntFromIdentifier(NPIdentifier identifier)
{
  return GetNPNFuncs().intfromidentifier(identifier);
}

NPObject *NPN_CreateObject(NPP npp, NPClass *aClass)
{
  return GetNPNFuncs().createobject(npp, aClass);
}

NPObject *NPN_RetainObject(NPObject *obj)
{
  return GetNPNFuncs().retainobject(obj);
}

void NPN_ReleaseObject(NPObject *obj)
{
  return GetNPNFuncs().releaseobject(obj);
}

bool NPN_Invoke(NPP npp, NPObject* obj, NPIdentifier methodName,
                const NPVariant *args, uint32_t argCount, NPVariant *result)
{
  return GetNPNFuncs().invoke(npp, obj, methodName, args, argCount, result);
}

bool NPN_InvokeDefault(NPP npp, NPObject* obj, const NPVariant *args,
                       uint32_t argCount, NPVariant *result)
{
  return GetNPNFuncs().invokeDefault(npp, obj, args, argCount, result);
}

bool NPN_Evaluate(NPP npp, NPObject* obj, NPString *script,
                  NPVariant *result)
{
  return GetNPNFuncs().evaluate(npp, obj, script, result);
}

bool NPN_GetProperty(NPP npp, NPObject* obj, NPIdentifier propertyName,
                     NPVariant *result)
{
// Workaround for bug in WebKit: GetProperty() fails when attempting to
// read a null value from an array, however it fills in the variant structure
// correctly.
// The workaround is to chek if GetProprety() touches the variant structure,
// if so, we assume it succeeded.
#ifdef BROWSER_WEBKIT
  result->type = static_cast<NPVariantType>(-1);
  
  bool ret = GetNPNFuncs().getproperty(npp, obj, propertyName, result);
  
  if (result->type != -1 && !ret) {
    ret = true;
  }
  return ret;
#else
  return GetNPNFuncs().getproperty(npp, obj, propertyName, result);
#endif
}

bool NPN_SetProperty(NPP npp, NPObject* obj, NPIdentifier propertyName,
                     const NPVariant *value)
{
  return GetNPNFuncs().setproperty(npp, obj, propertyName, value);
}

bool NPN_RemoveProperty(NPP npp, NPObject* obj, NPIdentifier propertyName)
{
  return GetNPNFuncs().removeproperty(npp, obj, propertyName);
}

#ifdef BROWSER_WEBKIT
// This field of NPN functions isn't filled in by WebKit on OSX.
#else
bool NPN_HasProperty(NPP npp, NPObject* obj, NPIdentifier propertyName)
{
  return GetNPNFuncs().hasproperty(npp, obj, propertyName);
}
#endif

#ifdef BROWSER_WEBKIT
// This field of NPN functions isn't filled in by WebKit on OSX.
#else
bool NPN_HasMethod(NPP npp, NPObject* obj, NPIdentifier methodName)
{
  return GetNPNFuncs().hasmethod(npp, obj, methodName);
}
#endif

void NPN_ReleaseVariantValue(NPVariant *variant)
{
  GetNPNFuncs().releasevariantvalue(variant);
}

#ifdef BROWSER_WEBKIT
// This function is buggy in WebKit, see 
// http://bugs.webkit.org/show_bug.cgi?id=16829
#else
void NPN_SetException(NPObject* obj, const NPUTF8 *message)
{
  GetNPNFuncs().setexception(obj, message);
}
#endif
