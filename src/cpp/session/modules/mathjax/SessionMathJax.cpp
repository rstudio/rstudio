/*
 * SessionMathJax.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionMathJax.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/Exec.hpp>

#include <core/http/Util.hpp>

#include <session/SessionModuleContext.hpp>

#define kMathJaxURIPrefix "/mathjax/"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace mathjax {

namespace {

void handleMathJax(const http::Request& request, http::Response* pResponse)
{
   // extract path from URI
   std::string path = request.path().substr(strlen(kMathJaxURIPrefix));
   
   // construct path to resource
   FilePath mathjaxPath = options().mathjaxPath();
   FilePath resourcePath = mathjaxPath.completePath(path);
   pResponse->setCacheableFile(resourcePath, request);
}

} // end anonymous namespace

Error initialize()
{
   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerUriHandler, "/mathjax", handleMathJax))
   ;
   return initBlock.execute();
}

} // end namespace mathjax
} // end namespace modules
} // end namespace session
} // end namespace rstudio
