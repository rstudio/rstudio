/*
 * Error.hpp
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

#ifndef SHARED_CORE_ERROR_HPP
#define SHARED_CORE_ERROR_HPP

#include <string>
#include <system_error>
#include <vector>

#include <boost/current_function.hpp>
#include <boost/system/error_code.hpp>

#include "Logger.hpp"
#include "PImpl.hpp"

namespace rstudio {
namespace core {

class FilePath;
class Error;
class Success;

/**
 * @brief A class which can be derived from in order to prevent child classes from being derived from further.
 */
class ErrorLock
{
   friend class Error;
   friend class Success;

   /**
    * @brief Virtual Destructor.
    */
   virtual ~ErrorLock() = default;

private:
   /**
    * @brief Private constructor to prevent further derivation of Error and Success.
    */
   ErrorLock() = default;

   /**
    * @brief Private copy constructor to prevent further derivation of Error and Success.
    */
   ErrorLock(const ErrorLock&) = default;
};

/**
 * @brief Class which represents the location of an error.
 */
class ErrorLocation
{
public:
   /**
    * @brief Default constructor.
    */
   ErrorLocation();

   /**
    * @brief Copy constructor.
    *
    * @param in_other   The error location to move to this.
    */
   ErrorLocation(const ErrorLocation& in_other);

   /**
    * @brief Move constructor.
    *
    * @param in_other   The error location to move to this.
    */
   ErrorLocation(ErrorLocation&& in_other) noexcept;

   /**
    * @brief Constructor.
    *
    * @param in_function    The function in which the error occurred.
    * @param in_file        The file in which the error occurred.
    * @param in_line        The line at which the error occurred.
    */
   ErrorLocation(const char* in_function, const char* in_file, long in_line);

   /**
    * @brief Assignment operator.
    *
    * @param in_other    The location to copy to this location.
    *
    * @return A reference to this location.
    */
   ErrorLocation& operator=(const ErrorLocation& in_other);

   /**
    * @brief Equality comparison operator.
    *
    * @param in_location    The location to compare this location with.
    *
    * @return True if in_location is the same as this location; false otherwise.
    */
   bool operator==(const ErrorLocation& in_location) const;

   /**
    * @brief Formats the error location as a string.
    *
    * @return The error location formatted as a string.
    */
   std::string asString() const;

   /**
    * @brief Gets the file where the error occurred.
    *
    * @return The file where the error occurred.
    */
   const std::string& getFile() const;

   /**
    * @brief Gets the function where the error occurred.
    *
    * @return The function where the error occurred.
    */
   const std::string& getFunction() const;

   /**
    * @brief Gets the line where the error occurred.
    *
    * @return The line where the error occurred.
    */
   long getLine() const;

   /**
    * @brief Checks whether the location is set.
    *
    * @return True if a location has been set; false otherwise.
    */
   bool hasLocation() const;

private:
   // The private implementation of ErrorLocation.
   PRIVATE_IMPL(m_impl);
};

/**
 * @brief Convenience typedef for error properties.
 */
typedef std::vector<std::pair<std::string, std::string> > ErrorProperties;


/**
 * @brief Class which represents an error.
 *
 * This class should not be derived from since it is returned by value throughout the SDK. Instead, create helper
 * functions for each "subclass" of Error that would be desired.
 */
class Error : public virtual ErrorLock
{
public:
   /**
    * @brief Default constructor.
    */
   Error() = default;

   /**
    * @brief Copy constructor.
    *
    * @param in_other   The error to copy.
    */
   Error(const Error& in_other);

   /**
    * @brief Constructor.
    *
    * @param in_ec              The boost error code to convert from.
    * @param in_location        The location of the error.
    */
   Error(const boost::system::error_code& in_ec, const ErrorLocation& in_location);

   /**
    * @brief Constructor.
    *
    * @param in_ec              The boost error code to convert from.
    * @param in_cause           The error which caused this error.
    * @param in_location        The location of the error.
    */
   Error(const boost::system::error_code& in_ec, const Error& in_cause, const ErrorLocation& in_location);

