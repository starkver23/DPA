import { useState, useCallback, useMemo, useEffect, type FormEvent } from 'react';
import dagre from 'dagre';
import {
  ReactFlow,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  MarkerType,
  type Connection,
  type Edge,
  type Node,
  type ReactFlowInstance,
} from '@xyflow/react';
import { Settings, FileText } from 'lucide-react';
import '@xyflow/react/dist/style.css';

import { type Field, type Method, type EntityNode, type RelationshipType } from '../types/modeling';
import { EntityNodeComponent } from './EntityNodeComponent';
import { RelationshipEdgeComponent } from './RelationshipEdgeComponent';
import { generateJavaCode, generateProject, type ProjectGenerationOptions } from '../api/generatorApi';

// Extracted UI Components
import ModellingToolbar from './ModellingToolbar';
import EntityInspector from './EntityInspector';
import RelationshipInspector from './RelationshipInspector';
import SidebarControlBelt from './SidebarControlBelt';

const nodeTypes = { entityNode: EntityNodeComponent };
const edgeTypes = { relationship: RelationshipEdgeComponent };
const defaultGenerationOptions: ProjectGenerationOptions = {
  applicationName: 'Generated App',
  repositoryName: 'generated-app',
  defaultJavaPackageName: 'com.mycompany.codeclassroom',
  javaVersion: '21',
  databaseType: 'postgresql',
  authenticationType: 'jwt',
  buildTool: 'maven',
};

const samplePDL = `abstract entity Person {
  name String
  email String
}

interface Payable {
  calculateSalary() Double
}

interface Identifiable {
  getIdentifier() String
}

entity Employee extends Person implements Payable, Identifiable {
  employeeId String
  salary Double
  calculateSalary() Double
  getIdentifier() String
}

entity Department {
  name String
  code String
}

entity Project {
  name String
  budget Double
}

relationship OneToOne {
  Employee{officeDepartment} to Department{primaryEmployee}
}

relationship OneToMany {
  Department{employees} to Employee{homeDepartment}
}

relationship ManyToMany {
  Employee{projects} to Project{employees}
}`;

// ponytail: pure helper to avoid duplicate edge styling rules across 4 locations
const getEdgeStyle = (typeLabel: string) => {
  const isInheritance = typeLabel === 'Inheritance' || typeLabel === 'extends';
  const isImplementation = typeLabel === 'Implementation' || typeLabel === 'implements';
  const color = isInheritance ? '#dc2626' : isImplementation ? '#0891b2' : '#6366f1';
  const lineStyle = isInheritance
    ? { strokeDasharray: '8,4', strokeWidth: 2.5, stroke: color }
    : isImplementation
      ? { strokeDasharray: '2,5', strokeWidth: 2.5, stroke: color }
      : { strokeWidth: 2, stroke: color };
  return {
    label: isInheritance ? 'extends' : isImplementation ? 'implements' : typeLabel,
    data: {
      label: isInheritance ? 'extends' : isImplementation ? 'implements' : typeLabel,
      cardinality: isInheritance || isImplementation ? '' : typeLabel,
    },
    style: lineStyle,
    markerEnd: { type: MarkerType.ArrowClosed, color }
  };
};

const isMethodLine = (line: string) => line.includes('(') && line.includes(')');

const parseFieldLine = (line: string): Field[] => {
  const parts = line.split(/\s+/).filter(Boolean);
  const fields: Field[] = [];
  for (let i = 0; i < parts.length; i += 2) {
    const name = parts[i];
    if (!name) continue;
    fields.push({ id: crypto.randomUUID(), name, type: parts[i + 1] || 'String' });
  }
  return fields;
};

const getParallelRelationshipKey = (edge: Edge) => [edge.source, edge.target].sort().join('::');

const reserveRelationshipProperty = (
  usedPropertiesByEntity: Map<string, Set<string>>,
  entityId: string,
  requestedProperty: string,
) => {
  const usedProperties = usedPropertiesByEntity.get(entityId) || new Set<string>();
  let candidate = requestedProperty;
  let suffix = 2;
  while (usedProperties.has(candidate.toLowerCase())) {
    candidate = `${requestedProperty}${suffix}`;
    suffix += 1;
  }
  usedProperties.add(candidate.toLowerCase());
  usedPropertiesByEntity.set(entityId, usedProperties);
  return candidate;
};

