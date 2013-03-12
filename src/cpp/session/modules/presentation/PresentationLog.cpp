/*
 * PresentationLog.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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


#include "PresentationLog.hpp"

#include <boost/bind.hpp>
#include <boost/algorithm/string/join.hpp>

#include <core/Error.hpp>
#include <core/DateTime.hpp>
#include <core/StringUtils.hpp>
#include <core/SafeConvert.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>

#include "PresentationState.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace presentation {

Log& log()
{
   static Log instance;
   return instance;
}


Error Log::initialize()
{

   // connect to console events
   using namespace boost;
   using namespace session::module_context;

   events().onConsolePrompt.connect(boost::bind(&Log::onConsolePrompt,
                                                this, _1));
   events().onConsoleInput.connect(boost::bind(&Log::onConsoleInput,
                                               this, _1));
   events().onConsoleOutput.connect(boost::bind(&Log::onConsoleOutput,
                                                this, _1, _2));

   return Success();
}

void Log::onSlideIndexChanged(int index)
{
   currentSlideIndex_ = index;

   append(NavigationEntry,
          presentation::state::directory(),
          currentSlideIndex_,
          "",
          "");
}

void Log::onConsolePrompt(const std::string& prompt)
{
   if (!presentation::state::isActive())
      return;

   if (!consoleInputBuffer_.empty())
   {
      std::string input = boost::algorithm::join(consoleInputBuffer_, "\n");
      std::string errors = boost::algorithm::join(errorOutputBuffer_, "\n");

      append(InputEntry,
             presentation::state::directory(),
             currentSlideIndex_,
             input,
             errors);
   }

   consoleInputBuffer_.clear();
   errorOutputBuffer_.clear();
}


void Log::onConsoleInput(const std::string& text)
{
   if (!presentation::state::isActive())
      return;

   consoleInputBuffer_.push_back(text);

}


void Log::onConsoleOutput(module_context::ConsoleOutputType type,
                     const std::string& output)
{
   if (!presentation::state::isActive())
      return;

   if (type == module_context::ConsoleOutputError)
      errorOutputBuffer_.push_back(output);

}

void Log::append(EntryType type,
                 const FilePath& presPath,
                 int slideIndex,
                 const std::string& input,
                 const std::string& errors)
{
   /*
   FilePath logFilePath =  module_context::userScratchPath().childPath(
                                                            "presentation.log");
   std::string logFile = string_utils::utf8ToSystem(logFilePath.absolutePath());

   r::exec::RFunction func(".rs.logPresentationEvent");
   func.addParam("file", logFile);
   func.addParam("type", (type == NavigationEntry) ? "Navigation" : "Input");
   func.addParam("time", date_time::millisecondsSinceEpoch());
   func.addParam("presentation", module_context::createAliasedPath(presPath));
   func.addParam("slide", slideIndex);
   func.addParam("input", input);
   func.addParam("errors", errors);

   Error error = func.call();
   if (error)
      LOG_ERROR(error);
   */
}



} // namespace presentation
} // namespace modules
} // namesapce session

