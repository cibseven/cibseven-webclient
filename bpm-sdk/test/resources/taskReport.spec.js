// Adapted from legacy taskReportSpec.js for ES modules and Vitest
import { describe, it, expect } from 'vitest';
import TaskReport from '../../api-client/resources/task-report.js';

describe('The Task Report resource usage', () => {
  it('does not blow when loading', () => {
    expect(() => {
      void TaskReport;
    }).not.toThrow();
  });
  it('has a `path` static property', () => {
    expect(TaskReport.path).toBe('task/report');
  });
  it('has a `countByCandidateGroup` method', () => {
    expect(typeof TaskReport.countByCandidateGroup).toBe('function');
  });
  it('has a `countByCandidateGroupAsCsv` method', () => {
    expect(typeof TaskReport.countByCandidateGroupAsCsv).toBe('function');
  });
  // Skipping resolve and complete methods as in legacy (marked xit)
  // Skipping instance construction (xdescribe in legacy)
});
