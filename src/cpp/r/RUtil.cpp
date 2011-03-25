/*
 * RUtil.cpp
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


#include <r/RUtil.hpp>

#include <boost/algorithm/string/replace.hpp>
#include <boost/regex.hpp>

#include <core/FilePath.hpp>
#include <core/StringUtils.hpp>
#include <core/Error.hpp>

#include <r/RExec.hpp>

#include <R_ext/Utils.h>

using namespace core;

namespace r {
namespace util {

std::string expandFileName(const std::string& name)
{
   return std::string(R_ExpandFileName(name.c_str()));
}

std::string fixPath(const std::string& path)
{
   // R sometimes gives us a path a double slashes in it ("//"). Eliminate them.
   std::string fixedPath(path);
   boost::algorithm::replace_all(fixedPath, "//", "/");
   return fixedPath;
}

bool hasRequiredVersion(const std::string& version)
{
   std::string versionTest("getRversion() >= \"" + version + "\"");
   bool hasRequired;
   Error error = r::exec::evaluateString(versionTest, &hasRequired);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   else
   {
      return hasRequired;
   }
}

std::string rconsole2utf8(const std::string& encoded)
{
   boost::regex utf8("\x02\xFF\xFE(.*?)(\x03\xFF\xFE|\\')");

   std::string output;
   std::string::const_iterator pos = encoded.begin();
   boost::smatch m;
   while (pos != encoded.end() && boost::regex_search(pos, encoded.end(), m, utf8))
   {
      if (pos < m[0].first)
         output.append(string_utils::systemToUtf8(std::string(pos, m[0].first)));
      output.append(m[1].first, m[1].second);
      pos = m[0].second;
   }
   if (pos != encoded.end())
      output.append(string_utils::systemToUtf8(std::string(pos, encoded.end())));

   return output;
}

core::Error iconv(const std::string& value,
                  const std::string& from,
                  const std::string& to,
                  std::string* pResult)
{
   std::string effectiveFrom = from;
   if (effectiveFrom.empty())
      effectiveFrom = "UTF-8";
   std::string effectiveTo = to;
   if (effectiveTo.empty())
      effectiveTo = "UTF-8";

   if (effectiveFrom == effectiveTo)
   {
      *pResult = value;
      return Success();
   }

   r::exec::RFunction func("iconv");
   func.addParam("x", value);
   func.addParam("from", effectiveFrom);
   func.addParam("to", effectiveTo);
   func.addParam("sub", "?");

   return func.call(pResult);
}

} // namespace util
} // namespace r



