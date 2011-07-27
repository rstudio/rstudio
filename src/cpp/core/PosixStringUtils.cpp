/*
 * PosixStringUtils.cpp
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

#include <core/StringUtils.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>


#include <boost/program_options/detail/convert.hpp>
#include <boost/program_options/detail/utf8_codecvt_facet.hpp>

namespace core {
namespace string_utils {

std::string wideToUtf8(const std::wstring& value)
{
   boost::program_options::detail::utf8_codecvt_facet utf8_facet;
   return boost::to_8_bit(value, utf8_facet);
}

std::wstring utf8ToWide(const std::string& value)
{
   boost::program_options::detail::utf8_codecvt_facet utf8_facet;
   return boost::from_8_bit(value, utf8_facet);
}

} // namespace string_utils
} // namespace core



