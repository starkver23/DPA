import React, { memo } from 'react';
import { Handle, Position, type NodeProps } from '@xyflow/react';
import { type EntityNode } from '../types/modeling';

export const EntityNodeComponent = memo(({ data, selected }: NodeProps<EntityNode>) => {
  return (
    <div className={`uml-node-container ${selected ? 'uml-node-selected' : ''}`}>
      <Handle type="target" position={Position.Top} style={{ width: 10, height: 10, background: '#6366f1' }} />
      
      <div className="uml-compartment-header">
        <div className="uml-classname-display">{data.label || 'UnnamedEntity'}</div>
      </div>

      <div className="uml-compartment-body">
        {data.fields.length === 0 && <p className="uml-empty-text">No attributes defined</p>}
        {data.fields.map((field) => (
          <div key={field.id} className="uml-static-row">
            <span className="uml-static-field">{field.name}</span>
            <span className="uml-separator">:</span>
            <span className="uml-static-type">{field.type}</span>
          </div>
        ))}
      </div>

      <div className="uml-compartment-body uml-border-top">
        {(!data.methods || data.methods.length === 0) && <p className="uml-empty-text">No operations defined</p>}
        {data.methods?.map((method) => (
          <div key={method.id} className="uml-static-row">
            <span className="uml-static-method">{method.definition}</span>
          </div>
        ))}
      </div>

      <Handle type="source" position={Position.Bottom} style={{ width: 10, height: 10, background: '#6366f1' }} />
    </div>
  );
});

EntityNodeComponent.displayName = 'EntityNodeComponent';