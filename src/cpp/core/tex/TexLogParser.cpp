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

// Helper function, returns true if str begins with any of these values
bool beginsWith(const std::string& str,
                const std::string& test1,
                const std::string& test2=std::string(),
                const std::string& test3=std::string(),
                const std::string& test4=std::string())
{
   using namespace boost::algorithm;
   if (starts_with(str, test1))
      return true;

   if (test2.empty())
      return false;
   else if (starts_with(str, test2))
      return true;

   if (test3.empty())
      return false;
   else if (starts_with(str, test3))
      return true;

   if (test4.empty())
      return false;
   else if (starts_with(str, test4))
      return true;

   return false;
}

// Finds unmatched parens in `line` and puts them in pParens. Can be either
// ( or ). Logically the result can only be [zero or more ')'] followed by
// [zero or more '('].
void findUnmatchedParens(const std::string& line,
                         std::vector<std::string::const_iterator>* pParens)
{
   for (std::string::const_iterator it = line.begin(); it != line.end(); it++)
   {
      switch (*it)
      {
      case '(':
         pParens->push_back(it);
         break;
      case ')':
         if (pParens->empty() || *(pParens->back()) == ')')
            pParens->push_back(it);
         else
            pParens->pop_back();
      }
   }
}

// TeX wraps lines hard at 79 characters. We use heuristics as described in
// Sublime Text's TeX plugin to determine where these breaks are.
void unwrapLines(std::vector<std::string>* pLines)
{
   static boost::regex regexLine("^l\\.(\\d+)\\s");
   static boost::regex regexAssignment("^\\\\.*?=");

   std::vector<std::string>::iterator pos = pLines->begin();

   for ( ; pos != pLines->end(); pos++)
   {
      // The first line is always long, and not artificially wrapped
      if (pos == pLines->begin())
         continue;

      if (pos->length() != 79)
         continue;

      // The **<filename> line may be long, but we don't care about it
      if (beginsWith(*pos, "**"))
         continue;

      while (true)
      {
         std::vector<std::string>::iterator nextPos = pos + 1;
         // No more lines to add
         if (nextPos == pLines->end())
            break;

         if (nextPos->empty())
            break;

         if (beginsWith(*nextPos, "File:", "Package:", "Document Class:"))
            break;

         if (beginsWith(*nextPos, "LaTeX Info:", "LaTeX2e <"))
            break;

         if (boost::regex_search(*nextPos, regexAssignment))
            break;

         if (boost::regex_search(*nextPos, regexLine))
            break;

         bool breakAfterAppend = nextPos->length() != 79;

         pos->append(*nextPos);
         // NOTE: Erase is a simple but inefficient way of handling this. Would
         //    be way faster to maintain an output iterator that points to the
         //    correct point in pLines, and when finished, truncate whatever
         //    elements come after the final position of the output iterator.
         pLines->erase(nextPos, nextPos+1);

         if (breakAfterAppend)
            break;
      }
   }
}

// Given the value of the line, push and pop files from the stack as needed.
void maintainFileStack(const std::string line,
                       std::vector<std::string>* pFileStack)
{
   std::vector<std::string::const_iterator> parens;
   findUnmatchedParens(line, &parens);
   BOOST_FOREACH(std::string::const_iterator it, parens)
   {
      if (*it == ')')
      {
         pFileStack->pop_back();
      }
      else if (*it == '(')
      {
         std::string filename;
         std::copy(it, line.end(), std::back_inserter(filename));

         // Remove quotes if present
         if (filename.size() >= 2 &&
             filename[0] == '"' &&
             filename[filename.size()-1] == '"')
         {
            filename = filename.substr(1, filename.size()-2);
         }

         pFileStack->push_back(filename);
      }
      else
         BOOST_ASSERT(false);
   }
}

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



