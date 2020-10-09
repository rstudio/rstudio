/*
 * SessionCustomSource.cpp
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

#include "SessionCustomSource.hpp"

#include <boost/format.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/RUtil.hpp>
#include <r/ROptions.hpp>

#include <r/session/RGraphics.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace customsource {

namespace {

#define kRCustomSource "r-custom-source"

std::string onDetectSourceType(
      boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if ((pDoc->type() == kSourceDocumentTypeRSource))
   {
      static const boost::regex reCustomSourceComment("^#\\s*!source\\s+\\w+.*$");
      std::string contents = pDoc->contents();
      if (regex_utils::search(contents.begin(), contents.end(), reCustomSourceComment))
      {
         return kRCustomSource;
      }
   }

   return std::string();
}

} // anonymous namespace

Error initialize()
{
   using namespace module_context;
   module_context::events().onDetectSourceExtendedType
                                        .connect(onDetectSourceType);

   ExecBlock initBlock;
   return initBlock.execute();
}


} // namespace customsource
} // namespace modules
} // namespace session
} // namespace rstudio

