/*
 * StdErrLogDestination.hpp
 * 
 * Copyright (C) 2019 by RStudio, Inc.
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

#ifndef SHARED_STD_ERR_LOG_DESTINATION_HPP
#define SHARED_STD_ERR_LOG_DESTINATION_HPP

#include "ILogDestination.hpp"

namespace rstudio {
namespace shared {

/**
 * @brief A class which logs messages to stderr.
 *
 * If stderr is not a TTY, no logs will be written. In that case, it is better not to register the destination.
 * Only one of these should be created per program.
 */
class StderrDestination : public ILogDestination
{
public:
   /**
    * @brief Checks whether stderr is a TTY
    */
   static bool isStderrTty();

   /**
    * @brief Gets the unique ID for the stderr destination. There should only be one stderr destination for the whole
    *        program.
    *
    * @return The unique ID of the stderr destination.
    */
   static unsigned int getStderrId();

   /**
    * @brief Gets the unique ID of the stderr destination.
    *
    * @return The unique ID of the stderr destination.
    */
   unsigned int getId() const override;

   /**
    * @brief Writes a message to stderr.
    *
    * @param in_logLevel    The log level of the message to write. Filtering is done prior to this call. This is for
    *                       informational purposes only.
    * @param in_message     The message to write to stderr.
    */
   void writeLog(LogLevel in_logLevel, const std::string& in_message) override;
};

} // namespace shared
} // namespace rstudio

#endif
