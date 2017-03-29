/*
 * TexLogParser.cpp
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

#include <core/tex/TexLogParser.hpp>

#include <boost/foreach.hpp>
#include <boost/regex.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/algorithm/string.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/RegexUtils.hpp>
#include <core/SafeConvert.hpp>
#include <core/system/System.hpp>

namespace rstudio {
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
   // We need to ignore close parens unless they are at the start of a line,
   // preceded by nothing but whitespace and/or other close parens. Without
   // this, sample.Rnw has some false positives due to some math errors, e.g.:
   //
   // l.204 (x + (y^
   //               2))
   //
   // The first line is ignored because it's part of an error message. The rest
   // gets parsed and underflows the file stack.
   bool ignoreCloseParens = false;

   // NOTE: I don't know if it's possible for (<filename> to appear anywhere
   // but the beginning of the line (preceded only by whitespace(?) and close
   // parens). But the Sublime Text 2 plugin code seemed to imply that it is
   // possible.

   for (std::string::const_iterator it = line.begin(); it != line.end(); it++)
   {
      switch (*it)
      {
      case '(':
         pParens->push_back(it);
         ignoreCloseParens = true;
         break;
      case ')':
         if (pParens->empty() || *(pParens->back()) == ')')
         {
            if (!ignoreCloseParens)
               pParens->push_back(it);
         }
         else
            pParens->pop_back();
         break;
      case ' ':
      case '\t':
         break;
      default:
         ignoreCloseParens = true;
         break;
      }
   }
}

FilePath resolveFilename(const FilePath& rootDir,
                         const std::string& filename)
{
   std::string result = filename;

   // Remove quotes if necessary
   if (result.size() > 2 &&
       boost::algorithm::starts_with(result, "\"") &&
       boost::algorithm::ends_with(result, "\""))
   {
      result.erase(result.size()-1, 1);
   }

   // Strip leading ./
   if (boost::algorithm::starts_with(result, "./"))
      result.erase(0, 2);

   if (result.empty())
      return FilePath();

   // Check for existence of file
   FilePath file = rootDir.complete(result);
   if (file.exists() && !file.isDirectory())
      return file;
   else
      return FilePath();
}

// TeX wraps lines hard at 79 characters. We use heuristics as described in
// Sublime Text's TeX plugin to determine where these breaks are.
void unwrapLines(std::vector<std::string>* pLines,
                 std::vector<size_t>* pLinesUnwrapped=NULL)
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

         // Underfull/Overfull terminator
         if (*nextPos == " []")
            break;

         // Common prefixes
         if (beginsWith(*nextPos, "File:", "Package:", "Document Class:"))
            break;

         // More prefixes
         if (beginsWith(*nextPos, "LaTeX Warning:", "LaTeX Info:", "LaTeX2e <"))
            break;

         if (regex_utils::search(*nextPos, regexAssignment))
            break;

         if (regex_utils::search(*nextPos, regexLine))
            break;

         bool breakAfterAppend = nextPos->length() != 79;

         pos->append(*nextPos);
         // NOTE: Erase is a simple but inefficient way of handling this. Would
         //    be way faster to maintain an output iterator that points to the
         //    correct point in pLines, and when finished, truncate whatever
         //    elements come after the final position of the output iterator.
         pLines->erase(nextPos, nextPos+1);
         if (pLinesUnwrapped)
            pLinesUnwrapped->push_back(1 + (pos - pLines->begin()));

         if (breakAfterAppend)
            break;
      }
   }
}

class FileStack : public boost::noncopyable
{
public:
   explicit FileStack(FilePath rootDir) : rootDir_(rootDir)
   {
   }

   FilePath currentFile()
   {
      return currentFile_;
   }

   void processLine(const std::string& line)
   {
      typedef std::vector<std::string::const_iterator> Iterators;
      Iterators parens;
      findUnmatchedParens(line, &parens);
      for (Iterators::const_iterator itParen = parens.begin();
           itParen != parens.end();
           itParen++)
      {
         std::string::const_iterator it = *itParen;

         if (*it == ')')
         {
            if (!fileStack_.empty())
            {
               fileStack_.pop_back();
               updateCurrentFile();
            }
            else
            {
               LOG_WARNING_MESSAGE("File context stack underflow while parsing "
                                   "TeX log");
            }
         }
         else if (*it == '(')
         {
            std::string::const_iterator itFilenameEnd =
                  // case: no other ( on this line
                  (itParen + 1 == parens.end()) ? line.end() :
                  // case: space before next paren, eat it
                  *(*(itParen+1)-1) == ' ' ? *(itParen+1)-1 :
                  // case: other
                  *(itParen+1);

            std::string filename = std::string(it+1, itFilenameEnd);
            fileStack_.push_back(resolveFilename(rootDir_, filename));

            updateCurrentFile();
         }
         else
            BOOST_ASSERT(false);
      }
   }

private:

   void updateCurrentFile()
   {
      for (std::vector<FilePath>::reverse_iterator it = fileStack_.rbegin();
           it != fileStack_.rend();
           it++)
      {
         if (!it->empty())
         {
            currentFile_ = *it;
            return;
         }
      }
      currentFile_ = FilePath();
   }

   FilePath rootDir_;
   FilePath currentFile_;
   std::vector<FilePath> fileStack_;
};

FilePath texFilePath(const std::string& logPath, const FilePath& compileDir)
{
   // some tex compilers report file names with absolute paths and some
   // report them relative to the compilation directory -- on Posix use
   // realPath to get a clean full path back

   FilePath path = compileDir.complete(logPath);
   FilePath realPath;
   Error error = core::system::realPath(path, &realPath);
   if (error)
   {
      // log any error which isn't no such file or directory
      if (error.code() !=
          boost::system::errc::no_such_file_or_directory)
      {
         LOG_ERROR(error);
      }

      return path;
   }
   else
   {
      return realPath;
   }
}

size_t calculateWrappedLine(const std::vector<size_t>& unwrappedLines,
                               size_t unwrappedLineNum)
{
   for (std::vector<size_t>::const_iterator it = unwrappedLines.begin();
        it != unwrappedLines.end();
        it++)
   {
      if (*it >= unwrappedLineNum)
      {
         return unwrappedLineNum + (it - unwrappedLines.begin());
      }
   }

   return unwrappedLineNum + unwrappedLines.size();
}

} // anonymous namespace

Error parseLatexLog(const FilePath& logFilePath, LogEntries* pLogEntries)
{
   static boost::regex regexOverUnderfullLines(" at lines (\\d+)--(\\d+)\\s*(?:\\[])?$");
   static boost::regex regexWarning("^(?:.*?) Warning: (.+)");
   static boost::regex regexWarningEnd(" input line (\\d+)\\.$");
   static boost::regex regexLnn("^l\\.(\\d+)\\s");
   static boost::regex regexCStyleError("^(.+):(\\d+):\\s(.+)$");

   std::vector<std::string> lines;
   Error error = readStringVectorFromFile(logFilePath, &lines, false);
   if (error)
      return error;

   std::vector<size_t> linesUnwrapped;
   unwrapLines(&lines, &linesUnwrapped);

   FilePath rootDir = logFilePath.parent();
   FileStack fileStack(rootDir);

   for (std::vector<std::string>::const_iterator it = lines.begin();
        it != lines.end();
        it++)
   {
      const std::string& line = *it;
      int logLineNum = (it - lines.begin()) + 1;

      // We slurp overfull/underfull messages with no further processing
      // (i.e. not manipulating the file stack)

      if (beginsWith(line, "Overfull ", "Underfull "))
      {
         std::string msg = line;
         int lineNum = -1;

         // Parse lines, if present
         boost::smatch overUnderfullLinesMatch;
         if (regex_utils::search(line,
                                 overUnderfullLinesMatch,
                                 regexOverUnderfullLines))
         {
            lineNum = safe_convert::stringTo<int>(overUnderfullLinesMatch[1],
                                                  -1);
         }

         // Single line case
         bool singleLine = boost::algorithm::ends_with(line, "[]");

         if (singleLine)
         {
            msg.erase(line.size()-2, 2);
            boost::algorithm::trim_right(msg);
         }

         pLogEntries->push_back(LogEntry(logFilePath,
                                         calculateWrappedLine(linesUnwrapped,
                                                              logLineNum),
                                         LogEntry::Box,
                                         fileStack.currentFile(),
                                         lineNum,
                                         msg));

         if (singleLine)
            continue;

         for (; it != lines.end(); it++)
         {
            // For multi-line case, we're looking for " []" on a line by itself
            if (*it == " []")
               break;
         }

         // The iterator would be incremented by the outer for loop, must not
         // let it go past the end! (If we did get to the end, it would
         // mean the log file was malformed, but we still can't crash in this
         // situation.)
         if (it == lines.end())
            break;
         else
            continue;
      }

      fileStack.processLine(line);

      // Now see if it's an error or warning

      if (beginsWith(line, "! "))
      {
         std::string errorMsg = line.substr(2);
         int lineNum = -1;

         boost::smatch match;
         for (it++; it != lines.end(); it++)
         {
            if (regex_utils::search(*it, match, regexLnn))
            {
               lineNum = safe_convert::stringTo<int>(match[1], -1);
               break;
            }
         }

         pLogEntries->push_back(LogEntry(logFilePath,
                                         calculateWrappedLine(linesUnwrapped,
                                                              logLineNum),
                                         LogEntry::Error,
                                         fileStack.currentFile(),
                                         lineNum,
                                         errorMsg));

         // The iterator would be incremented by the outer for loop, must not
         // let it go past the end! (If we did get to the end, it would
         // mean the log file was malformed, but we still can't crash in this
         // situation.)
         if (it == lines.end())
            break;
         else
            continue;
      }

      boost::smatch warningMatch;
      if (regex_utils::search(line, warningMatch, regexWarning))
      {
         std::string warningMsg = warningMatch[1];
         int lineNum = -1;
         while (true)
         {
            if (boost::algorithm::ends_with(warningMsg, "."))
            {
               boost::smatch warningEndMatch;
               if (regex_utils::search(*it, warningEndMatch, regexWarningEnd))
               {
                  lineNum = safe_convert::stringTo<int>(warningEndMatch[1], -1);
               }
               break;
            }

            if (++it == lines.end())
               break;
            warningMsg.append(*it);
         }

         pLogEntries->push_back(LogEntry(logFilePath,
                                         calculateWrappedLine(linesUnwrapped,
                                                              logLineNum),
                                         LogEntry::Warning,
                                         fileStack.currentFile(),
                                         lineNum,
                                         warningMsg));

         // The iterator would be incremented by the outer for loop, must not
         // let it go past the end! (If we did get to the end, it would
         // mean the log file was malformed, but we still can't crash in this
         // situation.)
         if (it == lines.end())
            break;
         else
            continue;
      }

      boost::smatch cStyleErrorMatch;
      if (regex_utils::search(line, cStyleErrorMatch, regexCStyleError))
      {
         FilePath cstyleFile = resolveFilename(rootDir, cStyleErrorMatch[1]);
         if (cstyleFile.exists())
         {
            int lineNum = safe_convert::stringTo<int>(cStyleErrorMatch[2], -1);
            pLogEntries->push_back(LogEntry(logFilePath,
                                            calculateWrappedLine(linesUnwrapped,
                                                                 logLineNum),
                                            LogEntry::Error,
                                            cstyleFile,
                                            lineNum,
                                            cStyleErrorMatch[3]));
         }
      }
   }

   return Success();
}

Error parseBibtexLog(const FilePath& logFilePath, LogEntries* pLogEntries)
{
   boost::regex re("^(.*)---line ([0-9]+) of file (.*)$");

   // get the lines
   std::vector<std::string> lines;
   Error error = core::readStringVectorFromFile(logFilePath, &lines, false);
   if (error)
      return error;

   // look for error messages
   for (std::vector<std::string>::const_iterator it = lines.begin();
        it != lines.end();
        it++)
   {
      boost::smatch match;
      if (regex_utils::match(*it, match, re))
      {
         pLogEntries->push_back(
               LogEntry(
                     logFilePath,
                     (it - lines.begin()) + 1,
                     LogEntry::Error,
                     texFilePath(match[3], logFilePath.parent()),
                     boost::lexical_cast<int>(match[2]),
                     match[1]));
      }
   }

   return Success();
}

} // namespace tex
} // namespace core 
} // namespace rstudio



