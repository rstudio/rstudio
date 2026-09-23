/*
 * LoggerTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant to the terms of a commercial license agreement
 * with Posit, then this program is licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#include <gtest/gtest.h>

#include <memory>
#include <string>
#include <vector>

#include <shared_core/ILogDestination.hpp>
#include <shared_core/Logger.hpp>

namespace rstudio {
namespace core {
namespace log {
namespace {

class RecordingLogDestination : public ILogDestination
{
public:
   explicit RecordingLogDestination(const std::string& in_id)
      : ILogDestination(in_id, LogLevel::INFO, LogMessageFormatType::PRETTY, false)
   {
   }

   void refresh(const RefreshParams&) override
   {
   }

   void writeLog(LogLevel, const std::string& in_message) override
   {
      m_messages.push_back(in_message);
   }

   bool received(const std::string& in_text) const
   {
      for (const std::string& message : m_messages)
      {
         if (message.find(in_text) != std::string::npos)
            return true;
      }

      return false;
   }

private:
   std::vector<std::string> m_messages;
};

} // anonymous namespace

TEST(LoggerTests, RemoveLogDestinationRemovesItFromSectionsItShares)
{
   const std::string section = "logger-tests-shared-section";
   auto pRemoved = std::make_shared<RecordingLogDestination>("logger-tests-shared-removed");
   auto pKept = std::make_shared<RecordingLogDestination>("logger-tests-shared-kept");
   addLogDestination(pRemoved, section);
   addLogDestination(pKept, section);

   removeLogDestination(pRemoved->getId());
   logInfoMessage("after removal", section);

   EXPECT_FALSE(pRemoved->received("after removal"));
   EXPECT_TRUE(pKept->received("after removal"));

   removeLogDestination(pKept->getId());
}

TEST(LoggerTests, RemoveLogDestinationDropsSectionsItWasAloneIn)
{
   const std::string section = "logger-tests-sole-section";
   auto pDefault = std::make_shared<RecordingLogDestination>("logger-tests-sole-default");
   auto pRemoved = std::make_shared<RecordingLogDestination>("logger-tests-sole-removed");
   addLogDestination(pDefault);
   addLogDestination(pRemoved, section);

   removeLogDestination(pRemoved->getId());
   logInfoMessage("after removal", section);

   // with its section gone, the message falls back to the default destinations
   EXPECT_FALSE(pRemoved->received("after removal"));
   EXPECT_TRUE(pDefault->received("after removal"));

   removeLogDestination(pDefault->getId());
}

} // namespace log
} // namespace core
} // namespace rstudio
