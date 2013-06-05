/*
 * Metric.cpp
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

#include <monitor/metrics/Metric.hpp>

#include <ostream>

#include <core/Error.hpp>
#include <core/DateTime.hpp>

#include <core/json/JsonRpc.hpp>

using namespace core;

namespace monitor {
namespace metrics {
  
namespace {


} // anonymous namespace
   

std::string metricToJson(const Metric& metric)
{
   json::Object metricJson;
   metricJson["scope"] = metric.scope();
   metricJson["name"] = metric.name();
   metricJson["value"] = metric.value();
   metricJson["type"] = metric.type();
   metricJson["unit"] = metric.unit();
   metricJson["ts"] = date_time::millisecondsSinceEpoch(metric.timestamp());
   std::ostringstream ostr;
   json::write(metricJson, ostr);
   return ostr.str();
}

Error metricFromJson(const std::string& metricJson, Metric* pMetric)
{
   // parse the json into an object
   json::Value jsonValue;
   if (!json::parse(metricJson, &jsonValue) ||
       !json::isType<json::Object>(jsonValue))
   {
      Error error = systemError(boost::system::errc::protocol_error,
                                "Error parsing metric json: " + metricJson,
                                 ERROR_LOCATION);
      return error;
   }
   json::Object jsonObject = jsonValue.get_obj();

   // read the fields
   std::string scope, name, unit;
   double value, ts;
   int type;
   Error error = json::readObject(jsonObject,
                                  "scope", &scope,
                                  "name", &name,
                                  "value", &value,
                                  "type", &type,
                                  "unit", &unit,
                                  "ts", &ts);
   if (error)
      return error;

   *pMetric = Metric(scope,
                     name,
                     value,
                     (MetricType)type,
                     unit,
                     date_time::timeFromMillisecondsSinceEpoch(ts));

   return Success();
}


} // namespace metrics
} // namespace monitor

