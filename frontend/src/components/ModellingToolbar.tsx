import { Link } from 'react-router-dom';
import { Layers, Plus, Trash2 } from 'lucide-react';
import ThemeToggle from './ThemeToggle';
import { type RelationshipType } from '../types/modeling';

interface ModellingToolbarProps {
  relationshipType: RelationshipType;
  setRelationshipType: (type: RelationshipType) => void;
  addNewEntity: () => void;
  addNewInterface: () => void;
  clearAllNodesAndEdges: () => void;
}

export default function ModellingToolbar({
  relationshipType,
  setRelationshipType,
  addNewEntity,
  addNewInterface,
  clearAllNodesAndEdges,
}: ModellingToolbarProps) {
  return (
    <div className="navbar-controls">
      <div className="nav-brand-area">
        <Link to="/" className="btn-back">← Home Hub</Link>
        <div className="nav-titles">
          <span className="editor-title">CodeClassroom</span>
          <span className="editor-tag">JDL Engine v2.0</span>
        </div>
      </div>

      <div className="toolbar-actions">
        <div className="selector-box">
          <Layers size={13} style={{ color: '#475569' }} /> 
          <span>Add Relation:</span>
          <select 
            value={relationshipType} 
            onChange={(e) => setRelationshipType(e.target.value as RelationshipType)} 
            className="select-dropdown"
          >
            <option value="ManyToMany">ManyToMany</option>
            <option value="OneToMany">OneToMany</option>
            <option value="OneToOne">OneToOne</option>
            <option value="Inheritance">Inheritance</option>
            <option value="Implementation">Implementation</option>
          </select>
        </div>

        <button onClick={addNewEntity} className="btn-action-green">
          <Plus size={15} /> Add Entity
        </button>

        <button onClick={addNewInterface} className="btn-action-green">
          <Plus size={15} /> Add Interface
        </button>

        <button onClick={clearAllNodesAndEdges} className="btn-action-red">
          <Trash2 size={14} /> Clear Canvas
        </button>
        <ThemeToggle />
      </div>
    </div>
  );
}
