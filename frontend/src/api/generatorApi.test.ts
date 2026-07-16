import { describe, it, expect, vi, beforeEach } from 'vitest';
import { generateProject } from './generatorApi';

describe('generatorApi', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('should successfully make POST /api/generate request and return a Blob', async () => {
    const mockBlob = new Blob(['dummy zip content'], { type: 'application/zip' });
    
    // Mock global fetch
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      blob: () => Promise.resolve(mockBlob),
    });
    vi.stubGlobal('fetch', fetchMock);

    const result = await generateProject('entity Student {}');

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/generate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ cdl: 'entity Student {}' }),
    });
    expect(result).toBe(mockBlob);
  });

  it('should throw an error on failed HTTP responses', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
    });
    vi.stubGlobal('fetch', fetchMock);

    await expect(generateProject('entity Student {}')).rejects.toThrow(
      'Project generation failed with status code 400'
    );
  });
});
