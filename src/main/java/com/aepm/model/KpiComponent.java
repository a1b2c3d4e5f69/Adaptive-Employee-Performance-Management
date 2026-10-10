package com.aepm.model;

public class KpiComponent {

    private Long componentId;
    private Long configId;
    private String componentName;
    private double weighting;
    private String componentStatus;


    public KpiComponent() {
    }


    public Long getComponentId() {
        return componentId;
    }

    public void setComponentId(Long componentId) {
        this.componentId = componentId;
    }


    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }


    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }


    public double getWeighting() {
        return weighting;
    }

    public void setWeighting(double weighting) {
        this.weighting = weighting;
    }


    public String getComponentStatus() {
        return componentStatus;
    }

    public void setComponentStatus(String componentStatus) {
        this.componentStatus = componentStatus;
    }


    @Override
    public String toString() {
        return componentName;
    }
}