const sanitizeDownloadName = (value: string) => {
  const cleaned = value.trim().toLowerCase().replace(/[^a-z0-9._-]+/g, '-').replace(/^-+|-+$/g, '');
  return cleaned || 'generated-app';
};

const applyParallelEdgeLayout = (edgeList: Edge[]): Edge[] => {
  const groups = new Map<string, Edge[]>();
  edgeList.forEach((edge) => {
    const key = getParallelRelationshipKey(edge);
    groups.set(key, [...(groups.get(key) || []), edge]);
  });

  return edgeList.map((edge) => {
    const siblings = groups.get(getParallelRelationshipKey(edge)) || [edge];
    const parallelIndex = siblings.findIndex((candidate) => candidate.id === edge.id);
    const labels = new Set(siblings.map((candidate) => String(candidate.label || candidate.data?.label || '')));
    const hasMixedLabels = labels.size > 1;
    const mixedColor = '#14b8a6';
    return {
      ...edge,
      type: 'relationship',
      style: {
        ...(edge.style || {}),
        stroke: hasMixedLabels ? mixedColor : edge.style?.stroke,
      },
      markerEnd: hasMixedLabels
        ? { type: MarkerType.ArrowClosed, color: mixedColor }
        : edge.markerEnd,
      data: {
        ...(edge.data || {}),
        label: String(edge.label || edge.data?.label || ''),
        parallelIndex,
        parallelTotal: siblings.length,
      },
    };
  });
};

const buildPDLFromGraph = (nodes: EntityNode[], edges: Edge[]) => {
  let pdlString = '';
  const inheritanceMap: Record<string, string> = {};
  const implementsMap: Record<string, string[]> = {};

  edges.forEach((e) => {
    const label = String(e.label || e.data?.label || '');
    if (label === 'extends') {
      const parentNode = nodes.find((n) => n.id === e.target);
      if (parentNode) inheritanceMap[e.source] = parentNode.data.label;
    } else if (label === 'implements') {
      const interfaceNode = nodes.find((n) => n.id === e.target && n.data.kind === 'interface');
      if (interfaceNode) {
        implementsMap[e.source] = [...(implementsMap[e.source] || []), interfaceNode.data.label];
      }
    }
  });

  nodes.forEach((n) => {
    const parentName = inheritanceMap[n.id];
    const extendsClause = parentName ? ` extends ${parentName}` : '';
    if (n.data.kind === 'interface') {
      pdlString += `interface ${n.data.label}${extendsClause} {\n`;
      n.data.fields.forEach((f) => { pdlString += `  ${f.name} ${f.type}\n`; });
      n.data.methods.forEach((m) => { pdlString += `  ${m.definition}\n`; });
      pdlString += `}\n\n`;
      return;
    }

    const abstractPrefix = n.data.abstract ? 'abstract ' : '';
    const implementsList = implementsMap[n.id] || [];
    const implementsClause = implementsList.length > 0 ? ` implements ${implementsList.join(', ')}` : '';
    pdlString += `${abstractPrefix}entity ${n.data.label}${extendsClause}${implementsClause} {\n`;
    n.data.fields.forEach((f) => { pdlString += `  ${f.name} ${f.type}\n`; });
    n.data.methods.forEach((m) => { pdlString += `  ${m.definition}\n`; });
    pdlString += `}\n\n`;
  });

  const relationshipEdges = edges.filter((e) => ['OneToOne', 'OneToMany', 'ManyToMany'].includes(String(e.label || e.data?.label || '')));
  const pairCounts = new Map<string, number>();
  const pairIndexes = new Map<string, number>();
  const usedRelationshipProperties = new Map<string, Set<string>>();
  relationshipEdges.forEach((edge) => {
    const key = getParallelRelationshipKey(edge);
    pairCounts.set(key, (pairCounts.get(key) || 0) + 1);
  });

  ['OneToOne', 'OneToMany', 'ManyToMany'].forEach((type) => {
    const edgeList = relationshipEdges.filter((e) => String(e.label || e.data?.label || '') === type);
    if (edgeList.length === 0) return;
    pdlString += `relationship ${type} {\n`;
    edgeList.forEach((e) => {
      const s = nodes.find((n) => n.id === e.source);
      const t = nodes.find((n) => n.id === e.target);
      if (s && t) {
        const pairKey = getParallelRelationshipKey(e);
        const nextPairIndex = (pairIndexes.get(pairKey) || 0) + 1;
        pairIndexes.set(pairKey, nextPairIndex);
        const suffix = (pairCounts.get(pairKey) || 0) > 1 ? `${nextPairIndex}` : '';
        const sourceProperty = typeof e.data?.sourceProperty === 'string' ? e.data.sourceProperty : undefined;
        const targetProperty = typeof e.data?.targetProperty === 'string' ? e.data.targetProperty : undefined;
        const safeSourceProperty = reserveRelationshipProperty(
          usedRelationshipProperties,
          s.id,
          sourceProperty || `${t.data.label.toLowerCase()}${type === 'ManyToMany' ? 's' : ''}${suffix}`,
        );
        if (type === 'ManyToMany') {
          const safeTargetProperty = reserveRelationshipProperty(
            usedRelationshipProperties,
            t.id,
            targetProperty || `${s.data.label.toLowerCase()}s${suffix}`,
          );
          pdlString += `  ${s.data.label}{${safeSourceProperty}} to ${t.data.label}{${safeTargetProperty}}\n`;
        } else {
          const safeTargetProperty = targetProperty
            ? reserveRelationshipProperty(usedRelationshipProperties, t.id, targetProperty)
            : undefined;
          pdlString += `  ${s.data.label}{${safeSourceProperty}} to ${t.data.label}${safeTargetProperty ? `{${safeTargetProperty}}` : ''}\n`;
        }
      }
    });
    pdlString += `}\n\n`;
  });

  return pdlString.trim();
};

