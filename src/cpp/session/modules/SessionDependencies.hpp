/*
 * SessionDependencies.hpp
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

#ifndef SESSION_SESSION_DEPENDENCIES_HPP
#define SESSION_SESSION_DEPENDENCIES_HPP

#include <string>
#include <vector>

#include <r/RSexp.hpp>

#define kCRANPackageDependency "cran"
#define kEmbeddedPackageDependency "embedded"

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace dependencies {

struct Dependency
{
   Dependency() = default;

   // Construct a new Dependency record from an S-expression containing a named
   // list. Fields not present in the list keep their defaults; in particular,
   // a dependency is presumed version-satisfied until demonstrated otherwise.
   Dependency(SEXP sexp);

   bool empty() const { return name.empty(); }

   // Return a version of the dependency as an S-expression for processing in R.
   SEXP asSEXP(r::sexp::Protect* protect) const;

   std::string location = kCRANPackageDependency;
   std::string name;
   std::string version;
   bool source = false;
   std::string availableVersion;
   bool versionSatisfied = true;
};

// Builds an installation script which will install all the dependencies at
// once. Exposed for testing.
std::string buildCombinedInstallScript(const std::vector<Dependency>& deps);

core::Error initialize();

} // namespace dependencies
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_DEPENDENCIES_HPP
