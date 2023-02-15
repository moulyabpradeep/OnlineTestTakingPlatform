package com.merittrac.apollo.rps.services.bean;
/*
 * Copyright © MeritTrac Services Pvt. Ltd.
 * All Rights Reserved.
 */

import java.io.Serializable;

/**
 * @author Ajit_K
 * @author $LastChangedBy: Ajit_K $
 * @version $Revision: 1595 $,
 *          $Date: 02-06-2015 11:12 AM#$
 */
public class ResponseMarkBean implements Serializable {
    private String response;
    private String responseAnswer;
    private Double responsePositiveMarks;
    private Double responseNegativeMarks;
    private boolean caseSensitive;

    public ResponseMarkBean() {
        super();
    }

    public ResponseMarkBean(String response, String responseAnswer, Double responsePositiveMarks,
                            Double responseNegativeMarks, boolean caseSensitive) {
        this.response = response;
        this.responseAnswer = responseAnswer;
        this.responsePositiveMarks = responsePositiveMarks;
        this.responseNegativeMarks = responseNegativeMarks;
        this.caseSensitive = caseSensitive;
    }

    public ResponseMarkBean(String response, String responseAnswer, Double responsePositiveMarks,
                            Double responseNegativeMarks) {
        this.response = response;
        this.responseAnswer = responseAnswer;
        this.responsePositiveMarks = responsePositiveMarks;
        this.responseNegativeMarks = responseNegativeMarks;
        this.caseSensitive = false;

    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getResponseAnswer() {
        return responseAnswer;
    }

    public void setResponseAnswer(String responseAnswer) {
        this.responseAnswer = responseAnswer;
    }

    public Double getResponsePositiveMarks() {
        return responsePositiveMarks;
    }

    public void setResponsePositiveMarks(Double responsePositiveMarks) {
        this.responsePositiveMarks = responsePositiveMarks;
    }

    public Double getResponseNegativeMarks() {
        return responseNegativeMarks;
    }

    public void setResponseNegativeMarks(Double responseNegativeMarks) {
        this.responseNegativeMarks = responseNegativeMarks;
    }

    @Override
    public String toString() {
        return "ResponseMarkBean{" +
                "response='" + response + '\'' +
                ", responseAnswer='" + responseAnswer + '\'' +
                ", responsePositiveMarks=" + responsePositiveMarks +
                ", responseNegativeMarks=" + responseNegativeMarks +
                ", isCaseSensitive=" + caseSensitive +
                '}';
    }
}
