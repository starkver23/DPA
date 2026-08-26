import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { useState } from 'react';
import { MemoryRouter } from 'react-router-dom';
import ModellingEditor from './ModellingEditor';
import { generateJavaCode, generateProject } from '../api/generatorApi';

// Mock generatorApi
vi.mock('../api/generatorApi', () => ({
  generateJavaCode: vi.fn(),
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
  let createdDownloadLinks: HTMLAnchorElement[];

  const submitGenerationForm = () => {
    fireEvent.click(screen.getByRole('button', { name: 'Generate Application' }));
  };

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
    createdDownloadLinks = [];
    const originalCreateElement = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
      const el = originalCreateElement(tagName);
      if (tagName === 'a') {
        createdDownloadLinks.push(el as HTMLAnchorElement);
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

    const generateBtn = screen.getByRole('button', { name: /Generate Full Application/i }) as HTMLButtonElement;
    expect(generateBtn).toBeDefined();
    expect(generateBtn.disabled).toBe(false);

    fireEvent.click(generateBtn);
    expect(screen.getByText('Application generation')).toBeDefined();
    fireEvent.change(screen.getByLabelText('Application name'), { target: { value: 'Course Planner' } });
    fireEvent.change(screen.getByLabelText('Repository name'), { target: { value: 'course-planner' } });
    fireEvent.change(screen.getByLabelText('Default Java package name'), { target: { value: 'com.acme.courseplanner' } });
    submitGenerationForm();

    // Verify loading state is triggered
    expect(screen.getByText(/Generating\.\.\./i)).toBeDefined();
    expect(generateBtn.disabled).toBe(true);

    // Wait for the async API flow to resolve
    await waitFor(() => {
      expect(generateProject).toHaveBeenCalledTimes(1);
    });
    expect(generateProject).toHaveBeenCalledWith(expect.any(String), expect.objectContaining({
      applicationName: 'Course Planner',
      repositoryName: 'course-planner',
      defaultJavaPackageName: 'com.acme.courseplanner',
    }));

    // Verify download triggered
    expect(createObjectURLMock).toHaveBeenCalledWith(mockBlob);
    expect(linkClickMock).toHaveBeenCalledTimes(1);
    expect(revokeObjectURLMock).toHaveBeenCalledWith('blob:mock-url');

    // Verify success banner is shown
    expect(screen.getByText(/Project generated successfully\./i)).toBeDefined();
    expect(generateBtn.disabled).toBe(false);
    expect(generateBtn.textContent).toContain('Generate Full Application');
  });

  it('should trigger Java source ZIP download from the Java code button', async () => {
    const mockBlob = new Blob(['mock java zip content'], { type: 'application/zip' });
    vi.mocked(generateJavaCode).mockResolvedValue(mockBlob);

    render(
      <MemoryRouter>
        <ModellingEditor />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByRole('button', { name: /Add Entity/i }));
    fireEvent.click(screen.getByRole('button', { name: /Generate Java Code/i }));

    await waitFor(() => {
      expect(generateJavaCode).toHaveBeenCalledTimes(1);
    });

    expect(createObjectURLMock).toHaveBeenCalledWith(mockBlob);
    expect(linkClickMock).toHaveBeenCalledTimes(1);
    expect(createdDownloadLinks.at(-1)?.download).toBe('generated-java-source.zip');
    expect(screen.getByText(/Java code generated successfully\./i)).toBeDefined();
    expect(generateProject).not.toHaveBeenCalled();
  });

  it('should expose exactly the requested main action buttons', () => {
    render(
      <MemoryRouter>
        <ModellingEditor />
      </MemoryRouter>
    );

    expect(screen.getByRole('button', { name: 'Parse PDL' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Generate Java Code' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Generate Full Application' })).toBeDefined();
    expect(screen.getByRole('button', { name: /Load Sample/i })).toBeDefined();
    expect(screen.queryByRole('button', { name: /Add Interface/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /Parse PlantUML/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /Export/i })).toBeNull();
  });

  it('should load the requested rich sample into the PDL editor', () => {
    render(
      <MemoryRouter>
        <ModellingEditor />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByRole('button', { name: /Load Sample/i }));

    const editor = screen.getByPlaceholderText(/Write raw PDL/i) as HTMLTextAreaElement;
    expect(editor.value).toContain('abstract entity Person');
    expect(editor.value).toContain('interface Payable');
    expect(editor.value).toContain('interface Identifiable');
    expect(editor.value).toContain('entity Employee extends Person implements Payable, Identifiable');
    expect(editor.value).toContain('relationship OneToOne');
    expect(editor.value).toContain('Employee{officeDepartment} to Department{primaryEmployee}');
    expect(editor.value).toContain('Department{employees} to Employee{homeDepartment}');
    expect(editor.value).toContain('Employee{projects} to Project{employees}');
    expect(editor.value).toContain('calculateSalary() Double');
    expect(editor.value).toContain('getIdentifier() String');
  });

  it('should preserve interface fields after parsing compact PDL', () => {
    render(
      <MemoryRouter>
        <ModellingEditor />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByPlaceholderText(/Write raw PDL/i), {
      target: { value: 'interface StaffMember { staffId String salary Double }' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Parse PDL' }));

    const editor = screen.getByPlaceholderText(/Write raw PDL/i) as HTMLTextAreaElement;
    expect(editor.value).toContain('interface StaffMember');
    expect(editor.value).toContain('staffId String');
    expect(editor.value).toContain('salary Double');
  });

  it('should parse the exact abstraction/interface PDL model before Java generation', async () => {
    const mockBlob = new Blob(['mock java zip content'], { type: 'application/zip' });
    vi.mocked(generateJavaCode).mockResolvedValue(mockBlob);
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});

    render(
      <MemoryRouter>
        <ModellingEditor />
      </MemoryRouter>
    );

    const exactPdl = `abstract entity Person {
  name String
  email String
}

interface Payable {
  calculateSalary() Double
}

entity Student extends Person implements Payable {
  studentNumber String
  calculateSalary() Double
}

entity Course {
  title String
  code String
}

relationship ManyToMany {
  Student{courses} to Course{students}
}`;

    fireEvent.change(screen.getByPlaceholderText(/Write raw PDL/i), { target: { value: exactPdl } });
    fireEvent.click(screen.getByRole('button', { name: 'Parse PDL' }));
    fireEvent.click(screen.getByRole('button', { name: 'Generate Java Code' }));

    await waitFor(() => {
      expect(generateJavaCode).toHaveBeenCalledTimes(1);
    });

    const generatedPdl = vi.mocked(generateJavaCode).mock.calls[0][0];
    expect(generatedPdl).toContain('abstract entity Person');
    expect(generatedPdl).toContain('interface Payable');
    expect(generatedPdl).toContain('calculateSalary() Double');
    expect(generatedPdl).toContain('entity Student extends Person implements Payable');
    expect(generatedPdl).toContain('relationship ManyToMany');
    expect(alertSpy).not.toHaveBeenCalled();
  });

  it('should add and generate operation lines from the entity inspector', async () => {
    const mockBlob = new Blob(['mock java zip content'], { type: 'application/zip' });
    vi.mocked(generateJavaCode).mockResolvedValue(mockBlob);

    render(
      <MemoryRouter>
        <ModellingEditor />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByRole('button', { name: /Add Entity/i }));
    fireEvent.click(screen.getByRole('button', { name: /Operation/i }));
    fireEvent.change(screen.getByPlaceholderText('operation() String'), {
      target: { value: 'calculateScore() Integer' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Generate Java Code' }));

    await waitFor(() => {
      expect(generateJavaCode).toHaveBeenCalledTimes(1);
    });

    const generatedPdl = vi.mocked(generateJavaCode).mock.calls[0][0];
    expect(generatedPdl).toContain('calculateScore() Integer');
  });

  it('should keep multiple different relationships between the same entities distinguishable in PDL', async () => {
    const mockBlob = new Blob(['mock full zip content'], { type: 'application/zip' });
    vi.mocked(generateProject).mockResolvedValue(mockBlob);

    render(
      <MemoryRouter>
        <ModellingEditor />
      </MemoryRouter>
    );

    const multiRelationshipPdl = `entity Student {
  email String
}

entity Course {
  title String
}

relationship ManyToMany {
  Student{courses} to Course{students}
}

relationship OneToMany {
  Student{course} to Course
}`;

    fireEvent.change(screen.getByPlaceholderText(/Write raw PDL/i), { target: { value: multiRelationshipPdl } });
    fireEvent.click(screen.getByRole('button', { name: 'Parse PDL' }));
    fireEvent.click(screen.getByRole('button', { name: 'Generate Full Application' }));
    submitGenerationForm();

    await waitFor(() => {
      expect(generateProject).toHaveBeenCalledTimes(1);
    });

    const generatedPdl = vi.mocked(generateProject).mock.calls[0][0];
    expect(generatedPdl).toContain('relationship OneToMany');
    expect(generatedPdl).toContain('relationship ManyToMany');
    expect(generatedPdl).toContain('Student{course} to Course');
    expect(generatedPdl).toContain('Student{courses} to Course{students}');
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

    const generateBtn = screen.getByRole('button', { name: /Generate Full Application/i }) as HTMLButtonElement;

    fireEvent.click(generateBtn);
    submitGenerationForm();

    // Wait for failure flow to resolve
    await waitFor(() => {
      expect(generateProject).toHaveBeenCalledTimes(1);
    });

    // Verify error banner is shown and loading finishes
    expect(screen.getByText(/Network error/i)).toBeDefined();
    expect(generateBtn.disabled).toBe(false);
    expect(generateBtn.textContent).toContain('Generate Full Application');
    expect(linkClickMock).not.toHaveBeenCalled();
  });
});
