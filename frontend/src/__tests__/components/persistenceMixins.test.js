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
import { describe, it, expect, beforeEach, vi } from 'vitest'
import bpmnViewportPersistenceMixin from '@/components/process/mixins/bpmnViewportPersistenceMixin.js'
import viewerFrameSizePersistenceMixin from '@/components/process/mixins/viewerFrameSizePersistenceMixin.js'
import sidebarOpenPersistenceMixin from '@/mixins/sidebarOpenPersistenceMixin.js'

describe('bpmnViewportPersistenceMixin', () => {
  const { viewboxStorageKey, onViewboxChanged, restoreViewboxIfSaved } = bpmnViewportPersistenceMixin.methods

  beforeEach(() => sessionStorage.clear())

  it('defaults the storage key to the BPMN process id', () => {
    expect(viewboxStorageKey.call({ process: { id: 'proc-1' } })).toBe('cibseven:bpmn-viewbox:proc-1')
  })

  it('persists and restores a viewbox using a host-overridden key (e.g. a DMN decision id)', () => {
    const setViewbox = vi.fn()
    const ctx = {
      viewboxStorageKey: () => 'cibseven:dmn-viewbox:dec-1',
      $refs: { diagram: { setViewbox } }
    }
    const viewbox = { x: 1, y: 2, width: 100, height: 50 }
    onViewboxChanged.call(ctx, viewbox)
    expect(sessionStorage.getItem('cibseven:dmn-viewbox:dec-1')).toBe(JSON.stringify(viewbox))

    restoreViewboxIfSaved.call(ctx)
    expect(setViewbox).toHaveBeenCalledWith(viewbox)
  })

  it('does not call setViewbox when nothing was saved for that key', () => {
    const setViewbox = vi.fn()
    restoreViewboxIfSaved.call({ viewboxStorageKey: () => 'cibseven:dmn-viewbox:none', $refs: { diagram: { setViewbox } } })
    expect(setViewbox).not.toHaveBeenCalled()
  })
})

describe('viewerFrameSizePersistenceMixin', () => {
  const { viewerFrameStorageKey, restoreViewerFrameSize, saveViewerFrameSize } = viewerFrameSizePersistenceMixin.methods

  beforeEach(() => localStorage.clear())

  it('defaults to the process storage key', () => {
    expect(viewerFrameStorageKey.call({})).toBe('cibseven:viewer-frame-size:process')
  })

  it('saves and restores height/icon under the resolved key', () => {
    const saveCtx = { viewerFrameStorageKey, bpmnViewerHeight: 350, toggleIcon: 'mdi-chevron-up' }
    saveViewerFrameSize.call(saveCtx)

    const restoreCtx = { viewerFrameStorageKey, maxViewerFrameHeight: () => 1000, bpmnViewerHeight: 0, toggleIcon: '' }
    restoreViewerFrameSize.call(restoreCtx)
    expect(restoreCtx.bpmnViewerHeight).toBe(350)
    expect(restoreCtx.toggleIcon).toBe('mdi-chevron-up')
  })

  it('keeps a host-overridden storage key independent from the default one', () => {
    const ctx = { viewerFrameStorageKey: () => 'cibseven:viewer-frame-size:decision', bpmnViewerHeight: 200, toggleIcon: 'mdi-chevron-down' }
    saveViewerFrameSize.call(ctx)
    expect(localStorage.getItem('cibseven:viewer-frame-size:process')).toBeNull()
    expect(localStorage.getItem('cibseven:viewer-frame-size:decision')).toContain('200')
  })
})

describe('sidebarOpenPersistenceMixin', () => {
  const { getSavedLeftOpen, saveLeftOpen } = sidebarOpenPersistenceMixin.methods

  beforeEach(() => localStorage.clear())

  it('defaults to open when nothing was saved for that scope', () => {
    expect(getSavedLeftOpen('decision')).toBe(true)
  })

  it('persists and restores independently per scope', () => {
    saveLeftOpen('process-instance', false)
    expect(getSavedLeftOpen('process-instance')).toBe(false)
    expect(getSavedLeftOpen('process-definition')).toBe(true)
  })

  it('falls back to true when localStorage is unavailable', () => {
    const original = Storage.prototype.getItem
    Storage.prototype.getItem = () => { throw new Error('blocked') }
    expect(getSavedLeftOpen('decision')).toBe(true)
    Storage.prototype.getItem = original
  })
})
