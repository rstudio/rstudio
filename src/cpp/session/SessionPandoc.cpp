/*
 * SessionPanmirrorPandoc.cpp
 *
 * Copyright (C) 2022 by RStudio, Inc.
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



#include <shared_core/Error.hpp>

#include <core/StringUtils.hpp>

#include <core/system/Process.hpp>

#include <r/ROptions.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace module_context {

namespace {

std::string pandocBinary(const std::string& binary)
{
#ifndef WIN32
   std::string target = binary;
#else
   std::string target = binary + ".exe";
#endif
  FilePath pandocPath = session::options().pandocPath().completeChildPath(target);
  return string_utils::utf8ToSystem(pandocPath.getAbsolutePath());
}

core::system::ProcessOptions withPandocDefaultOptions(core::system::ProcessOptions options)
{
#ifdef _WIN32
   options.createNewConsole = true;
#else
   options.terminateChildren = true;
#endif
   return options;
}

Error runAsync(const std::string& executablePath,
               const std::vector<std::string>& args,
               const std::string&input,
               core::system::ProcessOptions options,
               const boost::function<void(const core::system::ProcessResult&)>& onCompleted)
{
   return module_context::processSupervisor().runProgram(
      executablePath,
      args,
      input,
      withPandocDefaultOptions(options),
      onCompleted
   );
}

std::vector<std::string> prependStackSize(const std::vector<std::string>& args)
{  
   // hard code stack size because this code can *unexpectedly* run on a background thread
   // std::string size = r::options::getOption<std::string>("pandoc.editor.stack.size", "128m", false);
   std::string size = "128m";
   std::vector<std::string> newArgs = { "+RTS", "-K" + size, "-RTS" };
   std::copy(args.begin(), args.end(), std::back_inserter(newArgs));
   return newArgs;
}


} // anonymous namespace

std::string pandocPath()
{
   return pandocBinary("pandoc");
}

Error runPandoc(const std::string& pandocPath,
                const std::vector<std::string>& args,
                const std::string& input,
                core::system::ProcessOptions options,
                core::system::ProcessResult* pResult)
{

   return core::system::runProgram(
      pandocPath,
      prependStackSize(args),
      input,
      withPandocDefaultOptions(options),
      pResult
   );
}


Error runPandoc(const std::vector<std::string>& args, const std::string& input, core::system::ProcessResult* pResult)
{
   return runPandoc(pandocPath(), args, input, core::system::ProcessOptions(), pResult);
}

Error runPandocAsync(const std::string& pandocPath,
                     const std::vector<std::string>& args,
                     const std::string &input,
                     core::system::ProcessOptions options,
                     const boost::function<void(const core::system::ProcessResult&)>& onCompleted)
{
   return runAsync(pandocPath, prependStackSize(args), input, options, onCompleted);
}


Error runPandocAsync(const std::vector<std::string>& args,
                     const std::string&input,
                     const boost::function<void(const core::system::ProcessResult&)>& onCompleted)
{
   return runPandocAsync(pandocPath(), args, input, core::system::ProcessOptions(), onCompleted);
}


} // end namespace module_context
} // end namespace session
} // end namespace rstudio
