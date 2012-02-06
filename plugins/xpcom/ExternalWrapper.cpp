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
#include "nsIDOMWindow.h"
#if GECKO_VERSION <= 7000
#include "nsIDOMWindowInternal.h"
#endif //GECKO_VERSION
#include "nsIDOMLocation.h"
#include "nsXPCOMStrings.h"
#include "nsICategoryManager.h"
#include "nsIJSContextStack.h"
#include "nsIScriptContext.h"
#include "nsIScriptGlobalObject.h"
#include "nsPIDOMWindow.h"
#include "LoadModuleMessage.h"
#include "ServerMethods.h"
#include "BrowserChannel.h"
#include "AllowedConnections.h"

#if GECKO_VERSION >= 1900
#include "nsIClassInfoImpl.h"
#endif //GECKO_VERSION

#if GECKO_VERSION >= 2000
NS_IMPL_CLASSINFO(ExternalWrapper, NULL, 0, OOPHM_CID)
#endif //GECKO_VERSION

NS_IMPL_ISUPPORTS2_CI(ExternalWrapper, IOOPHM, nsISecurityCheckedComponent)

ExternalWrapper::ExternalWrapper() {
  Debug::log(Debug::Debugging) << "ExternalWrapper::ExternalWrapper(this="
      << this << ")" << Debug::flush;
  preferences = new Preferences();
  windowWatcher = do_GetService(NS_WINDOWWATCHER_CONTRACTID);
  if (!windowWatcher) {
    Debug::log(Debug::Warning) << "Can't get WindowWatcher service"
        << Debug::flush;
    return;
  }
}

