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
#include <r/session/RSessionUtils.hpp>

#include "PresentationState.hpp"

using namespace rstudio::core;

namespace rstudio {
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

void Log::recordCommand(int slideIndex, const Command& command)
{
   if (command.name() == "console-input")
   {
      std::string params = boost::algorithm::trim_copy(command.params());
      slideDeckInputCommands_[slideIndex].insert(params);
   }
   else if (command.name() == "help-topic")
   {
      slideHelpTopics_[slideIndex] = command.params();
   }
   else if (command.name() == "help-doc")
   {
      slideHelpDocs_[slideIndex] = command.params();
   }
}

void Log::onSlideDeckChanged(const SlideDeck& slideDeck)
{
   slideDeckInputCommands_.clear();
   slideTypes_.clear();

   slideHelpTopics_ = std::vector<std::string>(slideDeck.slides().size());
   slideHelpDocs_ = std::vector<std::string>(slideDeck.slides().size());

   const std::vector<Slide>& slides = slideDeck.slides();
   for (std::size_t i = 0; i<slides.size(); i++)
   {
      slideTypes_.push_back(slides[i].type());

      const std::vector<Command>& commands = slides[i].commands();
      BOOST_FOREACH(const Command& command, commands)
      {
         recordCommand(i, command);
      }

      const std::vector<AtCommand>& atCommands = slides[i].atCommands();
      BOOST_FOREACH(const AtCommand& atCommand, atCommands)
      {
         recordCommand(i, atCommand.command());
      }
   }
}

void Log::onSlideIndexChanged(int index)
{
   currentSlideIndex_ = index;

   append(NavigationEntry,
          currentSlideIndex_,
          currentSlideType(),
          currentSlideHelpTopic(),
          currentSlideHelpDoc(),
          "",
          "");
}

void Log::onConsolePrompt(const std::string& prompt)
{
   if (!presentation::state::isActive())
      return;

   // ignore if this isn't the default prompt
   if (!r::session::utils::isDefaultPrompt(prompt))
      return;

   if (!consoleInputBuffer_.empty())
   {
      using namespace boost::algorithm;
      std::string input = trim_copy(join(consoleInputBuffer_, "\n"));
      std::string errors = trim_copy(join(errorOutputBuffer_, "\n"));

      // check to see if this command was one of the ones instrumented
      // by the current slide
      if (slideDeckInputCommands_[currentSlideIndex_].count(input) == 0)
      {
         append(InputEntry,
                currentSlideIndex_,
                currentSlideType(),
                currentSlideHelpTopic(),
                currentSlideHelpDoc(),
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
   boost::algorithm::replace_all(str, "\"", "\"\"");
   return "\"" + str + "\"";
}

std::string timestamp()
{
   // generate timestamp
   using namespace boost::posix_time;
   ptime time = microsec_clock::universal_time();
   std::string dateTime = date_time::format(time, "%Y-%m-%dT%H:%M:%SZ");
   return dateTime;
}

std::string csvPresentationPath()
{
   return csvString(module_context::createAliasedPath(
                                         presentation::state::filePath()));
}

Error ensureTargetFile(const std::string& filename,
                       const std::string& header,
                       FilePath* pTargetFile)
{
   using namespace module_context;
   FilePath presDir =  userScratchPath().childPath("presentation");
   Error error = presDir.ensureDirectory();
   if (error)
      return error;

   *pTargetFile = presDir.childPath(filename);
   if (!pTargetFile->exists())
   {
      Error error = core::writeStringToFile(*pTargetFile, header + "\n");
      if (error)
         return error;
   }

   return Success();
}


} // anonymous namespace

std::string Log::currentSlideType() const
{
   if (currentSlideIndex_ < slideTypes_.size())
      return slideTypes_[currentSlideIndex_];
   else
      return "default";
}

std::string Log::currentSlideHelpTopic() const
{
   if (currentSlideIndex_ < slideHelpTopics_.size())
      return slideHelpTopics_[currentSlideIndex_];
   else
      return "";
}

std::string Log::currentSlideHelpDoc() const
{
   if (currentSlideIndex_ < slideHelpDocs_.size())
      return slideHelpDocs_[currentSlideIndex_];
   else
      return "";
}

void Log::append(EntryType type,
                 int slideIndex,
                 const std::string& slideType,
                 const std::string& helpTopic,
                 const std::string& helpDoc,
                 const std::string& input,
                 const std::string& errors)
{
   // bail if this isn't a tutorial
   if (!presentation::state::isTutorial())
      return;

   // ensure target file
   FilePath logFilePath;
   Error error = ensureTargetFile(
            "presentation-log-v2.csv",
            "type, timestamp, username, presentation, slide, slide-type, "
            "help-topic, help-doc, input, errors\n",
            &logFilePath);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // generate entry
   std::vector<std::string> fields;
   fields.push_back((type == NavigationEntry) ? "Navigation" : "Input");
   fields.push_back(timestamp());
   fields.push_back(csvString(core::system::username()));
   fields.push_back(csvPresentationPath());
   fields.push_back(safe_convert::numberToString(slideIndex));
   fields.push_back(slideType);
   fields.push_back(csvString(helpTopic));
   fields.push_back(csvString(helpDoc));
   fields.push_back(csvString(input));
   fields.push_back(csvString(errors));
   std::string entry = boost::algorithm::join(fields, ",");

   // append entry
   error = core::appendToFile(logFilePath, entry + "\n");
   if (error)
      LOG_ERROR(error);
}

void Log::recordFeedback(const std::string& feedback)
{
   // bail if this isn't a tutorial
   if (!presentation::state::isTutorial())
      return;

   // ensure target file
   FilePath feedbackFilePath;
   Error error = ensureTargetFile("feedback-v2.csv",
                                  "timestamp, username, presentation, "
                                  "slide, feedback\n",
                                  &feedbackFilePath);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // generate entry
   std::vector<std::string> fields;
   fields.push_back(timestamp());
   fields.push_back(csvString(core::system::username()));
   fields.push_back(csvPresentationPath());
   fields.push_back(safe_convert::numberToString(currentSlideIndex_));
   fields.push_back(csvString(feedback));
   std::string entry = boost::algorithm::join(fields, ",");

   // append entry
   error = core::appendToFile(feedbackFilePath, entry + "\n");
   if (error)
      LOG_ERROR(error);
}

void Log::recordQuizResponse(int index, int answer, bool correct)
{
   // bail if this isn't a tutorial
   if (!presentation::state::isTutorial())
      return;

   // ensure target file
   FilePath quizResponseFilePath;
   Error error = ensureTargetFile(
                      "quiz-responses-v2.csv",
                      "timestamp, username, presentation, slide, "
                      "answer, correct",
                       &quizResponseFilePath);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // generate entry
   std::vector<std::string> fields;
   fields.push_back(timestamp());
   fields.push_back(csvString(core::system::username()));
   fields.push_back(csvPresentationPath());
   fields.push_back(safe_convert::numberToString(index));
   fields.push_back(safe_convert::numberToString(answer));
   fields.push_back(safe_convert::numberToString(correct));
   std::string entry = boost::algorithm::join(fields, ",");

   // append entry
   error = core::appendToFile(quizResponseFilePath, entry + "\n");
   if (error)
      LOG_ERROR(error);
}




} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

