/*
 * ShellUtils.hpp
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

#ifndef CORE_SHELL_UTILS_HPP
#define CORE_SHELL_UTILS_HPP

#include <string>
#include <core/Error.hpp>
#include <core/FilePath.hpp>

using namespace core;

namespace core {
namespace shell_utils {

std::string escape(const std::string& arg);
std::string escape(const core::FilePath& path);

std::string join(const std::string& command1, const std::string& command2);
std::string join_and(const std::string& command1, const std::string& command2);
std::string join_or(const std::string& command1, const std::string& command2);

std::string sendStdErrToStdOut(const std::string& command);

class ShellCommand
{
public:
   explicit ShellCommand(const std::string& program)
   {
      output_ = program;
   }

   ShellCommand& operator<<(const std::string& arg);
   ShellCommand& operator<<(const FilePath& path);
   ShellCommand& operator<<(const std::vector<std::string> args);

   operator std::string() const
   {
      return output_;
   }

private:
   std::string output_;
};

}
}

#endif // CORE_SHELL_UTILS_HPP
