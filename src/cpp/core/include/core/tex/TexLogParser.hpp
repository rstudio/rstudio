/*
 * TexLogParser.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef CORE_TEX_TEX_LOG_PARSER_HPP
#define CORE_TEX_TEX_LOG_PARSER_HPP

#include <string>
#include <vector>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {

class Error;

namespace tex {

class LogEntry
{
public:
   enum Type { Error = 0, Warning = 1, Box = 2};

public:
   LogEntry(const FilePath& logFilePath,
            int logLine,
            Type type,
            const FilePath& filePath,
            int line,
            const std::string& message)
      : type_(type), logFilePath_(logFilePath), logLine_(logLine),
        filePath_(filePath), line_(line), message_(message)
   {
   }

   // COPYING: via compiler

public:
   Type type() const { return type_; }

   // The log file in which this entry can be found
   const FilePath& logFilePath() const { return logFilePath_; }

   // The line number at which this entry can be found in the log file
   int logLine() const { return logLine_; }

   // The source file that this error refers to
   const FilePath& filePath() const { return filePath_; }

   // The line number in the source file that this error refers to
   int line() const { return line_; }

   const std::string& message() const { return message_; }

private:
   Type type_;
   FilePath logFilePath_;
   int logLine_;
   FilePath filePath_;
   int line_;
   std::string message_;
};

typedef std::vector<LogEntry> LogEntries;

Error parseLatexLog(const FilePath& logFilePath, LogEntries* pLogEntries);

Error parseBibtexLog(const FilePath& logFilePath, LogEntries* pLogEntries);

} // namespace tex
} // namespace core 
} // namespace rstudio


#endif // CORE_TEX_TEX_LOG_PARSER_HPP