ExternalWrapper::~ExternalWrapper() {
  Debug::log(Debug::Debugging) << "ExternalWrapper::~ExternalWrapper(this="
      << this << ")" << Debug::flush;
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

/**
 * Get JS window object.
 *
 * @param win output parameter to store the window object
 * @return true on success
 */
static bool getWindowObject(nsIDOMWindow** win) {
  // Get JSContext from stack.
  nsCOMPtr<nsIJSContextStack> stack =
      do_GetService("@mozilla.org/js/xpc/ContextStack;1");
  if (!stack) {
    Debug::log(Debug::Error) << "getWindowObject: no context stack"
        << Debug::flush;
    return false;
  }
  JSContext *cx;
  if (NS_FAILED(stack->Peek(&cx)) || !cx) {
    Debug::log(Debug::Error) << "getWindowObject: no context on stack"
        << Debug::flush;
    return false;
  }
  if (!(::JS_GetOptions(cx) & JSOPTION_PRIVATE_IS_NSISUPPORTS)) {
    Debug::log(Debug::Error)
        << "getWindowObject: context doesn't have nsISupports" << Debug::flush;
    return false;
  }

  nsCOMPtr<nsIScriptContext> scx =
    do_QueryInterface(static_cast<nsISupports *>
                                 (::JS_GetContextPrivate(cx)));
  if (!scx) {
    Debug::log(Debug::Error) << "getWindowObject: no script context"
        << Debug::flush;
    return false;
  }
  nsCOMPtr<nsIScriptGlobalObject> globalObj = scx->GetGlobalObject();
  if (!globalObj) {
    Debug::log(Debug::Error) << "getWindowObject: no global object"
        << Debug::flush;
    return false;
  }
  nsCOMPtr<nsPIDOMWindow> window = do_QueryInterface(globalObj);
  if (!window) {
    Debug::log(Debug::Error) << "getWindowObject: window is null"
        << Debug::flush;
    return false;
  }
  NS_ADDREF(*win = window);
  return true;
}

/**
 * Get the URL of a window.
 *
 * @param win DOMWindowInternal instance
 * @param url output wide string for the URL
 * @return true if successful
 */
static bool getWindowUrl(nsIDOMWindowInternal* win, nsAString& url) {
  nsCOMPtr<nsIDOMLocation> loc;
  if (win->GetLocation(getter_AddRefs(loc)) != NS_OK) {
    Debug::log(Debug::Info) << "Unable to get location" << Debug::flush;
    return false;
  }
  if (loc->GetHref(url) != NS_OK) {
    Debug::log(Debug::Info) << "Unable to get URL" << Debug::flush;
    return false;
  }
  return true;
}

/**
 * Get the top-level window for a given window, and its URL.
 *
 * @param win window to start from
 * @param topWinRet output parameter to store top window
 * @param topUrl output parameter to store URL
 * @return true on success, false on error (already logged)
 */
static bool getTopWindow(nsIDOMWindow* win, nsIDOMWindowInternal** topWinRet,
    nsAString& topUrl) {
  nsCOMPtr<nsIDOMWindow> topWin;
  if (win->GetTop(getter_AddRefs(topWin)) != NS_OK) {
    Debug::log(Debug::Error) << "Unable to get top window" << Debug::flush;
    return false;
  }
  nsresult rv;
  nsCOMPtr<nsIDOMWindowInternal> topWinInt = do_QueryInterface(topWin, &rv);
  if (rv != NS_OK) {
    Debug::log(Debug::Error) << "Unable to QI DOMWindowInternal"
        << Debug::flush;
    return false;
  }
  if (!getWindowUrl(topWinInt, topUrl)) {
    Debug::log(Debug::Error) << "Unable to get url of top window"
        << Debug::flush;
    return false;
  }
  NS_ADDREF(*topWinRet = topWinInt);
  return true;
}

std::string ExternalWrapper::computeTabIdentity() {
  std::string returnVal;
  if (!windowWatcher) {
    return returnVal;
  }
  // The nsPIDOMWindow interface of our top-level window appears to be stable
  // across refreshes, so we will use that for our tab ID.
  nsCOMPtr<nsPIDOMWindow> privateWin = do_QueryInterface(topWindow);
  if (!privateWin) {
    return returnVal;
  }
  char buf[20]; // typically 8-16 hex digits plus 0x, not horrible if truncated
  snprintf(buf, sizeof(buf), "%p", privateWin.get());
  buf[19] = 0; // ensure null termination
  returnVal = buf;
  return returnVal;
}

// TODO(jat): remove suppliedWindow and update hosted.html API

#if GECKO_VERSION < 10000
NS_IMETHODIMP ExternalWrapper::Init(nsIDOMWindow* suppliedWindow,
    PRBool *_retval) {
#else
NS_IMETHODIMP ExternalWrapper::Init(nsIDOMWindow* suppliedWindow,
    bool *_retval) {
#endif //GECKO_VERSION

  Debug::log(Debug::Debugging) << "Plugin initialized from hosted.html"
      << Debug::flush;
  *_retval = false;
  nsCOMPtr<nsIDOMWindow> computedWindow;
  if (getWindowObject(getter_AddRefs(computedWindow))) {
    Debug::log(Debug::Debugging) << " passed window=" << suppliedWindow
        << ", computed=" << computedWindow << Debug::flush;
    domWindow = computedWindow;
  } else {
    Debug::log(Debug::Warning) << " using supplied window object"
        << Debug::flush;
    // TODO(jat): remove this
    domWindow = suppliedWindow;
  }
  if (getTopWindow(domWindow, getter_AddRefs(topWindow), url)) {
    *_retval = true;
  }
  return NS_OK;
}

bool ExternalWrapper::askUserToAllow(const std::string& url) {
  nsCOMPtr<nsIPromptService> promptService = do_GetService(
      "@mozilla.org/embedcomp/prompt-service;1");
  if (!promptService) {
    return false;
  }
  NS_ConvertASCIItoUTF16 title("Allow GWT Developer Plugin Connection");
  NS_ConvertASCIItoUTF16 text("The web and code server combination is unrecognized and requesting a GWT "
      "developer plugin connection -- do you want to allow it?");
  NS_ConvertASCIItoUTF16 checkMsg("Remember this decision for this server "
      "(change in GWT Developer Plugin preferences)");

#if GECKO_VERSION < 10000
  // Please see: https://bugzilla.mozilla.org/show_bug.cgi?id=681188
  PRBool remember = false;
  PRBool include = true;
  if (promptService->ConfirmCheck(domWindow.get(), title.get(), text.get(),
      checkMsg.get(), &remember, &include) != NS_OK) {
    return false;
  }

  if (remember) {
    std::string host = AllowedConnections::getHostFromUrl(url);
    std::string server = AllowedConnections::getCodeServerFromUrl(url);
    preferences->addNewRule(host + "/" + server, !include);
  }
  return include;

#else

  bool remember = false;
  bool include = true;
  if (promptService->ConfirmCheck(domWindow.get(), title.get(), text.get(),
      checkMsg.get(), &remember, &include) != NS_OK) {
    return false;
  }

  if (remember) {
    std::string host = AllowedConnections::getHostFromUrl(url);
    std::string server = AllowedConnections::getCodeServerFromUrl(url);
    preferences->addNewRule(host + "/" + server, !include);
  }

  return include;

#endif //GECKO_VERSION
}

// TODO(jat): remove suppliedUrl and update hosted.html API
#if GECKO_VERSION < 10000
NS_IMETHODIMP ExternalWrapper::Connect(const nsACString& suppliedUrl,
                const nsACString& sessionKey, const nsACString& aAddr,
                const nsACString& aModuleName, const nsACString& hostedHtmlVersion,
                PRBool *_retval) {
#else
NS_IMETHODIMP ExternalWrapper::Connect(const nsACString& suppliedUrl,
                const nsACString& sessionKey, const nsACString& aAddr,
                const nsACString& aModuleName, const nsACString& hostedHtmlVersion,
                bool *_retval) {
#endif //GECKO_VERSION

  Debug::log(Debug::Info) << "Connect(url=" <<  url << ", sessionKey="
      << sessionKey << ", address=" << aAddr << ", module=" << aModuleName
      << ", hostedHtmlVersion=" << hostedHtmlVersion << Debug::flush;

  // TODO: string utilities?
  nsCString urlAutoStr;
  NS_UTF16ToCString(url, NS_CSTRING_ENCODING_UTF8, urlAutoStr);
  nsCString sessionKeyAutoStr(sessionKey);
  nsCString addrAutoStr(aAddr);
  nsCString moduleAutoStr(aModuleName);
  nsCString hostedHtmlVersionAutoStr(hostedHtmlVersion);
  std::string hostedUrl(addrAutoStr.get());
  std::string urlStr(urlAutoStr.get());

  bool allowed = false;
  std::string webHost = AllowedConnections::getHostFromUrl(urlStr);
  std::string codeServer = AllowedConnections::getCodeServerFromUrl(urlStr);
  if (!AllowedConnections::matchesRule( webHost, codeServer, &allowed)) {
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

  // TODO(jat): leaks HostChannel -- need to save it in a session object and
  // return that so the host page can call a disconnect method on it at unload
  // time or when it gets GC'd.
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
      BROWSERCHANNEL_PROTOCOL_VERSION, hostedHtmlVersionStr)) {
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

NS_IMETHODIMP ExternalWrapper::CanCreateWrapper(const nsIID * iid,
    char **_retval) {
  Debug::log(Debug::Spam) << "ExternalWrapper::CanCreateWrapper"
      << Debug::flush;
  *_retval = cloneAllAccess();
  return NS_OK;
}

NS_IMETHODIMP ExternalWrapper::CanCallMethod(const nsIID * iid,
    const PRUnichar *methodName, char **_retval) {
  Debug::log(Debug::Spam) << "ExternalWrapper::CanCallMethod" << Debug::flush;
  if (strEquals(methodName, "connect") || strEquals(methodName, "init")) {
    *_retval = cloneAllAccess();
  } else {
    *_retval = nsnull;
  }
  return NS_OK;
}

NS_IMETHODIMP ExternalWrapper::CanGetProperty(const nsIID * iid,
    const PRUnichar *propertyName, char **_retval) {
  Debug::log(Debug::Spam) << "ExternalWrapper::CanGetProperty" << Debug::flush;
  *_retval = nsnull;
  return NS_OK;
}
NS_IMETHODIMP ExternalWrapper::CanSetProperty(const nsIID * iid,
    const PRUnichar *propertyName, char **_retval) {
  Debug::log(Debug::Spam) << "ExternalWrapper::CanSetProperty" << Debug::flush;
  *_retval = nsnull;
  return NS_OK;
}
