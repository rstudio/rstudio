/*
 * PosixShellUtils.cpp
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

#include <core/StringUtils.hpp>
#include <boost/regex.hpp>
#include <core/system/ShellUtils.hpp>

namespace rstudio {
namespace core {
namespace shell_utils {

std::string escape(const std::string& arg)
{
   boost::regex pattern("'");
   return "'" + boost::regex_replace(arg, pattern, R"('"'"')") + "'";
}

std::string escape(const core::FilePath &path)
{
   return escape(string_utils::utf8ToSystem(path.getAbsolutePath()));
}

std::string join(const std::string& command1, const std::string& command2)
{
   return command1 + "; " + command2;
}

std::string join_and(const std::string& command1, const std::string& command2)
{
   return command1 + " && " + command2;
}

std::string join_or(const std::string& command1, const std::string& command2)
{
   return "(" + command1 + ") || (" + command2 + ")";
}

std::string sendStdErrToStdOut(const std::string& command)
{
   return "(" + command + ") 2>&1";
}

std::string sendAllOutputToNull(const std::string& command)
{
   return "(" + command + ") > /dev/null 2>&1";
}

std::string sendStdErrToNull(const std::string& command)
{
   return "(" + command + ") 2> /dev/null";
}

std::string sendNullToStdIn(const std::string& command)
{
   return "(" + command + ") < /dev/null";
}

namespace {
FilePath s_devnull("/dev/null");
} // namespace

const FilePath& devnull()
{
   return s_devnull;
}


}
}
}

