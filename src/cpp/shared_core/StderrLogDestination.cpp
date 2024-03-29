/*
 * StderrDestination.cpp
 * 
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <shared_core/StderrLogDestination.hpp>

#include <iostream>

namespace rstudio {
namespace core {
namespace log {

StderrLogDestination::StderrLogDestination(const std::string& in_id,
                                           LogLevel in_logLevel,
                                           LogMessageFormatType in_formatType,
                                           bool in_reloadable) :
   ILogDestination(in_id, in_logLevel, in_formatType, in_reloadable)
{
}

void StderrLogDestination::refresh(const RefreshParams&)
{
   // No action necessary.
}

void StderrLogDestination::writeLog(LogLevel in_logLevel, const std::string& in_message)
{
   // Don't write logs that are more detailed than the configured maximum
   if (in_logLevel <= m_logLevel)
      std::cerr << in_message;
}

} // namespace log
} // namespace core
} // namespace rstudio

