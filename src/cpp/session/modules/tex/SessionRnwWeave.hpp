/*
 * SessionRnwWeave.hpp
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

#ifndef SESSION_MODULES_RNW_WEAVE_HPP
#define SESSION_MODULES_RNW_WEAVE_HPP

#include <boost/function.hpp>

#include <core/tex/TexLogParser.hpp>
#include <core/tex/TexMagicComment.hpp>

#include <core/json/Json.hpp>

#include "SessionRnwConcordance.hpp"

namespace rstudiocore {
   class Error;
   class FilePath;
}
 
namespace session {
namespace modules { 
namespace tex {

namespace rnw_weave {

rstudiocore::json::Array supportedTypes();
void getTypesInstalledStatus(rstudiocore::json::Object* pObj);

rstudiocore::json::Value chunkOptions(const std::string& weaveType);

struct Result
{
   static Result error(const std::string& errorMessage)
   {
      Result result;
      result.succeeded = false;
      result.errorMessage = errorMessage;
      return result;
   }

   static Result error(const rstudiocore::tex::LogEntries& logEntries)
   {
      Result result;
      result.succeeded = false;
      result.errorLogEntries = logEntries;
      return result;
   }

   static Result success(
                  const tex::rnw_concordance::Concordances& concordances)
   {
      Result result;
      result.succeeded = true;
      result.concordances = concordances;
      return result;
   }

   bool succeeded;
   std::string errorMessage;
   rstudiocore::tex::LogEntries errorLogEntries;
   tex::rnw_concordance::Concordances concordances;
};

typedef boost::function<void(const Result&)> CompletedFunction;

void runTangle(const std::string& filePath, const std::string& rnwWeave);

void runWeave(const rstudiocore::FilePath& filePath,
              const std::string& encoding,
              const rstudiocore::tex::TexMagicComments& magicComments,
              const boost::function<void(const std::string&)>& onOutput,
              const CompletedFunction& onCompleted);


} // namespace rnw_weave
} // namespace tex
} // namespace modules
} // namesapce session

#endif // SESSION_MODULES_RNW_WEAVE_HPP