export default function ModellingEditor() {
  const [nodes, setNodes, onNodesChange] = useNodesState<EntityNode>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const [reactFlowInstance, setReactFlowInstance] = useState<ReactFlowInstance<EntityNode, Edge> | null>(null);
  const [relationshipType, setRelationshipType] = useState<RelationshipType>('ManyToMany');
  const [activeSelection, setActiveSelection] = useState<{ type: 'node' | 'edge'; id: string } | null>(null);

  const [inputJDL, setInputJDL] = useState<string>(
    `entity Student {\n  email String\n}\n\nentity Course {\n  title String\n}\n\nrelationship ManyToMany {\n  Student to Course\n}`
  );

  const [isGenerating, setIsGenerating] = useState(false);
  const [notification, setNotification] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [showGenerationForm, setShowGenerationForm] = useState(false);
  const [generationOptions, setGenerationOptions] = useState<ProjectGenerationOptions>(defaultGenerationOptions);

  const onSelectionChange = useCallback(({ nodes: selectedNodes, edges: selectedEdges }: { nodes: Node[]; edges: Edge[] }) => {
    if (selectedNodes.length > 0) {
      setActiveSelection({ type: 'node', id: selectedNodes[0].id });
    } else if (selectedEdges.length > 0) {
      setActiveSelection({ type: 'edge', id: selectedEdges[0].id });
    } else {
      setActiveSelection(null);
    }
  }, []);

  const updateNodeData = useCallback((id: string, updater: (data: EntityNode['data']) => Partial<EntityNode['data']>) => {
    setNodes((nds) => nds.map((n) => (n.id === id ? { ...n, data: { ...n.data, ...updater(n.data) } } : n)));
  }, [setNodes]);

  const updateEdgeType = useCallback((edgeId: string, typeLabel: string) => {
    setEdges((eds) => applyParallelEdgeLayout(eds.map((e) => {
      if (e.id !== edgeId) return e;
      return {
        ...e,
        ...getEdgeStyle(typeLabel)
      };
    })));
  }, [setEdges]);

  const deleteSelectedEntity = useCallback((id: string) => {
    setNodes((nds) => nds.filter((n) => n.id !== id));
    setEdges((eds) => eds.filter((e) => e.source !== id && e.target !== id));
    setActiveSelection(null);
  }, [setNodes, setEdges]);

  const deleteSelectedEdge = useCallback((id: string) => {
    setEdges((eds) => eds.filter((e) => e.id !== id));
    setActiveSelection(null);
  }, [setEdges]);

  const addNewEntity = useCallback(() => {
    const id = crypto.randomUUID();
    const newNode: EntityNode = {
      id,
      type: 'entityNode',
      position: { x: 150 + Math.random() * 100, y: 150 + Math.random() * 100 },
      data: { label: `NewEntity${nodes.length + 1}`, kind: 'class', fields: [], methods: [] },
    };
    setNodes((nds) => nds.concat(newNode));
    setActiveSelection({ type: 'node', id });
  }, [nodes.length, setNodes]);

  const clearAllNodesAndEdges = useCallback(() => {
    if (window.confirm("Are you sure you want to completely erase the canvas layout?")) {
      setNodes([]);
      setEdges([]);
      setActiveSelection(null);
      setInputJDL('');
    }
  }, [setNodes, setEdges]);

  const downloadGeneratedBlob = (blob: Blob, filename: string) => {
    const downloadUrl = window.URL.createObjectURL(blob);
    const downloadLink = document.createElement('a');
    downloadLink.href = downloadUrl;
    downloadLink.download = filename;
    document.body.appendChild(downloadLink);
    downloadLink.click();
    document.body.removeChild(downloadLink);
    window.URL.revokeObjectURL(downloadUrl);
  };

  const runGeneration = useCallback(async (mode: 'java' | 'full', options?: ProjectGenerationOptions) => {
    const finalCDL = buildPDLFromGraph(nodes, edges);
    setInputJDL(finalCDL || '// No visual structure configured.');
    setActiveSelection(null); // Forces the sidebar view to toggle directly back into the code text editor view

    if (!finalCDL) {
      setNotification({
        type: 'error',
        // ponytail: explicit warning when canvas is empty instead of generic failure message
        message: 'Project generation failed. No entities or structures are configured on the canvas.'
      });
      return;
    }

    setIsGenerating(true);
    setNotification(null);

    try {
      const blob = mode === 'java' ? await generateJavaCode(finalCDL) : await generateProject(finalCDL, options);
      const filename = mode === 'java'
        ? 'generated-java-source.zip'
        : `${sanitizeDownloadName(options?.repositoryName || defaultGenerationOptions.repositoryName)}.zip`;
      downloadGeneratedBlob(blob, filename);

      setNotification({
        type: 'success',
        message: mode === 'java' ? 'Java code generated successfully.' : 'Project generated successfully.'
      });
    } catch (error) {
      console.error(error);
      // ponytail: read and display the specific descriptive error message propagated from the server
      const errorMessage = error instanceof Error ? error.message : 'Project generation failed. Please check your model and try again.';
      setNotification({
        type: 'error',
        message: errorMessage
      });
    } finally {
      setIsGenerating(false);
    }
  }, [nodes, edges]);

  const generateJavaCodeFromCanvas = useCallback(() => runGeneration('java'), [runGeneration]);
  const generateFullApplicationFromCanvas = useCallback(() => setShowGenerationForm(true), []);
  const submitFullApplicationGeneration = useCallback((event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setShowGenerationForm(false);
    runGeneration('full', generationOptions);
  }, [generationOptions, runGeneration]);
  const updateGenerationOption = useCallback((key: keyof ProjectGenerationOptions, value: string) => {
    setGenerationOptions((current) => ({ ...current, [key]: value }));
  }, []);

  const fitDiagramIntoView = useCallback(() => {
    window.setTimeout(() => {
      reactFlowInstance?.fitView({ padding: 0.18, duration: 300, includeHiddenNodes: false });
    }, 0);
  }, [reactFlowInstance]);

  const parsePDLSourceToCanvas = useCallback((source: string) => {
    try {
      const parsedNodes: EntityNode[] = [];
      const parsedEdges: Edge[] = [];

      const entityBlocks = source.match(/(?:abstract\s+)?entity\s+\w+(?:\s+extends\s+\w+)?(?:\s+implements\s+[\w\s,]+)?\s*\{[^}]*\}|interface\s+\w+(?:\s+extends\s+[\w\s,]+)?\s*\{[^}]*\}/g);
      
      if (entityBlocks) {
        entityBlocks.forEach((block, index) => {
          const match = block.match(/(?:(abstract)\s+)?(entity|interface)\s+(\w+)(?:\s+extends\s+([\w\s,]+?))?(?:\s+implements\s+([\w\s,]+?))?\s*\{([^}]*)\}/);
          if (!match) return;

          const isAbstract = !!match[1];
          const declarationType = match[2];
          const entityName = match[3];
          const parentNames = (match[4] || '').split(',').map((name) => name.trim()).filter(Boolean);
          const implementedNames = (match[5] || '').split(',').map((name) => name.trim()).filter(Boolean);
          const contentBody = match[6];
          const fields: Field[] = [];
          const methods: Method[] = [];

          contentBody.split('\n').map(l => l.trim()).filter(Boolean).forEach(line => {
            if (isMethodLine(line)) {
              methods.push({ id: crypto.randomUUID(), definition: line });
              return;
            }
            fields.push(...parseFieldLine(line));
          });

          const entityId = entityName.toLowerCase() + '-id';
          parsedNodes.push({
            id: entityId,
            type: 'entityNode',
            position: { x: 100 + (index % 3) * 280, y: 150 + Math.floor(index / 3) * 320 },
            data: { label: entityName, fields, methods, kind: declarationType === 'interface' ? 'interface' : 'class', abstract: declarationType === 'interface' ? false : isAbstract }
          });

          parentNames.forEach((parentName) => {
            parsedEdges.push({
              id: `edge-inherit-${crypto.randomUUID()}`,
              source: entityId,
              target: parentName.toLowerCase() + '-id',
              type: 'relationship',
              ...getEdgeStyle('extends')
            });
          });

          implementedNames.forEach((interfaceName) => {
            parsedEdges.push({
              id: `edge-implements-${crypto.randomUUID()}`,
              source: entityId,
              target: interfaceName.toLowerCase() + '-id',
              type: 'relationship',
              ...getEdgeStyle('implements')
            });
          });
        });
      }

      const relationshipBlocks = [...source.matchAll(/relationship\s+(\w+)\s*\{([\s\S]*?)\n\s*\}/g)];
      if (relationshipBlocks) {
        relationshipBlocks.forEach(match => {
          const [, relType, contentBody] = match;
          contentBody.split('\n').map(l => l.trim()).filter(Boolean).forEach(line => {
            const linkMatch = line.match(/(\w+)(?:\{([^}]*)\})?\s+to\s+(\w+)(?:\{([^}]*)\})?/);
            if (linkMatch) {
              const [, sourceEnt, sourceProperty, targetEnt, targetProperty] = linkMatch;
              parsedEdges.push({
                id: `edge-rel-${crypto.randomUUID()}`,
                source: sourceEnt.toLowerCase() + '-id',
                target: targetEnt.toLowerCase() + '-id',
                type: 'relationship',
                ...getEdgeStyle(relType),
                data: {
                  ...getEdgeStyle(relType).data,
                  sourceProperty,
                  targetProperty,
                },
              });
            }
          });
        });
      }

      if (parsedNodes.length === 0) {
        alert("Parser complete: No valid structures identified.");
        return;
      }

      setNodes(parsedNodes);
      setEdges(applyParallelEdgeLayout(parsedEdges));
      setActiveSelection(null);
      fitDiagramIntoView();
    } catch {
      alert("Parsing Error: Verify your JDL configuration layout syntax.");
    }
  }, [setNodes, setEdges, fitDiagramIntoView]);

  const parseJDLToCanvas = useCallback(() => {
    parsePDLSourceToCanvas(inputJDL);
  }, [inputJDL, parsePDLSourceToCanvas]);

  const loadSample = useCallback(() => {
    setInputJDL(samplePDL);
    parsePDLSourceToCanvas(samplePDL);
  }, [parsePDLSourceToCanvas]);

  const onConnect = useCallback((params: Connection) => {
    const stylizedEdge: Edge = {
      id: `edge-${crypto.randomUUID()}`,
      source: params.source,
      target: params.target,
      sourceHandle: params.sourceHandle,
      targetHandle: params.targetHandle,
      type: 'relationship',
      ...getEdgeStyle(relationshipType)
    };
    setEdges((eds) => applyParallelEdgeLayout(eds.concat(stylizedEdge)));
  }, [relationshipType, setEdges]);

  const toggleEdgeDirection = useCallback((edgeId: string) => {
    setEdges((eds) => applyParallelEdgeLayout(eds.map((e) => (e.id === edgeId ? { ...e, source: e.target, target: e.source } : e))));
  }, [setEdges]);

  const autoLayout = useCallback(() => {
    const graph = new dagre.graphlib.Graph();
    graph.setDefaultEdgeLabel(() => ({}));
    graph.setGraph({ rankdir: 'LR', ranksep: 120, nodesep: 80, marginx: 40, marginy: 40 });

    nodes.forEach((node) => {
      graph.setNode(node.id, { width: 220, height: 150 });
    });

    edges.forEach((edge) => {
      graph.setEdge(edge.source, edge.target);
    });

    dagre.layout(graph);

    setNodes((currentNodes) => currentNodes.map((node) => {
      const layoutNode = graph.node(node.id);
      if (!layoutNode) return node;
      return {
        ...node,
        position: {
          x: layoutNode.x - 110,
          y: layoutNode.y - 75,
        },
      };
    }));
    setEdges((currentEdges) => applyParallelEdgeLayout(currentEdges));
    fitDiagramIntoView();
  }, [nodes, edges, setNodes, setEdges, fitDiagramIntoView]);

  useEffect(() => {
    if (!activeSelection && nodes.length > 0) {
      setInputJDL(buildPDLFromGraph(nodes, edges));
    }
  }, [nodes, edges, activeSelection]);

  const selectedNode = useMemo(() => activeSelection?.type === 'node' ? nodes.find(n => n.id === activeSelection.id) : null, [activeSelection, nodes]);
  const selectedEdge = useMemo(() => activeSelection?.type === 'edge' ? edges.find(e => e.id === activeSelection.id) : null, [activeSelection, edges]);
  const currentParentNode = useMemo(() => {
    if (!selectedNode) return null;
    const parentEdge = edges.find(e => e.source === selectedNode.id && e.label === 'extends');
    return parentEdge ? (nodes.find(n => n.id === parentEdge.target) ?? null) : null;
  }, [selectedNode, edges, nodes]);

  const handleParentChange = useCallback((nodeId: string, targetParentId: string) => {
    setEdges(eds => eds.filter(e => !(e.source === nodeId && e.label === 'extends')));
    if (targetParentId === 'none') return;

    const parentEdgeSpec: Edge = {
      id: `edge-inherit-${crypto.randomUUID()}`,
      source: nodeId,
      target: targetParentId,
      type: 'relationship',
      ...getEdgeStyle('extends')
    };
    setEdges(eds => applyParallelEdgeLayout(eds.concat(parentEdgeSpec)));
  }, [setEdges]);

  return (
    <div className="editor-window">
      <div className="canvas-pane">
        <ModellingToolbar
          relationshipType={relationshipType}
          setRelationshipType={setRelationshipType}
          addNewEntity={addNewEntity}
          autoLayout={autoLayout}
          clearAllNodesAndEdges={clearAllNodesAndEdges}
        />

        <div style={{ flexGrow: 1, width: '100%', height: '100%', position: 'relative', backgroundColor: 'var(--bg-canvas)' }}>
          <ReactFlow 
            nodes={nodes} 
            edges={edges} 
            onNodesChange={onNodesChange} 
            onEdgesChange={onEdgesChange} 
            onConnect={onConnect} 
            onInit={setReactFlowInstance}
            nodeTypes={nodeTypes}
            edgeTypes={edgeTypes}
            onSelectionChange={onSelectionChange}
            selectNodesOnDrag={false}
            fitView
          >
            <Background color="var(--border-main)" gap={20} size={1} />
            <Controls />
          </ReactFlow>
        </div>
      </div>

      <div className="developer-sidebar">
        <div className="sidebar-section-header">
          {activeSelection ? <Settings size={13} /> : <FileText size={13} />}
          {activeSelection ? `${activeSelection.type.toUpperCase()} PROPERTIES` : 'RAW PDL SCRIPT EDITOR'}
        </div>

        <div className="sidebar-main-scroller">
          {notification && (
            <div 
              className={`notification-banner ${notification.type}`} 
              style={{ 
                padding: '0.75rem', 
                margin: '1rem', 
                borderRadius: '4px', 
                fontSize: '0.85rem', 
                fontWeight: 500, 
                backgroundColor: notification.type === 'success' ? '#065f46' : '#991b1b', 
                color: '#fff', 
                display: 'flex', 
                justifyContent: 'space-between', 
                alignItems: 'center' 
              }}
            >
              <span>{notification.message}</span>
              <button 
                onClick={() => setNotification(null)} 
                style={{ 
                  background: 'none', 
                  border: 'none', 
                  color: '#fff', 
                  cursor: 'pointer', 
                  fontSize: '1rem', 
                  fontWeight: 'bold',
                  marginLeft: '0.5rem'
                }}
              >
                ×
              </button>
            </div>
          )}

          {selectedNode && (
            <EntityInspector
              selectedNode={selectedNode}
              nodes={nodes}
              currentParentNode={currentParentNode}
              updateNodeData={updateNodeData}
              handleParentChange={handleParentChange}
              deleteSelectedEntity={deleteSelectedEntity}
            />
          )}

          {selectedEdge && (
            <RelationshipInspector
              selectedEdge={selectedEdge}
              updateEdgeType={updateEdgeType}
              toggleEdgeDirection={toggleEdgeDirection}
              deleteSelectedEdge={deleteSelectedEdge}
            />
          )}

          {!activeSelection && (
            <div style={{ display: 'flex', flexDirection: 'column', height: '100%', width: '100%' }}>
              <div className="sidebar-editor-wrapper" style={{ height: '100%' }}>
                <textarea
                  value={inputJDL}
                  onChange={(e) => setInputJDL(e.target.value)}
                  className="code-editor-textarea"
                  placeholder={`// Write raw PDL schemas here...\nentity Customer {\n  name String\n}`}
                />
              </div>
            </div>
          )}
        </div>

        <SidebarControlBelt
          loadSample={loadSample}
          parseJDLToCanvas={parseJDLToCanvas}
          generateJavaCodeFromCanvas={generateJavaCodeFromCanvas}
          generateFullApplicationFromCanvas={generateFullApplicationFromCanvas}
          isGenerating={isGenerating}
        />
      </div>

      {showGenerationForm && (
        <div className="generation-modal-backdrop" role="presentation">
          <form className="generation-modal" onSubmit={submitFullApplicationGeneration}>
            <div className="generation-modal-header">
              <div>
                <p className="generation-modal-eyebrow">Application generation</p>
                <h2>Project configuration</h2>
              </div>
              <button
                type="button"
                className="generation-modal-close"
                onClick={() => setShowGenerationForm(false)}
                aria-label="Close generation form"
              >
                ×
              </button>
            </div>

            <div className="generation-form-grid">
              <label className="generation-form-field">
                <span>Application name</span>
                <input
                  value={generationOptions.applicationName}
                  onChange={(event) => updateGenerationOption('applicationName', event.target.value)}
                  placeholder="Generated App"
                />
              </label>

              <label className="generation-form-field">
                <span>Repository name</span>
                <input
                  value={generationOptions.repositoryName}
                  onChange={(event) => updateGenerationOption('repositoryName', event.target.value)}
                  placeholder="generated-app"
                />
              </label>

              <label className="generation-form-field generation-form-wide">
                <span>Default Java package name</span>
                <input
                  value={generationOptions.defaultJavaPackageName}
                  onChange={(event) => updateGenerationOption('defaultJavaPackageName', event.target.value)}
                  placeholder="com.mycompany.codeclassroom"
                />
              </label>
            </div>

            <div className="generation-form-section">
              <h3>Server side options</h3>
              <div className="generation-form-grid">
                <label className="generation-form-field">
                  <span>Java version</span>
                  <select
                    value={generationOptions.javaVersion}
                    onChange={(event) => updateGenerationOption('javaVersion', event.target.value)}
                  >
                    <option value="17">17</option>
                    <option value="21">21</option>
                  </select>
                </label>

                <label className="generation-form-field">
                  <span>Database</span>
                  <select
                    value={generationOptions.databaseType}
                    onChange={(event) => updateGenerationOption('databaseType', event.target.value)}
                  >
                    <option value="postgresql">PostgreSQL</option>
                    <option value="mysql">MySQL</option>
                    <option value="mariadb">MariaDB</option>
                    <option value="h2Disk">H2 disk</option>
                  </select>
                </label>

                <label className="generation-form-field">
                  <span>Authentication</span>
                  <select
                    value={generationOptions.authenticationType}
                    onChange={(event) => updateGenerationOption('authenticationType', event.target.value)}
                  >
                    <option value="jwt">JWT</option>
                    <option value="session">Session</option>
                    <option value="oauth2">OAuth 2</option>
                  </select>
                </label>

                <label className="generation-form-field">
                  <span>Build tool</span>
                  <select
                    value={generationOptions.buildTool}
                    onChange={(event) => updateGenerationOption('buildTool', event.target.value)}
                  >
                    <option value="maven">Maven</option>
                    <option value="gradle">Gradle</option>
                  </select>
                </label>
              </div>
            </div>

            <div className="generation-modal-actions">
              <button type="button" className="btn-generation-secondary" onClick={() => setShowGenerationForm(false)}>
                Cancel
              </button>
              <button type="submit" className="btn-generation-primary" disabled={isGenerating}>
                Generate Application
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
