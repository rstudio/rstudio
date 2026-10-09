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

#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
   class Error;
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
// running. Work left over after a failed or capped build is retained for a
// deferred event build. Main thread only. Exposed for testing.
class PackageStateBuilder : boost::noncopyable
{
public:
   typedef boost::function<core::Error(core::json::Object*)> BuildFunction;

   // Upper bound on the passes one build makes. Each extra pass needs a fresh
   // request to arrive mid-scan, so this is a guard against a pathological
   // stream of requests. Unfinished work is retried asynchronously.
   static constexpr int kMaxPasses = 3;

   explicit PackageStateBuilder(const BuildFunction& build);

   // Build the package state into *pJson for a caller that needs the result
   // (the get_package_state RPC). Always builds, even inside another build --
   // the caller can't wait -- and in that case has the outer build run again,
   // since its list is now older than this one.
   //
   // *pDeliverEvent is set when the caller should also enqueue *pJson as a
   // kPackageStateChanged event, on behalf of a buildForEvent() request that
   // was folded into this build. At the pass limit, returns the newest
   // successful snapshot (including nested results) and defers the event.
   core::Error build(core::json::Object* pJson, bool* pDeliverEvent);

   // Build the package state into *pJson for delivery as a kPackageStateChanged
   // event. If a build is already on the stack, the request is folded into it:
   // *pJson is left untouched, *pDeliverEvent is false, and the in-progress
   // build runs again and delivers the event instead. *pDeliverEvent is set
   // only when a successful build has no outstanding refresh requests.
   core::Error buildForEvent(core::json::Object* pJson, bool* pDeliverEvent);

   // Whether an event still needs a build, and no build is on the stack.
   // Callers schedule an idle retry when this is true; a successful intervening
   // build can satisfy it before the scheduled callback runs.
   bool needsDeferredBuild() const;

private:
   core::Error run(bool needsResult,
                   bool wantsEvent,
                   core::json::Object* pJson,
                   bool* pDeliverEvent);

   BuildFunction build_;
   int depth_ = 0;               // builds on the stack
   bool rebuildPending_ = false; // a request arrived during the current pass
   bool eventPending_ = false;   // an event is still owed, even after a failure
   unsigned generation_ = 0;
   unsigned latestGeneration_ = 0;
   core::json::Object latestResult_;
};

} // namespace packages
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_PACKAGES_HPP
