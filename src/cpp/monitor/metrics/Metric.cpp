/*
 * Metric.cpp
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

#include <monitor/metrics/Metric.hpp>

#include <ostream>

#include <shared_core/Error.hpp>
#include <core/DateTime.hpp>

#include <core/json/JsonRpc.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace monitor {
namespace metrics {

namespace {

json::Object metricBaseToJson(const MetricBase& metric)
{
   json::Object metricJson;
   metricJson["scope"] = metric.scope();
   metricJson["type"] = metric.type();
   metricJson["unit"] = metric.unit();
   metricJson["ts"] = date_time::secondsSinceEpoch(metric.timestamp());
   return metricJson;
}

Error metricBaseFromJson(const json::Object& metricJson,
                         std::string* pScope,
                         std::string* pType,
                         std::string* pUnit,
                         double* pTimestamp)
{
   return json::readObject(metricJson,
                          "scope", *pScope,
                          "type", *pType,
                          "unit", *pUnit,
                          "ts", *pTimestamp);
}

json::Value toMetricDataJson(const MetricData& data)
{
   json::Object dataJson;
   dataJson["name"] = data.name;
   dataJson["value"] = data.value;
   return std::move(dataJson);
}


} // anonymous namespace

json::Object metricToJson(const Metric& metric)
{
   json::Object metricJson = metricBaseToJson(metric);
   metricJson["name"] = metric.data().name;
   metricJson["value"] = metric.data().value;
   return metricJson;
}

Error metricFromJson(const json::Object& metricJson, Metric* pMetric)
{
   // read the fields
   std::string scope, name, type, unit;
   double value, ts;
   Error error = metricBaseFromJson(metricJson,
                                    &scope,
                                    &type,
                                    &unit,
                                    &ts);
   if (error)
      return error;

   error = json::readObject(metricJson,
                            "name", name,
                            "value", value);
   if (error)
      return error;

   *pMetric = Metric(scope,
                     MetricData(name, value),
                     type,
                     unit,
                     date_time::timeFromSecondsSinceEpoch(ts));

   return Success();
}

json::Object metricToJson(const MultiMetric& multiMetric)
{
   json::Object multiMetricJson = metricBaseToJson(multiMetric);

   json::Array dataJson;
   std::transform(multiMetric.data().begin(),
                  multiMetric.data().end(),
                  std::back_inserter(dataJson),
                  toMetricDataJson);
   multiMetricJson["data"] = dataJson;

   return multiMetricJson;
}

Error metricFromJson(const json::Object& multiMetricJson,
                     MultiMetric* pMultiMetric)
{
   // read the fields
   std::string scope, type, unit;
   double ts;
   Error error = metricBaseFromJson(multiMetricJson,
                                    &scope,
                                    &type,
                                    &unit,
                                    &ts);
   if (error)
      return error;

   // read the data array
   json::Array dataJson;
   error = json::readObject(multiMetricJson, "data", dataJson);
   if (error)
      return error;

   // create vector of metric data
   std::vector<MetricData> data;
   for (const json::Value& value : dataJson)
   {
      if (!json::isType<json::Object>(value))
         return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);

      MetricData dataItem;
      const json::Object& valueObj = value.getObject();
      Error error = json::readObject(valueObj,
                                     "name", dataItem.name,
                                     "value", dataItem.value);
      if (error)
         return error;

      data.push_back(dataItem);
   }

   *pMultiMetric = MultiMetric(scope,
                               data,
                               type,
                               unit,
                               date_time::timeFromSecondsSinceEpoch(ts));

   return Success();
}


} // namespace metrics
} // namespace monitor
} // namespace rstudio

