/*
 * LogWriter.cpp
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

#include <core/LogWriter.hpp>

#include <boost/algorithm/string/replace.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/DateTime.hpp>

namespace core {

std::string LogWriter::formatLogEntry(const std::string& programIdentity,
                                      const std::string& message,
                                      bool escapeNewlines)
{
   // replace newlines with standard escape sequence if requested
   std::string cleanedMessage(message);
   if (escapeNewlines)
      boost::algorithm::replace_all(cleanedMessage, "\n", "|||");

   // generate time string
   using namespace boost::posix_time;
   ptime time = microsec_clock::universal_time();
   std::string dateTime = date_time::format(time,  "%d %b %Y %H:%M:%S");

   // generate log entry
   std::ostringstream ostr;
   ostr << dateTime
        << " [" << programIdentity << "] "
        << cleanedMessage
        << std::endl;
   return ostr.str();
}

} // namespace core
