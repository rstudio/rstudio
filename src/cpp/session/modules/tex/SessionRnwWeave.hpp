/*
 * SessionRnwWeave.hpp
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

#ifndef SESSION_MODULES_RNW_WEAVE_HPP
#define SESSION_MODULES_RNW_WEAVE_HPP

#include <boost/function.hpp>

#include <core/tex/TexMagicComment.hpp>

#include <core/json/Json.hpp>

#include "SessionRnwConcordance.hpp"

namespace core {
   class Error;
   class FilePath;
}
 
namespace session {
namespace modules { 
namespace tex {

namespace rnw_weave {

core::json::Array supportedTypes();
void getTypesInstalledStatus(core::json::Object* pObj);

struct Result
{
   static Result error(const std::string& errorMessage)
   {
      Result result;
      result.succeeded = false;
      result.errorMessage = errorMessage;
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
   tex::rnw_concordance::Concordances concordances;
};

typedef boost::function<void(const Result&)> CompletedFunction;

void runWeave(const core::FilePath& filePath,
              const core::tex::TexMagicComments& magicComments,
              const boost::function<void(const std::string&)>& onOutput,
              const CompletedFunction& onCompleted);


} // namespace rnw_weave
} // namespace tex
} // namespace modules
} // namesapce session

#endif // SESSION_MODULES_RNW_WEAVE_HPP
