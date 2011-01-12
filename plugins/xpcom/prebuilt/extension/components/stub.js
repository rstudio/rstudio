// Copyright 2009, Google Inc.
//
// Redistribution and use in source and binary forms, with or without 
// modification, are permitted provided that the following conditions are met:
//
//  1. Redistributions of source code must retain the above copyright notice, 
//   this list of conditions and the following disclaimer.
//  2. Redistributions in binary form must reproduce the above copyright notice,
//   this list of conditions and the following disclaimer in the documentation
//   and/or other materials provided with the distribution.
//  3. Neither the name of Google Inc. nor the names of its contributors may be
//   used to endorse or promote products derived from this software without
//   specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
// EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
// OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
// OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// Our binary is compiled against different versions of the Gecko SDK for
// different versions of Firefox. But we want a single XPI so that users can
// switch between versions of Firefox without having to change their Gears
// version.

// This JavaScript file is detected and loaded by Gecko when our extension is
// installed. We then use nsIComponentRegistrar to tell Gecko where our real
// components are located, depending on what version of Firefox we detect we are
// running in.

// NOTE: This file is only used pre Gecko 2.0 (FF4). The registration mechanism
// in Gecko 2.0 allows the chrome.manifest to indicate the appropriate binary
// component for each ABI.

const Cc = Components.classes;
const Ci = Components.interfaces;

// Detect which version of our lib we should use.
function getLibFileName() {
  var appInfo = Cc["@mozilla.org/xre/app-info;1"].getService(Ci.nsIXULAppInfo);
  var geckoVersion = appInfo.platformVersion.substring(0, 3);

  if (geckoVersion == "1.8") {
    return "ff2";
  }

  if (geckoVersion.substring(0, 3) == "1.9") {
    var firefoxVersion = appInfo.version.substring(0, 3);

    if (firefoxVersion == "3.0") {
      if (isFedora()) {
        return "ff3+";
      }
      return "ff3";
    }

    if (firefoxVersion == "3.5") {
      return "ff35";
    }

    if (firefoxVersion == "3.6") {
      return "ff36";
    }

    if (firefoxVersion == "3.7" || firefoxVersion == "4.0") {
      return "ff40";
    }

    throw "Unsupported Firefox version: " + firefoxVersion;
  }

  throw "Unsupported Gecko version: " + geckoVersion;
}

function getPlatform() {
  var runtime = Cc["@mozilla.org/xre/app-info;1"].getService(Ci.nsIXULRuntime);

  if (runtime.OS == "Darwin") {
    return runtime.OS + "-gcc3";
  }

  return runtime.OS + "_" + runtime.XPCOMABI;
}

function isFedora() {
  var navigator = Cc["@mozilla.org/network/protocol;1?name=http"].
    getService(Ci.nsIHttpProtocolHandler);

  return navigator.userAgent.indexOf("Fedora") != -1;
}

// This function is called by Firefox at installation time.
function NSGetModule() {
  return {
    registerSelf: function(compMgr, location, loaderStr, type) {
      var libFile = location.parent.parent;
      libFile.append("lib");
      libFile.append(getPlatform());
      libFile.append(getLibFileName());

      // Note: we register a directory instead of an individual file because
      // Gecko will only load components with a specific file name pattern. We 
      // don't want this file to have to know about that. Luckily, if you
      // register a directory, Gecko will look inside the directory for files
      // to load.
      var compMgr = compMgr.QueryInterface(Ci.nsIComponentRegistrar);
      compMgr.autoRegister(libFile);
    }
  }
}
