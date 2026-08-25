import { useState, useCallback, useMemo } from 'react';
import {
  ReactFlow,
  MiniMap,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  MarkerType,
  type Connection,
  type Edge,
  type Node,
} from '@xyflow/react';
import { Settings, FileText } from 'lucide-react';
import '@xyflow/react/dist/style.css';

import { type Field, type Method, type EntityNode, type RelationshipType } from '../types/modeling';
import { EntityNodeComponent } from './EntityNodeComponent';
import { generateProject } from '../api/generatorApi';

// Extracted UI Components
import ModellingToolbar from './ModellingToolbar';
import EntityInspector from './EntityInspector';
import RelationshipInspector from './RelationshipInspector';
import SidebarControlBelt from './SidebarControlBelt';

const nodeTypes = { entityNode: EntityNodeComponent };

// ponytail: pure helper to avoid duplicate edge styling rules across 4 locations
const getEdgeStyle = (typeLabel: string) => {
  const isInheritance = typeLabel === 'Inheritance' || typeLabel === 'extends';
  const isImplementation = typeLabel === 'Implementation' || typeLabel === 'implements';
  const color = isInheritance ? '#ef4444' : isImplementation ? '#059669' : '#6366f1';
  return {
    label: isInheritance ? 'extends' : isImplementation ? 'implements' : typeLabel,
    style: isInheritance || isImplementation ? { strokeDasharray: '5,5', strokeWidth: 2, stroke: color } : { strokeWidth: 2, stroke: color },
    markerEnd: { type: MarkerType.ArrowClosed, color }
  };
};

const isMethodLine = (line: string) => line.includes('(') && line.includes(')');

