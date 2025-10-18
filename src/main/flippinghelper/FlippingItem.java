package flippinghelper;

import lombok.Data;

import java.util.List;

@Data
public class FlippingItem {
    private String id;
    private String name;
    private String detailIcon;
    private int quantity;
    private long dailyVolume;
    private long adjustedLowPrice;
    private long adjustedHighPrice;
    private long profit;
    private double score;
    private long medianHourlyVolume;
    private boolean members;
    private List<Integer> sparklineData;
    private String predictedAction;

    // Dump detection fields
    private Double dumpSignalScore;  // API returns decimal values like 0.443, 0.291
    private List<String> dumpSignalReasons;
    private Long dumpPeakPrice;
    private Long dumpDetectedAt;
}