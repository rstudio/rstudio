/*
 * Copyright 2009 Google Inc.
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

#include "Preferences.h"
#include "Debug.h"
#include "AllowedConnections.h"

#include "nsCOMPtr.h"
#include "nsStringAPI.h"
#include "nsISupportsImpl.h"
#include "nsIObserver.h"
#include "nsIPrefService.h"
#include "nsIPrefBranch.h"
#include "nsIPrefBranch2.h"
#include "nsServiceManagerUtils.h"

#define GWT_DEV_PREFS_PREFIX "gwt-dev-plugin."
#define GWT_DEV_ACCESS_LIST "accessList"

NS_IMPL_ADDREF(Preferences)
NS_IMPL_RELEASE(Preferences)
NS_IMPL_QUERY_INTERFACE1(Preferences, nsIObserver)

Preferences::Preferences() {
  nsCOMPtr<nsIPrefService> prefService = do_GetService(
      NS_PREFSERVICE_CONTRACTID);
  if (!prefService) {
    Debug::log(Debug::Error) << "Unable to get preference service"
        << Debug::flush;
    return;
  }
  nsCOMPtr<nsIPrefBranch> branch;
  prefService->GetBranch(GWT_DEV_PREFS_PREFIX, getter_AddRefs(branch));
  if (!branch) {
    Debug::log(Debug::Error) << "Unable to get " GWT_DEV_PREFS_PREFIX
        " preference branch" << Debug::flush;
    return;
  }
  prefs = do_QueryInterface(branch);
  if (!prefs) {
    Debug::log(Debug::Error) << "Unable to get nsIPrefBranch2" << Debug::flush;
    return;
  }
  prefs->AddObserver(GWT_DEV_ACCESS_LIST, this, PR_FALSE);
  nsCString prefValue;
  if (branch->GetCharPref(GWT_DEV_ACCESS_LIST, getter_Copies(prefValue))
      == NS_OK) {
    loadAccessList(prefValue.get());
  }
}

// implements nsIObserver
NS_IMETHODIMP
Preferences::Observe(nsISupports *aSubject, const char* aTopic,
    const PRUnichar *aData) {
  nsresult rv = NS_OK;
  Debug::log(Debug::Spam) << "Preferences::Observe(subject="
      << aSubject << ", topic=" << aTopic << ", data=" << aData << Debug::flush;
  if (strcmp(aTopic, NS_PREFBRANCH_PREFCHANGE_TOPIC_ID)) {
    return NS_ERROR_UNEXPECTED;
  }
  // TODO(jat): check preference name in aData if we ever add another one
  nsCOMPtr<nsIPrefBranch> prefs(do_QueryInterface(aSubject, &rv));
  NS_ENSURE_SUCCESS(rv, rv);
  nsCString prefValue;
  if (prefs->GetCharPref(GWT_DEV_ACCESS_LIST, getter_Copies(prefValue))
      == NS_OK) {
    loadAccessList(prefValue.get());
  }
  return NS_OK;
}

void Preferences::addNewRule(const std::string& pattern, bool exclude) {
  nsCString prefValue;
  if (prefs->GetCharPref(GWT_DEV_ACCESS_LIST, getter_Copies(prefValue))
      != NS_OK) {
    Debug::log(Debug::Error) << "Unable to retrieve access list preference"
        << Debug::flush;
    return;
  }
  // TODO(jat): see if the same rule already exists
  std::string pref(prefValue.get());
  if (pref.length() > 0) {
    pref += ',';
  }
  if (exclude) {
    pref += '!';
  }
  pref += pattern;
  if (prefs->SetCharPref(GWT_DEV_ACCESS_LIST, pref.c_str()) != NS_OK) {
    Debug::log(Debug::Error) << "Unable to save modified access list preference"
        << Debug::flush;
    return;
  }
}

void Preferences::loadAccessList(const char* prefValue) {
  if (!prefValue) {
    return;
  }
  Debug::log(Debug::Spam) << "loadFromAccessList(prefValue=" << prefValue << ")"
      << Debug::flush;
  AllowedConnections::initFromAccessList(prefValue);
}

Preferences::~Preferences() {
  if (prefs) {
    prefs->RemoveObserver(GWT_DEV_ACCESS_LIST, this);
  }
}
