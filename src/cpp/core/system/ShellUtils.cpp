/*
 * ShellUtils.cpp
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

#include <core/system/ShellUtils.hpp>

#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>

namespace rstudio {
namespace core {
namespace shell_utils {

std::string pipe(const std::string& command1, const std::string& command2)
{
   return command1 + " | " + command2;
}

std::string ShellCommand::maybeEscape(const std::string &value)
{
   if (escapeMode_ == EscapeAll)
      return escape(value);
   else
      return value;
}

ShellCommand& ShellCommand::operator<<(EscapeMode escapeMode)
{
   escapeMode_ = escapeMode;
   return *this;
}

ShellCommand& ShellCommand::operator<<(const std::string& arg)
{
   output_.push_back(' ');
   output_.append(maybeEscape(arg));
   return *this;
}

ShellCommand& ShellCommand::operator<<(int arg)
{
   output_.push_back(' ');
   output_.append(safe_convert::numberToString(arg));
   return *this;
}

ShellCommand& ShellCommand::operator<<(const FilePath& path)
{
   output_.push_back(' ');
   // TODO: check encoding
   output_.append(escape(path.getAbsolutePath()));
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

ShellArgs& ShellArgs::operator <<(EncodingMode mode)
{
   encodingMode_ = mode;
   return *this;
}

ShellArgs& ShellArgs::operator<<(const std::string& arg)
{
   args_.push_back(arg);
   return *this;
}

ShellArgs& ShellArgs::operator<<(int arg)
{
   args_.push_back(safe_convert::numberToString(arg));
   return *this;
}

ShellArgs& ShellArgs::operator<<(const FilePath& path)
{
   if (encodingMode_ == SystemEncoding)
      *this << string_utils::utf8ToSystem(path.getAbsolutePath());
   else
      *this << path.getAbsolutePath();
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
}