export default function ModellingEditor() {
  const [nodes, setNodes, onNodesChange] = useNodesState<EntityNode>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const [relationshipType, setRelationshipType] = useState<RelationshipType>('ManyToMany');
  const [activeSelection, setActiveSelection] = useState<{ type: 'node' | 'edge'; id: string } | null>(null);

  const [inputJDL, setInputJDL] = useState<string>(
    `entity Student {\n  email String\n}\n\nentity Course {\n  title String\n}\n\nrelationship ManyToMany {\n  Student to Course\n}`
  );

  const [isGenerating, setIsGenerating] = useState(false);
  const [notification, setNotification] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

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
    setEdges((eds) => eds.map((e) => {
      if (e.id !== edgeId) return e;
      return {
        ...e,
        ...getEdgeStyle(typeLabel)
      };
    }));
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

  const addNewInterface = useCallback(() => {
    const id = crypto.randomUUID();
    const newNode: EntityNode = {
      id,
      type: 'entityNode',
      position: { x: 150 + Math.random() * 100, y: 150 + Math.random() * 100 },
      data: { label: `NewInterface${nodes.length + 1}`, kind: 'interface', abstract: false, fields: [], methods: [] },
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

  const loadComplexJDLScriptSample = useCallback(() => {
    setInputJDL(`entity Professor {\n  name String\n  email String\n}\n\nentity Department {\n  title String\n  budget BigDecimal\n}\n\nentity GraduateProject {\n  topic String\n  deadline LocalDate\n}\n\nrelationship Inheritance {\n  Department to Professor\n}\n\nrelationship OneToOne {\n  Professor to GraduateProject\n}`);
  }, []);

  const generateJDLFromCanvas = useCallback(async () => {
    let jdlString = '';
    const inheritanceMap: Record<string, string> = {};
    const implementsMap: Record<string, string[]> = {};

    edges.forEach((e) => {
      if (e.label === 'extends') {
        const parentNode = nodes.find((n) => n.id === e.target);
        if (parentNode) inheritanceMap[e.source] = parentNode.data.label;
      } else if (e.label === 'implements') {
        const interfaceNode = nodes.find((n) => n.id === e.target && n.data.kind === 'interface');
        if (interfaceNode) {
          implementsMap[e.source] = [...(implementsMap[e.source] || []), interfaceNode.data.label];
        }
      }
    });

    nodes.forEach((n) => {
      const methods = n.data.methods || [];
      const parentName = inheritanceMap[n.id];
      const extendsClause = parentName ? ` extends ${parentName}` : '';
      if (n.data.kind === 'interface') {
        jdlString += `interface ${n.data.label}${extendsClause} {\n`;
        methods.forEach((m) => { jdlString += `  ${m.definition}\n`; });
        jdlString += `}\n\n`;
        return;
      }

      const abstractPrefix = n.data.abstract ? 'abstract ' : '';
      const implementsList = implementsMap[n.id] || [];
      const implementsClause = implementsList.length > 0 ? ` implements ${implementsList.join(', ')}` : '';
      jdlString += `${abstractPrefix}entity ${n.data.label}${extendsClause}${implementsClause} {\n`;
      n.data.fields.forEach((f) => { jdlString += `  ${f.name} ${f.type}\n`; });
      methods.forEach((m) => { jdlString += `  ${m.definition}\n`; });
      jdlString += `}\n\n`;
    });

    ['OneToOne', 'OneToMany', 'ManyToMany'].forEach((type) => {
      const edgeList = edges.filter((e) => e.label === type);
      if (edgeList.length === 0) return;
      jdlString += `relationship ${type} {\n`;
      edgeList.forEach((e) => {
        const s = nodes.find((n) => n.id === e.source);
        const t = nodes.find((n) => n.id === e.target);
        if (s && t) {
          jdlString += type === 'ManyToMany'
            ? `  ${s.data.label}{${t.data.label.toLowerCase()}s} to ${t.data.label}{${s.data.label.toLowerCase()}s}\n`
            : `  ${s.data.label} to ${t.data.label}\n`;
        }
      });
      jdlString += `}\n\n`;
    });

    const finalCDL = jdlString.trim();
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
      const blob = await generateProject(finalCDL);
      
      const downloadUrl = window.URL.createObjectURL(blob);
      const downloadLink = document.createElement('a');
      downloadLink.href = downloadUrl;
      downloadLink.download = 'generated-project.zip';
      document.body.appendChild(downloadLink);
      downloadLink.click();
      document.body.removeChild(downloadLink);
      window.URL.revokeObjectURL(downloadUrl);

      setNotification({
        type: 'success',
        message: 'Project generated successfully.'
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

  const parseJDLToCanvas = useCallback(() => {
    try {
      const parsedNodes: EntityNode[] = [];
      const parsedEdges: Edge[] = [];

      const entityBlocks = inputJDL.match(/(?:abstract\s+)?entity\s+\w+(?:\s+extends\s+\w+)?(?:\s+implements\s+[\w\s,]+)?\s*\{[^}]*\}|interface\s+\w+(?:\s+extends\s+[\w\s,]+)?\s*\{[^}]*\}/g);
      
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
            const parts = line.split(/\s+/);
            if (parts.length >= 2) {
              fields.push({ id: crypto.randomUUID(), name: parts[0], type: parts[1] });
            } else if (parts.length === 1 && parts[0]) {
              fields.push({ id: crypto.randomUUID(), name: parts[0], type: 'String' });
            }
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
              type: 'default',
              ...getEdgeStyle('extends')
            });
          });

          implementedNames.forEach((interfaceName) => {
            parsedEdges.push({
              id: `edge-implements-${crypto.randomUUID()}`,
              source: entityId,
              target: interfaceName.toLowerCase() + '-id',
              type: 'default',
              ...getEdgeStyle('implements')
            });
          });
        });
      }

      const relationshipBlocks = inputJDL.match(/relationship\s+(\w+)\s*\{([^}]*)\}/g);
      if (relationshipBlocks) {
        relationshipBlocks.forEach(block => {
          const match = block.match(/relationship\s+(\w+)\s*\{([^}]*)\}/);
          if (!match) return;

          const [, relType, contentBody] = match;
          contentBody.split('\n').map(l => l.trim()).filter(Boolean).forEach(line => {
            const linkMatch = line.match(/(\w+)(?:\{.*\})?\s+to\s+(\w+)/);
            if (linkMatch) {
              const [, sourceEnt, targetEnt] = linkMatch;
              parsedEdges.push({
                id: `edge-rel-${crypto.randomUUID()}`,
                source: sourceEnt.toLowerCase() + '-id',
                target: targetEnt.toLowerCase() + '-id',
                type: 'default',
                ...getEdgeStyle(relType)
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
      setEdges(parsedEdges);
      setActiveSelection(null);
    } catch {
      alert("Parsing Error: Verify your JDL configuration layout syntax.");
    }
  }, [inputJDL, setNodes, setEdges]);

  const onConnect = useCallback((params: Connection) => {
    const stylizedEdge: Edge = {
      id: `edge-${crypto.randomUUID()}`,
      source: params.source,
      target: params.target,
      sourceHandle: params.sourceHandle,
      targetHandle: params.targetHandle,
      type: 'default',
      ...getEdgeStyle(relationshipType)
    };
    setEdges((eds) => addEdge(stylizedEdge, eds));
  }, [relationshipType, setEdges]);

  const toggleEdgeDirection = useCallback((edgeId: string) => {
    setEdges((eds) => eds.map((e) => (e.id === edgeId ? { ...e, source: e.target, target: e.source } : e)));
  }, [setEdges]);

  const downloadJDLHandler = useCallback(() => {
    const element = document.createElement('a');
    element.href = URL.createObjectURL(new Blob([inputJDL], { type: 'text/plain' }));
    element.download = 'model.jdl';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  }, [inputJDL]);

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
      type: 'default',
      ...getEdgeStyle('extends')
    };
    setEdges(eds => eds.concat(parentEdgeSpec));
  }, [setEdges]);

  return (
    <div className="editor-window">
      <div className="canvas-pane">
        <ModellingToolbar
          relationshipType={relationshipType}
          setRelationshipType={setRelationshipType}
          addNewEntity={addNewEntity}
          addNewInterface={addNewInterface}
          clearAllNodesAndEdges={clearAllNodesAndEdges}
        />

        <div style={{ flexGrow: 1, width: '100%', height: '100%', position: 'relative', backgroundColor: 'var(--bg-canvas)' }}>
          <ReactFlow 
            nodes={nodes} 
            edges={edges} 
            onNodesChange={onNodesChange} 
            onEdgesChange={onEdgesChange} 
            onConnect={onConnect} 
            nodeTypes={nodeTypes}
            onSelectionChange={onSelectionChange}
            selectNodesOnDrag={false}
            fitView
          >
            <Background color="var(--border-main)" gap={20} size={1} />
            <Controls />
            <MiniMap nodeColor="#1e1b4b" maskColor="var(--shadow-small)" style={{ backgroundColor: 'var(--bg-card)', border: '1px solid var(--border-main)' }} />
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
          loadComplexJDLScriptSample={loadComplexJDLScriptSample}
          parseJDLToCanvas={parseJDLToCanvas}
          generateJDLFromCanvas={generateJDLFromCanvas}
          downloadJDLHandler={downloadJDLHandler}
          isGenerating={isGenerating}
        />
      </div>
    </div>
  );
}
