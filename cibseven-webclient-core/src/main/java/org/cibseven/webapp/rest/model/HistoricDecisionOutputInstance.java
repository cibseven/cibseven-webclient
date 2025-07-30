package org.cibseven.webapp.rest.model;

import java.io.Serializable;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricDecisionOutputInstance implements Serializable {
    protected String clauseId;
    protected String clauseName;
    protected String decisionInstanceId;
    protected String errorMessage;
    protected String id;
    protected String ruleId;
    protected Integer ruleOrder;
    protected String variableName;
}
