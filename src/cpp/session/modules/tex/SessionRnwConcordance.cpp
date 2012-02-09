/*
 * SessionRnwConcordance.cpp
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

#include "SessionRnwConcordance.hpp"

#include <iostream>

#include <boost/foreach.hpp>
#include <boost/regex.hpp>

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/split.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/SafeConvert.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

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

Error Concordance::readFromFile(const FilePath& inputFile)
{
   // read lines
   std::vector<std::string> lines;
   Error error = core::readStringVectorFromFile(inputFile, &lines);
   if (error)
      return error;

   // paste them together (removing trailing %)
   using namespace boost::algorithm;
   std::string concordance;
   BOOST_FOREACH(const std::string& line, lines)
   {
      concordance.append(trim_right_copy_if(line, is_any_of("%")));
   }

   // extract concordance structure
   boost::regex re("\\\\Sconcordance\\{([^\\}]+)\\}");
   boost::smatch match;
   if (!boost::regex_match(concordance, match, re))
      return badFormatError(inputFile, "body", ERROR_LOCATION);

   // split into sections
   std::vector<std::string> sections;
   boost::algorithm::split(sections,
                           static_cast<const std::string>(match[1]),
                           boost::algorithm::is_any_of(":"));

   // validate the number of sections
   if (sections.size() < 4 || sections.size() > 5)
       return badFormatError(inputFile, "sections", ERROR_LOCATION);

   // get input and output file names
   outputFile_ = sections[1];
   inputFile_ = sections[2];

   // get offset and values
   std::string valuesSection;
   if (sections.size() == 5)
   {
      boost::regex re("^ofs ([0-9]+)");
      boost::smatch match;
      if (!boost::regex_match(sections[3], match, re))
         return badFormatError(inputFile, "offset", ERROR_LOCATION);

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
      return badFormatError(inputFile, "values", ERROR_LOCATION);
   }

   // confirm we have at least one element and extract it as the start line
   if (rleValues.size() < 1)
      return badFormatError(inputFile, "no-values", ERROR_LOCATION);
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

void removePrevious(const core::FilePath& rnwFile)
{
   Error error = concordanceFilePath(rnwFile).removeIfExists();
   if (error)
      LOG_ERROR(error);
}


Error readIfExists(const core::FilePath& rnwFile, Concordance* pConcordance)
{
   FilePath concordanceFile = concordanceFilePath(rnwFile);
   if (concordanceFile.exists())
   {
      return pConcordance->readFromFile(concordanceFile);
   }
   else
   {
      return Success();
   }
}


SweaveConcordanceInjector::SweaveConcordanceInjector(const FilePath& rnwFile)
   : ConcordanceInjector(rnwFile)
{
   // backup the rnw file
   FilePath backupFile = module_context::tempFile(
                                    rnwFile.stem() + "_backup",
                                    ".Rnw");
   Error error = rnwFile.copy(backupFile);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // append the concordance option
   std::ostringstream ostr;
   ostr << std::endl << "\\SweaveOpts{concordance=TRUE}" << std::endl;
   error = core::appendToFile(rnwFile, ostr.str());
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // save the path of the backupFile (for restoration)
   rnwFileBackup_ = backupFile;
}

SweaveConcordanceInjector::~SweaveConcordanceInjector()
{
   try
   {
      // restore the backup
      if (!rnwFileBackup_.empty())
      {
         Error error = rnwFileBackup_.move(rnwFilePath());
         if (error)
         {
            LOG_ERROR(error);
            return;
         }
      }
   }
   catch(...)
   {
   }
}


} // namespace rnw_concordance
} // namespace tex
} // namespace modules
} // namesapce session

