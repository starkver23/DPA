import React, { useState, useCallback, useMemo } from 'react';
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
import { Plus, Trash2, Layers, ArrowLeftRight, Settings, Type, LayoutGrid, FileText } from 'lucide-react';
import { Link } from 'react-router-dom';
import '@xyflow/react/dist/style.css';

import { type Field, type EntityNode, type RelationshipType } from '../types/modeling';
import { DESIGN_PATTERN_TEMPLATES } from '../constants/patterns';
import { EntityNodeComponent } from './EntityNodeComponent';

const nodeTypes = { entityNode: EntityNodeComponent };

export default function ModellingEditor() {
  const [nodes, setNodes, onNodesChange] = useNodesState<EntityNode>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const [relationshipType, setRelationshipType] = useState<RelationshipType>('ManyToMany');
  const [activeSelection, setActiveSelection] = useState<{ type: 'node' | 'edge'; id: string } | null>(null);

  const [inputJDL, setInputJDL] = useState<string>(
    `entity Student {\n  email String\n}\n\nentity Course {\n  title String\n}\n\nrelationship ManyToMany {\n  Student to Course\n}`
  );

  const handleLoadPatternTemplate = (patternName: string) => {
    if (!patternName) return;
    const blueprint = DESIGN_PATTERN_TEMPLATES[patternName];
    if (blueprint) {
      setNodes(structuredClone(blueprint.nodes));
      setEdges(structuredClone(blueprint.edges));
      setActiveSelection(null);
      
      const structuralInfo = `// Interactive Template: Loaded ${patternName} Pattern.\n// Modify fields/methods or extend elements on the canvas workspace, then click Generate.`;
      setInputJDL(structuralInfo);
    }
  };

  const onSelectionChange = useCallback(({ nodes: selectedNodes, edges: selectedEdges }: { nodes: Node[]; edges: Edge[] }) => {
    if (selectedNodes.length > 0) {
      setActiveSelection({ type: 'node', id: selectedNodes[0].id });
    } else if (selectedEdges.length > 0) {
      setActiveSelection({ type: 'edge', id: selectedEdges[0].id });
    } else {
      setActiveSelection(null);
    }
  }, []);

  const updateNodeLabel = useCallback((id: string, label: string) => {
    setNodes((nds) => nds.map((n) => (n.id === id ? { ...n, data: { ...n.data, label } } : n)));
  }, [setNodes]);

  const addFieldToNode = useCallback((id: string) => {
    setNodes((nds) => nds.map((n) => {
      if (n.id !== id) return n;
      const currentFields = n.data.fields || [];
      return {
        ...n,
        data: {
          ...n.data,
          fields: [...currentFields, { id: crypto.randomUUID(), name: `field${currentFields.length + 1}`, type: 'String' }]
        }
      };
    }));
  }, [setNodes]);

  const updateNodeField = useCallback((nodeId: string, fieldId: string, updated: Partial<Field>) => {
    setNodes((nds) => nds.map((n) => (n.id === nodeId ? { ...n, data: { ...n.data, fields: n.data.fields.map((f) => (f.id === fieldId ? { ...f, ...updated } : f)) } } : n)));
  }, [setNodes]);

  const deleteNodeField = useCallback((nodeId: string, fieldId: string) => {
    setNodes((nds) => nds.map((n) => (n.id === nodeId ? { ...n, data: { ...n.data, fields: n.data.fields.filter((f) => f.id !== fieldId) } } : n)));
  }, [setNodes]);

  const addMethodToNode = useCallback((id: string) => {
    setNodes((nds) => nds.map((n) => {
      if (n.id !== id) return n;
      const currentMethods = n.data.methods || [];
      return {
        ...n,
        data: {
          ...n.data,
          methods: [...currentMethods, { id: crypto.randomUUID(), definition: `operation${currentMethods.length + 1}()` }]
        }
      };
    }));
  }, [setNodes]);

  const updateNodeMethod = useCallback((nodeId: string, methodId: string, definition: string) => {
    setNodes((nds) => nds.map((n) => (n.id === nodeId ? { ...n, data: { ...n.data, methods: (n.data.methods || []).map((m) => (m.id === methodId ? { ...m, definition } : m)) } } : n)));
  }, [setNodes]);

  const deleteNodeMethod = useCallback((nodeId: string, methodId: string) => {
    setNodes((nds) => nds.map((n) => (n.id === nodeId ? { ...n, data: { ...n.data, methods: (n.data.methods || []).filter((m) => m.id !== methodId) } } : n)));
  }, [setNodes]);

  const updateEdgeType = useCallback((edgeId: string, typeLabel: string) => {
    const isInheritance = typeLabel === 'Inheritance' || typeLabel === 'extends';
    setEdges((eds) => eds.map((e) => {
      if (e.id !== edgeId) return e;
      return {
        ...e,
        label: isInheritance ? 'extends' : typeLabel,
        style: isInheritance ? { strokeDasharray: '5,5', strokeWidth: 2, stroke: '#ef4444' } : { strokeWidth: 2, stroke: '#6366f1' },
        markerEnd: { type: MarkerType.ArrowClosed, color: isInheritance ? '#ef4444' : '#6366f1' }
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
      data: { label: `NewEntity${nodes.length + 1}`, fields: [], methods: [] },
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
    setInputJDL(`entity Professor {\n  name String\n  department String\n}\n\nentity Department {\n  title String\n  budget BigDecimal\n}\n\nentity GraduateProject {\n  topic String\n  deadline LocalDate\n}\n\nrelationship OneToMany {\n  Department to Professor\n}\n\nrelationship OneToOne {\n  Professor to GraduateProject\n}`);
  }, []);

  const generateJDLFromCanvas = useCallback(() => {
    let jdlString = '';
    const inheritanceMap: Record<string, string> = {};

    edges.forEach((e) => {
      if (e.label === 'extends') {
        const parentNode = nodes.find((n) => n.id === e.target);
        if (parentNode) inheritanceMap[e.source] = parentNode.data.label;
      }
    });

    nodes.forEach((n) => {
      const parentName = inheritanceMap[n.id];
      const extendsClause = parentName ? ` extends ${parentName}` : '';
      jdlString += `entity ${n.data.label}${extendsClause} {\n`;
      n.data.fields.forEach((f) => { jdlString += `  ${f.name} ${f.type}\n`; });
      jdlString += `}\n\n`;
    });

    const relationsMap: Record<string, Edge[]> = { OneToOne: [], OneToMany: [], ManyToMany: [] };
    edges.forEach((e) => {
      if (e.label !== 'extends' && String(e.label) in relationsMap) {
        relationsMap[String(e.label)].push(e);
      }
    });

    Object.entries(relationsMap).forEach(([type, edgeList]) => {
      if (edgeList.length === 0) return;
      jdlString += `relationship ${type} {\n`;
      edgeList.forEach((e) => {
        const s = nodes.find((n) => n.id === e.source);
        const t = nodes.find((n) => n.id === e.target);
        if (s && t) {
          if (type === 'ManyToMany') {
            jdlString += `  ${s.data.label}{${t.data.label.toLowerCase()}s} to ${t.data.label}{${s.data.label.toLowerCase()}s}\n`;
          } else {
            jdlString += `  ${s.data.label} to ${t.data.label}\n`;
          }
        }
      });
      jdlString += `}\n\n`;
    });

    setInputJDL(jdlString.trim() || '// No visual structure configured.');
    setActiveSelection(null); // Forces the sidebar view to toggle directly back into the code text editor view
  }, [nodes, edges]);

  const parseJDLToCanvas = useCallback(() => {
    try {
      const parsedNodes: EntityNode[] = [];
      const parsedEdges: Edge[] = [];

      const entityBlocks = inputJDL.match(/entity\s+(\w+)(?:\s+extends\s+(\w+))?\s*\{([^}]*)\}/g);
      
      if (entityBlocks) {
        entityBlocks.forEach((block, index) => {
          const match = block.match(/entity\s+(\w+)(?:\s+extends\s+(\w+))?\s*\{([^}]*)\}/);
          if (!match) return;

          const [, entityName, parentName, contentBody] = match;
          const fields: Field[] = [];

          contentBody.split('\n').map(l => l.trim()).filter(Boolean).forEach(line => {
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
            data: { label: entityName, fields, methods: [] }
          });

          if (parentName) {
            parsedEdges.push({
              id: `edge-inherit-${crypto.randomUUID()}`,
              source: entityId,
              target: parentName.toLowerCase() + '-id',
              label: 'extends',
              type: 'default',
              style: { strokeDasharray: '5,5', strokeWidth: 2, stroke: '#ef4444' },
              markerEnd: { type: MarkerType.ArrowClosed, color: '#ef4444' }
            });
          }
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
                label: relType,
                type: 'default',
                style: { strokeWidth: 2, stroke: '#6366f1' },
                markerEnd: { type: MarkerType.ArrowClosed, color: '#6366f1' }
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
    } catch (err) {
      alert("Parsing Error: Verify your JDL configuration layout syntax.");
    }
  }, [inputJDL, setNodes, setEdges]);

  const onConnect = useCallback((params: Connection) => {
    const isInheritance = relationshipType === 'Inheritance';
    const edgeId = `edge-${crypto.randomUUID()}`;

    const stylizedEdge: Edge = {
      id: edgeId,
      source: params.source,
      target: params.target,
      sourceHandle: params.sourceHandle,
      targetHandle: params.targetHandle,
      label: isInheritance ? 'extends' : relationshipType,
      type: 'default',
      style: isInheritance ? { strokeDasharray: '5,5', strokeWidth: 2, stroke: '#ef4444' } : { strokeWidth: 2, stroke: '#6366f1' },
      markerEnd: { type: MarkerType.ArrowClosed, color: isInheritance ? '#ef4444' : '#6366f1' }
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
    return parentEdge ? nodes.find(n => n.id === parentEdge.target) : null;
  }, [selectedNode, edges, nodes]);

  const handleParentChange = useCallback((nodeId: string, targetParentId: string) => {
    setEdges(eds => eds.filter(e => !(e.source === nodeId && e.label === 'extends')));
    if (targetParentId === 'none') return;

    const edgeId = `edge-inherit-${crypto.randomUUID()}`;
    const parentEdgeSpec: Edge = {
      id: edgeId,
      source: nodeId,
      target: targetParentId,
      label: 'extends',
      type: 'default',
      style: { strokeDasharray: '5,5', strokeWidth: 2, stroke: '#ef4444' },
      markerEnd: { type: MarkerType.ArrowClosed, color: '#ef4444' }
    };
    setEdges(eds => eds.concat(parentEdgeSpec));
  }, [setEdges]);

  return (
    <div className="editor-window">
      <div className="canvas-pane">
        <div className="navbar-controls">
          <div className="nav-brand-area">
            <Link to="/" className="btn-back">← Home Hub</Link>
            <div className="nav-titles">
              <span className="editor-title">CodeClassroom</span>
              <span className="editor-tag">JDL Engine v2.0</span>
            </div>
          </div>

          <div className="toolbar-actions">
            <div className="selector-box custom-pattern-dropdown-wrapper">
              <LayoutGrid size={13} className="pattern-decorator-icon" />
              <select
                defaultValue=""
                onChange={(e) => {
                  handleLoadPatternTemplate(e.target.value);
                  e.target.value = "";
                }}
                className="select-dropdown pattern-select-menu"
              >
                <option value="" disabled>Examples / Patterns</option>
                <option value="Singleton">Singleton</option>
                <option value="Factory">Factory</option>
                <option value="Observer">Observer</option>
                <option value="Decorator">Decorator</option>
                <option value="Strategy">Strategy</option>
              </select>
            </div>

            <div className="selector-box">
              <Layers size={13} style={{ color: '#475569' }} /> 
              <span>Link Edge:</span>
              <select value={relationshipType} onChange={(e) => setRelationshipType(e.target.value as RelationshipType)} className="select-dropdown">
                <option value="ManyToMany">ManyToMany</option>
                <option value="OneToMany">OneToMany</option>
                <option value="OneToOne">OneToOne</option>
                <option value="Inheritance">Inheritance</option>
              </select>
            </div>

            <button onClick={addNewEntity} className="btn-action-green">
              <Plus size={15} /> Add Entity
            </button>

            <button onClick={clearAllNodesAndEdges} className="btn-action-red">
              <Trash2 size={14} /> Clear Canvas
            </button>
          </div>
        </div>

        <div style={{ flexGrow: 1, width: '100%', height: '100%', position: 'relative', backgroundColor: '#0a0e17' }}>
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
            <Background color="#1e293b" gap={20} size={1} />
            <Controls />
            <MiniMap nodeColor="#1e1b4b" maskColor="rgba(5, 7, 12, 0.7)" style={{ backgroundColor: '#0f172a', border: '1px solid #1e293b' }} />
          </ReactFlow>
        </div>
      </div>

      <div className="developer-sidebar">
        <div className="sidebar-section-header">
          {activeSelection ? <Settings size={13} /> : <FileText size={13} />}
          {activeSelection ? `${activeSelection.type.toUpperCase()} PROPERTIES` : 'RAW CDL SCRIPT EDITOR'}
        </div>

        <div className="sidebar-main-scroller">
          {selectedNode && (
            <div className="inspector-container">
              <div className="inspector-group">
                <label className="inspector-field-label">Entity Name</label>
                <div className="inspector-input-wrapper">
                  <Type size={12} className="input-decorator-icon" />
                  <input
                    type="text"
                    value={selectedNode.data.label}
                    onChange={(e) => updateNodeLabel(selectedNode.id, e.target.value)}
                    className="inspector-text-input"
                    placeholder="ClassName"
                  />
                </div>
              </div>

              <div className="inspector-group" style={{ marginTop: '0.75rem' }}>
                <label className="inspector-field-label">Extends Parent Class</label>
                <select
                  value={currentParentNode ? currentParentNode.id : 'none'}
                  onChange={(e) => handleParentChange(selectedNode.id, e.target.value)}
                  className="inspector-select-large"
                >
                  <option value="none">-- None (Root Entity) --</option>
                  {nodes
                    .filter((n) => n.id !== selectedNode.id)
                    .map((n) => (
                      <option key={n.id} value={n.id}>{n.data.label}</option>
                    ))}
                </select>
              </div>

              <div className="inspector-group border-top-divider" style={{ marginTop: '1.25rem', paddingTop: '1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <label className="inspector-field-label">Fields / Attributes</label>
                  <button onClick={() => addFieldToNode(selectedNode.id)} className="btn-inspector-add">+ Field</button>
                </div>
                {selectedNode.data.fields.length === 0 && <p className="uml-empty-text">No fields mapped on this target block.</p>}
                {selectedNode.data.fields.map((f) => (
                  <div key={f.id} className="inspector-row-item">
                    <input
                      type="text"
                      value={f.name}
                      placeholder="attribute"
                      onChange={(e) => updateNodeField(selectedNode.id, f.id, { name: e.target.value })}
                      className="inspector-row-input"
                    />
                    <select
                      value={f.type}
                      onChange={(e) => updateNodeField(selectedNode.id, f.id, { type: e.target.value })}
                      className="inspector-row-select"
                    >
                      <option value="String">String</option>
                      <option value="Integer">Integer</option>
                      <option value="Long">Long</option>
                      <option value="BigDecimal">BigDecimal</option>
                      <option value="LocalDate">LocalDate</option>
                      <option value="Boolean">Boolean</option>
                    </select>
                    <button onClick={() => deleteNodeField(selectedNode.id, f.id)} className="btn-row-delete">
                      <Trash2 size={11} />
                    </button>
                  </div>
                ))}
              </div>

              <div className="inspector-group border-top-divider" style={{ marginTop: '1.25rem', paddingTop: '1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <label className="inspector-field-label">Operations / Methods</label>
                  <button onClick={() => addMethodToNode(selectedNode.id)} className="btn-inspector-add">+ Method</button>
                </div>
                {(!selectedNode.data.methods || selectedNode.data.methods.length === 0) && <p className="uml-empty-text">No operational logic defined.</p>}
                {selectedNode.data.methods?.map((m) => (
                  <div key={m.id} className="inspector-row-item">
                    <input
                      type="text"
                      value={m.definition}
                      placeholder="operation()"
                      onChange={(e) => updateNodeMethod(selectedNode.id, m.id, e.target.value)}
                      className="inspector-row-input"
                      style={{ width: '85%' }}
                    />
                    <button onClick={() => deleteNodeMethod(selectedNode.id, m.id)} className="btn-row-delete">
                      <Trash2 size={11} />
                    </button>
                  </div>
                ))}
              </div>

              <div className="inspector-group border-top-divider" style={{ marginTop: '1.5rem', paddingTop: '1rem' }}>
                <button onClick={() => deleteSelectedEntity(selectedNode.id)} className="btn-inspector-delete-entity">
                  <Trash2 size={12} /> Delete Entity Diagram Block
                </button>
              </div>
            </div>
          )}

          {selectedEdge && (
            <div className="inspector-container">
              <div className="inspector-group">
                <label className="inspector-field-label">Relationship Line Mapping Pattern</label>
                <select
                  value={selectedEdge.label === 'extends' ? 'Inheritance' : String(selectedEdge.label)}
                  onChange={(e) => updateEdgeType(selectedEdge.id, e.target.value)}
                  className="inspector-select-large"
                >
                  <option value="ManyToMany">ManyToMany</option>
                  <option value="OneToMany">OneToMany</option>
                  <option value="OneToOne">OneToOne</option>
                  <option value="Inheritance">Inheritance (Extends Pointer)</option>
                </select>
              </div>

              <div className="inspector-group" style={{ marginTop: '1rem' }}>
                <button onClick={() => toggleEdgeDirection(selectedEdge.id)} className="btn-sidebar-action btn-slate" style={{ width: '100%', display: 'flex', gap: '0.5rem', justifyContent: 'center', alignItems: 'center' }}>
                  <ArrowLeftRight size={12} /> Flip Cardinality Vector Direction
                </button>
              </div>

              <div className="inspector-group border-top-divider" style={{ marginTop: '1.5rem', paddingTop: '1rem' }}>
                <button onClick={() => deleteSelectedEdge(selectedEdge.id)} className="btn-inspector-delete-entity">
                  <Trash2 size={12} /> Sever Association Path
                </button>
              </div>
            </div>
          )}

          {!activeSelection && (
            <div style={{ display: 'flex', flexDirection: 'column', height: '100%', width: '100%' }}>
              <div className="sidebar-editor-wrapper" style={{ height: '100%' }}>
                <textarea
                  value={inputJDL}
                  onChange={(e) => setInputJDL(e.target.value)}
                  className="code-editor-textarea"
                  placeholder={`// Write raw CDL schemas here...\nentity Customer {\n  name String\n}`}
                />
              </div>
            </div>
          )}
        </div>

        <div className="sidebar-control-belt">
          <button onClick={loadComplexJDLScriptSample} className="btn-sidebar-action btn-slate">
            Load Sample
          </button>
          <button onClick={parseJDLToCanvas} className="btn-sidebar-action btn-amber">
            Parse CDL
          </button>
          <button onClick={generateJDLFromCanvas} className="btn-sidebar-action btn-green">
            Generate
          </button>
          <button onClick={downloadJDLHandler} className="btn-sidebar-action btn-blue">
            Export
          </button>
        </div>
      </div>
    </div>
  );
}