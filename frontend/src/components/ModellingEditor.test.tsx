import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { useState } from 'react';
import { MemoryRouter } from 'react-router-dom';
import ModellingEditor from './ModellingEditor';
import { generateProject } from '../api/generatorApi';

// Mock generatorApi
vi.mock('../api/generatorApi', () => ({
  generateProject: vi.fn(),
}));

// Mock @xyflow/react to avoid JSDOM layout measurement constraints
vi.mock('@xyflow/react', () => {
  return {
    ReactFlow: ({ children }: any) => <div data-testid="react-flow-mock">{children}</div>,
    MiniMap: () => null,
    Controls: () => null,
    Background: () => null,
    useNodesState: (initial: any) => {
      const [val, set] = useState(initial || []);
      const onNodesChange = () => {};
      return [val, set, onNodesChange];
    },
    useEdgesState: (initial: any) => {
      const [val, set] = useState(initial || []);
      const onEdgesChange = () => {};
      return [val, set, onEdgesChange];
    },
    addEdge: (edge: any, edges: any) => [...edges, edge],
    MarkerType: { ArrowClosed: 'arrowclosed' },
  };
});

describe('ModellingEditor ZIP Generation', () => {
  let createObjectURLMock: any;
  let revokeObjectURLMock: any;
  let linkClickMock: any;

  beforeEach(() => {
    vi.restoreAllMocks();
    vi.clearAllMocks();

    // Mock window.URL methods
    createObjectURLMock = vi.fn().mockReturnValue('blob:mock-url');
    revokeObjectURLMock = vi.fn();
    vi.stubGlobal('URL', {
      createObjectURL: createObjectURLMock,
      revokeObjectURL: revokeObjectURLMock,
    });

    // Mock anchor tag clicks for download triggers
    linkClickMock = vi.fn();
    const originalCreateElement = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
      const el = originalCreateElement(tagName);
      if (tagName === 'a') {
        vi.spyOn(el, 'click').mockImplementation(linkClickMock);
      }
      return el;
    });
  });

  it('should successfully trigger ZIP download and show success banner on click', async () => {
    const mockBlob = new Blob(['mock zip content'], { type: 'application/zip' });
    vi.mocked(generateProject).mockResolvedValue(mockBlob);

    render(
      <MemoryRouter>
        <ModellingEditor />
      </MemoryRouter>
    );

    // Click Add Entity to populate canvas with a node so CDL is not empty
    const addEntityBtn = screen.getByRole('button', { name: /Add Entity/i });
    fireEvent.click(addEntityBtn);

    // Locate the Generate button
    const generateBtn = screen.getByRole('button', { name: /Generate/i }) as HTMLButtonElement;
    expect(generateBtn).toBeDefined();
    expect(generateBtn.disabled).toBe(false);

    // Trigger generate action
    fireEvent.click(generateBtn);

    // Verify loading state is triggered
    expect(screen.getByText(/Generating\.\.\./i)).toBeDefined();
    expect(generateBtn.disabled).toBe(true);

    // Wait for the async API flow to resolve
    await waitFor(() => {
      expect(generateProject).toHaveBeenCalledTimes(1);
    });

    // Verify download triggered
    expect(createObjectURLMock).toHaveBeenCalledWith(mockBlob);
    expect(linkClickMock).toHaveBeenCalledTimes(1);
    expect(revokeObjectURLMock).toHaveBeenCalledWith('blob:mock-url');

    // Verify success banner is shown
    expect(screen.getByText(/Project generated successfully\./i)).toBeDefined();
    expect(generateBtn.disabled).toBe(false);
    expect(generateBtn.textContent).toContain('Generate');
  });

  it('should show error banner when API service call fails', async () => {
    vi.mocked(generateProject).mockRejectedValue(new Error('Network error'));

    render(
      <MemoryRouter>
        <ModellingEditor />
      </MemoryRouter>
    );

    // Click Add Entity to populate canvas with a node so CDL is not empty
    const addEntityBtn = screen.getByRole('button', { name: /Add Entity/i });
    fireEvent.click(addEntityBtn);

    const generateBtn = screen.getByRole('button', { name: /Generate/i }) as HTMLButtonElement;

    // Trigger generate action
    fireEvent.click(generateBtn);

    // Wait for failure flow to resolve
    await waitFor(() => {
      expect(generateProject).toHaveBeenCalledTimes(1);
    });

    // Verify error banner is shown and loading finishes
    expect(screen.getByText(/Project generation failed\. Please check your model and try again\./i)).toBeDefined();
    expect(generateBtn.disabled).toBe(false);
    expect(generateBtn.textContent).toContain('Generate');
    expect(linkClickMock).not.toHaveBeenCalled();
  });
});
