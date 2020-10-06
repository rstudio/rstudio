/*
 * DcfParser.hpp
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

#ifndef DCF_PARSER_HPP
#define DCF_PARSER_HPP

#include <string>
#include <map>

#include <boost/function.hpp>


namespace rstudio {
namespace core {

class Error;
class FilePath;

namespace text {

extern const char * const kDcfFieldRegex;

// Function which is called to record the results of parsing. If
// a blank line (i.e. record delimiter) is encountered then the
// function will be called with a pair of empty strings. Return
// true to continue parsing and false to terminate parsing.
typedef boost::function<bool(const std::pair<std::string,std::string>&)>
                                                           DcfFieldRecorder;

Error parseDcfFile(const std::string& dcfFileContents,
                   bool preserveKeyCase,
                   DcfFieldRecorder recordField,
                   std::string* pUserErrMsg);

Error parseDcfFile(const std::string& dcfFileContents,
                   bool preserveKeyCase,
                   std::map<std::string, std::string>* pFields,
                   std::string* pUserErrMsg);

Error parseDcfFile(const FilePath& dcfFilePath,
                   bool preserveKeyCase,
                   std::map<std::string,std::string>* pFields,
                   std::string* pUserErrMsg);

Error parseMultiDcfFile(const std::string& dcfFileContents,
                        bool preserveKeyCase,
                        const boost::function<Error(int, const std::map<std::string, std::string>&)>& handleEntry);

Error parseMultiDcfFile(const FilePath& dcfFilePath,
                        bool preserveKeyCase,
                        const boost::function<Error(int, const std::map<std::string, std::string>&)>& handleEntry);

std::string dcfMultilineAsFolded(const std::string& line);


} // namespace text
} // namespace core
} // namespace rstudio

#endif // DCF_PARSER_HPP
