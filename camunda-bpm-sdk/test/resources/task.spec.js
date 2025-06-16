// Adapted from legacy taskSpec.js for ES modules and Vitest
import { describe, it, expect } from 'vitest';
import Task from '../../api-client/resources/task.js';

describe('The Task resource usage', () => {
  it('does not blow when loading', () => {
    expect(() => {
      void Task;
    }).not.toThrow();
  });
  it('has a `path` static property', () => {
    expect(Task.path).toBe('task');
  });
  it('has a `assignee` method', () => {
    expect(typeof Task.assignee).toBe('function');
  });
  it('has a `delegate` method', () => {
    expect(typeof Task.delegate).toBe('function');
  });
  it('has a `claim` method', () => {
    expect(typeof Task.claim).toBe('function');
  });
  it('has a `unclaim` method', () => {
    expect(typeof Task.unclaim).toBe('function');
  });
  it('has a `complete` method', () => {
    expect(typeof Task.complete).toBe('function');
  });
  // Skipping resolve method as in legacy (marked xit)
});
