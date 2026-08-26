import { Type, Trash2 } from 'lucide-react';
import { type EntityNode } from '../types/modeling';

interface EntityInspectorProps {
  selectedNode: EntityNode;
  nodes: EntityNode[];
  currentParentNode: EntityNode | null;
  updateNodeData: (id: string, updater: (data: EntityNode['data']) => Partial<EntityNode['data']>) => void;
  handleParentChange: (nodeId: string, targetParentId: string) => void;
  deleteSelectedEntity: (id: string) => void;
}

export default function EntityInspector({
  selectedNode,
  nodes,
  currentParentNode,
  updateNodeData,
  handleParentChange,
  deleteSelectedEntity,
}: EntityInspectorProps) {
  return (
    <div className="inspector-container">
      <div className="inspector-group">
        <label className="inspector-field-label">Entity Name</label>
        <div className="inspector-input-wrapper">
          <Type size={12} className="input-decorator-icon" />
          <input
            type="text"
            value={selectedNode.data.label}
            onChange={(e) => updateNodeData(selectedNode.id, () => ({ label: e.target.value }))}
            className="inspector-text-input"
            placeholder="ClassName"
          />
        </div>
      </div>

      <div className="inspector-group" style={{ marginTop: '0.75rem' }}>
        <label className="inspector-field-label">Type</label>
        <select
          value={selectedNode.data.kind === 'interface' ? 'interface' : selectedNode.data.abstract ? 'abstract' : 'class'}
          onChange={(e) => updateNodeData(selectedNode.id, () => {
            if (e.target.value === 'interface') {
              return { kind: 'interface', abstract: false };
            }
            return { kind: 'class', abstract: e.target.value === 'abstract' };
          })}
          className="inspector-select-large"
        >
          <option value="class">Normal Class</option>
          <option value="abstract">Abstract Class</option>
          <option value="interface">Interface</option>
        </select>
      </div>

      <div className="inspector-group" style={{ marginTop: '0.75rem' }}>
        <label className="inspector-field-label">{selectedNode.data.kind === 'interface' ? 'Extends Parent Interface' : 'Extends Parent Class'}</label>
        <select
          value={currentParentNode ? currentParentNode.id : 'none'}
          onChange={(e) => handleParentChange(selectedNode.id, e.target.value)}
          className="inspector-select-large"
        >
          <option value="none">-- None --</option>
          {nodes
            .filter((n) => n.id !== selectedNode.id)
            .filter((n) => selectedNode.data.kind === 'interface' ? n.data.kind === 'interface' : n.data.kind !== 'interface')
            .map((n) => (
              <option key={n.id} value={n.id}>{n.data.label}</option>
            ))}
        </select>
      </div>

      {selectedNode.data.kind !== 'interface' && (
        <div className="inspector-group" style={{ marginTop: '0.75rem', flexDirection: 'row', alignItems: 'center', gap: '0.5rem' }}>
        <input
          type="checkbox"
          id="abstract-checkbox"
          checked={!!selectedNode.data.abstract}
          onChange={(e) => updateNodeData(selectedNode.id, () => ({ abstract: e.target.checked }))}
          style={{ cursor: 'pointer', width: '1rem', height: '1rem' }}
        />
        <label htmlFor="abstract-checkbox" style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.05em', color: '#64748b', fontWeight: 700, cursor: 'pointer', userSelect: 'none' }}>
          Abstract Class
        </label>
        </div>
      )}

      <div className="inspector-group border-top-divider" style={{ marginTop: '1.25rem', paddingTop: '1rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
          <label className="inspector-field-label">Fields / Attributes</label>
          <button
            onClick={() => updateNodeData(selectedNode.id, (data) => {
              const currentFields = data.fields || [];
              return {
                fields: [...currentFields, { id: crypto.randomUUID(), name: `field${currentFields.length + 1}`, type: 'String' }]
              };
            })}
            className="btn-inspector-add"
          >
            + Field
          </button>
        </div>
        {selectedNode.data.fields.length === 0 && <p className="uml-empty-text">No fields mapped on this target block.</p>}
        {selectedNode.data.fields.map((f) => (
          <div key={f.id} className="inspector-row-item">
            <input
              type="text"
              value={f.name}
              placeholder="attribute"
              onChange={(e) => updateNodeData(selectedNode.id, (data) => ({
                fields: data.fields.map((field) => (field.id === f.id ? { ...field, name: e.target.value } : field))
              }))}
              className="inspector-row-input"
            />
            <select
              value={f.type}
              onChange={(e) => updateNodeData(selectedNode.id, (data) => ({
                fields: data.fields.map((field) => (field.id === f.id ? { ...field, type: e.target.value } : field))
              }))}
              className="inspector-row-select"
            >
              <option value="String">String</option>
              <option value="Integer">Integer</option>
              <option value="Long">Long</option>
              <option value="BigDecimal">BigDecimal</option>
              <option value="LocalDate">LocalDate</option>
              <option value="Boolean">Boolean</option>
            </select>
            <button
              onClick={() => updateNodeData(selectedNode.id, (data) => ({
                fields: data.fields.filter((field) => field.id !== f.id)
              }))}
              className="btn-row-delete"
            >
              <Trash2 size={11} />
            </button>
          </div>
        ))}
      </div>

      <div className="inspector-group border-top-divider" style={{ marginTop: '1.25rem', paddingTop: '1rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
          <label className="inspector-field-label">Methods / Operations</label>
          <button
            onClick={() => updateNodeData(selectedNode.id, (data) => {
              const currentMethods = data.methods || [];
              return {
                methods: [...currentMethods, { id: crypto.randomUUID(), definition: `operation${currentMethods.length + 1}()` }]
              };
            })}
            className="btn-inspector-add"
          >
            + Operation
          </button>
        </div>
        {selectedNode.data.methods.length === 0 && <p className="uml-empty-text">No operations mapped on this target block.</p>}
        {selectedNode.data.methods.map((method) => (
          <div key={method.id} className="inspector-row-item">
            <input
              type="text"
              value={method.definition}
              placeholder="operation() String"
              onChange={(e) => updateNodeData(selectedNode.id, (data) => ({
                methods: data.methods.map((item) => (item.id === method.id ? { ...item, definition: e.target.value } : item))
              }))}
              className="inspector-row-input inspector-operation-input"
            />
            <button
              onClick={() => updateNodeData(selectedNode.id, (data) => ({
                methods: data.methods.filter((item) => item.id !== method.id)
              }))}
              className="btn-row-delete"
            >
              <Trash2 size={11} />
            </button>
          </div>
        ))}
      </div>

      <div className="inspector-group border-top-divider" style={{ marginTop: '1.5rem', paddingTop: '1rem' }}>
        <button onClick={() => deleteSelectedEntity(selectedNode.id)} className="btn-inspector-delete-entity">
          <Trash2 size={12} /> Delete Entity
        </button>
      </div>
    </div>
  );
}
