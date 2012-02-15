/*
 * TexLogParser.cpp
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

#include <core/tex/TexLogParser.hpp>

#include <boost/foreach.hpp>
#include <boost/regex.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/algorithm/string.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>

namespace core {
namespace tex {

namespace {

Error parseLog(
     const FilePath& logFilePath,
     const boost::regex& re,
     const boost::function<LogEntry(const boost::smatch& match,
                                    const FilePath&)> matchToEntry,
     LogEntries* pLogEntries)
{
   // get the lines
   std::vector<std::string> lines;
   Error error = core::readStringVectorFromFile(logFilePath, &lines);
   if (error)
      return error;

   // look for error messages
   BOOST_FOREACH(std::string line, lines)
   {
      boost::smatch match;
      if (regex_match(line, match, re))
      {
         pLogEntries->push_back(matchToEntry(match, logFilePath.parent()));
      }
   }

   return Success();
}

FilePath texFilePath(const std::string& logPath, const FilePath& compileDir)
{
   // some tex compilers report file names with absolute paths and some
   // report them relative to the compilation directory -- on Posix use
   // realPath to get a clean full path back -- note the fact that we
   // don't do this on Windows is a tacit assumption that Windows TeX logs
   // are either absolute or don't require interpretation of .., etc.

   FilePath path = compileDir.complete(logPath);

#ifdef _WIN32
   return path;
#else
   FilePath realPath;
   Error error = core::system::realPath(path.absolutePath(), &realPath);
   if (error)
   {
      LOG_ERROR(error);
      return path;
   }
   else
   {
      return realPath;
   }
#endif
}

LogEntry fromLatexMatch(const boost::smatch& match,
                        const FilePath& compileDir)
{
   return LogEntry(LogEntry::Error,
                   texFilePath(match[1], compileDir),
                   boost::lexical_cast<int>(match[2]),
                   match[3]);
}

LogEntry fromBibtexMatch(const boost::smatch& match,
                         const FilePath& compileDir)
{
   return LogEntry(LogEntry::Error,
                   texFilePath(match[3], compileDir),
                   boost::lexical_cast<int>(match[2]),
                   match[1]);
}


} // anonymous namespace

Error parseLatexLog(const FilePath& logFilePath, LogEntries* pLogEntries)
{
   return parseLog(logFilePath,
                   boost::regex ("^((?:[A-Z]:)?[^:]+):([0-9]+): ([^\n]+)$"),
                   fromLatexMatch,
                   pLogEntries);
}

Error parseBibtexLog(const FilePath& logFilePath, LogEntries* pLogEntries)
{
   return parseLog(logFilePath,
                   boost::regex("^(.*)---line ([0-9]+) of file (.*)$"),
                   fromBibtexMatch,
                   pLogEntries);
  ;
}

} // namespace tex
} // namespace core 



