/*
 * StderrLogWriter.cpp
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

#include <core/StderrLogWriter.hpp>

#include <boost/algorithm/string/replace.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/DateTime.hpp>
#include <core/FileInfo.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>

namespace core {

StderrLogWriter::StderrLogWriter(const std::string& programIdentity,
                                 int logLevel)
   : programIdentity_(programIdentity), logLevel_(logLevel)
{
}

StderrLogWriter::~StderrLogWriter()
{
   try
   {
   }
   catch(...)
   {
   }
}

void StderrLogWriter::log(core::system::LogLevel logLevel,
                          const std::string& message)
{
   log(programIdentity_, logLevel, message);
}

void StderrLogWriter::log(const std::string& programIdentity,
                          core::system::LogLevel logLevel,
                          const std::string& message)
{
   if (logLevel > logLevel_)
      return;

   std::cerr << formatLogEntry(programIdentity, message);
}


} // namespace core
