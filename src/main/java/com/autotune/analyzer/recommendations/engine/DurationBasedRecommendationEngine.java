package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.recommendations.Recommendation;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.algos.DurationBasedRecommendationSubCategory;
import com.autotune.analyzer.recommendations.algos.RecommendationSubCategory;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.StartEndTimeStampResults;
import com.autotune.common.k8sObjects.ContainerObject;
import com.autotune.common.utils.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DurationBasedRecommendationEngine implements KruizeRecommendationEngine{
    private static final Logger LOGGER = LoggerFactory.getLogger(DurationBasedRecommendationEngine.class);
    private String name;
    private String key;
    private AnalyzerConstants.RecommendationCategory category;

    public DurationBasedRecommendationEngine() {
        this.name = AnalyzerConstants.RecommendationEngine.EngineNames.DURATION_BASED;
        this.key = AnalyzerConstants.RecommendationEngine.EngineKeys.DURATION_BASED_KEY;
    }

    public DurationBasedRecommendationEngine(String name) {
        this.name = name;
    }

    @Override
    public String getEngineName() {
        return this.name;
    }

    @Override
    public String getEngineKey() {
        return this.key;
    }

    @Override
    public AnalyzerConstants.RecommendationCategory getEngineCategory() {
        return this.category;
    }

    @Override
    public HashMap<String, Recommendation> getRecommendations(ContainerObject containerObject, Timestamp monitoringEndTime) {
        // TODO: Needs to be implemented
        AnalyzerConstants.RecommendationCategory recommendationCategory = AnalyzerConstants.RecommendationCategory.DURATION_BASED;
        HashMap<Timestamp, StartEndTimeStampResults> resultsMap = containerObject.getResults();
        Timestamp minDate = resultsMap.keySet().stream().min(Timestamp::compareTo).get();
        HashMap<String, Recommendation> resultRecommendation = new HashMap<String, Recommendation>();
        for (RecommendationSubCategory recommendationSubCategory : recommendationCategory.getRecommendationSubCategories()) {
            DurationBasedRecommendationSubCategory durationBasedRecommendationSubCategory = (DurationBasedRecommendationSubCategory) recommendationSubCategory;
            String recPeriod = durationBasedRecommendationSubCategory.getSubCategory();
            int days = durationBasedRecommendationSubCategory.getDuration();
            Timestamp monitorStartDate = CommonUtils.addDays(monitoringEndTime, -1 * days);
            if (monitorStartDate.compareTo(minDate) >= 0 || days == 1) {
                Timestamp finalMonitorStartDate = monitorStartDate;
                Map<Timestamp, StartEndTimeStampResults> filteredResultsMap = containerObject.getResults().entrySet().stream()
                        .filter((x -> ((x.getKey().compareTo(finalMonitorStartDate) >= 0)
                                && (x.getKey().compareTo(monitoringEndTime) <= 0))))
                        .collect((Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                Recommendation recommendation = new Recommendation(monitorStartDate, monitoringEndTime);
                HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config = new HashMap<>();
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestsMap = new HashMap<>();
                requestsMap.put(AnalyzerConstants.RecommendationItem.cpu, getCPURequestRecommendation(filteredResultsMap));
                requestsMap.put(AnalyzerConstants.RecommendationItem.memory, getMemoryRequestRecommendation(filteredResultsMap));
                config.put(AnalyzerConstants.ResourceSetting.requests, requestsMap);
                HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsMap = new HashMap<>();
                limitsMap.put(AnalyzerConstants.RecommendationItem.cpu, getCPULimitRecommendation(filteredResultsMap));
                limitsMap.put(AnalyzerConstants.RecommendationItem.memory, getMemoryLimitRecommendation(filteredResultsMap));
                config.put(AnalyzerConstants.ResourceSetting.limits, limitsMap);
                Double hours = filteredResultsMap.values().stream().map((x) -> (x.getDurationInMinutes()))
                        .collect(Collectors.toList())
                        .stream()
                        .mapToDouble(f -> f.doubleValue()).sum() / 60;
                recommendation.setDuration_in_hours(hours);
                recommendation.setConfig(config);
                resultRecommendation.put(recPeriod, recommendation);
            } else {
                RecommendationNotification notification = new RecommendationNotification(
                        AnalyzerConstants.RecommendationNotificationTypes.INFO.getName(),
                        AnalyzerConstants.RecommendationNotificationMsgConstant.NOT_ENOUGH_DATA);
                resultRecommendation.put(recPeriod, new Recommendation(notification));
            }
        }
        return resultRecommendation;
    }

    private static RecommendationConfigItem getCPURequestRecommendation(Map<Timestamp, StartEndTimeStampResults> filteredResultsMap) {
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        try {
            List<Double> doubleList = filteredResultsMap.values()
                    .stream()
                    .map(e -> e.getMetrics().get(AnalyzerConstants.AggregatorType.cpuUsage).getSum() + e.getMetrics().get(AnalyzerConstants.AggregatorType.cpuThrottle).getSum())
                    .collect(Collectors.toList());

            for (StartEndTimeStampResults startEndTimeStampResults: filteredResultsMap.values()) {
                format = startEndTimeStampResults.getMetrics().get(AnalyzerConstants.AggregatorType.cpuUsage).getFormat();
                if (null != format && !format.isEmpty())
                    break;
            }
            recommendationConfigItem = new RecommendationConfigItem(CommonUtils.percentile(0.9, doubleList), format);

        } catch (Exception e) {
            LOGGER.error("Not able to get getCPUCapacityRecommendation: " + e.getMessage());
            recommendationConfigItem = new RecommendationConfigItem(e.getMessage());
        }
        return recommendationConfigItem;
    }

    private static RecommendationConfigItem getCPULimitRecommendation(Map<Timestamp, StartEndTimeStampResults> filteredResultsMap) {
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        try {
            Double max_cpu = filteredResultsMap.values()
                    .stream()
                    .map(e -> e.getMetrics().get(AnalyzerConstants.AggregatorType.cpuUsage).getMax() + e.getMetrics().get(AnalyzerConstants.AggregatorType.cpuThrottle).getMax())
                    .max(Double::compareTo).get();
            Double max_pods = filteredResultsMap.values()
                    .stream()
                    .map(e -> e.getMetrics().get(AnalyzerConstants.AggregatorType.cpuUsage).getSum() / e.getMetrics().get(AnalyzerConstants.AggregatorType.cpuUsage).getAvg())
                    .max(Double::compareTo).get();
            for (StartEndTimeStampResults startEndTimeStampResults: filteredResultsMap.values()) {
                format = startEndTimeStampResults.getMetrics().get(AnalyzerConstants.AggregatorType.cpuUsage).getFormat();
                if (null != format && !format.isEmpty())
                    break;
            }
            recommendationConfigItem = new RecommendationConfigItem(max_cpu * max_pods, format);
            LOGGER.debug("Max_cpu : {} , max_pods : {}", max_cpu, max_pods);
        } catch (Exception e) {
            LOGGER.error("Not able to get getCPUMaxRecommendation: " + e.getMessage());
            recommendationConfigItem = new RecommendationConfigItem(e.getMessage());
        }
        return recommendationConfigItem;
    }

    private static RecommendationConfigItem getMemoryRequestRecommendation(Map<Timestamp, StartEndTimeStampResults> filteredResultsMap) {
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        try {
            List<Double> doubleList = filteredResultsMap.values()
                    .stream()
                    .map(e -> e.getMetrics().get(AnalyzerConstants.AggregatorType.memoryRSS).getSum())
                    .collect(Collectors.toList());
            for (StartEndTimeStampResults startEndTimeStampResults: filteredResultsMap.values()) {
                format = startEndTimeStampResults.getMetrics().get(AnalyzerConstants.AggregatorType.memoryRSS).getFormat();
                if (null != format && !format.isEmpty())
                    break;
            }
            recommendationConfigItem = new RecommendationConfigItem(CommonUtils.percentile(0.9, doubleList), format);
        } catch (Exception e) {
            LOGGER.error("Not able to get getMemoryCapacityRecommendation: " + e.getMessage());
            recommendationConfigItem = new RecommendationConfigItem(e.getMessage());
        }
        return recommendationConfigItem;
    }

    private static RecommendationConfigItem getMemoryLimitRecommendation(Map<Timestamp, StartEndTimeStampResults> filteredResultsMap) {
        RecommendationConfigItem recommendationConfigItem = null;
        String format = "";
        try {
            Double max_mem = filteredResultsMap.values()
                    .stream()
                    .map(e -> e.getMetrics().get(AnalyzerConstants.AggregatorType.memoryUsage).getMax())
                    .max(Double::compareTo).get();
            Double max_pods = filteredResultsMap.values()
                    .stream()
                    .map(e -> e.getMetrics().get(AnalyzerConstants.AggregatorType.memoryUsage).getSum() / e.getMetrics().get(AnalyzerConstants.AggregatorType.memoryUsage).getAvg())
                    .max(Double::compareTo).get();
            for (StartEndTimeStampResults startEndTimeStampResults: filteredResultsMap.values()) {
                format = startEndTimeStampResults.getMetrics().get(AnalyzerConstants.AggregatorType.memoryUsage).getFormat();
                if (null != format && !format.isEmpty())
                    break;
            }
            recommendationConfigItem = new RecommendationConfigItem(max_mem * max_pods, format);
            LOGGER.debug("Max_cpu : {} , max_pods : {}", max_mem, max_pods);
        } catch (Exception e) {
            LOGGER.error("Not able to get getCPUMaxRecommendation: " + e.getMessage());
            recommendationConfigItem = new RecommendationConfigItem(e.getMessage());
        }
        return recommendationConfigItem;
    }
}
