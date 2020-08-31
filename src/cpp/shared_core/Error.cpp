/*
 * Error.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant to the terms of a commercial license agreement
 * with RStudio, then this program is licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#include <ostream>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/Logger.hpp>
#include <shared_core/SafeConvert.hpp>

#ifdef _WIN32
#include <boost/system/windows_error.hpp>
#endif

namespace rstudio {
namespace core {

namespace {

constexpr const char* s_occurredAt = "OCCURRED AT";
constexpr const char* s_causedBy = "CAUSED BY";

} // anonymous namespace


// Error Location ======================================================================================================
std::ostream& operator<<(std::ostream& in_os, const ErrorLocation& in_location)
{
   in_os << in_location.getFunction() << " "
         << in_location.getFile() << ":"
         << in_location.getLine();

   return in_os;
}

struct ErrorLocation::Impl
{
   Impl() : Line(0)
   {
   }

   Impl(const char* in_function, const char* in_file, long in_line) :
      Function(in_function), File(in_file), Line(in_line)
   {
   }

   std::string Function;
   std::string File;
   long Line;
};

PRIVATE_IMPL_DELETER_IMPL(ErrorLocation)

ErrorLocation::ErrorLocation() :
   m_impl(new Impl())
{
}

ErrorLocation::ErrorLocation(const ErrorLocation& in_other) :
   m_impl(new Impl(*in_other.m_impl))
{
}

ErrorLocation::ErrorLocation(rstudio::core::ErrorLocation&& in_other) noexcept :
   m_impl(std::move(in_other.m_impl))
{
   // Don't leave an error location with a nullptr impl.
   in_other.m_impl.reset(new Impl());
}

ErrorLocation::ErrorLocation(const char* in_function, const char* in_file, long in_line) :
   m_impl(new Impl(in_function, in_file, in_line))
{
}

ErrorLocation& ErrorLocation::operator=(const ErrorLocation& in_other)
{
   m_impl->File = in_other.m_impl->File;
   m_impl->Function = in_other.m_impl->Function;
   m_impl->Line = in_other.m_impl->Line;

   return *this;
}

bool ErrorLocation::operator==(const ErrorLocation& in_location) const
{
   return getFunction() == in_location.getFunction() &&
          getFile() == in_location.getFile() &&
          getLine() == in_location.getLine();
}

std::string ErrorLocation::asString() const
{
   std::ostringstream ostr;
   ostr << *this;
   return ostr.str();
}

const std::string& ErrorLocation::getFunction() const
{
   return m_impl->Function;
}

const std::string& ErrorLocation::getFile() const
{
   return m_impl->File;
}

long ErrorLocation::getLine() const
{
   return m_impl->Line;
}

bool ErrorLocation::hasLocation() const
{
   return getLine() > 0;
}

// Error ===============================================================================================================
// COPYING: via shared_ptr, mutating functions must call copyOnWrite prior to executing
struct Error::Impl
{
   Impl() : Code(0)
   { }

   Impl(const Impl& in_other) = default;

   Impl(int in_code, std::string in_name, ErrorLocation in_location) :
      Code(in_code),
      Name(std::move(in_name)),
      Location(std::move(in_location))
   { }

   Impl(int in_code, std::string in_name, const Error& in_cause, ErrorLocation in_location) :
      Code(in_code),
      Name(std::move(in_name)),
      Cause(in_cause),
      Location(std::move(in_location))
   { }

   Impl(int in_code, std::string in_name, std::string in_message, ErrorLocation in_location) :
      Code(in_code),
      Name(std::move(in_name)),
      Message(std::move(in_message)),
      Location(std::move(in_location))
   { }

   Impl(int in_code, std::string in_name, std::string in_message, const Error& in_cause, ErrorLocation in_location) :
      Code(in_code),
      Name(std::move(in_name)),
      Message(std::move(in_message)),
      Cause(in_cause),
      Location(std::move(in_location))
   { }

   int Code;
   std::string Name;
   std::string Message;
   ErrorProperties Properties;
   Error Cause;
   ErrorLocation Location;
   bool Expected = false;
};

// This is a shallow copy because deep copy will only be performed on a write.
Error::Error(const Error& in_other) :
   m_impl(in_other.m_impl)
{
}

Error::Error(const boost::system::error_code& in_ec, const ErrorLocation& in_location) :
   m_impl(new Impl(in_ec.value(), in_ec.category().name(), in_ec.message(), in_location))
{
}

Error::Error(const boost::system::error_code& in_ec, const Error& in_cause, const ErrorLocation& in_location) :
   m_impl(new Impl(in_ec.value(), in_ec.category().name(), in_ec.message(), in_cause, in_location))
{
}

Error::Error(const boost::system::error_code& in_ec, std::string in_message, const ErrorLocation& in_location) :
   m_impl(new Impl(in_ec.value(), in_ec.category().name(), std::move(in_message), in_location))
{
}

Error::Error(
   const boost::system::error_code& in_ec,
   std::string in_message,
   const Error& in_cause,
   const ErrorLocation& in_location) :
   m_impl(new Impl(in_ec.value(), in_ec.category().name(), std::move(in_message), in_cause, in_location))
{
}

Error::Error(const boost::system::error_condition& in_ec, const ErrorLocation& in_location) :
   m_impl(new Impl(in_ec.value(), in_ec.category().name(), in_ec.message(), in_location))
{
}

Error::Error(const boost::system::error_condition& in_ec, const Error& in_cause, const ErrorLocation& in_location) :
   m_impl(new Impl(in_ec.value(), in_ec.category().name(), in_ec.message(), in_cause, in_location))
{
}

Error::Error(const boost::system::error_condition& in_ec, std::string in_message, const ErrorLocation& in_location) :
   m_impl(new Impl(in_ec.value(), in_ec.category().name(), std::move(in_message), in_location))
{
}

Error::Error(
   const boost::system::error_condition& in_ec,
   std::string in_message,
   const Error& in_cause,
   const ErrorLocation& in_location) :
   m_impl(new Impl(in_ec.value(), in_ec.category().name(), std::move(in_message), in_cause, in_location))
{
}

Error::Error(std::string in_name, int in_code, const ErrorLocation& in_location) :
   m_impl(new Impl(in_code, std::move(in_name), in_location))
{
}

Error::Error(std::string in_name, int in_code, const Error& in_cause, const ErrorLocation& in_location) :
   m_impl(new Impl(in_code, std::move(in_name), in_cause, in_location))
{
}

Error::Error(std::string in_name, int in_code, std::string in_message, const ErrorLocation& in_location) :
   m_impl(new Impl(in_code, std::move(in_name), std::move(in_message), in_location))
{
}

Error::Error(
   std::string in_name,
   int in_code,
   std::string in_message,
   const Error& in_cause,
   const ErrorLocation& in_location) :
   m_impl(new Impl(in_code, std::move(in_name), std::move(in_message), in_cause, in_location))
{
}

Error::operator bool() const
{
   return isError();
}

bool Error::operator!() const
{
   return !isError();
}

bool Error::operator==(const Error& in_other) const
{
   if ((m_impl == nullptr) || (m_impl->Code == 0))
      return (in_other.m_impl == nullptr) || (in_other.m_impl->Code == 0);
   if ((in_other.m_impl == nullptr) || (in_other.m_impl->Code == 0))
      return false; // This error is neither empty nor 0.
   return (m_impl->Code == in_other.m_impl->Code) && (m_impl->Name == in_other.m_impl->Name);
}

bool Error::operator==(const boost::system::error_code& in_ec) const
{
   if ((m_impl == nullptr) || (m_impl->Code == 0))
      return in_ec.value() == 0;
   return (m_impl->Code == in_ec.value()) && (m_impl->Name == in_ec.category().name());
}

bool Error::operator!=(const rstudio::core::Error& in_other) const
{
   return !(*this == in_other);
}

bool Error::operator!=(const boost::system::error_code& in_ec) const
{
   return !(*this == in_ec);
}

void Error::addOrUpdateProperty(const std::string& in_name, const std::string& in_value)
{
   copyOnWrite();

   for (auto & property : impl().Properties)
   {
      if (property.first == in_name)
      {
         property.second = in_value;
         return;
      }
   }

   addProperty(in_name, in_value);
}

void Error::addOrUpdateProperty(const std::string& in_name, const FilePath& in_value)
{
   addOrUpdateProperty(in_name, in_value.getAbsolutePath());
}

void Error::addOrUpdateProperty(const std::string& in_name, int in_value)
{
   addOrUpdateProperty(in_name, safe_convert::numberToString(in_value));
}

void Error::addProperty(const std::string& in_name, const std::string& in_value)
{
   copyOnWrite();
   impl().Properties.push_back(std::make_pair(in_name, in_value));
}

void Error::addProperty(const std::string& in_name, const FilePath& in_value)
{
   addProperty(in_name, in_value.getAbsolutePath());
}

void Error::addProperty(const std::string& in_name, int in_value)
{
   addProperty(in_name, safe_convert::numberToString(in_value));
}

std::string Error::asString() const
{
   std::ostringstream ostr;
   std::ostringstream errorStream;
   errorStream << getSummary();
   auto& props = getProperties();
   if (!props.empty())
   {
      errorStream << " [";
      for (size_t i = 0; i < props.size(); i++)
      {
         errorStream << props[i].first << ": " << props[i].second;
         if (i < props.size() - 1)
            errorStream << ", ";
      }
      errorStream << "]";
   }

   ostr << log::cleanDelimiters(errorStream.str());

   ostr << log::s_delim << " " << s_occurredAt << " "
        << log::cleanDelimiters(getLocation().asString());

   if (getCause())
   {
      ostr << log::s_delim << " " << s_causedBy << ": " << getCause().asString();
   }

   return ostr.str();
}

const Error& Error::getCause() const
{
   return impl().Cause;
}

int Error::getCode() const
{
   return impl().Code;
}

const ErrorLocation& Error::getLocation() const
{
   return impl().Location;
}

const std::string& Error::getMessage() const
{
   return impl().Message;
}

const std::string& Error::getName() const
{
   return impl().Name;
}

const ErrorProperties& Error::getProperties() const
{
   return impl().Properties;
}

std::string Error::getProperty(const std::string& in_name) const
{
   for (const auto & it : getProperties())
   {
      if (it.first == in_name)
         return it.second;
   }

   return std::string();
}

std::string Error::getSummary() const
{
   std::ostringstream ostr;
   ostr << impl().Name << " error "
        << impl().Code << " (" << impl().Message << ")";
   return ostr.str();
}

bool Error::isExpected() const
{
   return m_impl->Expected;
}

void Error::setExpected()
{
   m_impl->Expected = true;
}

void Error::copyOnWrite()
{
   if ((m_impl != nullptr) && !m_impl.unique())
      m_impl.reset(new Impl(impl()));
}

bool Error::isError() const
{
   if (m_impl != nullptr)
      return impl().Code != 0;
   else
      return false;
}

Error::Impl& Error::impl() const
{
   // This doesn't really change the internal meaning of the object, so it's safe to const_cast here.
   if (m_impl == nullptr)
      const_cast<std::shared_ptr<Impl>*>(&m_impl)->reset(new Impl());
   return *m_impl;
}

std::ostream& operator<<(std::ostream& io_ostream, const Error& in_error)
{
   return io_ostream << in_error.asString();
}

// Common error creation functions =====================================================================================
Error systemError(int in_code, const ErrorLocation& in_location)
{
   using namespace boost::system;
   return Error(error_code(in_code, system_category()), in_location);
}

Error systemError(const std::error_code& in_code, const ErrorLocation& in_location)
{
   return Error(in_code.category().name(), in_code.value(), in_code.message(), in_location);
}

Error systemError(const std::system_error& in_error, const ErrorLocation& in_location)
{
   return Error(in_error.code().category().name(), in_error.code().value(), in_error.what(), in_location);
}

Error systemError(int in_code,
                  const Error& in_cause,
                  const ErrorLocation& in_location)
{
   using namespace boost::system;
   return Error(error_code(in_code, system_category()), in_cause, in_location);
}

Error systemError(const std::error_code& in_code, const Error& in_cause, const ErrorLocation& in_location)
{
   return Error(in_code.category().name(), in_code.value(), in_code.message(), in_cause, in_location);
}

Error systemError(const std::system_error& in_error, const Error& in_cause, const ErrorLocation& in_location)
{
   return Error(
      in_error.code().category().name(),
      in_error.code().value(),
      in_error.what(),
      in_cause,
      in_location);
}

Error systemError(int in_code,
                  const std::string& in_description,
                  const ErrorLocation& in_location)
{
   Error error = systemError(in_code, in_location);
   error.addProperty("description", in_description);
   return error;
}

Error systemError(const std::error_code& in_code,
                  const std::string& in_description,
                  const ErrorLocation& in_location)
{
   Error error = systemError(in_code, in_location);
   error.addProperty("description", in_description);
   return error;
}

Error systemError(const std::system_error& in_error,
                  const std::string& in_description,
                  const ErrorLocation& in_location)
{
   Error error = systemError(in_error, in_location);
   error.addProperty("description", in_description);
   return error;
}

Error systemError(int in_code,
                  const std::string& in_description,
                  const Error& in_cause,
                  const ErrorLocation& in_location)
{
   Error error = systemError(in_code, in_cause, in_location);
   error.addProperty("description", in_description);
   return error;
}

Error systemError(const std::error_code&  in_code,
                  const std::string& in_description,
                  const Error& in_cause,
                  const ErrorLocation& in_location)
{
   Error error = systemError(in_code, in_cause, in_location);
   error.addProperty("description", in_description);
   return error;
}

Error systemError(const std::system_error& in_error,
                  const std::string& in_description,
                  const Error& in_cause,
                  const ErrorLocation& in_location)
{
   Error error = systemError(in_error, in_cause, in_location);
   error.addProperty("description", in_description);
   return error;
}

Error unknownError(const std::string& in_message, const ErrorLocation&  in_location)
{
   return Error(
      "UnknownError",
      1,
      in_message,
      in_location);
}

Error unknownError(const std::string& in_message, const Error& in_cause, const ErrorLocation& in_location)
{
   return Error(
      "UnknownError",
      1,
      in_message,
      in_cause,
      in_location);
}

// return an error description (either the description property or a message)
std::string errorDescription(const Error& error)
{
   std::string description = error.getProperty("description");
   if (description.empty())
      description = errorMessage(error);
   return description;
}

// return a printable error message from an error (depending on the error this
// might require consulting the message, category, or name)
std::string errorMessage(const core::Error& error)
{
   std::string msg = error.getMessage();
   if (msg.length() == 0)
   {
      msg = error.getProperty("category");
   }
   if (msg.length() == 0)
   {
      msg = error.getName();
   }
   return msg;
}


} // namespace core 
} // namespace rstudio
