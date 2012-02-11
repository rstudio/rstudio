/*
 * TexLogParser.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

namespace core {

class Error;
class FilePath;

namespace tex {

class LogEntry
{
public:
   enum Type { Error = 0, Warning = 1, Box = 2};

public:
   LogEntry(Type type,
            const std::string& file,
            int line,
            const std::string& message)
      : file_(file), line_(line), message_(message)
   {
   }

   // COPYING: via compiler

public:
   Type type() const { return type_; }
   const std::string& file() const { return file_; }
   int line() const { return line_; }
   const std::string& message() const { return message_; }

private:
   Type type_;
   std::string file_;
   int line_;
   std::string message_;
};

typedef std::vector<LogEntry> LogEntries;

Error parseLatexLog(const FilePath& logFilePath, LogEntries* pLogEntries);

Error parseBibtexLog(const FilePath& logFilePath, LogEntries* pLogEntries);

} // namespace tex
} // namespace core 


#endif // CORE_TEX_TEX_LOG_PARSER_HPP

