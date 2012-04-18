/*
 * RErrorCategory.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <r/RErrorCategory.hpp>

namespace r {

class RErrorCategory : public boost::system::error_category
{
public:
   virtual const char * name() const;
   virtual std::string message( int ev ) const;
};

const boost::system::error_category& rCategory()
{
   static RErrorCategory rErrorCategoryConst ;
	return rErrorCategoryConst ;
}

const char * RErrorCategory::name() const
{
	return "r" ;
}

std::string RErrorCategory::message( int ev ) const
{
	std::string message ;
	switch (ev)
	{         
      case errc::RHomeNotFound:
         message = "Unable to find R home directory";
         break;

      case errc::UnsupportedLocale:
         message = "Unsupported locale (UTF-8 required)";
         break;
         
      case errc::ExpressionParsingError:
         message = "Expression parsing error";
         break;
         
      case errc::CodeExecutionError:
         message = "R code execution error";
         break;
         
      case errc::SymbolNotFoundError:
         message = "R symbol not found";
         break;

      case errc::ListElementNotFoundError:
         message = "List element not found";
         break;
         
      case errc::UnexpectedDataTypeError:
         message = "Unexpected data type";
         break;
         
      case errc::NoDataAvailableError:
         message = "No data available from R";
         break;
         
		default:
			message = "Unknown error" ;
			break;
	}

	return message ;
}

core::Error rCodeExecutionError(const std::string& errMsg, 
                                const core::ErrorLocation& location)
{
   core::Error error(errc::CodeExecutionError, location);
   error.addProperty("errormsg", errMsg);
   return error;
}
   
   
bool isCodeExecutionError(const core::Error& error, std::string* pErrMsg)
{
   if (error.code() == r::errc::CodeExecutionError)
   {
      if (pErrMsg != NULL)
         *pErrMsg = error.getProperty("errormsg");
      return true;
   }
   else
   {
      return false;
   }
}
   
std::string endUserErrorMessage(const core::Error& error)
{
   std::string errMsg;
   if (isCodeExecutionError(error, &errMsg))
      return errMsg;
   else
      return error.code().message();
}

   
   
} // namespace r
