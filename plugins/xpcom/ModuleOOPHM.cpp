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

#include "Debug.h"
#include "ExternalWrapper.h"

#include "nsCOMPtr.h"
#include "nsISupports.h"
#include "nsIXULAppInfo.h"
#include "nsIXULRuntime.h"
#include "nsServiceManagerUtils.h"
#include "nsXPCOMCID.h"

#if GECKO_VERSION >= 1900
#include "nsIClassInfoImpl.h"
#endif

#if GECKO_VERSION >= 2000
#include "mozilla/ModuleUtils.h"
#else
#include "nsIGenericFactory.h"
#include "nsICategoryManager.h"
#endif


// Allow a macro to be treated as a C string, ie -Dfoo=bar; QUOTE(foo) = "bar"
#define QUOTE_HELPER(x) #x
#define QUOTE(x) QUOTE_HELPER(x)

#ifdef _WINDOWS
#include <windows.h>

BOOL APIENTRY DllMain(HMODULE hModule, DWORD ulReasonForCall, LPVOID lpReserved) {
  switch (ulReasonForCall) {
    case DLL_PROCESS_ATTACH:
    case DLL_THREAD_ATTACH:
    case DLL_THREAD_DETACH:
    case DLL_PROCESS_DETACH:
      break;
  }
  return TRUE;
}
#endif

// This defines ExternalWrapperConstructor, which in turn instantiates via
// ExternalWrapper::ExternalWrapper()
NS_GENERIC_FACTORY_CONSTRUCTOR(ExternalWrapper);

static nsresult Load() {
  nsresult nr;
  nsCOMPtr<nsIXULAppInfo> app_info
      = do_GetService("@mozilla.org/xre/app-info;1", &nr);
  if (NS_FAILED(nr) || !app_info) {
    return NS_ERROR_FAILURE;
  }
  nsCString gecko_version;
  app_info->GetPlatformVersion(gecko_version);
  nsCString browser_version;
  app_info->GetVersion(browser_version);
  nsCOMPtr<nsIXULRuntime> xulRuntime
      = do_GetService("@mozilla.org/xre/app-info;1", &nr);
  if (NS_FAILED(nr) || !app_info) {
    return NS_ERROR_FAILURE;
  }
  nsCString os;
  xulRuntime->GetOS(os);
  nsCString abi;
  xulRuntime->GetXPCOMABI(abi);
  Debug::log(Debug::Info) << "Initializing GWT Developer Plugin"
      << Debug::flush;
  Debug::log(Debug::Info) << "  gecko=" << gecko_version.BeginReading()
      << ", firefox=" << browser_version.BeginReading() << ", abi="
      << os.BeginReading() << "_" << abi.BeginReading() << ", built for "
      QUOTE(BROWSER) << Debug::flush;
  return NS_OK;
}

static void Unload() {
  Debug::log(Debug::Debugging) << "ModuleOOPHM Unload()"
      << Debug::flush;
}

#if GECKO_VERSION >= 2000
/**
 * Gecko 2.0 has a completely different initialization mechanism
 */

// This defines kOOPHM_CID variable that refers to the OOPHM_CID
NS_DEFINE_NAMED_CID(OOPHM_CID);

// Build a table of ClassIDs (CIDs) which are implemented by this module. CIDs
// should be completely unique UUIDs.
// each entry has the form { CID, service, factoryproc, constructorproc }
// where factoryproc is usually NULL.
static const mozilla::Module::CIDEntry kOOPHMCIDs[] = {
  {&kOOPHM_CID, false, NULL, ExternalWrapperConstructor},
  {NULL }
};

// Build a table which maps contract IDs to CIDs.
// A contract is a string which identifies a particular set of functionality. In some
// cases an extension component may override the contract ID of a builtin gecko component
// to modify or extend functionality.
static const mozilla::Module::ContractIDEntry kOOPHMContracts[] = {
  {OOPHM_CONTRACTID, &kOOPHM_CID},
  {NULL}
};

// Category entries are category/key/value triples which can be used
// to register contract ID as content handlers or to observe certain
// notifications.
static const mozilla::Module::CategoryEntry kOOPHMCategories[] = {
  {"JavaScript-global-property", "__gwt_HostedModePlugin", OOPHM_CONTRACTID},
  {NULL}
};

static const mozilla::Module kModuleOOPHM = {
  mozilla::Module::kVersion,
  kOOPHMCIDs,
  kOOPHMContracts,
  kOOPHMCategories,
  NULL, /* Use the default factory */
  Load,
  Unload
};

NSMODULE_DEFN(ModuleOOPHM) = &kModuleOOPHM;

#else
/**
 * pre-Gecko2.0 initialization
 */
NS_DECL_CLASSINFO(ExternalWrapper);
static NS_IMETHODIMP registerSelf(nsIComponentManager *aCompMgr, nsIFile *aPath,
    const char *aLoaderStr, const char *aType,
    const nsModuleComponentInfo *aInfo) {

  Debug::log(Debug::Info)
      << "  successfully registered GWT Developer Plugin"
      << Debug::flush;
  nsresult rv;
  nsCOMPtr<nsICategoryManager> categoryManager =
      do_GetService(NS_CATEGORYMANAGER_CONTRACTID, &rv);

  NS_ENSURE_SUCCESS(rv, rv);

  rv = categoryManager->AddCategoryEntry("JavaScript global property",
      "__gwt_HostedModePlugin", OOPHM_CONTRACTID, true, true, nsnull);

  if (rv != NS_OK) {
    Debug::log(Debug::Error) << "ModuleOOPHM registerSelf returned " << rv
        << Debug::flush;
  }
  return rv;
}

static NS_IMETHODIMP factoryDestructor(void) {
  Unload();
  return NS_OK;
}

static NS_IMETHODIMP unregisterSelf(nsIComponentManager *aCompMgr,
    nsIFile *aPath, const char *aLoaderStr,
    const nsModuleComponentInfo *aInfo) {
  Debug::log(Debug::Info) << "Unregistered GWT Developer Plugin"
      << Debug::flush;
  return NS_OK;
}

static nsModuleComponentInfo components[] = {
    {
       OOPHM_CLASSNAME,
       OOPHM_CID,
       OOPHM_CONTRACTID,
       ExternalWrapperConstructor,
       registerSelf,
       unregisterSelf, /* unregister self */
       factoryDestructor, /* factory destructor */
       NS_CI_INTERFACE_GETTER_NAME(ExternalWrapper), /* get interfaces */
       nsnull, /* language helper */
       &NS_CLASSINFO_NAME(ExternalWrapper), /* global class-info pointer */
       0 /* class flags */
    }
};

// From Gears base/firefox/module.cc
static nsModuleInfo const kModuleInfo = {
  NS_MODULEINFO_VERSION,
  ("ExternalWrapperModule"),
  (components),
  (sizeof(components) / sizeof(components[0])),
  (nsnull),
  (nsnull)
};

NSGETMODULE_ENTRY_POINT(ExternalWrapperModule) (nsIComponentManager *servMgr,
    nsIFile* location, nsIModule** result) {
  Load();
  return NS_NewGenericModule2(&kModuleInfo, result);
}
#endif //GECKO_VERSION