   /**
    * @brief Constructor.
    *
    * @param in_ec              The boost error code to convert from.
    * @param in_message         The detailed error message. (e.g. "The JobNetworkRequest is not supported by this
    *                           plugin.")
    * @param in_location        The location of the error.
    */
   Error(const boost::system::error_code& in_ec, std::string in_message, const ErrorLocation& in_location);

   /**
    * @brief Constructor.
    *
    * @param in_ec              The boost error code to convert from.
    * @param in_message         The detailed error message. (e.g. "The JobNetworkRequest is not supported by this
    *                           plugin.")
    * @param in_cause           The error which caused this error.
    * @param in_location        The location of the error.
    */
   Error(const boost::system::error_code& in_ec,
         std::string in_message,
         const Error& in_cause,
         const ErrorLocation& in_location);

   /**
    * @brief Constructor.
    *
    * @param in_ec              The boost error condition to convert from.
    * @param in_location        The location of the error.
    */
   Error(const boost::system::error_condition& in_ec, const ErrorLocation& in_location);

   /**
    * @brief Constructor.
    *
    * @param in_ec              The boost error condition to convert from.
    * @param in_cause           The error which caused this error.
    * @param in_location        The location of the error.
    */
   Error(const boost::system::error_condition& in_ec, const Error& in_cause, const ErrorLocation& in_location);

   /**
    * @brief Constructor.
    *
    * @param in_ec              The boost error condition to convert from.
    * @param in_message         The detailed error message. (e.g. "The JobNetworkRequest is not supported by this
    *                           plugin.")
    * @param in_location        The location of the error.
    */
   Error(const boost::system::error_condition& in_ec, std::string in_message, const ErrorLocation& in_location);

   /**
    * @brief Constructor.
    *
    * @param in_ec              The boost error condition to convert from.
    * @param in_message         The detailed error message. (e.g. "The JobNetworkRequest is not supported by this
    *                           plugin.")
    * @param in_cause           The error which caused this error.
    * @param in_location        The location of the error.
    */
   Error(const boost::system::error_condition& in_ec,
         std::string in_message,
         const Error& in_cause,
         const ErrorLocation& in_location);

   /**
    * @brief Constructor.
    *
    * @param in_name            A contextual or categorical name for the error. (e.g. "RequestNotSupported")
    * @param in_code            The non-zero error code. Note that an error code of zero indicates success. (e.g. 1)
    * @param in_location        The location of the error.
    */
   Error(std::string in_name, int in_code, const ErrorLocation& in_location);

   /**
    * @brief Constructor.
    *
    * @param in_name            A contextual or categorical name for the error. (e.g. "RequestNotSupported")
    * @param in_code            The non-zero error code. Note that an error code of zero indicates success. (e.g. 1)
    * @param in_cause           The error which caused this error.
    * @param in_location        The location of the error.
    */
   Error(std::string in_name, int in_code, const Error& in_cause, const ErrorLocation& in_location);


   /**
    * @brief Constructor.
    *
    * @param in_name            A contextual or categorical name for the error. (e.g. "RequestNotSupported")
    * @param in_code            The non-zero error code. Note that an error code of zero indicates success. (e.g. 1)
    * @param in_message         The detailed error message. (e.g. "The JobNetworkRequest is not supported by this
    *                           plugin.")
    * @param in_location        The location of the error.
    */
   Error(std::string in_name, int in_code, std::string in_message, const ErrorLocation& in_location);

   /**
    * @brief Constructor.
    *
    * @param in_name            A contextual or categorical name for the error. (e.g. "RequestNotSupported")
    * @param in_code            The non-zero error code. Note that an error code of zero indicates success. (e.g. 1)
    * @param in_message         The detailed error message. (e.g. "The JobNetworkRequest is not supported by this
    *                           plugin.")
    * @param in_cause           The error which caused this error.
    * @param in_location        The location of the error.
    */
   Error(std::string in_name,
         int in_code,
         std::string in_message,
         const Error& in_cause,
         const ErrorLocation& in_location);

   /**
   * @brief Non-virtual destructor because only Success inherits Error and it will keep Error lightweight.
   */
   ~Error() override = default;

   /**
    * @brief Overloaded operator bool to allow Errors to be treated as boolean values.
    *
    * @return True if there is an error; false otherwise.
    */
   explicit operator bool() const;

