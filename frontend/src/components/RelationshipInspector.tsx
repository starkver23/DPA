import { ArrowLeftRight, Trash2 } from 'lucide-react';
import { type Edge } from '@xyflow/react';

interface RelationshipInspectorProps {
  selectedEdge: Edge;
  updateEdgeType: (edgeId: string, typeLabel: string) => void;
  toggleEdgeDirection: (edgeId: string) => void;
  deleteSelectedEdge: (id: string) => void;
}

export default function RelationshipInspector({
  selectedEdge,
  updateEdgeType,
  toggleEdgeDirection,
  deleteSelectedEdge,
}: RelationshipInspectorProps) {
  return (
    <div className="inspector-container">
      <div className="inspector-group">
        <label className="inspector-field-label">Relationship Line Mapping Pattern</label>
        <select
          value={selectedEdge.label === 'extends' ? 'Inheritance' : selectedEdge.label === 'implements' ? 'Implementation' : String(selectedEdge.label)}
          onChange={(e) => updateEdgeType(selectedEdge.id, e.target.value)}
          className="inspector-select-large"
        >
          <option value="ManyToMany">ManyToMany</option>
          <option value="OneToMany">OneToMany</option>
          <option value="OneToOne">OneToOne</option>
          <option value="Inheritance">Inheritance (Extends Pointer)</option>
          <option value="Implementation">Implementation (Implements Pointer)</option>
        </select>
      </div>

      <div className="inspector-group" style={{ marginTop: '1rem' }}>
        <button 
          onClick={() => toggleEdgeDirection(selectedEdge.id)} 
          className="btn-sidebar-action btn-slate" 
          style={{ width: '100%', display: 'flex', gap: '0.5rem', justifyContent: 'center', alignItems: 'center' }}
        >
          <ArrowLeftRight size={12} /> Flip Cardinality Vector Direction
        </button>
      </div>

      <div className="inspector-group border-top-divider" style={{ marginTop: '1.5rem', paddingTop: '1rem' }}>
        <button onClick={() => deleteSelectedEdge(selectedEdge.id)} className="btn-inspector-delete-entity">
          <Trash2 size={12} /> Sever Association Path
        </button>
      </div>
    </div>
  );
}
