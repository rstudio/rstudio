/*
 * RGraphicsPlotManipulator.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef R_SESSION_GRAPHICS_PLOT_MANIPULATOR_HPP
#define R_SESSION_GRAPHICS_PLOT_MANIPULATOR_HPP

#include <boost/utility.hpp>

#include <core/json/Json.hpp>

#include <r/RSexp.hpp>

namespace rscore {
   class Error;
   class FilePath;
}

namespace r {
namespace session {
namespace graphics {

class PlotManipulator : boost::noncopyable
{
public:
   PlotManipulator();
   explicit PlotManipulator(SEXP sexp);
   virtual ~PlotManipulator();

   bool empty() const { return !sexp_; }

   void clear();

   rscore::Error save(const rscore::FilePath& filePath) const;
   rscore::Error load(const rscore::FilePath& filePath);

   void asJson(rscore::json::Value* pValue) const;

   SEXP sexp() const;

private:
   SEXP get(const std::string& name) const;
   rscore::json::Value getAsJson(const std::string& name) const;
   rscore::json::Object getControlAsJson(SEXP controlSEXP) const;
   rscore::json::Object getControlsAsJson() const;
   SEXP getUserVisibleValuesList() const;

private:
   r::sexp::PreservedSEXP sexp_;
};

} // namespace graphics
} // namespace session
} // namespace r


#endif // R_SESSION_GRAPHICS_PLOT_MANIPULATOR_HPP

