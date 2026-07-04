import React, { useState, useCallback } from 'react';
import {
  ReactFlow,
  MiniMap,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  Handle,
  Position,
  MarkerType,
  type Connection,
  type Edge,
  type Node,
  type NodeProps,
} from '@xyflow/react';
import { Plus, Trash2, Code, Download, RefreshCw, Layers, ArrowLeftRight, Play } from 'lucide-react';
import { Link } from 'react-router-dom';
import '@xyflow/react/dist/style.css';

interface Field {
  id: string;
  name: string;
  type: string;
}

interface Method {
  id: string;
  definition: string;
}

interface EntityData extends Record<string, unknown> {
  label: string;
  fields: Field[];
  methods: Method[];
  onUpdateField: (entityId: string, fieldId: string, updatedField: Partial<Field>) => void;
  onAddField: (entityId: string) => void;
  onDeleteField: (entityId: string, fieldId: string) => void;
  onUpdateMethod: (entityId: string, methodId: string, value: string) => void;
  onAddMethod: (entityId: string) => void;
  onDeleteMethod: (entityId: string, methodId: string) => void;
  onDeleteEntity: (entityId: string) => void;
  onUpdateName: (entityId: string, newName: string) => void;
}

type EntityNode = Node<EntityData>;
type RelationshipType = 'OneToOne' | 'OneToMany' | 'ManyToMany' | 'Inheritance';

const EntityNodeComponent = ({ id, data }: NodeProps<EntityNode>) => {
  return (
    <div className="entity-node-container">
      <div className="node-header">
        <input
          type="text"
          value={data.label}
          onChange={(e) => data.onUpdateName(id, e.target.value)}
          className="node-header-input"
        />
        <button onClick={() => data.onDeleteEntity(id)} className="btn-node-delete" title="Delete Entity">
          <Trash2 size={14} />
        </button>
      </div>

      <Handle type="target" position={Position.Top} style={{ width: 12, height: 12, background: '#6366f1' }} />

      <div className="node-fields-list">
        {data.fields.length === 0 && (
          <p className="empty-fields-text">No database attributes</p>
        )}
        {data.fields.map((field) => (
          <div key={field.id} className="field-row">
            <input
              type="text"
              value={field.name}
              placeholder="attribute"
              onChange={(e) => data.onUpdateField(id, field.id, { name: e.target.value })}
              className="field-name-input"
            />
            <select
              value={field.type}
              onChange={(e) => data.onUpdateField(id, field.id, { type: e.target.value })}
              className="field-type-select"
            >
              <option value="String">String</option>
              <option value="Integer">Integer</option>
              <option value="Long">Long</option>
              <option value="BigDecimal">BigDecimal</option>
              <option value="LocalDate">LocalDate</option>
              <option value="Boolean">Boolean</option>
            </select>
            <button onClick={() => data.onDeleteField(id, field.id)} className="btn-node-delete" style={{ padding: '0.125rem' }}>
              <Trash2 size={12} />
            </button>
          </div>
        ))}
      </div>

      <div className="node-methods-list">
        <p className="inspector-lbl" style={{ margin: '2px 0', fontSize: '9px', color: '#64748b' }}>Class Functions</p>
        {data.methods?.length === 0 && (
          <p className="empty-fields-text" style={{ margin: '2px 0' }}>No service operations defined</p>
        )}
        {data.methods?.map((method) => (
          <div key={method.id} className="method-row">
            <input
              type="text"
              value={method.definition}
              placeholder="calculateTax(amt: BigDecimal)"
              onChange={(e) => data.onUpdateMethod(id, method.id, e.target.value)}
              className="method-input"
            />
            <button onClick={() => data.onDeleteMethod(id, method.id)} className="btn-node-delete" style={{ padding: '0.125rem' }}>
              <Trash2 size={12} />
            </button>
          </div>
        ))}
      </div>

      <div className="node-footer" style={{ gap: '0.375rem' }}>
        <button onClick={() => data.onAddMethod(id)} className="btn-append-field" style={{ backgroundColor: 'rgba(217,119,6,0.08)', borderColor: 'rgba(217,119,6,0.3)', color: '#b45309' }}>
          + Add Method
        </button>
        <button onClick={() => data.onAddField(id)} className="btn-append-field">
          + Add Field
        </button>
      </div>

      <Handle type="source" position={Position.Bottom} style={{ width: 12, height: 12, background: '#6366f1' }} />
    </div>
  );
};

