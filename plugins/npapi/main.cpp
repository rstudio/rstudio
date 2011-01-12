/*
 *  main.cpp
 *  gwt-hosted-mode
 *
 *  Created by Kelly Norton on 11/18/07.
 *  Copyright 2007 Google Inc. All rights reserved.
 *
 */

#ifndef _WINDOWS
#include <unistd.h>
#endif

#include "Debug.h"

#include "mozincludes.h"
#include "Plugin.h"
#include "ScriptableInstance.h"
#include "scoped_ptr/scoped_ptr.h"

#ifdef _WINDOWS
#include <windows.h>
BOOL APIENTRY DllMain(HMODULE hModule, DWORD ulReasonForCall, LPVOID lpReserved) {
  switch (ulReasonForCall) {
    case DLL_PROCESS_ATTACH:
      DisableThreadLibraryCalls(hModule);
      break;
    case DLL_THREAD_ATTACH:
    case DLL_THREAD_DETACH:
    case DLL_PROCESS_DETACH:
      break;
  }
  return TRUE;
}
#endif
extern "C" {

  static const NPNetscapeFuncs* browser;

  NPError	NPP_New(NPMIMEType pluginType, NPP instance, uint16 mode,
	  int16 argc, char* argn[], char* argv[], NPSavedData* saved);
  NPError	NPP_Destroy(NPP instance, NPSavedData** save);
  NPError	NPP_SetWindow(NPP instance, NPWindow* window);
  NPError	NPP_NewStream(NPP instance, NPMIMEType type, NPStream* stream,
	  NPBool seekable, uint16* stype);
  NPError	NPP_DestroyStream(NPP instance, NPStream* stream, NPReason reason);
  int32		NPP_WriteReady(NPP instance, NPStream* stream);
  int32		NPP_Write(NPP instance, NPStream* stream, int32 offset, int32 len,
	  void* buffer);
  void		NPP_StreamAsFile(NPP instance, NPStream* stream, const char* fname);
  void		NPP_Print(NPP instance, NPPrint* platformPrint);
  int16		NPP_HandleEvent(NPP instance, void* event);
  void		NPP_URLNotify(NPP instance, const char* URL, NPReason reason,
	  void* notifyData);
  NPError NP_GetValue(void*, NPPVariable variable, void *value);
  NPError	NPP_GetValue(NPP instance, NPPVariable variable, void *value);
  NPError	NPP_SetValue(NPP instance, NPNVariable variable, void *value);

  // necessary for Mac
#ifdef __APPLE_CC__
  #pragma export on
  int main(NPNetscapeFuncs* browserFuncs, NPPluginFuncs* pluginFuncs, NPP_ShutdownUPP* shutdownUPP);
#endif
  NPError WINAPI NP_Initialize(NPNetscapeFuncs* browserFuncs NPINIT_ARG(pluginFuncs));
  NPError WINAPI NP_GetEntryPoints(NPPluginFuncs* pluginFuncs);
  const char *NP_GetMIMEDescription();
  const char *NPP_GetMIMEDescription();
  NP_SHUTDOWN_RETURN_TYPE WINAPI NP_Shutdown(void);
#ifdef __APPLE_CC__
  #pragma export off
#endif

  // Function pointer variables:

  NPError WINAPI NP_Initialize(NPNetscapeFuncs* browserFuncs NPINIT_ARG(pluginFuncs)) {
    Debug::log(Debug::Info) << "NP_Initialize (gwt-hosted-mode/c++)";
#ifndef _WINDOWS
    Debug::log(Debug::Info) << ", pid=" << (unsigned) getpid();
#endif
    Debug::log(Debug::Info) << Debug::flush;
    SetNPNFuncs(browserFuncs);
    browser = &GetNPNFuncs();
#ifdef NPINIT_GETS_ENTRYPOINTS
    NP_GetEntryPoints(pluginFuncs);
#endif
    return NPERR_NO_ERROR;
  }

#ifdef __APPLE_CC__
  int main(NPNetscapeFuncs* browserFuncs, NPPluginFuncs* pluginFuncs, NPP_ShutdownUPP* shutdownUPP) {
    printf("main (gwt-dev-plugin/c++)\n");
    if (shutdownUPP == NULL) {
      return NPERR_INVALID_FUNCTABLE_ERROR;
    }
    *shutdownUPP = NP_Shutdown;//NewNPP_ShutdownProc(NP_Shutdown);
    NP_Initialize(browserFuncs);
    NP_GetEntryPoints(pluginFuncs);
    return NPERR_NO_ERROR;
  }
#endif

  NPError WINAPI NP_GetEntryPoints(NPPluginFuncs* pluginFuncs) {
    Debug::log(Debug::Debugging) << "NP_GetEntryPoints" << Debug::flush;
#ifdef BROWSER_WEBKIT
    pluginFuncs->size          = sizeof(NPPluginFuncs);
#else
    if (pluginFuncs->size < sizeof(NPPluginFuncs)) {
      return NPERR_INVALID_FUNCTABLE_ERROR;
    }
#endif
    pluginFuncs->version = (NP_VERSION_MAJOR << 8) | NP_VERSION_MINOR;
#if 1
    pluginFuncs->newp          = NPP_New;
    pluginFuncs->destroy       = NPP_Destroy;
    pluginFuncs->setwindow     = NPP_SetWindow;
    pluginFuncs->newstream     = NPP_NewStream;
    pluginFuncs->destroystream = NPP_DestroyStream;
    pluginFuncs->asfile        = NPP_StreamAsFile;
    pluginFuncs->writeready    = NPP_WriteReady;
#ifdef BROWSER_WEBKIT
    pluginFuncs->write         = reinterpret_cast<NPP_WriteProcPtr>(NPP_Write);
#else
    pluginFuncs->write         = NPP_Write;
#endif
    pluginFuncs->print         = NPP_Print;
    pluginFuncs->event         = NPP_HandleEvent;
    pluginFuncs->urlnotify     = NPP_URLNotify;
    pluginFuncs->getvalue      = NPP_GetValue;
    pluginFuncs->setvalue      = NPP_SetValue;
    pluginFuncs->javaClass     = NULL;
#else
    pluginFuncs->newp =          NewNPP_NewProc(NPP_New);
    pluginFuncs->destroy =       NewNPP_DestroyProc(NPP_Destroy);
    pluginFuncs->setwindow =     NewNPP_SetWindowProc(NPP_SetWindow);
    pluginFuncs->newstream =     NewNPP_NewStreamProc(NPP_NewStream);
    pluginFuncs->destroystream = NewNPP_DestroyStreamProc(NPP_DestroyStream);
    pluginFuncs->asfile =        NewNPP_StreamAsFileProc(NPP_StreamAsFile);
    pluginFuncs->writeready =    NewNPP_WriteReadyProc(NPP_WriteReady);
    pluginFuncs->write =         NewNPP_WriteProc(NPP_Write);
    pluginFuncs->print =         NewNPP_PrintProc(NPP_Print);
    pluginFuncs->event =         NewNPP_HandleEventProc(NPP_HandleEvent);
    pluginFuncs->urlnotify =     NewNPP_URLNotifyProc(NPP_URLNotify);
    pluginFuncs->getvalue =      NewNPP_GetValueProc(NPP_GetValue);
    pluginFuncs->setvalue =      NewNPP_SetValueProc(NPP_SetValue);
#endif
    return NPERR_NO_ERROR;
  }

  const char *NP_GetMIMEDescription() {
    Debug::log(Debug::Info) << "NP_GetMIMEDescription: returned mime description" << Debug::flush;
    return "application/x-gwt-dev-mode::GWT dev-mode plugin;application/x-gwt-hosted-mode::GWT dev-mode plugin";
  }

  const char *NPP_GetMIMEDescription() {
    return NP_GetMIMEDescription();
  }

  NP_SHUTDOWN_RETURN_TYPE WINAPI NP_Shutdown(void) {
    Debug::log(Debug::Debugging) << "NP_Shutdown" << Debug::flush;
    return NP_SHUTDOWN_RETURN(NPERR_NO_ERROR);
  }

  NPError NPP_New(NPMIMEType pluginType, NPP instance, uint16 mode, int16 argc, char* argn[],
      char* argv[], NPSavedData* saved) {
    Debug::log(Debug::Info) << "NPP_New(instance=" << instance << ",mode=" << mode << ",argc="
        << argc << ",args=[";
    for (int i = 0; i < argc; ++i) {
      Debug::log(Debug::Info) << (i ? "," : "") << argn[i] << "=" << argv[i];
    }
    Debug::log(Debug::Info) << "],saved=" << saved << "): version=" << browser->version
        << Debug::flush;
    // Version 14 provides browser->createobject, which we need for npruntime support.
    if (browser->version < 14) {
      return NPERR_INVALID_INSTANCE_ERROR;
    }
    if (instance == NULL) {
      return NPERR_INVALID_INSTANCE_ERROR;
    }
    Plugin* obj;
//    if (saved) {
//      obj = new Plugin(instance, reinterpret_cast<ScriptableInstance*>(saved));
//    } else {
      obj = new Plugin(instance);
//    }
    instance->pdata = obj;

    // Make this a windowless plugin.
    return NPN_SetValue(instance, NPPVpluginWindowBool, NULL);
  }

  NPError NPP_Destroy(NPP instance, NPSavedData** save) {
    Debug::log(Debug::Info) << "NPP_Destroy(instance=" << instance << ")" << Debug::flush;
    if (instance == NULL) {
      return NPERR_INVALID_INSTANCE_ERROR;
    }
    Plugin* plugin = static_cast<Plugin*>(instance->pdata);
    if (plugin) {
      delete plugin;
      instance->pdata = 0;
    }
    return NPERR_NO_ERROR;
  }

  NPError NPP_SetWindow(NPP instance, NPWindow* window) {
    Debug::log(Debug::Info) << "NPP_SetWindow(instance=" << instance << ",window=" << window
        << ")" << Debug::flush;
    return NPERR_NO_ERROR;
  }
   

  NPError NPP_NewStream(NPP instance, NPMIMEType type, NPStream* stream, NPBool seekable, uint16* stype) {
    Debug::log(Debug::Info) << "NPP_NewStream(instance=" << instance << ")" << Debug::flush;
    *stype = NP_ASFILEONLY;
    return NPERR_NO_ERROR;
  }

  NPError NPP_DestroyStream(NPP instance, NPStream* stream, NPReason reason) {
    Debug::log(Debug::Info) << "NPP_DestroyStream(instance=" << instance << ")" << Debug::flush;
    return NPERR_NO_ERROR;
  }

  int32 NPP_WriteReady(NPP instance, NPStream* stream) {
    Debug::log(Debug::Info) << "NPP_WriteReady(instance=" << instance << ")" << Debug::flush;
    return 0;
  }

  int32 NPP_Write(NPP instance, NPStream* stream, int32 offset, int32 len, void* buffer) {
    Debug::log(Debug::Info) << "NPP_Write(instance=" << instance << ")" << Debug::flush;
    return 0;
  }

  void NPP_StreamAsFile(NPP instance, NPStream* stream, const char* fname) {
    Debug::log(Debug::Info) << "NPP_StreamAsFile(instance=" << instance << ")" << Debug::flush;
  }

  void NPP_Print(NPP instance, NPPrint* platformPrint) {
    Debug::log(Debug::Info) << "NPP_Print(instance=" << instance << ")" << Debug::flush;
  }

  int16 NPP_HandleEvent(NPP instance, void* event) {
    //Debug::log(Debug::Spam) << "NPP_HandleEvent(instance=" << instance << ")" << Debug::flush;
    return 0 ;
  }

  void NPP_URLNotify(NPP instance, const char* url, NPReason reason, void* notifyData) {
    Debug::log(Debug::Info) << "NPP_URLNotify(instance=" << instance << ")" << Debug::flush;
  }

  NPObject *NPP_GetScriptableInstance(NPP instance) {
    Debug::log(Debug::Info) << "NPP_GetScriptableInstance(instance=" << instance << ")" << Debug::flush;
    if (!instance) {
      return 0;
    }
    Plugin* plugin = static_cast<Plugin*>(instance->pdata);
    ScriptableInstance* scriptableInstance = plugin->getScriptableInstance();
    NPN_RetainObject(scriptableInstance);  // caller expects it retained.
    return scriptableInstance;
  }
  
  NPError NPP_GetValue(NPP instance, NPPVariable variable, void *value) {
    Debug::log(Debug::Info) << "NPP_GetValue(instance=" << instance << ",var=" << variable << ")"
        << Debug::flush;
    switch (variable) {
      case NPPVpluginScriptableNPObject:
        // here the plugin is asked by Mozilla to tell if it is scriptable
        // we should return a valid interface id and a pointer to 
        // nsScriptablePeer interface which we should have implemented
        // and which should be defined in the corressponding *.xpt file
        // in the bin/components folder
        *static_cast<NPObject**>(value) = NPP_GetScriptableInstance(instance);
        break;
      default:
        // pass other ones to the static version of GetValue
        return NP_GetValue(0, variable, value);
    }
    return NPERR_NO_ERROR;
  }

  NPError NP_GetValue(void*, NPPVariable variable, void *value) {
    Debug::log(Debug::Info) << "NP_GetValue(var=" << variable << ")" << Debug::flush;
    switch (variable) {
      case NPPVpluginNameString:
        *static_cast<const char **>(value) = "GWT Development-Mode Plugin";
        break;
      case NPPVpluginDescriptionString:
        *static_cast<const char **>(value) = "Plugin to enable debugging of Google Web Toolkit "
            "applications in development mode.";
        break;
      default:
        Debug::log(Debug::Info) << "NPP_GetValue(var=" << variable
            << ") -- unexpected variable type" << Debug::flush;
        return NPERR_GENERIC_ERROR;
    }
    return NPERR_NO_ERROR;
  }

  NPError NPP_SetValue(NPP instance, NPNVariable variable, void *value) {
    Debug::log(Debug::Info) << "NPP_SetValue(instance=" << instance << ",var=" << variable << ")"
        << Debug::flush;
    return NPERR_NO_ERROR;
  }
}
