package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;

/**
 * Test class for GenericRecommendationModel limit calculation methods
 */
public class GenericRecommendationModelLimitTest {
    
    private GenericRecommendationModel model;
    private ArrayList<RecommendationNotification> notifications;
    
    @BeforeEach
    void setUp() {
        // Create a test model with default tunables
        RecommendationTunables tunables = new RecommendationTunables(90.0, 90.0, 90.0);
        
        model = new GenericRecommendationModel("test-model", tunables);
        notifications = new ArrayList<>();
    }
    
    @Test
    void testGetCPULimitRecommendation_NormalCase() {
        // Arrange: Current Request = 1.0 cores, Current Limit = 2.0 cores, Recommended Request = 1.5 cores
        // Expected: New Limit = 1.5 × (2.0/1.0) = 3.0 cores
        RecommendationConfigItem recommendedRequest = new RecommendationConfigItem(1.5, "cores");
        RecommendationConfigItem currentRequest = new RecommendationConfigItem(1.0, "cores");
        RecommendationConfigItem currentLimit = new RecommendationConfigItem(2.0, "cores");
        
        // Act
        RecommendationConfigItem result = model.getCPULimitRecommendation(
            recommendedRequest, currentRequest, currentLimit, notifications);
        
        // Assert
        assertNotNull(result);
        assertEquals(3.0, result.getAmount(), 0.001);
        assertEquals("cores", result.getFormat());
        assertTrue(notifications.size() > 0);
        // Should have INFO_LIMIT_CALCULATED_USING_RATIO notification
        boolean foundRatioNotification = notifications.stream()
            .anyMatch(n -> n.getMessage().contains("ratio preservation"));
        assertTrue(foundRatioNotification);
    }
    
    @Test
    void testGetMemoryLimitRecommendation_NormalCase() {
        // Arrange: Current Request = 256MB, Current Limit = 512MB, Recommended Request = 384MB
        // Expected: New Limit = 384 × (512/256) = 768MB
        RecommendationConfigItem recommendedRequest = new RecommendationConfigItem(384.0, "MiB");
        RecommendationConfigItem currentRequest = new RecommendationConfigItem(256.0, "MiB");
        RecommendationConfigItem currentLimit = new RecommendationConfigItem(512.0, "MiB");
        
        // Act
        RecommendationConfigItem result = model.getMemoryLimitRecommendation(
            recommendedRequest, currentRequest, currentLimit, notifications);
        
        // Assert
        assertNotNull(result);
        assertEquals(768.0, result.getAmount(), 0.001);
        assertEquals("MiB", result.getFormat());
        assertTrue(notifications.size() > 0);
    }
    
    @Test
    void testGetCPULimitRecommendation_NoCurrentLimit() {
        // Arrange: No current limit available
        RecommendationConfigItem recommendedRequest = new RecommendationConfigItem(1.5, "cores");
        RecommendationConfigItem currentRequest = new RecommendationConfigItem(1.0, "cores");
        RecommendationConfigItem currentLimit = null; // No current limit
        
        // Act
        RecommendationConfigItem result = model.getCPULimitRecommendation(
            recommendedRequest, currentRequest, currentLimit, notifications);
        
        // Assert
        assertNotNull(result);
        assertEquals(1.5, result.getAmount(), 0.001); // Should fallback to recommended request
        assertEquals("cores", result.getFormat());
        assertTrue(notifications.size() > 0);
        // Should have INFO_NO_CURRENT_LIMIT_USING_REQUEST_AS_LIMIT notification
        boolean foundFallbackNotification = notifications.stream()
            .anyMatch(n -> n.getMessage().contains("No current limit"));
        assertTrue(foundFallbackNotification);
    }
    
    @Test
    void testGetMemoryLimitRecommendation_NoCurrentRequest() {
        // Arrange: No current request available
        RecommendationConfigItem recommendedRequest = new RecommendationConfigItem(1.5, "cores");
        RecommendationConfigItem currentRequest = null; // No current request
        RecommendationConfigItem currentLimit = new RecommendationConfigItem(2.0, "cores");
        
        // Act
        RecommendationConfigItem result = model.getMemoryLimitRecommendation(
            recommendedRequest, currentRequest, currentLimit, notifications);
        
        // Assert
        assertNotNull(result);
        assertEquals(1.5, result.getAmount(), 0.001); // Should fallback to recommended request
        assertEquals("cores", result.getFormat());
        assertTrue(notifications.size() > 0);
        // Should have INFO_NO_CURRENT_REQUEST_USING_RECOMMENDED_REQUEST_AS_LIMIT notification
        boolean foundFallbackNotification = notifications.stream()
            .anyMatch(n -> n.getMessage().contains("No current request"));
        assertTrue(foundFallbackNotification);
    }
    
