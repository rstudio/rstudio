/*
 * Metric.hpp
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

#ifndef MONITOR_METRIC_METRIC_HPP
#define MONITOR_METRIC_METRIC_HPP

#include <string>
#include <vector>

#include <boost/function.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/json/Json.hpp>

namespace core {
   class Error;
}

namespace monitor {
namespace metrics {

// convenience base class for Metric and MultiMetric
class MetricBase
{
protected:
   MetricBase() {}

   MetricBase(const std::string& scope,
              int intervalSeconds,
              const std::string& type,
              const std::string& unit,
              boost::posix_time::ptime timestamp)
      : scope_(scope),
        intervalSeconds_(intervalSeconds),
        type_(type),
        unit_(unit),
        timestamp_(timestamp)
   {
   }

public:
   // check for empty
   bool isEmpty() const { return scope_.empty(); }

   // property accessors
   const std::string& scope() const { return scope_; }
   int intervalSeconds() const { return intervalSeconds_; }
   const std::string& type() const { return type_; }
   const std::string& unit() const { return unit_; }
   const boost::posix_time::ptime& timestamp() const { return timestamp_; }

private:
   std::string scope_;
   int intervalSeconds_;
   std::string type_;
   std::string unit_;
   boost::posix_time::ptime timestamp_;
};

struct MetricData
{
   MetricData()
      : value(0)
   {
   }

   MetricData(const MetricData& data)
      : name(data.name), value(data.value)
   {
   }

   MetricData(const std::string& name, double value)
      : name(name), value(value)
   {
   }

   bool isEmpty() const { return name.empty(); }

   std::string name;
   double value;
};

class Metric : public MetricBase
{
public:
   Metric() : MetricBase() {}

   Metric(const std::string& scope,
          int intervalSeconds,
          const MetricData& data,
          const std::string& type = "gauge",
          const std::string& unit = std::string(),
          boost::posix_time::ptime timestamp =
                     boost::posix_time::microsec_clock::universal_time())
      : MetricBase(scope, intervalSeconds, type, unit, timestamp),
        data_(data)
   {
   }

public:
   const MetricData& data() const { return data_; }

private:
   MetricData data_;
};


class MultiMetric : public MetricBase
{
public:
   MultiMetric() : MetricBase() {}

   MultiMetric(const std::string& scope,
               int intervalSeconds,
               const std::vector<MetricData>& data,
               const std::string& type = "gauge",
               const std::string& unit = std::string(),
               boost::posix_time::ptime timestamp =
                           boost::posix_time::microsec_clock::universal_time())
      : MetricBase(scope, intervalSeconds, type, unit, timestamp),
        data_(data)
   {
   }

public:
   const std::vector<MetricData>& data() const { return data_; }

private:
   std::vector<MetricData> data_;
};

// metric handlers
typedef boost::function<void(const Metric&)> MetricHandler;
typedef boost::function<void(const Metric&)> MultiMetricHandler;

// json serialization
core::json::Object metricToJson(const Metric& metric);
core::Error metricFromJson(const core::json::Object& metricJson,
                           Metric* pMetric);

core::json::Object metricToJson(const MultiMetric& multiMetric);
core::Error metricFromJson(const core::json::Object& multiMetricJson,
                           MultiMetric* pMultiMetric);


} // namespace metrics
} // namespace monitor

#endif // MONITOR_METRIC_METRIC_HPP

