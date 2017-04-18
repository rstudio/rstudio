/*
 * SessionRnwConcordance.cpp
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

#include "SessionRnwConcordance.hpp"

#include <iostream>

#include <boost/foreach.hpp>
#include <boost/regex.hpp>

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/regex.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/SafeConvert.hpp>
#include <core/Algorithm.hpp>

#include <core/tex/TexSynctex.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace tex {
namespace rnw_concordance {

namespace {

FilePath concordanceFilePath(const FilePath& rnwFilePath)
{
   FilePath parentDir = rnwFilePath.parent();
   return parentDir.complete(rnwFilePath.stem() + "-concordance.tex");
}

Error badFormatError(const FilePath& concordanceFile,
                     const std::string& context,
                     const ErrorLocation& location)
{
   return systemError(boost::system::errc::protocol_error,
                      "Unexpected concordance file format (" + context + ")",
                      location);
}

inline int strToInt(const std::string& str)
{
   return boost::lexical_cast<int>(str);
}

template<typename InputIterator, typename OutputIterator>
OutputIterator rleDecodeValues(InputIterator begin,
                               InputIterator end,
                               OutputIterator destBegin)
{
   while (begin != end)
   {
      int count = *begin++;

      if (begin == end)
         break;

      int val = *begin++;

      for (int i=0;i<count; i++)
         *destBegin++ = val;
   }
   return destBegin;
}

} // anonymous namespace

Error Concordance::parse(const FilePath& sourceFile,
                         const std::string& input,
                         const FilePath& baseDir)
{
   // split into lines
   std::vector<std::string> lines;
   boost::algorithm::split(lines, input,  boost::algorithm::is_any_of("\n"));

   // paste them back together (removing trailing %)
   using namespace boost::algorithm;
   std::string concordance;
   BOOST_FOREACH(const std::string& line, lines)
   {
      concordance.append(trim_right_copy_if(line, is_any_of("%")));
   }

   // extract concordance structure
   boost::regex re("\\\\Sconcordance\\{([^\\}]+)\\}");
   boost::smatch match;
   if (!regex_utils::match(concordance, match, re))
      return badFormatError(sourceFile, "body", ERROR_LOCATION);

   // split into sections
   std::vector<std::string> sections;
   boost::algorithm::split(sections,
                           static_cast<const std::string>(match[1]),
                           boost::algorithm::is_any_of(":"));

   // validate the number of sections
   if (sections.size() < 4 || sections.size() > 5)
       return badFormatError(sourceFile, "sections", ERROR_LOCATION);

   // get input and output file names
   outputFile_ = baseDir.complete(core::tex::normalizeSynctexName(sections[1]));
   inputFile_ = baseDir.complete(core::tex::normalizeSynctexName(sections[2]));

   // get offset and values
   std::string valuesSection;
   if (sections.size() == 5)
   {
      boost::regex re("^ofs ([0-9]+)");
      boost::smatch match;
      if (!regex_utils::match(sections[3], match, re))
         return badFormatError(sourceFile, "offset", ERROR_LOCATION);

      offset_ = safe_convert::stringTo<std::size_t>(match[1], 0);
      valuesSection = sections[4];
   }
   else
   {
      offset_ = 0;
      valuesSection = sections[3];
   }

   // convert values to integer array
   std::vector<std::string> strValues;
   boost::algorithm::split(strValues,
                           valuesSection,
                           boost::algorithm::is_space(),
                           boost::algorithm::token_compress_on);
   std::vector<int> rleValues;
   try
   {
      std::transform(strValues.begin(),
                     strValues.end(),
                     std::back_inserter(rleValues),
                     &strToInt);
   }
   catch(const boost::bad_lexical_cast&)
   {
      return badFormatError(sourceFile, "values", ERROR_LOCATION);
   }

   // confirm we have at least one element and extract it as the start line
   if (rleValues.size() < 1)
      return badFormatError(sourceFile, "no-values", ERROR_LOCATION);
   int startLine = rleValues[0];

   // unroll the RLE encoded values
   std::vector<int> diffs;
   rleDecodeValues(rleValues.begin() + 1,
                   rleValues.end(),
                   std::back_inserter(diffs));

   // use these values to create the mapping
   mapping_.resize(diffs.size());
   int pos = startLine;
   for (std::size_t i = 0; i<diffs.size(); i++)
   {
      mapping_[i] = pos;
      pos += diffs[i];
   }

   return Success();
}

void Concordance::append(const Concordance& concordance)
{
   // if we don't yet have an input and output file then initialize
   // from this concordance -- otherwise verify that the inbound
   // concordances match
   if (inputFile_.empty())
      inputFile_ = concordance.inputFile();
   if (outputFile_.empty())
      outputFile_ = concordance.outputFile();

   if (inputFile_ != concordance.inputFile())
   {
      LOG_WARNING_MESSAGE("Non matching concordance: " +
                          inputFile_.absolutePath() + ", " +
                          concordance.inputFile().absolutePath());
      return;
   }

   else if (outputFile_ != concordance.outputFile())
   {
      LOG_WARNING_MESSAGE("Non matching concordance: " +
                          outputFile_.absolutePath() + ", " +
                          concordance.outputFile().absolutePath());
      return;
   }

   // if the concordance being added has an offset greater than our
   // number of lines then we need to pad (so that we have a line for
   // each line in the output file even if our concordances aren't
   // responsible for the output)
   int rnwLine = mapping_.size() > 0 ? mapping_.back() : 1;
   while (mapping_.size() < concordance.offset())
      mapping_.push_back(rnwLine);

   // append the inbound concordance
   std::copy(concordance.mapping_.begin(),
             concordance.mapping_.end(),
             std::back_inserter(mapping_));
}

std::ostream& operator << (std::ostream& stream, const FileAndLine& fileLine)
{
   stream << fileLine.filePath() << ":" << fileLine.line();
   return stream;
}


namespace {

bool hasOutputFile(const Concordance& concord, const FilePath& outputFile)
{
   return concord.outputFile().isEquivalentTo(outputFile);
}

bool hasInputFile(const Concordance& concord, const FilePath& inputFile)
{
   return concord.inputFile().isEquivalentTo(inputFile);
}

} // anonymous namespace

FileAndLine Concordances::rnwLine(const FileAndLine& texLine) const
{
   if (texLine.filePath().empty())
      return FileAndLine();

   // inspect concordance where output file is equivliant to tex file
   std::vector<Concordance> texFileConcords;
   algorithm::copy_if(
      concordances_.begin(),
      concordances_.end(),
      std::back_inserter(texFileConcords),
      boost::bind(hasOutputFile, _1, texLine.filePath()));

   // reverse search for the first concordances whose offset is less than
   // the text line we are seeking concordance for
   for (std::vector<Concordance>::const_reverse_iterator it =
      texFileConcords.rbegin(); it != texFileConcords.rend(); ++it)
   {
      if (texLine.line() > static_cast<int>(it->offset()))
      {
          return FileAndLine(it->inputFile(),
                             it->rnwLine(texLine.line()));
      }
   }

   return FileAndLine();
}


FileAndLine Concordances::texLine(const FileAndLine& rnwLine) const
{
   if (rnwLine.filePath().empty())
      return FileAndLine();

   // inspect concordance where input file is equivilant to rnw file
   std::vector<Concordance> rnwFileConcords;
   algorithm::copy_if(
      concordances_.begin(),
      concordances_.end(),
      std::back_inserter(rnwFileConcords),
      boost::bind(hasInputFile, _1, rnwLine.filePath()));
   if (rnwFileConcords.size() == 0)
      return FileAndLine();

   // build a single concordance structure for seeking
   Concordance rnwFileConcord;
   for (std::vector<Concordance>::const_iterator
      it = rnwFileConcords.begin(); it != rnwFileConcords.end(); ++it)
   {
      rnwFileConcord.append(*it);
   }

   // seek
   FileAndLine texLine(rnwFileConcord.outputFile(),
                       rnwFileConcord.texLine(rnwLine.line()));
   return texLine;
}

std::string fixup_formatter(const Concordances& concordances,
                            const FilePath& sourceFile,
                            const boost::smatch& what)
{
   std::string result = what[0];

   for (unsigned int i = what.size()-1; i > 0; i--)
   {
      if (what[i].matched)
      {
         int inputLine = core::safe_convert::stringTo<int>(what[i], 1);
         FileAndLine dest = concordances.rnwLine(
                                           FileAndLine(sourceFile, inputLine));
         if (!dest.empty())
         {
            result.replace(what.position(i) - what.position(),
                           what.length(i),
                           safe_convert::numberToString(dest.line()));
         }
      }
   }

   return result;
}

core::tex::LogEntry Concordances::fixup(const core::tex::LogEntry &entry,
                                        bool *pSuccess) const
{
   // Error messages themselves can (and usually do) contain line numbers.
   // It looks silly when we show the Rnw line numbers juxtaposed with the
   // TeX line numbers (e.g. "Line 102: Error at line 192")
   static boost::regex regexLines("\\blines? (\\d+)(?:-{1,3}(\\d+))?\\b");

   FileAndLine mapped = rnwLine(FileAndLine(entry.filePath(), entry.line()));
   if (!mapped.empty())
   {
      boost::function<std::string(boost::smatch)> formatter =
            boost::bind(fixup_formatter, *this, entry.filePath(), _1);
      std::string mappedMsg =
            boost::regex_replace(entry.message(), regexLines, formatter);

      if (pSuccess)
         *pSuccess = true;

      return core::tex::LogEntry(entry.logFilePath(),
                                 entry.logLine(),
                                 entry.type(),
                                 mapped.filePath(),
                                 mapped.line(),
                                 mappedMsg);
   }
   else
   {
      if (pSuccess)
         *pSuccess = false;
      return entry;
   }
}

void removePrevious(const core::FilePath& rnwFile)
{
   Error error = concordanceFilePath(rnwFile).removeIfExists();
   if (error)
      LOG_ERROR(error);
}


Error readIfExists(const core::FilePath& srcFile, Concordances* pConcordances)
{
   // return success if the file doesn't exist
   FilePath concordanceFile = concordanceFilePath(srcFile);
   if (!concordanceFile.exists())
      return Success();

   // read the file
   std::string contents;
   Error error = core::readStringFromFile(concordanceFile,
                                          &contents,
                                          string_utils::LineEndingPosix);
   if (error)
      return error;

   // split on concordance
   const char * const kConcordance = "\\Sconcordance";
   boost::regex re("\\" + std::string(kConcordance));
   std::vector<std::string> concordances;
   boost::algorithm::split_regex(concordances, contents, re);
   BOOST_FOREACH(const std::string& concordance, concordances)
   {
      std::string entry = boost::algorithm::trim_copy(concordance);
      if (!entry.empty())
      {
         Concordance concord;
         Error error = concord.parse(concordanceFile,
                                     kConcordance + entry,
                                     srcFile.parent());
         if (error)
            LOG_ERROR(error);
         else
            pConcordances->add(concord);
      }
   }

   return Success();
}

} // namespace rnw_concordance
} // namespace tex
} // namespace modules
} // namespace session
} // namespace rstudio