   /**
    * @brief Overloaded operator ! to allow Errors to be treated as boolean values.
    *
    * @return True if there is not an error; false otherwise.
    */
   bool operator!() const;

   /**
    * @brief Equality operator. Two errors are equal if their codes and names are the same.
    *
    * @param in_other   The error to compare with this error.
    *
    * @return True if in_other is equal to this error; false otherwise.
    */
   bool operator==(const Error& in_other) const;

   /**
    * @brief Equality operator. Two errors are equal if their codes and names are the same.
    *
    * @param in_ec   The boost error code to compare with this error.
    *
    * @return True if in_ec has the same error code and category name as this error; false otherwise.
    */
   bool operator==(const boost::system::error_code& in_ec) const;

   /**
    * @brief Inequality operator. Two errors are equal if their codes and names are the same.
    *
    * @param in_other   The error to compare with this error.
    *
    * @return True if in_other is not equal to this error; false otherwise.
    */
   bool operator!=(const Error& in_other) const;

   /**
    * @brief Inequality operator. Two errors are equal if their codes and names are the same.
    *
    * @param in_ec   The boost error code to compare with this error.
    *
    * @return True if !(this == in_ec) would return true; false otherwise.
    */
   bool operator!=(const boost::system::error_code& in_ec) const;

   /**
    * @brief Add or updates a property of this error. If any properties with the specified name exist, they will all be
    *        updated.
    *
    * @param in_name        The name of the property to add or update.
    * @param in_value       The new value of the property.
    */
   void addOrUpdateProperty(const std::string& in_name, const std::string& in_value);

   /**
    * @brief Add or updates a property of this error. If any properties with the specified name exist, they will all be
    *        updated.
    *
    * @param in_name        The name of the property to add or update.
    * @param in_value       The new value of the property.
    */
   void addOrUpdateProperty(const std::string& in_name, const FilePath& in_value);

   /**
    * @brief Add or updates a property of this error. If any properties with the specified name exist, they will all be
    *        updated.
    *
    * @param in_name        The name of the property to add or update.
    * @param in_value       The new value of the property.
    */
   void addOrUpdateProperty(const std::string& in_name, int in_value);

   /**
    * @brief Adds a property of this error. If a property with the same name already exists, a duplicate will be added.
    *
    * @param in_name        The name of the property to add or update.
    * @param in_value       The new value of the property.
    */
   void addProperty(const std::string& in_name, const std::string& in_value);

   /**
    * @brief Adds a property of this error. If a property with the same name already exists, a duplicate will be added.
    *
    * @param in_name        The name of the property to add or update.
    * @param in_value       The new value of the property.
    */
   void addProperty(const std::string& in_name, const FilePath& in_value);

   /**
    * @brief Adds a property of this error. If a property with the same name already exists, a duplicate will be added.
    *
    * @param in_name        The name of the property to add or update.
    * @param in_value       The new value of the property.
    */
   void addProperty(const std::string& in_name, int in_value);

   /**
    * @brief Formats the error as a string.
    *
    * @return The error formatted as a string.
    */
   std::string asString() const;

   /**
    * @brief Gets the error which caused this error.
    *
    * @return The error which caused this error.
    */
   const Error& getCause() const;

   /**
    * @brief Gets the error code.
    *
    * @return The error code.
    */
   int getCode() const;

   /**
    * @brief Gets the location where the error occurred.
    *
    * @return The location where the error occurred.
    */
   const ErrorLocation& getLocation() const;

   /**
    * @brief Gets the error message.
    *
    * @return The error message.
    */
   const std::string& getMessage() const;

   /**
    * @brief Gets the name of the error.
    *
    * @return The name of the error.
    */
   const std::string& getName() const;

   /**
    * @brief Gets the custom properties of the error.
    *
    * @return The custom properties of this error.
    */
   const ErrorProperties& getProperties() const;

   /**
    * @brief Gets a custom property of this error.
    *
    * @param name   The name of the property to retrieve.
    *
    * @return The value of the specified property, if it exists; empty string otherwise.
    */
   std::string getProperty(const std::string& name) const;

