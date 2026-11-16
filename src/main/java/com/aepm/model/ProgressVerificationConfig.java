package com.aepm.model;


public class ProgressVerificationConfig {


    private long configId;

    private int reviewTimeoutHours;

    private boolean autoApprovalEnabled;

    private double minimumConfidence;

    private double maxAutoProgressIncrease;

    private boolean allowAutoApproveCompletion;


    public long getConfigId() {
        return configId;
    }


    public void setConfigId(
            long configId
    ) {

        this.configId =
                configId;
    }


    public int getReviewTimeoutHours() {
        return reviewTimeoutHours;
    }


    public void setReviewTimeoutHours(
            int reviewTimeoutHours
    ) {

        this.reviewTimeoutHours =
                reviewTimeoutHours;
    }


    public boolean isAutoApprovalEnabled() {
        return autoApprovalEnabled;
    }


    public void setAutoApprovalEnabled(
            boolean autoApprovalEnabled
    ) {

        this.autoApprovalEnabled =
                autoApprovalEnabled;
    }


    public double getMinimumConfidence() {
        return minimumConfidence;
    }


    public void setMinimumConfidence(
            double minimumConfidence
    ) {

        this.minimumConfidence =
                minimumConfidence;
    }


    public double getMaxAutoProgressIncrease() {
        return maxAutoProgressIncrease;
    }


    public void setMaxAutoProgressIncrease(
            double maxAutoProgressIncrease
    ) {

        this.maxAutoProgressIncrease =
                maxAutoProgressIncrease;
    }


    public boolean isAllowAutoApproveCompletion() {
        return allowAutoApproveCompletion;
    }


    public void setAllowAutoApproveCompletion(
            boolean allowAutoApproveCompletion
    ) {

        this.allowAutoApproveCompletion =
                allowAutoApproveCompletion;
    }
}