/*
 * ShellUtils.cpp
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

#include <core/system/ShellUtils.hpp>

#include <core/FilePath.hpp>

namespace core {
namespace shell_utils {

std::string pipe(const std::string& command1, const std::string& command2)
{
   return command1 + " | " + command2;
}

ShellCommand& ShellCommand::operator<<(const std::string& arg)
{
   output_.push_back(' ');
   output_.append(escape(arg));
   return *this;
}

ShellCommand& ShellCommand::operator<<(const FilePath& path)
{
   output_.push_back(' ');
   // TODO: check encoding
   output_.append(escape(path.absolutePath()));
   return *this;
}

ShellCommand& ShellCommand::operator<<(const std::vector<std::string> args)
{
   for (std::vector<std::string>::const_iterator it = args.begin();
        it != args.end();
        it++)
   {
      *this << *it;
   }
   return *this;
}

ShellCommand& ShellCommand::operator<<(const std::vector<FilePath> args)
{
   for (std::vector<FilePath>::const_iterator it = args.begin();
        it != args.end();
        it++)
   {
      *this << *it;
   }
   return *this;
}

ShellArgs& ShellArgs::operator<<(const std::string& arg)
{
   args_.push_back(arg);
   return *this;
}

ShellArgs& ShellArgs::operator<<(const FilePath& path)
{
   args_.push_back(string_utils::utf8ToSystem(path.absolutePath()));
   return *this;
}

ShellArgs& ShellArgs::operator<<(const std::vector<std::string> args)
{
   for (std::vector<std::string>::const_iterator it = args.begin();
        it != args.end();
        it++)
   {
      *this << *it;
   }
   return *this;
}

ShellArgs& ShellArgs::operator<<(const std::vector<FilePath> args)
{
   for (std::vector<FilePath>::const_iterator it = args.begin();
        it != args.end();
        it++)
   {
      *this << *it;
   }
   return *this;
}

}
}
