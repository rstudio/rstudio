/*
 * SessionPackages.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_PACKAGES_HPP
#define SESSION_PACKAGES_HPP

#include <string>

#include <boost/function.hpp>
#include <boost/noncopyable.hpp>

namespace rstudio {
namespace core {
   class Error;
   namespace json {
      class Object;
   }
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace packages {

core::Error initialize();
void enquePackageStateChanged();

// Cheap syntactic pre-filter: returns true if 'input' contains a function call
// of the form 'fn(' or 'pkg::fn('. This only gates the more expensive, precise
// check in '.rs.isPackageManagementCall' (which parses the input and resolves
// each call's namespace), so that call-free console input -- assignments,
// comments, bare expressions -- never pays for it. Exposed for testing; also
// used by onConsolePrompt.
bool containsCallSyntax(const std::string& input);

// Serializes package-state builds that nest (see the comment above
// buildPackageStateJson() in SessionPackages.cpp). A build that starts while
// another is on the stack is folded into it where possible, and a build runs
// again before handing anything back if a request arrived while it was
// running, so that whatever is returned or delivered reflects the last
// request. An event that a failed or capped build could not deliver stays
// owed until the next build delivers it. Main thread only. Exposed for
// testing.
class PackageStateBuilder : boost::noncopyable
{
public:
   typedef boost::function<core::Error(core::json::Object*)> BuildFunction;

   // Upper bound on the passes one build makes. Each extra pass needs a fresh
   // request to arrive mid-scan, so this is a guard against a pathological
   // stream of requests, not a working limit. Past it the last pass is handed
   // back and the event stays owed.
   static constexpr int kMaxPasses = 3;

   explicit PackageStateBuilder(const BuildFunction& build);

   // Build the package state into *pJson for a caller that needs the result
   // (the get_package_state RPC). Always builds, even inside another build --
   // the caller can't wait -- and in that case has the outer build run again,
   // since its list is now older than this one.
   //
   // *pDeliverEvent is set when the caller should also enqueue *pJson as a
   // kPackageStateChanged event, on behalf of a buildForEvent() request that
   // was folded into this build, or left owed by an earlier build.
   core::Error build(core::json::Object* pJson, bool* pDeliverEvent);

   // Build the package state into *pJson for delivery as a kPackageStateChanged
   // event. If a build is already on the stack, the request is folded into it:
   // *pJson is left untouched, *pDeliverEvent is false, and the in-progress
   // build runs again and delivers the event instead. Otherwise *pDeliverEvent
   // is set on success, unless the build hit the pass cap.
   core::Error buildForEvent(core::json::Object* pJson, bool* pDeliverEvent);

   // Whether a kPackageStateChanged event is still owed: a build failed or hit
   // the pass cap before it could be delivered. The next build delivers it.
   bool eventPending() const;

private:
   core::Error run(bool needsResult,
                   bool wantsEvent,
                   core::json::Object* pJson,
                   bool* pDeliverEvent);

   BuildFunction build_;
   int depth_ = 0;               // builds on the stack
   bool rebuildPending_ = false; // a request arrived during the current pass
   bool eventPending_ = false;   // an event is owed, even after a failed build
};

} // namespace packages
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_PACKAGES_HPP
