/*
 * SessionPanmirrorPandoc.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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



#include <shared_core/Error.hpp>

#include <core/StringUtils.hpp>

#include <core/system/Process.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace module_context {

namespace {

std::string pandocPath()
{
#ifndef WIN32
   std::string pandoc = "pandoc";
#else
   std::string pandoc = "pandoc.exe";
#endif
  FilePath pandocPath = FilePath(core::system::getenv("RSTUDIO_PANDOC")).completeChildPath(pandoc);
  return string_utils::utf8ToSystem(pandocPath.getAbsolutePath());
}

core::system::ProcessOptions pandocOptions()
{
   core::system::ProcessOptions options;
   options.terminateChildren = true;
   return options;
}

} // anonymous namespace

Error runPandoc(const std::vector<std::string>& args, const std::string& input, core::system::ProcessResult* pResult)
{
   core::system::ProcessOptions options;
   options.terminateChildren = true;

   return core::system::runProgram(
      pandocPath(),
      args,
      input,
      pandocOptions(),
      pResult
   );
}

Error runPandocAsync(const std::vector<std::string>& args,
                     const std::string&input,
                     const boost::function<void(const core::system::ProcessResult&)>& onCompleted)
{
   return module_context::processSupervisor().runProgram(
      pandocPath(),
      args,
      input,
      pandocOptions(),
      onCompleted
   );
}

} // end namespace module_context
} // end namespace session
} // end namespace rstudio
