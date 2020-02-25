/*
 * Logger.cpp
 *
 * Copyright (C) 2019 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant to the terms of a commercial license agreement
 * with RStudio, then this program is licensed to you under the following terms:
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

#include <tests/TestThat.hpp>

#include <queue>

#include <boost/asio.hpp>
#include <boost/thread.hpp>

#include <shared_core/Logger.hpp>
#include <shared_core/ILogDestination.hpp>

namespace rstudio {
namespace core {
namespace log {

struct LogRecord
{
   LogRecord(LogLevel in_level, std::string in_message) :
      Level(in_level),
      Message(std::move(in_message))
   {
   }

   LogLevel Level;
   std::string Message;
};

class MockLogDestintation : public ILogDestination
{
public:
   MockLogDestintation() : ILogDestination(LogLevel::DEBUG) {};

   // This really doesn't matter since it should be used only in these tests.
   unsigned getId() const override
   {
      return 1;
   }

   void writeLog(LogLevel in_logLevel, const std::string &in_message) override
   {
      m_logRecords.emplace(in_logLevel, in_message);
   }

   const LogRecord& peek() const
   {
      return m_logRecords.front();
   }

   LogRecord pop()
   {
      LogRecord record = m_logRecords.front();
      m_logRecords.pop();
      return record;
   }

   size_t size()
   {
      return m_logRecords.size();
   }

private:
   std::queue<LogRecord> m_logRecords;
};

TEST_CASE("Log after shutdown")
{
   // Initialize the log.
   std::shared_ptr<MockLogDestintation> mockLog(new MockLogDestintation());
   addLogDestination(std::static_pointer_cast<ILogDestination>(mockLog));

   // Start a background thread.
   boost::asio::io_service ioService;
   std::shared_ptr<boost::thread> thread(
      new boost::thread([&ioService]()
     {
         boost::asio::io_service::work work(ioService);
         ioService.run();
     }));

   // Post work that will sleep for a second and then log a message.
   boost::asio::post(
      ioService,
      [thread]() // Ensure the thread isn't deleted.
      {
         sleep(1);
         logDebugMessage("Log something");
      });

   // Immediately exit the main process so shutdown beings.
}

} // namespace log
} // namespace core
} // namespace rstudio
