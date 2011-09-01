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

namespace core {
namespace shell_utils {

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

}
}
