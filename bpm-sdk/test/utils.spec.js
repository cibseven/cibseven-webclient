import { describe, it, expect } from 'vitest';
import sdkUtils from '../utils.js';

describe('The SDK utilities', () => {
  describe('HAL tools', () => {
    describe('solveHALEmbedded()', () => {
      const HALResponse = {
        _embedded: {
          objA: [
            { id: 'obj-a-id-1', title: 'Obj A' },
            { id: 'obj-a-id-2', title: 'Obj B' },
            { id: 'obj-a-id-3', title: 'Obj C' }
          ],
          objB: [
            { objAId: 'obj-a-id-1', _embedded: null },
            { objAId: 'obj-a-id-2', objCId: 'obviously-not-present' },
            { objAId: 'obj-a-id-3' }
          ]
        }
      };
      it('remap the response object', () => {
        const remapped = sdkUtils.solveHALEmbedded(HALResponse);
        expect(remapped._embedded.objB[0]._embedded.objA[0].id).toBe(remapped._embedded.objB[0].objAId);
        expect(remapped._embedded.objB[1]._embedded.objA[0].id).toBe(remapped._embedded.objB[1].objAId);
        expect(remapped._embedded.objB[2]._embedded.objA[0].id).toBe(remapped._embedded.objB[2].objAId);
      });
    });
  });

  describe('control flow', () => {
    describe('series()', () => {
      it('is a function', () => {
        expect(typeof sdkUtils.series).toBe('function');
      });
      it('runs a array of functions', async () => {
        const result = await new Promise((resolve, reject) => {
          sdkUtils.series([
            cb => setTimeout(() => cb(null, 1), 1),
            cb => setTimeout(() => cb(null, 2), 1),
            cb => setTimeout(() => cb(null, 3), 1)
          ], (err, result) => {
            if (err) reject(err); else resolve(result);
          });
        });
        expect(result).toBeDefined();
        expect(result[0]).toBe(1);
        expect(result[1]).toBe(2);
        expect(result[2]).toBe(3);
      });
      it('runs an object of functions', async () => {
        const result = await new Promise((resolve, reject) => {
          sdkUtils.series({
            a: cb => setTimeout(() => cb(null, 1), 1),
            b: cb => setTimeout(() => cb(null, 2), 1),
            c: cb => setTimeout(() => cb(null, 3), 1)
          }, (err, result) => {
            if (err) reject(err); else resolve(result);
          });
        });
        expect(result).toBeDefined();
        expect(result.a).toBe(1);
        expect(result.b).toBe(2);
        expect(result.c).toBe(3);
      });
      it('stops the serie at the first error', async () => {
        await new Promise((resolve, reject) => {
          sdkUtils.series({
            a: cb => setTimeout(() => cb(null, 1), 1),
            b: cb => setTimeout(() => cb(new Error('Bang!')), 1),
            c: cb => setTimeout(() => cb(null, 3), 1)
          }, (err, result) => {
            try {
              expect(err).toBeDefined();
              expect(result).toBeDefined();
              expect(result.a).toBe(1);
              expect(result.b).toBeUndefined();
              resolve();
            } catch (e) {
              reject(e);
            }
          }).catch(reject);
        });
      });
    });
  });

  describe('escapeUrl', () => {
    it('should encode special characters', () => {
      expect(sdkUtils.escapeUrl('foo/bar')).toBe('foo%252Fbar');
      expect(sdkUtils.escapeUrl('foo*bar')).toBe('foo%2Abar');
      expect(sdkUtils.escapeUrl('foo\\bar')).toBe('foo%255Cbar');
    });
  });

  describe('debouncePromiseFactory', () => {
    it('should resolve only the latest promise', async () => {
      const debounce = sdkUtils.debouncePromiseFactory();
      let resolve1, resolve2;
      const p1 = new Promise(res => { resolve1 = res; });
      const p2 = new Promise(res => { resolve2 = res; });
      const r1 = debounce(p1);
      const r2 = debounce(p2);
      resolve1('first');
      resolve2('second');
      const result = await r2;
      expect(result).toBe('second');
      // r1 should never resolve, but to be sure, check that r1 is still pending
      let r1Settled = false;
      r1.then(() => { r1Settled = true; });
      await new Promise(res => setTimeout(res, 10));
      expect(r1Settled).toBe(false);
    });
  });
});
