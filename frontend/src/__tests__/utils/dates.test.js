/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { formatDate, formatDateForTooltips, formatDuration } from '@/utils/dates.js'

describe('dates utility', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  describe('formatDate', () => {
    it('should return empty string for falsy input', () => {
      expect(formatDate(null)).toBe('')
      expect(formatDate(undefined)).toBe('')
      expect(formatDate('')).toBe('')
    })

    it('should return empty string for an invalid date', () => {
      expect(formatDate('not a date')).toBe('')
    })

    it('should format using the default format when none is given', () => {
      expect(formatDate('2024-01-15T10:30:00')).toBe('January 15, 2024 10:30')
    })

    it('should format using an explicit format argument', () => {
      expect(formatDate('2024-01-15T10:30:00', 'YYYY-MM-DD')).toBe('2024-01-15')
    })

    it('should use the stored user preference format when no explicit format is given', () => {
      localStorage.setItem('cibseven:preferences:formatDefault', 'DD/MM/YYYY')
      expect(formatDate('2024-01-15T10:30:00')).toBe('15/01/2024')
    })

    it('should prefer an explicit format over the stored user preference', () => {
      localStorage.setItem('cibseven:preferences:formatDefault', 'DD/MM/YYYY')
      expect(formatDate('2024-01-15T10:30:00', 'YYYY')).toBe('2024')
    })
  })

  describe('formatDateForTooltips', () => {
    it('should return empty string for falsy input', () => {
      expect(formatDateForTooltips(null)).toBe('')
      expect(formatDateForTooltips(undefined)).toBe('')
      expect(formatDateForTooltips('')).toBe('')
    })

    it('should return empty string for an invalid date', () => {
      expect(formatDateForTooltips('not a date')).toBe('')
    })

    it('should format using the default long format when none is given', () => {
      expect(formatDateForTooltips('2024-01-15T10:30:00.123')).toBe('January 15, 2024 10:30:00.123')
    })

    it('should format using an explicit format argument', () => {
      expect(formatDateForTooltips('2024-01-15T10:30:00', 'YYYY-MM-DD HH:mm')).toBe('2024-01-15 10:30')
    })

    it('should use the stored user preference format when no explicit format is given', () => {
      localStorage.setItem('cibseven:preferences:formatLong', 'DD/MM/YYYY HH:mm:ss')
      expect(formatDateForTooltips('2024-01-15T10:30:00.123')).toBe('15/01/2024 10:30:00')
    })

    it('should prefer an explicit format over the stored user preference', () => {
      localStorage.setItem('cibseven:preferences:formatLong', 'DD/MM/YYYY HH:mm:ss')
      expect(formatDateForTooltips('2024-01-15T10:30:00', 'YYYY')).toBe('2024')
    })
  })

  describe('formatDuration', () => {
    it('should return empty string for falsy input', () => {
      expect(formatDuration(null)).toBe('')
      expect(formatDuration(undefined)).toBe('')
      expect(formatDuration(0)).toBe('')
    })

    it('should format a millisecond duration as an ISO 8601 duration string', () => {
      expect(formatDuration(3661000)).toBe('PT1H1M1S')
    })

    it('should format a sub-second duration', () => {
      expect(formatDuration(500)).toBe('PT0.5S')
    })
  })
})
