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
#include "JSRunner.h"

#include "nsCOMPtr.h"
#include "nsIPrincipal.h"
#include "nsIScriptGlobalObject.h"
#include "nsIScriptObjectPrincipal.h"
#include "nsIURI.h"
#include "nsIXPConnect.h"
#if GECKO_VERSION >= 13000
#include "nsJSPrincipals.h"
#endif
#include "nsStringAPI.h"

// from js_runner_ff.cc in Gears (http://code.google.com/p/gears/)

bool JSRunner::eval(JSContext* ctx, JSObject* object, const std::string& script) {
  // To eval the script, we need the JSPrincipals to be acquired through
  // nsIPrincipal.  nsIPrincipal can be queried through the
  // nsIScriptObjectPrincipal interface on the Script Global Object.  In order
  // to get the Script Global Object, we need to request the private data
  // associated with the global JSObject on the current context.
  nsCOMPtr<nsIScriptGlobalObject> sgo;
  nsISupports *priv = reinterpret_cast<nsISupports *>(JS_GetPrivate(
#if GECKO_VERSION < 13000
                                                          ctx,
#endif
                                                          object));
  nsCOMPtr<nsIXPConnectWrappedNative> wrapped_native = do_QueryInterface(priv);

  if (wrapped_native) {
    // The global object is a XPConnect wrapped native, the native in
    // the wrapper might be the nsIScriptGlobalObject.
    sgo = do_QueryWrappedNative(wrapped_native);
  } else {
    sgo = do_QueryInterface(priv);
  }

  JSPrincipals *jsprin = nsnull;
  std::string virtual_filename;
  nsresult nr;

  nsCOMPtr<nsIScriptObjectPrincipal> obj_prin = do_QueryInterface(sgo, &nr);
  if (NS_FAILED(nr)) {
    Debug::log(Debug::Error) << "Error getting object principal" << Debug::flush;
    return false;
  }

  nsIPrincipal *principal = obj_prin->GetPrincipal();
  if (!principal) {
    Debug::log(Debug::Error) << "Error getting principal" << Debug::flush;
    return false;
  }

  // Get the script scheme and host from the principal.  This is the URI that
  // Firefox treats this script as running from.

  // If the codebase is null, the script may be running from a chrome context.
  // In that case, don't construct a virtual filename.

  nsCOMPtr<nsIURI> codebase;
  nr = principal->GetURI(getter_AddRefs(codebase));
  if (codebase) { 
    nsCString scheme;
    nsCString host;

    if (NS_FAILED(codebase->GetScheme(scheme)) ||
        NS_FAILED(codebase->GetHostPort(host))) {
      Debug::log(Debug::Error) << "Error getting codebase" << Debug::flush;
      return false;
    }

    // Build a virtual filename that we'll run as.  This is to workaround
    // http://lxr.mozilla.org/seamonkey/source/dom/src/base/nsJSEnvironment.cpp#500
    // Bug: https://bugzilla.mozilla.org/show_bug.cgi?id=387477
    // The filename is being used as the security origin instead of the principal.
    // TODO(zork): Remove this section if this bug is resolved.
    virtual_filename = std::string(scheme.BeginReading());
    virtual_filename += "://";
    virtual_filename += host.BeginReading();
  }

#if GECKO_VERSION >= 13000
  jsprin = nsJSPrincipals::get(principal);
#else
  principal->GetJSPrincipals(ctx, &jsprin);
#endif

  if (jsprin == nsnull) {
    Debug::log(Debug::Error) << "Get JSPrincial failed at JSRunner::eval"
        << Debug::flush;
    return false;
  }

  // Set up the JS stack so that our context is on top.  This is needed to
  // play nicely with plugins that access the context stack, such as Firebug.
//  nsCOMPtr<nsIJSContextStack> stack =
//      do_GetService("@mozilla.org/js/xpc/ContextStack;1");
//  if (!stack) { return false; }
//
//  stack->Push(js_engine_context_);

  uintN line_number_start = 0;
  jsval rval;
  JSBool js_ok = JS_EvaluateScriptForPrincipals(ctx, object, jsprin,
      script.c_str(), script.length(), virtual_filename.c_str(),
      line_number_start, &rval);

  // Restore the context stack.
//  JSContext *cx;
//  stack->Pop(&cx);

  // Decrements ref count on jsprin (Was added in GetJSPrincipals()).
#if GECKO_VERSION >= 13000
  (void) JS_DropPrincipals(JS_GetRuntime(ctx), jsprin);
#else
  (void) JSPRINCIPALS_DROP(ctx, jsprin);
#endif
  if (!js_ok) {
    Debug::log(Debug::Error) << "JS execution failed in JSRunner::eval"
        << Debug::flush;
    return false;
  }

  return true;
}
