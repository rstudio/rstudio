/*
 * SessionLogging.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include "SessionLogging.hpp"

#include <map>

#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace logging {

namespace {

std::map<std::string, int> s_stderrLogLevels;

} // anonymous namespace

int stderrLogLevel(const std::string& section)
{
   auto it = s_stderrLogLevels.find(section);
   if (it != s_stderrLogLevels.end())
      return it->second;
   return 0;
}

void setStderrLogLevel(const std::string& section, int level)
{
   if (level <= 0)
      s_stderrLogLevels.erase(section);
   else
      s_stderrLogLevels[section] = level;
}

} // namespace logging
} // namespace session
} // namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace logging {

namespace {

SEXP rs_loggingSetStderrLogLevel(SEXP sectionSEXP, SEXP levelSEXP)
{
   std::string section = r::sexp::asString(sectionSEXP);
   int level = r::sexp::asInteger(levelSEXP);
   rstudio::session::logging::setStderrLogLevel(section, level);
   return R_NilValue;
}

} // anonymous namespace

core::Error initialize()
{
   RS_REGISTER_CALL_METHOD(rs_loggingSetStderrLogLevel);

   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionLogging.R"));

   return initBlock.execute();
}

} // namespace logging
} // namespace modules
} // namespace session
} // namespace rstudio
