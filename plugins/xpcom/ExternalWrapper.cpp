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

#include "ExternalWrapper.h"

#include "nsIHttpProtocolHandler.h"
#include "nsISupports.h"
#include "nsNetCID.h"
#include "nsCOMPtr.h"
#include "nsMemory.h"
#include "nsServiceManagerUtils.h"
#include "nsIPromptService.h"

#ifndef NS_IMPL_ISUPPORTS2_CI
#include "nsIClassInfoImpl.h" // 1.9 only
#endif

#include "LoadModuleMessage.h"
#include "ServerMethods.h"
#include "BrowserChannel.h"
#include "AllowedConnections.h"

NS_IMPL_ISUPPORTS2_CI(ExternalWrapper, IOOPHM, nsISecurityCheckedComponent)

ExternalWrapper::ExternalWrapper() {
  Debug::log(Debug::Spam) << "ExternalWrapper::ExternalWrapper()"
      << Debug::flush;
  preferences = new Preferences();
  windowWatcher = do_GetService(NS_WINDOWWATCHER_CONTRACTID);
  if (!windowWatcher) {
    Debug::log(Debug::Warning) << "Can't get WindowWatcher service"
        << Debug::flush;
    return;
  }
}

ExternalWrapper::~ExternalWrapper() {
  Debug::log(Debug::Spam) << "ExternalWrapper::~ExternalWrapper" << Debug::flush;
}

// define the CID for nsIHttpProtocolHandler
static NS_DEFINE_CID(kHttpHandlerCID, NS_HTTPPROTOCOLHANDLER_CID);

static nsresult getUserAgent(std::string& userAgent) {
  nsresult res;
  nsCOMPtr<nsIHttpProtocolHandler> http = do_GetService(kHttpHandlerCID, &res);
  if (NS_FAILED(res)) {
    return res;
  }
  nsCString userAgentStr;
  res = http->GetUserAgent(userAgentStr);
  if (NS_FAILED(res)) {
    return res;
  }
  userAgent.assign(userAgentStr.get());
  return NS_OK;
}

std::string ExternalWrapper::computeTabIdentity() {
  std::string returnVal;
  if (!windowWatcher) {
    return returnVal;
  }
  nsCOMPtr<nsIDOMWindow> topWindow(domWindow);
  if (topWindow->GetTop(getter_AddRefs(topWindow)) != NS_OK) {
    Debug::log(Debug::Warning) << "Unable to get top window" << Debug::flush;
    return returnVal;
  }
  nsCOMPtr<nsIWebBrowserChrome> chrome;
  if (windowWatcher->GetChromeForWindow(topWindow.get(),
      getter_AddRefs(chrome)) != NS_OK) {
    Debug::log(Debug::Warning) << "Unable to get browser chrome for window"
        << Debug::flush;
    return returnVal;
  }
  Debug::log(Debug::Debugging) << "computeTabIdentity: browserChrome = "
      << (void*) chrome.get() << Debug::flush;
  // TODO(jat): find a way to get the tab from the chrome window
  return returnVal;
}

NS_IMETHODIMP ExternalWrapper::Init(nsIDOMWindow* domWindow,
    PRBool *_retval) {
  Debug::log(Debug::Spam) << "Init" << Debug::flush;
  this->domWindow = domWindow;
  *_retval = true;
  return NS_OK;
}

bool ExternalWrapper::askUserToAllow(const std::string& url) {
  nsCOMPtr<nsIPromptService> promptService = do_GetService(
      "@mozilla.org/embedcomp/prompt-service;1");
  if (!promptService) {
    return false;
  }
  NS_ConvertASCIItoUTF16 title("Allow GWT Development Mode Connection");
  NS_ConvertASCIItoUTF16 text("This web server is requesting a GWT "
      "development mode connection -- do you want to allow it?");
  NS_ConvertASCIItoUTF16 checkMsg("Remember this decision for this server "
      "(change in GWT plugin preferences)");
  PRBool remember = false;
  PRBool include = true;
  if (promptService->ConfirmCheck(domWindow.get(), title.get(), text.get(),
      checkMsg.get(), &remember, &include) != NS_OK) {
    return false;
  }
  if (remember) {
    preferences->addNewRule(AllowedConnections::getHostFromUrl(url), !include);
  }
  return include;
}

