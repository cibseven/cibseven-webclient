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
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { getFileContentBase64 } from '@/utils/fileUtils'

describe('getFileContentBase64', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('resolves with the base64 content stripped of the data URL prefix', async () => {
    const mockBase64 = 'Some base64 content'
    const mockFile = { name: 'test.txt' }

    vi.spyOn(globalThis, 'FileReader').mockImplementation(() => ({
      readAsDataURL() {
        this.result = `data:text/plain;base64,${mockBase64}`
        this.onload()
      },
      onload: null,
      onerror: null,
    }))

    const result = await getFileContentBase64(mockFile)
    expect(result).toBe(mockBase64)
  })

  it('rejects when FileReader encounters an error', async () => {
    const mockFile = { name: 'broken.bin' }

    vi.spyOn(globalThis, 'FileReader').mockImplementation(() => ({
      readAsDataURL() {
        this.onerror()
      },
      onload: null,
      onerror: null,
    }))

    await expect(getFileContentBase64(mockFile)).rejects.toThrow(
      'Failed to read file: broken.bin'
    )
  })
})
