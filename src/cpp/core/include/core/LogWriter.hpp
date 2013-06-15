/*
 * LogWriter.hpp
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

#ifndef LOG_WRITER_HPP
#define LOG_WRITER_HPP

#include <core/system/System.hpp>

namespace core {

class LogWriter
{
public:
   virtual ~LogWriter() {}

   virtual void log(core::system::LogLevel level,
                    const std::string& message) = 0;

protected:
   std::string formatLogEntry(const std::string& programIdentify,
                              const std::string& message);
};

namespace system {

void addLogWriter(boost::shared_ptr<core::LogWriter> pLogWriter);

} // namespace system

} // namespace core

#endif // LOG_WRITER_HPP