const nodeTypes = { entityNode: EntityNodeComponent };

export default function ModellingEditor() {
  const [nodes, setNodes, onNodesChange] = useNodesState<EntityNode>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const [relationshipType, setRelationshipType] = useState<RelationshipType>('ManyToMany');
  
  const [inputJDL, setInputJDL] = useState<string>(
    `entity Student {\n  email String\n  registerStudent()\n}\n\nentity Course {\n  title String\n  archiveCourse()\n}\n\nrelationship ManyToMany {\n  Student to Course\n}`
  );
  const [generatedJDL, setGeneratedJDL] = useState<string>('// Code visualised successfully.');

  const handleUpdateName = useCallback((entityId: string, newName: string) => {
    setNodes((nds) => nds.map((n) => (n.id === entityId ? { ...n, data: { ...n.data, label: newName } } : n)));
  }, [setNodes]);

  const handleAddField = useCallback((entityId: string) => {
    setNodes((nds) => nds.map((n) => (n.id === entityId ? { ...n, data: { ...n.data, fields: [...n.data.fields, { id: crypto.randomUUID(), name: `field${n.data.fields.length + 1}`, type: 'String' }] } } : n)));
  }, [setNodes]);

  const handleUpdateField = useCallback((entityId: string, fieldId: string, updatedField: Partial<Field>) => {
    setNodes((nds) => nds.map((n) => (n.id === entityId ? { ...n, data: { ...n.data, fields: n.data.fields.map((f) => (f.id === fieldId ? { ...f, ...updatedField } : f)) } } : n)));
  }, [setNodes]);

  const handleDeleteField = useCallback((entityId: string, fieldId: string) => {
    setNodes((nds) => nds.map((n) => (n.id === entityId ? { ...n, data: { ...n.data, fields: n.data.fields.filter((f) => f.id !== fieldId) } } : n)));
  }, [setNodes]);

  const handleAddMethod = useCallback((entityId: string) => {
    setNodes((nds) => nds.map((n) => (n.id === entityId ? { ...n, data: { ...n.data, methods: [...(n.data.methods || []), { id: crypto.randomUUID(), definition: `newMethod${(n.data.methods || []).length + 1}()` }] } } : n)));
  }, [setNodes]);

  const handleUpdateMethod = useCallback((entityId: string, methodId: string, value: string) => {
    setNodes((nds) => nds.map((n) => (n.id === entityId ? { ...n, data: { ...n.data, methods: (n.data.methods || []).map((m) => (m.id === methodId ? { ...m, definition: value } : m)) } } : n)));
  }, [setNodes]);

  const handleDeleteMethod = useCallback((entityId: string, methodId: string) => {
    setNodes((nds) => nds.map((n) => (n.id === entityId ? { ...n, data: { ...n.data, methods: (n.data.methods || []).filter((m) => m.id !== methodId) } } : n)));
  }, [setNodes]);

  const handleDeleteEntity = useCallback((entityId: string) => {
    setNodes((nds) => nds.filter((n) => n.id !== entityId));
    setEdges((eds) => eds.filter((e) => e.source !== entityId && e.target !== entityId));
  }, [setNodes, setEdges]);

  const createNodeDataBag = useCallback(() => ({
    onUpdateName: handleUpdateName,
    onAddField: handleAddField,
    onUpdateField: handleUpdateField,
    onDeleteField: handleDeleteField,
    onAddMethod: handleAddMethod,
    onUpdateMethod: handleUpdateMethod,
    onDeleteMethod: handleDeleteMethod,
    onDeleteEntity: handleDeleteEntity,
  }), [handleUpdateName, handleAddField, handleUpdateField, handleDeleteField, handleAddMethod, handleUpdateMethod, handleDeleteMethod, handleDeleteEntity]);

  const addNewEntity = () => {
    const id = crypto.randomUUID();
    setNodes((nds) => nds.concat({
      id,
      type: 'entityNode',
      position: { x: 150 + Math.random() * 100, y: 150 + Math.random() * 100 },
      data: { label: `NewEntity${nodes.length + 1}`, fields: [], methods: [], ...createNodeDataBag() },
    }));
  };

  const clearAllNodesAndEdges = () => {
    const confirmWipe = window.confirm("Are you sure you want to completely erase the canvas layout? This actions cannot be undone.");
    if (confirmWipe) {
      setNodes([]);
      setEdges([]);
      setInputJDL('');
      setGeneratedJDL('// Canvas wiped cleanly.');
    }
  };

  // --- NEW CODE INPUT PANEL SAMPLE LOADER ---
  const loadComplexJDLScriptSample = () => {
    const sampleScript = `entity Professor {\n  name String\n  department String\n  assignGrade()\n}\n\nentity Department {\n  title String\n  budget BigDecimal\n  allocateFunds()\n}\n\nentity GraduateProject {\n  topic String\n  deadline LocalDate\n  submitThesis()\n}\n\nrelationship OneToMany {\n  Department to Professor\n}\n\nrelationship OneToOne {\n  Professor to GraduateProject\n}`;
    setInputJDL(sampleScript);
  };

  const generateJDLFromCanvas = () => {
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
      n.data.methods?.forEach((m) => { jdlString += `  ${m.definition}\n`; });
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

    const output = jdlString.trim() || '// No visual structure configured.';
    setGeneratedJDL(output);
    setInputJDL(output);
  };

  const parseJDLToCanvas = () => {
    try {
      const parsedNodes: EntityNode[] = [];
      const parsedEdges: Edge[] = [];
      const dataBag = createNodeDataBag();

      const entityBlocks = inputJDL.match(/entity\s+(\w+)(?:\s+extends\s+(\w+))?\s*\{([^}]*)\}/g);
      
      if (entityBlocks) {
        entityBlocks.forEach((block, index) => {
          const match = block.match(/entity\s+(\w+)(?:\s+extends\s+(\w+))?\s*\{([^}]*)\}/);
          if (!match) return;

          const [, entityName, parentName, contentBody] = match;
          const fields: Field[] = [];
          const methods: Method[] = [];

          const lines = contentBody.split('\n').map(l => l.trim()).filter(Boolean);
          
          lines.forEach(line => {
            if (line.includes('(') || line.endsWith(')')) {
              methods.push({ id: crypto.randomUUID(), definition: line });
            } else {
              const parts = line.split(/\s+/);
              if (parts.length >= 2) {
                fields.push({ id: crypto.randomUUID(), name: parts[0], type: parts[1] });
              } else if (parts.length === 1 && parts[0]) {
                fields.push({ id: crypto.randomUUID(), name: parts[0], type: 'String' });
              }
            }
          });

          const entityId = entityName.toLowerCase() + '-id';
          parsedNodes.push({
            id: entityId,
            type: 'entityNode',
            position: { x: 100 + (index % 3) * 280, y: 150 + Math.floor(index / 3) * 320 },
            data: { label: entityName, fields, methods, ...dataBag }
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
          const lines = contentBody.split('\n').map(l => l.trim()).filter(Boolean);

          lines.forEach(line => {
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
        alert("Parser complete: No valid entity schema layout structures identified inside declaration.");
        return;
      }

      setNodes(parsedNodes);
      setEdges(parsedEdges);
      setGeneratedJDL("// Code compiled to visual components successfully.");
    } catch (err) {
      alert("Parsing Error: Please ensure you are matching uniform JDL configurations syntax.");
    }
  };

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

  const toggleEdgeDirection = (edgeId: string) => {
    setEdges((eds) => eds.map((e) => (e.id === edgeId ? { ...e, source: e.target, target: e.source } : e)));
  };

  const downloadJDLHandler = () => {
    const element = document.createElement('a');
    element.href = URL.createObjectURL(new Blob([inputJDL], { type: 'text/plain' }));
    element.download = 'model.jdl';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  };

  return (
    <div className="editor-window">
      <div className="canvas-pane">
        <div className="navbar-controls">
          <div className="nav-brand-area">
            <Link to="/" className="btn-back">← Home Hub</Link>
            <div className="nav-titles">
              <span className="editor-title">CodeClassroom</span>
              <span className="editor-tag">JDL Bidirectional Engine v2.0</span>
            </div>
          </div>

          <div className="toolbar-actions">
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

            <button onClick={clearAllNodesAndEdges} className="btn-action-red" title="Erase All Workspace Nodes">
              <Trash2 size={14} /> Clear Canvas
            </button>
          </div>
        </div>

        <div style={{ flexGrow: 1, width: '100%', height: '100%', position: 'relative', backgroundColor: '#0a0e17' }}>
          <ReactFlow nodes={nodes} edges={edges} onNodesChange={onNodesChange} onEdgesChange={onEdgesChange} onConnect={onConnect} nodeTypes={nodeTypes} fitView>
            <Background color="#1e293b" gap={20} size={1} />
            <Controls />
            <MiniMap nodeColor="#1e1b4b" maskColor="rgba(5, 7, 12, 0.7)" style={{ backgroundColor: '#0f172a', border: '1px solid #1e293b' }} />
          </ReactFlow>
        </div>
      </div>

      <div className="compiler-panel">
        <div className="code-input-area">
          <div className="compiler-header" style={{ backgroundColor: '#111827' }}>
            <div className="compiler-title-area">
              <Code size={16} style={{ color: '#38bdf8' }} /> Write/Paste Raw JDL Code
            </div>
            
            <div style={{ display: 'flex', gap: '0.375rem' }}>
              {/* Added Sample Loader Blueprint Button */}
              <button onClick={loadComplexJDLScriptSample} className="btn-action-slate-light" title="Load sample academic dataset code">
                <RefreshCw size={12} /> Load Sample
              </button>
              
              <button onClick={parseJDLToCanvas} className="btn-action-amber" style={{ fontSize: '11px', padding: '0.4rem 0.75rem' }}>
                <Play size={12} /> Parse JDL to Visuals
              </button>
            </div>
          </div>
          <textarea
            value={inputJDL}
            onChange={(e) => setInputJDL(e.target.value)}
            className="code-editor-textarea"
            placeholder={`// Write raw definitions here...\nentity Passport {\n  number String\n  verifyDocument()\n}`}
          />
        </div>

        <div className="code-output-view">
          <div className="compiler-header">
            <div className="compiler-title-area">
              <Code size={16} style={{ color: '#34d399' }} /> Compiled JDL Log Output
            </div>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button onClick={generateJDLFromCanvas} className="btn-compile">Generate JDL</button>
              <button onClick={downloadJDLHandler} className="btn-action-slate">Export</button>
            </div>
          </div>
          
          {edges.length > 0 && (
            <div className="schema-inspector">
              <p className="inspector-lbl">Active System Map Tracks</p>
              {edges.map((e) => {
                const srcNode = nodes.find(n => n.id === e.source);
                const tgtNode = nodes.find(n => n.id === e.target);
                if (!srcNode || !tgtNode) return null;
                return (
                  <div key={e.id} className="inspector-row">
                    <span className="schema-mapping-text">
                      {srcNode.data.label} <span className="mapping-badge">{e.label}</span> {tgtNode.data.label}
                    </span>
                    <button onClick={() => { toggleEdgeDirection(e.id); generateJDLFromCanvas(); }} className="btn-inverse">
                      <ArrowLeftRight size={10} /> Inverse
                    </button>
                  </div>
                );
              })}
            </div>
          )}

          <div className="code-output-view" style={{ height: 'auto', flexGrow: 1 }}>
            <div className="code-output-view" style={{ width: '100%', height: '100%', padding: '0' }}>
              <div className="code-output-view" style={{ width: '100%', height: '100%', padding: '1rem', background: '#020408' }}>
                <pre className="code-output-text" style={{ margin: 0 }}>{generatedJDL}</pre>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}