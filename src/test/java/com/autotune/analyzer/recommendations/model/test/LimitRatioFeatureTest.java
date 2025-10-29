package com.autotune.analyzer.recommendations.model.test;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.model.GenericRecommendationModel;
import com.autotune.analyzer.recommendations.model.RecommendationTunables;
import com.autotune.operator.KruizeDeploymentInfo;

import java.util.ArrayList;

/**
 * Test class to demonstrate the adjust_mem_usage feature
 */
public class LimitRatioFeatureTest {
    
    public static void main(String[] args) {
        // Create test data - simulates a scenario where:
        // Current CPU Request: 1.0 cores, Current CPU Limit: 2.0 cores (2:1 ratio)
        // Recommended CPU Request: 0.8 cores
        // Expected CPU Limit with ratio: 1.6 cores (0.8 * 2.0)
        
        RecommendationConfigItem currentCpuRequest = new RecommendationConfigItem(1.0, "cores");
        RecommendationConfigItem currentCpuLimit = new RecommendationConfigItem(2.0, "cores");
        RecommendationConfigItem recommendedCpuRequest = new RecommendationConfigItem(0.8, "cores");
        
        // Similar test data for memory: 1GiB request, 2GiB limit (2:1 ratio)
        // Recommended memory request: 800MiB
        // Expected memory limit with ratio: 1600MiB (800MiB * 2.0)
        RecommendationConfigItem currentMemRequest = new RecommendationConfigItem(1073741824.0, "bytes"); // 1GiB
        RecommendationConfigItem currentMemLimit = new RecommendationConfigItem(2147483648.0, "bytes"); // 2GiB
        RecommendationConfigItem recommendedMemRequest = new RecommendationConfigItem(838860800.0, "bytes"); // ~800MiB
        
        // Create RecommendationTunables with default percentiles
        RecommendationTunables tunables = new RecommendationTunables(0.9, 0.9, 0.9);
        GenericRecommendationModel model = new GenericRecommendationModel("TestModel", tunables);
        ArrayList<RecommendationNotification> notifications = new ArrayList<>();
        
        System.out.println("=== Testing Limit Ratio Feature ===\n");
        
        // Test 1: Default behavior (adjust_mem_usage = false)
        System.out.println("Test 1: Default behavior (adjust_mem_usage = false)");
        KruizeDeploymentInfo.adjust_mem_usage = false;
        
        System.out.println("Current CPU Request: " + currentCpuRequest.getAmount() + " " + currentCpuRequest.getFormat());
        System.out.println("Current CPU Limit: " + currentCpuLimit.getAmount() + " " + currentCpuLimit.getFormat());
        System.out.println("Recommended CPU Request: " + recommendedCpuRequest.getAmount() + " " + recommendedCpuRequest.getFormat());
        
        notifications.clear();
        RecommendationConfigItem cpuLimitDefault = model.getCPULimitRecommendation(
            recommendedCpuRequest, currentCpuRequest, currentCpuLimit, notifications);
            
        System.out.println("Recommended CPU Limit (default): " + 
            (cpuLimitDefault != null ? cpuLimitDefault.getAmount() + " " + cpuLimitDefault.getFormat() : "null"));
        System.out.println("Notifications: ");
        for (RecommendationNotification notification : notifications) {
            System.out.println("  - " + notification.getMessage());
        }
        System.out.println();
        
        // Test 2: Ratio-based calculation (adjust_mem_usage = true)
        System.out.println("Test 2: Ratio-based calculation (adjust_mem_usage = true)");
        KruizeDeploymentInfo.adjust_mem_usage = true;
        
        notifications.clear();
        RecommendationConfigItem cpuLimitRatio = model.getCPULimitRecommendation(
            recommendedCpuRequest, currentCpuRequest, currentCpuLimit, notifications);
            
        System.out.println("Recommended CPU Limit (ratio): " + 
            (cpuLimitRatio != null ? cpuLimitRatio.getAmount() + " " + cpuLimitRatio.getFormat() : "null"));
        System.out.println("Expected: " + (recommendedCpuRequest.getAmount() * (currentCpuLimit.getAmount() / currentCpuRequest.getAmount())));
        System.out.println("Notifications: ");
        for (RecommendationNotification notification : notifications) {
            System.out.println("  - " + notification.getMessage());
        }
        System.out.println();
        
        // Test 3: Memory with ratio calculation
        System.out.println("Test 3: Memory with ratio calculation (adjust_mem_usage = true)");
        
        System.out.println("Current Memory Request: " + currentMemRequest.getAmount() + " " + currentMemRequest.getFormat() + " (" + (currentMemRequest.getAmount() / (1024*1024*1024)) + " GiB)");
        System.out.println("Current Memory Limit: " + currentMemLimit.getAmount() + " " + currentMemLimit.getFormat() + " (" + (currentMemLimit.getAmount() / (1024*1024*1024)) + " GiB)");
        System.out.println("Recommended Memory Request: " + recommendedMemRequest.getAmount() + " " + recommendedMemRequest.getFormat() + " (" + (recommendedMemRequest.getAmount() / (1024*1024)) + " MiB)");
        
        notifications.clear();
        RecommendationConfigItem memLimitRatio = model.getMemoryLimitRecommendation(
            recommendedMemRequest, currentMemRequest, currentMemLimit, notifications);
            
        System.out.println("Recommended Memory Limit (ratio): " + 
            (memLimitRatio != null ? memLimitRatio.getAmount() + " " + memLimitRatio.getFormat() + " (" + (memLimitRatio.getAmount() / (1024*1024)) + " MiB)" : "null"));
        System.out.println("Expected: " + (recommendedMemRequest.getAmount() * (currentMemLimit.getAmount() / currentMemRequest.getAmount())) + " bytes");
        System.out.println("Notifications: ");
        for (RecommendationNotification notification : notifications) {
            System.out.println("  - " + notification.getMessage());
        }
        System.out.println();
        
        // Test 4: Memory with default behavior
        System.out.println("Test 4: Memory with default behavior (adjust_mem_usage = false)");
        KruizeDeploymentInfo.adjust_mem_usage = false;
        
        notifications.clear();
        RecommendationConfigItem memLimitDefault = model.getMemoryLimitRecommendation(
            recommendedMemRequest, currentMemRequest, currentMemLimit, notifications);
            
        System.out.println("Recommended Memory Limit (default): " + 
            (memLimitDefault != null ? memLimitDefault.getAmount() + " " + memLimitDefault.getFormat() + " (" + (memLimitDefault.getAmount() / (1024*1024)) + " MiB)" : "null"));
        System.out.println("Notifications: ");
        for (RecommendationNotification notification : notifications) {
            System.out.println("  - " + notification.getMessage());
        }
        
        System.out.println("\n=== Feature Test Complete ===");
    }
}