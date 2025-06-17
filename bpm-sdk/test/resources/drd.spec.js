// Adapted from legacy drdSpec.js for ES modules and Vitest
import { describe, it, beforeEach, expect, vi } from 'vitest';
import DRD from '../../api-client/resources/drd.js';

describe('DRD Resource', () => {
  let http;
  const done = () => {};
  const params = { a: 1 };

  beforeEach(() => {
    http = {
      get: vi.fn()
    };
    DRD.http = http;
  });

  it('should use decision-requirements-definition path', () => {
    expect(DRD.path).toBe('decision-requirements-definition');
  });

  describe('count', () => {
    it('should use first argument as done callback if it is function', () => {
      DRD.count(done);
      const usedDone = http.get.mock.calls[0][1].done;
      expect(usedDone).toBe(done);
    });
    it('should use given params', () => {
      DRD.count(params, done);
      const usedParams = http.get.mock.calls[0][1].data;
      expect(usedParams).toEqual(params);
    });
    it('should use /count path', () => {
      DRD.count(params, done);
      expect(http.get).toHaveBeenCalledWith(DRD.path + '/count', expect.any(Object));
    });
  });

  describe('list', () => {
    it('should use first argument as done callback if it is function', () => {
      DRD.list(done);
      const usedDone = http.get.mock.calls[0][1].done;
      expect(usedDone).toBe(done);
    });
    it('should use given params', () => {
      DRD.list(params, done);
      const usedParams = http.get.mock.calls[0][1].data;
      expect(usedParams).toEqual(params);
    });
    it('should use path', () => {
      DRD.list(params, done);
      expect(http.get).toHaveBeenCalledWith(DRD.path, expect.any(Object));
    });
  });

  describe('get', () => {
    const id = 'id23';
    beforeEach(() => {
      DRD.get(id, done);
    });
    it('should add id to path', () => {
      expect(http.get).toHaveBeenCalledWith(DRD.path + '/' + id, expect.any(Object));
    });
    it('should pass done callback', () => {
      const usedDone = http.get.mock.calls[0][1].done;
      expect(usedDone).toBe(done);
    });
  });

  describe('getXML', () => {
    const id = 'id23';
    beforeEach(() => {
      DRD.getXML(id, done);
    });
    it('should add id to path', () => {
      expect(http.get).toHaveBeenCalledWith(DRD.path + '/' + id + '/xml', expect.any(Object));
    });
    it('should pass done callback', () => {
      const usedDone = http.get.mock.calls[0][1].done;
      expect(usedDone).toBe(done);
    });
  });

  describe('getByKey', () => {
    const key = 'id23';
    const tenantId = 'dd';
    beforeEach(() => {
      DRD.getByKey(key, tenantId, done);
    });
    it('should use correct path', () => {
      expect(http.get).toHaveBeenCalledWith(DRD.path + '/key/' + key + '/tenant-id/' + tenantId, expect.any(Object));
    });
    it('should pass done callback', () => {
      const usedDone = http.get.mock.calls[0][1].done;
      expect(usedDone).toBe(done);
    });
    it('should tenant-id should be optional', () => {
      http.get.mockClear();
      DRD.getByKey(key, done);
      const usedDone = http.get.mock.calls[0][1].done;
      expect(http.get).toHaveBeenCalledWith(DRD.path + '/key/' + key, expect.any(Object));
      expect(usedDone).toBe(done);
    });
  });

  describe('getXMLByKey', () => {
    const key = 'id23';
    const tenantId = 'dd';
    beforeEach(() => {
      DRD.getXMLByKey(key, tenantId, done);
    });
    it('should use correct path', () => {
      expect(http.get).toHaveBeenCalledWith(DRD.path + '/key/' + key + '/tenant-id/' + tenantId + '/xml', expect.any(Object));
    });
    it('should pass done callback', () => {
      const usedDone = http.get.mock.calls[0][1].done;
      expect(usedDone).toBe(done);
    });
    it('should tenant-id should be optional', () => {
      http.get.mockClear();
      DRD.getXMLByKey(key, done);
      const usedDone = http.get.mock.calls[0][1].done;
      expect(http.get).toHaveBeenCalledWith(DRD.path + '/key/' + key + '/xml', expect.any(Object));
      expect(usedDone).toBe(done);
    });
  });
});