   /**
    * @brief Gets the cause of the error.
    *
    * @return The cause of the error.
    */
   std::string getSummary() const;

   /**
    * @brief Gets whether this error was expected or not.
    *
    * @return True if this error was expected; false otherwise.
    */
   bool isExpected() const;

   /**
    * @brief Sets the property that indicates that this error was expected.
    *        Errors are unexpected by default; only unexpected errors will be logged.
    *        Expected errors can be marked as such to suppress logging of those errors.
    */
   void setExpected();

private:
   /**
    * @brief Helper method to copy the error object on write.
    */
   void copyOnWrite();

   /**
    * @brief Helper method that checks whether this error object represents an error or not (i.e. a non-zero error
    *        code).
    *
    * @return True if the error code is non-zero; false otherwise.
    */
   bool isError() const;

   // The private implementation of Error.
   PRIVATE_IMPL_SHARED(m_impl);

   /**
    * @brief Gets a reference to the private implementation member.
    *
    * @return A reference to the private implementation member.
    */
   Impl& impl() const;
};

/**
 * @brief Class which represents a successful operation (i.e. no error).
 */
class Success : public Error
{
public:
   /**
    * @brief Constructor.
    */
   Success() : Error() {}
};

/**
 * @brief Output stream operator. Writes the specified error to the provided output stream.
 *
 * @param io_ostream     The output stream to which to write the error.
 * @param in_error       The error to write to the output stream.
 *
 * @return A reference to the provided output stream, for chaining writes.
 */
std::ostream& operator<<(std::ostream& io_ostream, const Error& in_error);

#ifdef _WIN32

// Use this macro instead of systemError(::GetLastError(), ERROR_LOCATION) otherwise
// the ERROR_LOCATION macro may evaluate first and reset the Win32 error code to
// zero (no error), causing the wrong value to be passed to systemError. This is currently
// the case on debug builds using MSVC.
#define LAST_SYSTEM_ERROR() []() {auto lastErr = ::GetLastError(); return systemError(lastErr, ERROR_LOCATION);}()

#endif // _WIN32

/**
 * @brief Function which creates a system error.
 *
 * @param in_code            The error code. (e.g. 1)
 * @param in_location        The location of the error.
 *
 * @return A system error.
 */
Error systemError(int in_code, const ErrorLocation& in_location);

/**
 * @brief Function which creates a system error.
 *
 * @param in_code            The std system error code.
 * @param in_location        The location of the error.
 *
 * @return A system error.
 */
Error systemError(const std::error_code& in_code, const ErrorLocation& in_location);

/**
 * @brief Function which creates a system error.
 *
 * @param in_error           The system error which occurred.
 * @param in_location        The location of the error.
 *
 * @return A system error.
 */
Error systemError(const std::system_error& in_error, const ErrorLocation& in_location);

/**
 * @brief Function which creates a system error.
 *
 * @param in_code            The error code. (e.g. 1)
 * @param in_cause           The error which caused this error.
 * @param in_location        The location of the error.
 *
 * @return A system error.
 */
Error systemError(int in_code, const Error& in_cause, const ErrorLocation& in_location);

/**
 * @brief Function which creates a system error.
 *
 * @param in_code            The std system error code.
 * @param in_cause           The error which caused this error.
 * @param in_location        The location of the error.
 *
 * @return A system error.
 */
Error systemError(const std::error_code& in_code, const Error& in_cause, const ErrorLocation& in_location);

/**
 * @brief Function which creates a system error.
 *
 * @param in_error           The system error which occurred.
 * @param in_cause           The error which caused this error.
 * @param in_location        The location of the error.
 *
 * @return A system error.
 */
Error systemError(const std::system_error& in_error, const Error& in_cause, const ErrorLocation& in_location);

/**
 * @brief Function which creates a system error.
 *
 * @param in_code            The error code. (e.g. 1)
 * @param in_description     A detailed description of the error. (e.g. "Failed to open socket while attempting to
 *                           connect to Kubernetes.")
 * @param in_location        The location of the error.
 *
 * @return A system error.
 */
Error systemError(int in_code, const std::string& in_description, const ErrorLocation& in_location);

