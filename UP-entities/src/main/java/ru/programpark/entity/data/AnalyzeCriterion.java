package ru.programpark.entity.data;

import java.util.ArrayList;
import java.util.List;

public class AnalyzeCriterion {
    private Long id;
    private Double value;

    List<String> infoString = new ArrayList<>();
    List<AnalyzerInfoEntry> info = new ArrayList<>();

    public void addInfo(AnalyzerInfoEntry infoEntry) {
        info.add(infoEntry);
    }


    public void addInfoString(String stringEntry) {
        infoString.add(stringEntry);
    }

    public void addInfoString(Long longEntry) {
        infoString.add(Long.toString(longEntry));
    }

    public List<String> getInfoString() {
        return infoString;
    }

    public List<AnalyzerInfoEntry> getInfo() {
        return info;
    }

    public AnalyzeCriterion() {}

    public AnalyzeCriterion(Long id, Double value) {
        this.id = id;
        this.value = value;
    }

    // added 20/08/2015 by A.Takmazian
    public AnalyzeCriterion(Long id) {
        this.id = id;
    }

    public AnalyzeCriterion(AnalyzeCriterion ac){
        this.id = ac.id;
        this.value = ac.value;
        this.infoString = ac.infoString;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

}
