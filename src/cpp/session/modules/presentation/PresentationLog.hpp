/*
 * PresentationLog.hpp
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

#ifndef SESSION_PRESENTATION_LOG_HPP
#define SESSION_PRESENTATION_LOG_HPP

#include <string>
#include <set>
#include <vector>

#include <boost/utility.hpp>

#include <session/SessionModuleContext.hpp>

#include "SlideParser.hpp"

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {

class Log;
Log& log();

class Log : boost::noncopyable
{
private:
   Log() : currentSlideIndex_(0) {}
   friend Log& log();

public:
   core::Error initialize();

   void onSlideDeckChanged(const SlideDeck& slideDeck);
   void onSlideIndexChanged(int index);

   void recordFeedback(const std::string& feedback);
   void recordQuizResponse(int index, int answer, bool correct);

private:
   void onConsolePrompt(const std::string& prompt);
   void onConsoleInput(const std::string& text);
   void onConsoleOutput(module_context::ConsoleOutputType type,
                        const std::string& output);

   enum EntryType { NavigationEntry, InputEntry };

   static void append(EntryType type,
                      int slideIndex,
                      const std::string& slideType,
                      const std::string& helpTopic,
                      const std::string& helpDoc,
                      const std::string& input,
                      const std::string& errors);

   std::string currentSlideType() const;
   std::string currentSlideHelpTopic() const;
   std::string currentSlideHelpDoc() const;

   void recordCommand(int slideIndex, const Command& command);

private:
   std::size_t currentSlideIndex_;
   SlideDeck currentSlideDeck_;

   std::map<int, std::set<std::string> > slideDeckInputCommands_;

   std::vector<std::string> slideHelpTopics_;
   std::vector<std::string> slideHelpDocs_;

   std::vector<std::string> slideTypes_;

   std::vector<std::string> consoleInputBuffer_;
   std::vector<std::string> errorOutputBuffer_;



};

} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_PRESENTATION_LOG_HPP
