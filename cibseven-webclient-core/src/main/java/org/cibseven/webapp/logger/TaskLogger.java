package org.cibseven.webapp.logger;

import org.apache.logging.log4j.ThreadContext;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor @NoArgsConstructor
public class TaskLogger {
  
  private static final String PROCESS_KEY_TC = "processKey";
  private static final String PROCESS_ID_TC = "processId";
  private static final String TASK_NAME_TC = "taskName";
  private static final String TASK_ID_TC = "taskId";
  
  private String processKey;
  private String processId;
  private String taskName;
  private String taskId;
  
  private void prepareLogging() {
    ThreadContext.put(PROCESS_KEY_TC, processKey.split(":")[0]); 
    ThreadContext.put(PROCESS_ID_TC, processId);
    ThreadContext.put(TASK_NAME_TC, taskName);
    ThreadContext.put(TASK_ID_TC, taskId);  
  }

  private void tearDownLogging() {  
    ThreadContext.remove(PROCESS_KEY_TC);
    ThreadContext.remove(PROCESS_ID_TC);
    ThreadContext.remove(TASK_NAME_TC);
    ThreadContext.remove(TASK_ID_TC);
  }
  
  public void info(String message) {
    prepareLogging();
    log.info(message);
    tearDownLogging();
  }
  
  public void info(String message, Object object) {
    prepareLogging();
    log.info(message, object);
    tearDownLogging();
  }
  
  public void infoStartEvent(String message) {
    //prepareLogging();
    log.info(message);
    //tearDownLogging();
  }
  
}
