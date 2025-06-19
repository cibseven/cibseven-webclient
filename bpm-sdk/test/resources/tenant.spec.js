// Adapted from legacy tenantSpec.js for ES modules and Vitest
import { describe, it, expect } from 'vitest';
import Tenant from '../../api-client/resources/tenant.js';

describe('The Tenant resource usage', () => {
  it('does not blow when loading', () => {
    expect(() => {
      void Tenant;
    }).not.toThrow();
  });
  it('has a `path` static property', () => {
    expect(Tenant.path).toBe('tenant');
  });
  it('has a `create` method', () => {
    expect(typeof Tenant.create).toBe('function');
  });
  it('has a `count` method', () => {
    expect(typeof Tenant.count).toBe('function');
  });
  it('has a `get` method', () => {
    expect(typeof Tenant.get).toBe('function');
  });
  it('has a `list` method', () => {
    expect(typeof Tenant.list).toBe('function');
  });
  it('has a `createUserMember` method', () => {
    expect(typeof Tenant.createUserMember).toBe('function');
  });
  it('has a `createGroupMember` method', () => {
    expect(typeof Tenant.createGroupMember).toBe('function');
  });
  it('has a `deleteUserMember` method', () => {
    expect(typeof Tenant.deleteUserMember).toBe('function');
  });
});
