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
import { Plus, Trash2, Code, Download, RefreshCw, Layers, ArrowLeftRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import '@xyflow/react/dist/style.css';

interface Field {
  id: string;
  name: string;
  type: string;
}

interface EntityData extends Record<string, unknown> {
  label: string;
  fields: Field[];
  onUpdateField: (entityId: string, fieldId: string, updatedField: Partial<Field>) => void;
  onAddField: (entityId: string) => void;
  onDeleteField: (entityId: string, fieldId: string) => void;
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

      <div className="node-footer">
        <button onClick={() => data.onAddField(id)} className="btn-append-field">
          <Plus size={12} /> Append Field
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
  const [generatedJDL, setGeneratedJDL] = useState<string>('// Click "Generate JDL" to view compilation output.');

  const handleUpdateName = useCallback((entityId: string, newName: string) => {
    setNodes((nds) => nds.map((n) => (n.id === entityId ? { ...n, data: { ...n.data, label: newName } } : n)));
  }, [setNodes]);

  const handleAddField = useCallback((entityId: string) => {
    setNodes((nds) => nds.map((n) => {
      if (n.id === entityId) {
        return {
          ...n,
          data: {
            ...n.data,
            fields: [...n.data.fields, { id: crypto.randomUUID(), name: `field${n.data.fields.length + 1}`, type: 'String' }]
          }
        };
      }
      return n;
    }));
  }, [setNodes]);

  const handleUpdateField = useCallback((entityId: string, fieldId: string, updatedField: Partial<Field>) => {
    setNodes((nds) => nds.map((n) => {
      if (n.id === entityId) {
        return { ...n, data: { ...n.data, fields: n.data.fields.map((f) => (f.id === fieldId ? { ...f, ...updatedField } : f)) } };
      }
      return n;
    }));
  }, [setNodes]);

  const handleDeleteField = useCallback((entityId: string, fieldId: string) => {
    setNodes((nds) => nds.map((n) => (n.id === entityId ? { ...n, data: { ...n.data, fields: n.data.fields.filter((f) => f.id !== fieldId) } } : n)));
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
    onDeleteEntity: handleDeleteEntity,
  }), [handleUpdateName, handleAddField, handleUpdateField, handleDeleteField, handleDeleteEntity]);

  const addNewEntity = () => {
    const id = crypto.randomUUID();
    setNodes((nds) => nds.concat({
      id,
      type: 'entityNode',
      position: { x: 100 + Math.random() * 150, y: 150 + Math.random() * 150 },
      data: { label: `NewEntity${nodes.length + 1}`, fields: [], ...createNodeDataBag() },
    }));
  };

  const loadExampleConfig = () => {
    const studentId = 'student-id';
    const courseId = 'course-id';
    const bag = createNodeDataBag();

    setNodes([
      { id: studentId, type: 'entityNode', position: { x: 100, y: 200 }, data: { label: 'Student', fields: [{ id: 's1', name: 'email', type: 'String' }], ...bag } },
      { id: courseId, type: 'entityNode', position: { x: 480, y: 200 }, data: { label: 'Course', fields: [{ id: 'c1', name: 'title', type: 'String' }], ...bag } },
    ]);
    setEdges([
      { 
        id: 'e-student-course', 
        source: studentId, 
        target: courseId, 
        label: 'ManyToMany', 
        type: 'default', 
        style: { strokeWidth: 2, stroke: '#6366f1' },
        markerEnd: { type: MarkerType.ArrowClosed, color: '#6366f1' } 
      },
    ]);
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
      style: isInheritance 
        ? { strokeDasharray: '5,5', strokeWidth: 2, stroke: '#ef4444' } 
        : { strokeWidth: 2, stroke: '#6366f1' },
      markerEnd: {
        type: MarkerType.ArrowClosed,
        color: isInheritance ? '#ef4444' : '#6366f1',
      }
    };

    setEdges((eds) => addEdge(stylizedEdge, eds));
  }, [relationshipType, setEdges]);

  const toggleEdgeDirection = (edgeId: string) => {
    setEdges((eds) =>
      eds.map((e) => {
        if (e.id === edgeId) {
          return { ...e, source: e.target, target: e.source };
        }
        return e;
      })
    );
  };

  const generateJDLHandler = () => {
    let jdlString = '';

    const inheritanceMap: Record<string, string> = {};
    edges.forEach((e) => {
      if (e.label === 'extends') {
        const parentNode = nodes.find((n) => n.id === e.target);
        if (parentNode) {
          inheritanceMap[e.source] = parentNode.data.label;
        }
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

    setGeneratedJDL(jdlString.trim() || '// No visual structure compiled.');
  };

  const downloadJDLHandler = () => {
    const element = document.createElement('a');
    element.href = URL.createObjectURL(new Blob([generatedJDL], { type: 'text/plain' }));
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
              <span className="editor-tag">JDL.v1</span>
            </div>
          </div>

          <div className="toolbar-actions">
            <div className="selector-box">
              <Layers size={13} style={{ color: '#475569' }} /> 
              <span>Link Edge:</span>
              <select 
                value={relationshipType} 
                onChange={(e) => setRelationshipType(e.target.value as RelationshipType)} 
                className="select-dropdown"
              >
                <option value="ManyToMany">ManyToMany</option>
                <option value="OneToMany">OneToMany</option>
                <option value="OneToOne">OneToOne</option>
                <option value="Inheritance">Inheritance</option>
              </select>
            </div>

            <button onClick={addNewEntity} className="btn-action-green">
              <Plus size={15} /> Add Entity
            </button>
            <button onClick={loadExampleConfig} className="btn-action-slate">
              <RefreshCw size={14} /> Load Setup
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
            fitView
          >
            <Background color="#1e293b" gap={20} size={1} />
            <Controls />
            <MiniMap nodeColor="#1e1b4b" maskColor="rgba(5, 7, 12, 0.7)" style={{ backgroundColor: '#0f172a', border: '1px solid #1e293b' }} />
          </ReactFlow>
        </div>
      </div>

      <div className="compiler-panel">
        <div className="compiler-header">
          <div className="compiler-title-area">
            <Code size={16} style={{ color: '#6366f1' }} /> JDL Terminal Engine
          </div>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button onClick={generateJDLHandler} className="btn-compile">Generate JDL</button>
            <button onClick={downloadJDLHandler} className="btn-action-slate">Export</button>
          </div>
        </div>
        
        {edges.length > 0 && (
          <div className="schema-inspector">
            <p className="inspector-lbl">Active Schema Mappings</p>
            {edges.map((e) => {
              const srcNode = nodes.find(n => n.id === e.source);
              const tgtNode = nodes.find(n => n.id === e.target);
              if (!srcNode || !tgtNode) return null;
              return (
                <div key={e.id} className="inspector-row">
                  <span className="schema-mapping-text">
                    {srcNode.data.label} <span className="mapping-badge">{e.label}</span> {tgtNode.data.label}
                  </span>
                  <button onClick={() => { toggleEdgeDirection(e.id); generateJDLHandler(); }} className="btn-inverse">
                    <ArrowLeftRight size={10} /> Inverse
                  </button>
                </div>
              );
            })}
          </div>
        )}

        <div className="code-output-view">
          <pre className="code-output-text">{generatedJDL}</pre>
        </div>
      </div>
    </div>
  );
}