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
#include <boost/foreach.hpp>
#include <boost/algorithm/string/join.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/Error.hpp>
#include <core/DateTime.hpp>
#include <core/StringUtils.hpp>
#include <core/SafeConvert.hpp>
#include <core/FileSerializer.hpp>

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

void Log::onSlideDeckChanged(const SlideDeck& slideDeck)
{
   slideDeckInputCommands_.clear();

   const std::vector<Slide>& slides = slideDeck.slides();
   for (std::size_t i = 0; i<slides.size(); i++)
   {
      const std::vector<Command>& commands = slides[i].commands();
      BOOST_FOREACH(const Command& command, commands)
      {
         if (command.name() == "console-input")
         {
            std::string params = boost::algorithm::trim_copy(command.params());
            slideDeckInputCommands_[i].insert(params);
         }
      }

      const std::vector<AtCommand>& atCommands = slides[i].atCommands();
      BOOST_FOREACH(const AtCommand& atCommand, atCommands)
      {
         if (atCommand.command().name() == "console-input")
         {
            std::string params = boost::algorithm::trim_copy(
                                             atCommand.command().params());
            slideDeckInputCommands_[i].insert(params);
         }
      }
   }
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
      std::string input = boost::algorithm::trim_copy(
                            boost::algorithm::join(consoleInputBuffer_, "\n"));
      std::string errors = boost::algorithm::trim_copy(
                            boost::algorithm::join(errorOutputBuffer_, "\n"));

      // check to see if this command was one of the ones instrumented
      // by the current slide
      if (slideDeckInputCommands_[currentSlideIndex_].count(input) == 0)
      {
         append(InputEntry,
                presentation::state::directory(),
                currentSlideIndex_,
                input,
                errors);
      }
   }

   consoleInputBuffer_.clear();
   errorOutputBuffer_.clear();
}


void Log::onConsoleInput(const std::string& text)
{
   if (!presentation::state::isActive())
      return;

   consoleInputBuffer_.push_back(text);

   errorOutputBuffer_.clear();
}


void Log::onConsoleOutput(module_context::ConsoleOutputType type,
                     const std::string& output)
{
   if (!presentation::state::isActive())
      return;

   if (type == module_context::ConsoleOutputError)
      errorOutputBuffer_.push_back(output);

}

namespace {

std::string csvString(std::string str)
{
   boost::algorithm::replace_all(str, "\n", "\\n");
   boost::algorithm::replace_all(str, "\"", "\\\"");
   return "\"" + str + "\"";
}

} // anonymous namespace

void Log::append(EntryType type,
                 const FilePath& presPath,
                 int slideIndex,
                 const std::string& input,
                 const std::string& errors)
{
   // determine log file path and ensure it exists
   using namespace module_context;
   FilePath presDir =  userScratchPath().childPath("presentation");
   Error error = presDir.ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   FilePath logFilePath = presDir.childPath("presentation-log.csv");
   if (!logFilePath.exists())
   {
      Error error = core::writeStringToFile(logFilePath,
         "type, timestamp, presentation, slide, input, errors\n");
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
   }

   // generate timestamp
   using namespace boost::posix_time;
   ptime time = microsec_clock::universal_time();
   std::string dateTime = date_time::format(time, "%Y-%m-%dT%H:%M:%SZ");

   // generate entry
   std::vector<std::string> fields;
   fields.push_back((type == NavigationEntry) ? "Navigation" : "Input");
   fields.push_back(dateTime);
   fields.push_back(csvString(module_context::createAliasedPath(presPath)));
   fields.push_back(safe_convert::numberToString(slideIndex));
   fields.push_back(csvString(input));
   fields.push_back(csvString(errors));
   std::string entry = boost::algorithm::join(fields, ",");

   // append entry
   error = core::appendToFile(logFilePath, entry + "\n");
   if (error)
      LOG_ERROR(error);
}


} // namespace presentation
} // namespace modules
} // namesapce session