    @Test
    void testGetCPULimitRecommendation_NoRecommendedRequest() {
        // Arrange: No recommended request available
        RecommendationConfigItem recommendedRequest = null;
        RecommendationConfigItem currentRequest = new RecommendationConfigItem(1.0, "cores");
        RecommendationConfigItem currentLimit = new RecommendationConfigItem(2.0, "cores");
        
        // Act
        RecommendationConfigItem result = model.getCPULimitRecommendation(
            recommendedRequest, currentRequest, currentLimit, notifications);
        
        // Assert
        assertNull(result); // Should return null when no recommended request
        assertTrue(notifications.size() > 0);
        // Should have ERROR_NO_RECOMMENDED_REQUEST_FOR_LIMIT_CALCULATION notification
        boolean foundErrorNotification = notifications.stream()
            .anyMatch(n -> n.getMessage().contains("No recommended request"));
        assertTrue(foundErrorNotification);
    }
    
    @Test
    void testGetCPULimitRecommendationForNamespace() {
        // Test namespace CPU limit calculation
        RecommendationConfigItem recommendedRequest = new RecommendationConfigItem(2.0, "cores");
        RecommendationConfigItem currentRequest = new RecommendationConfigItem(1.0, "cores");
        RecommendationConfigItem currentLimit = new RecommendationConfigItem(1.5, "cores");
        
        // Act
        RecommendationConfigItem result = model.getCPULimitRecommendationForNamespace(
            recommendedRequest, currentRequest, currentLimit, notifications);
        
        // Assert
        assertNotNull(result);
        assertEquals(3.0, result.getAmount(), 0.001); // 2.0 × (1.5/1.0) = 3.0
        assertEquals("cores", result.getFormat());
    }
    
    @Test
    void testGetMemoryLimitRecommendationForNamespace() {
        // Test namespace memory limit calculation
        RecommendationConfigItem recommendedRequest = new RecommendationConfigItem(500.0, "MiB");
        RecommendationConfigItem currentRequest = new RecommendationConfigItem(250.0, "MiB");
        RecommendationConfigItem currentLimit = new RecommendationConfigItem(1000.0, "MiB");
        
        // Act
        RecommendationConfigItem result = model.getMemoryLimitRecommendationForNamespace(
            recommendedRequest, currentRequest, currentLimit, notifications);
        
        // Assert
        assertNotNull(result);
        assertEquals(2000.0, result.getAmount(), 0.001); // 500.0 × (1000.0/250.0) = 2000.0
        assertEquals("MiB", result.getFormat());
    }
    
    @Test
    void testRatioCalculation_DifferentRatios() {
        // Test various ratios
        
        // Test 1: Ratio = 1 (limit equals request)
        RecommendationConfigItem result1 = model.getCPULimitRecommendation(
            new RecommendationConfigItem(2.0, "cores"),  // recommended
            new RecommendationConfigItem(1.0, "cores"),  // current request
            new RecommendationConfigItem(1.0, "cores"),  // current limit (ratio = 1.0)
            new ArrayList<>()
        );
        assertEquals(2.0, result1.getAmount(), 0.001); // 2.0 × 1.0 = 2.0
        
        // Test 2: Ratio = 0.5 (limit is half of request)
        RecommendationConfigItem result2 = model.getCPULimitRecommendation(
            new RecommendationConfigItem(2.0, "cores"),  // recommended
            new RecommendationConfigItem(2.0, "cores"),  // current request
            new RecommendationConfigItem(1.0, "cores"),  // current limit (ratio = 0.5)
            new ArrayList<>()
        );
        assertEquals(1.0, result2.getAmount(), 0.001); // 2.0 × 0.5 = 1.0
        
        // Test 3: Ratio = 4 (limit is 4x request)
        RecommendationConfigItem result3 = model.getCPULimitRecommendation(
            new RecommendationConfigItem(1.0, "cores"),  // recommended
            new RecommendationConfigItem(0.5, "cores"),  // current request
            new RecommendationConfigItem(2.0, "cores"),  // current limit (ratio = 4.0)
            new ArrayList<>()
        );
        assertEquals(4.0, result3.getAmount(), 0.001); // 1.0 × 4.0 = 4.0
    }
}