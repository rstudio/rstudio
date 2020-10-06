/*
 * NotebookCapture.cpp
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

#include "SessionRmdNotebook.hpp"
#include "NotebookCapture.hpp"

#include <boost/make_shared.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

using namespace rstudio::core;

NotebookCapture::NotebookCapture():
   connected_(false)
{}

NotebookCapture::~NotebookCapture()
{
   if (connected_)
      disconnect();
}

void NotebookCapture::connect()
{
   connected_ = true;
}

void NotebookCapture::disconnect()
{
   connected_ = false;
}

bool NotebookCapture::connected()
{
   return connected_;
}

void NotebookCapture::onExprComplete()
{
   // stub implementation
}

bool NotebookCapture::onCondition(Condition condition, 
                                  const std::string& message)
{
   if (capturingConditions())
   {
      json::Array cond;
      cond.push_back(static_cast<int>(condition));
      cond.push_back(message);
      conditions_->push_back(cond);
      return true;
   }

   // default is to ignore condition
   return false;
}

void NotebookCapture::beginConditionCapture()
{
   // skip if already capturing conditions
   if (conditions_)
      return;
   conditions_ = boost::make_shared<core::json::Array>();
}

json::Value NotebookCapture::endConditionCapture()
{
   if (conditions_)
   {
      json::Value conditions = *conditions_;
      conditions_.reset();
      return conditions;
   }
   
   // return null by default;
   return json::Value();
}

bool NotebookCapture::capturingConditions()
{
   return !!conditions_;
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

