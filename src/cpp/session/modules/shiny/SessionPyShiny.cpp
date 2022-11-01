/*
 * SessionPyShiny.cpp
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

#include "SessionPyShiny.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <core/Algorithm.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/YamlUtil.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionRUtil.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionUrlPorts.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#define kPyShinyXt           "pyshiny-app"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace pyshiny {

namespace {

std::string onDetectPyShinySourceType(
      boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (!pDoc->path().empty() && pDoc->type().size() != 0 && pDoc->type() == kSourceDocumentTypePython)
   {
      FilePath filePath = module_context::resolveAliasedPath(pDoc->path());
      std::string filename = filePath.getFilename();
      if (boost::algorithm::iequals(filename, "app.py") &&
          boost::algorithm::icontains(pDoc->contents(), "shiny"))
      {
         return kPyShinyXt;
      }
   }

   return std::string();
}

} // anonymous namespace

Error initialize()
{
   using namespace module_context;
   using boost::bind;

   events().onDetectSourceExtendedType.connect(onDetectPyShinySourceType);

   return Success();
}


} // namespace crypto
} // namespace modules
} // namespace session
} // namespace rstudio