NS_IMETHODIMP ExternalWrapper::Connect(const nsACString& url,
		const nsACString& sessionKey, const nsACString& aAddr,
		const nsACString& aModuleName, const nsACString& hostedHtmlVersion,
		PRBool *_retval) {
  Debug::log(Debug::Spam) << "Connect(url=" << url << ", sessionKey="
      << sessionKey << ", address=" << aAddr << ", module=" << aModuleName
      << ", hostedHtmlVersion=" << hostedHtmlVersion << Debug::flush;

  // TODO: string utilities?
  nsCString urlAutoStr(url);
  nsCString sessionKeyAutoStr(sessionKey);
  nsCString addrAutoStr(aAddr);
  nsCString moduleAutoStr(aModuleName);
  nsCString hostedHtmlVersionAutoStr(hostedHtmlVersion);
  std::string hostedUrl(addrAutoStr.get());
  std::string urlStr(urlAutoStr.get());

  bool allowed = false;
  if (!AllowedConnections::matchesRule(urlStr, &allowed)) {
    // If we didn't match an existing rule, prompt the user
    allowed = askUserToAllow(urlStr);
  }
  if (!allowed) {
    *_retval = false;
    return NS_OK;
  }

  size_t index = hostedUrl.find(':');
  if (index == std::string::npos) {
    *_retval = false;
    return NS_OK;
  }
  std::string hostPart = hostedUrl.substr(0, index);
  std::string portPart = hostedUrl.substr(index + 1);

  HostChannel* channel = new HostChannel();

  Debug::log(Debug::Debugging) << "Connecting..." << Debug::flush;

  if (!channel->connectToHost(hostPart.c_str(),
      atoi(portPart.c_str()))) {
    *_retval = false;
    return NS_OK;
  }

  Debug::log(Debug::Debugging) << "...Connected" << Debug::flush;
  sessionHandler.reset(new FFSessionHandler(channel/*, ctx*/));

  std::string hostedHtmlVersionStr(hostedHtmlVersionAutoStr.get());
  if (!channel->init(sessionHandler.get(), BROWSERCHANNEL_PROTOCOL_VERSION,
      BROWSERCHANNEL_PROTOCOL_VERSION,
      hostedHtmlVersionStr)) {
    *_retval = false;
    return NS_OK;
  }

  std::string moduleName(moduleAutoStr.get());
  std::string userAgent;

  // get the user agent
  nsresult res = getUserAgent(userAgent);
  if (NS_FAILED(res)) {
    return res;
  }

  std::string tabKeyStr = computeTabIdentity();
  std::string sessionKeyStr(sessionKeyAutoStr.get());

  LoadModuleMessage::send(*channel, urlStr, tabKeyStr, sessionKeyStr,
      moduleName, userAgent, sessionHandler.get());

  // TODO: return session object?
  *_retval = true;
  return NS_OK;
}

// nsISecurityCheckedComponent
static char* cloneAllAccess() {
  static const char allAccess[] = "allAccess";
  return static_cast<char*>(nsMemory::Clone(allAccess, sizeof(allAccess)));
}

static bool strEquals(const PRUnichar* utf16, const char* ascii) {
  nsCString utf8;
  NS_UTF16ToCString(nsDependentString(utf16), NS_CSTRING_ENCODING_UTF8, utf8);
  return strcmp(ascii, utf8.get()) == 0;
}

NS_IMETHODIMP ExternalWrapper::CanCreateWrapper(const nsIID * iid, char **_retval) {
  Debug::log(Debug::Spam) << "ExternalWrapper::CanCreateWrapper" << Debug::flush;
  *_retval = cloneAllAccess();
  return NS_OK;
}

NS_IMETHODIMP ExternalWrapper::CanCallMethod(const nsIID * iid, const PRUnichar *methodName, char **_retval) {
  Debug::log(Debug::Spam) << "ExternalWrapper::CanCallMethod" << Debug::flush;
  if (strEquals(methodName, "connect") || strEquals(methodName, "init")) {
    *_retval = cloneAllAccess();
  } else {
    *_retval = nsnull;
  }
  return NS_OK;
}

NS_IMETHODIMP ExternalWrapper::CanGetProperty(const nsIID * iid, const PRUnichar *propertyName, char **_retval) {
  Debug::log(Debug::Spam) << "ExternalWrapper::CanGetProperty" << Debug::flush;
  *_retval = nsnull;
  return NS_OK;
}
NS_IMETHODIMP ExternalWrapper::CanSetProperty(const nsIID * iid, const PRUnichar *propertyName, char **_retval) {
  Debug::log(Debug::Spam) << "ExternalWrapper::CanSetProperty" << Debug::flush;
  *_retval = nsnull;
  return NS_OK;
}