/**
 * @brief Function which creates a system error.
 *
 * @param in_code            The std system error code.
 * @param in_description     A detailed description of the error. (e.g. "Failed to open socket while attempting to
 *                           connect to Kubernetes.")
 * @param in_location        The location of the error.
 *
 * @return A system error.
 */
Error systemError(const std::error_code& in_code, const std::string& in_description, const ErrorLocation& in_location);

/**
 * @brief Function which creates a system error.
 *
 * @param in_error           The system error which occurred.
 * @param in_description     A detailed description of the error. (e.g. "Failed to open socket while attempting to
 *                           connect to Kubernetes.")
 * @param in_location        The location of the error.
 *
 * @return A system error.
 */
Error systemError(
   const std::system_error& in_error,
   const std::string& in_description,
   const ErrorLocation& in_location);

/**
 * @brief Function which creates a system error.
 *
 * @param in_code            The error code. (e.g. 1)
 * @param in_description     A detailed description of the error. (e.g. "Failed to open socket while attempting to
 *                           connect to Kubernetes.")
 * @param in_cause           The error which caused this error.
 * @param in_location        The location of the error.
 *
 * @return A system error.
 */
Error systemError(
   int in_code,
   const std::string& in_description,
   const Error& in_cause,
   const ErrorLocation& in_location);

/**
 * @brief Function which creates a system error.
 *
 * @param in_code            The std system error code.
 * @param in_description     A detailed description of the error. (e.g. "Failed to open socket while attempting to
 *                           connect to Kubernetes.")
 * @param in_cause           The error which caused this error.
 * @param in_location        The location of the error.
 *
 * @return A system error.
 */
Error systemError(
   const std::error_code& in_code,
   const std::string& in_description,
   const Error& in_cause,
   const ErrorLocation& in_location);

/**
 * @brief Function which creates a system error.
 *
 * @param in_error           The system error which occurred.
 * @param in_description     A detailed description of the error. (e.g. "Failed to open socket while attempting to
 *                           connect to Kubernetes.")
 * @param in_cause           The error which caused this error.
 * @param in_location        The location of the error.
 *
 * @return A system error.
 */
Error systemError(
   const std::system_error& in_error,
   const std::string& in_description,
   const Error& in_cause,
   const ErrorLocation& in_location);

/**
 * @brief Function which creates an unknown error. This should be used only when a specific error code cannot be
 *        determined.
 *
 * @param in_message         The detailed error message. (e.g. "Failed to open socket while attempting to connect to
 *                           Kubernetes.")
 * @param in_cause           The error which caused this error.
 * @param in_location        The location of the error.
 *
 * @return An unknown error.
 */
Error unknownError(const std::string& in_message, const ErrorLocation& in_location);

/**
 * @brief Function which creates an unknown error. This should be used only when a specific error code cannot be
 *        determined.
 *
 * @param in_message         The detailed error message. (e.g. "Failed to open socket while attempting to connect to
 *                           Kubernetes.")
 * @param in_cause           The error which caused this error.
 * @param in_location        The location of the error.
 *
 * @return An unknown error.
 */
Error unknownError(const std::string& in_message, const Error& in_cause, const ErrorLocation& in_location);


// return a printable error message from an error (depending on the error this
// might require consulting the message, category, or name)
std::string errorDescription(const Error& error);
std::string errorMessage(const core::Error& error);


} // namespace core
} // namespace rstudio

#ifdef STRIPPED_FILENAME
#define ERROR_LOCATION rstudio::core::ErrorLocation( \
      BOOST_CURRENT_FUNCTION, STRIPPED_FILENAME, __LINE__)
#else
#define ERROR_LOCATION rstudio::core::ErrorLocation( \
      BOOST_CURRENT_FUNCTION, __FILE__, __LINE__)
#endif

#define CATCH_UNEXPECTED_EXCEPTION                                                  \
   catch(const std::exception& e)                                                   \
   {                                                                                \
      rstudio::core::log::logErrorMessage(std::string("Unexpected exception: ") +   \
                        e.what(), ERROR_LOCATION);                                  \
   }                                                                                \
   catch(...)                                                                       \
   {                                                                                \
      rstudio::core::log::logErrorMessage("Unknown exception", ERROR_LOCATION);     \
   }

#endif
