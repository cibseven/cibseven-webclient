package org.cibseven.webapp.rest.model;

import lombok.Data;

@Data
public class Metric {
    private String metric;
    private int sum;
    private int subscriptionYear;
    private int subscriptionMonth;
}
