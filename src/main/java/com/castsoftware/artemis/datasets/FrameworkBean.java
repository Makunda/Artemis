package com.castsoftware.artemis.datasets;

import com.opencsv.bean.CsvBindByPosition;

public class FrameworkBean {

    @CsvBindByPosition(position = 0)
    private String name;

    @CsvBindByPosition(position = 1)
    private String discoveryDate;

    @CsvBindByPosition(position = 2)
    private String location;

    @CsvBindByPosition(position = 3)
    private String description;

    @CsvBindByPosition(position = 4)
    private Integer numberOfDetection;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDiscoveryDate() {
        return discoveryDate;
    }

    public void setDiscoveryDate(String discoveryDate) {
        this.discoveryDate = discoveryDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getNumberOfDetection() {
        return numberOfDetection;
    }

    public void setNumberOfDetection(Integer numberOfDetection) {
        this.numberOfDetection = numberOfDetection;
    }

    public FrameworkBean(String name, String discoveryDate, String location, String description, Integer numberOfDetection) {
        this.name = name;
        this.discoveryDate = discoveryDate;
        this.location = location;
        this.description = description;
        this.numberOfDetection = numberOfDetection;
    }
}
