import { MarkerType, type Edge } from '@xyflow/react';
import { type EntityNode } from '../types/modeling';

export const DESIGN_PATTERN_TEMPLATES: Record<string, { nodes: EntityNode[]; edges: Edge[] }> = {
  Singleton: {
    nodes: [
      {
        id: 'singleton-instance-id',
        type: 'entityNode',
        position: { x: 250, y: 150 },
        data: {
          label: 'Singleton',
          fields: [{ id: 'sf1', name: 'instance', type: 'Singleton' }],
          methods: [
            { id: 'sm1', definition: 'getInstance(): Singleton' },
            { id: 'sm2', definition: 'operation()' }
          ]
        }
      }
    ],
    edges: []
  },
  Factory: {
    nodes: [
      {
        id: 'creator-id',
        type: 'entityNode',
        position: { x: 100, y: 150 },
        data: { label: 'Creator', fields: [], methods: [{ id: 'fm1', definition: 'factoryMethod(): Product' }] }
      },
      {
        id: 'product-id',
        type: 'entityNode',
        position: { x: 480, y: 150 },
        data: { label: 'Product', fields: [], methods: [{ id: 'fm2', definition: 'useProduct()' }] }
      }
    ],
    edges: [
      {
        id: 'edge-factory-link',
        source: 'creator-id',
        target: 'product-id',
        label: 'OneToOne',
        type: 'default',
        style: { strokeWidth: 2, stroke: '#6366f1' },
        markerEnd: { type: MarkerType.ArrowClosed, color: '#6366f1' }
      }
    ]
  },
  Observer: {
    nodes: [
      {
        id: 'subject-id',
        type: 'entityNode',
        position: { x: 100, y: 150 },
        data: {
          label: 'Subject',
          fields: [{ id: 'of1', name: 'observers', type: 'List<Observer>' }],
          methods: [{ id: 'om1', definition: 'attach(Observer)' }, { id: 'om2', definition: 'notify()' }]
        }
      },
      {
        id: 'observer-id',
        type: 'entityNode',
        position: { x: 480, y: 150 },
        data: { label: 'Observer', fields: [], methods: [{ id: 'om3', definition: 'update()' }] }
      }
    ],
    edges: [
      {
        id: 'edge-observer-link',
        source: 'subject-id',
        target: 'observer-id',
        label: 'OneToMany',
        type: 'default',
        style: { strokeWidth: 2, stroke: '#6366f1' },
        markerEnd: { type: MarkerType.ArrowClosed, color: '#6366f1' }
      }
    ]
  },
  Decorator: {
    nodes: [
      {
        id: 'component-id',
        type: 'entityNode',
        position: { x: 280, y: 80 },
        data: { label: 'Component', fields: [], methods: [{ id: 'df1', definition: 'operation()' }] }
      },
      {
        id: 'decorator-id',
        type: 'entityNode',
        position: { x: 280, y: 320 },
        data: { label: 'Decorator', fields: [{ id: 'df2', name: 'component', type: 'Component' }], methods: [{ id: 'dm1', definition: 'operation()' }] }
      }
    ],
    edges: [
      {
        id: 'edge-decorator-link',
        source: 'decorator-id',
        target: 'component-id',
        label: 'extends',
        type: 'default',
        style: { strokeDasharray: '5,5', strokeWidth: 2, stroke: '#ef4444' },
        markerEnd: { type: MarkerType.ArrowClosed, color: '#ef4444' }
      }
    ]
  },
  Strategy: {
    nodes: [
      {
        id: 'context-id',
        type: 'entityNode',
        position: { x: 100, y: 150 },
        data: { label: 'Context', fields: [{ id: 'stf1', name: 'strategy', type: 'Strategy' }], methods: [{ id: 'stm1', definition: 'executeStrategy()' }] }
      },
      {
        id: 'strategy-id',
        type: 'entityNode',
        position: { x: 480, y: 150 },
        data: { label: 'Strategy', fields: [], methods: [{ id: 'stm2', definition: 'algorithm()' }] }
      }
    ],
    edges: [
      {
        id: 'edge-strategy-link',
                source: 'context-id',
        target: 'strategy-id',
        label: 'OneToOne',
        type: 'default',
        style: { strokeWidth: 2, stroke: '#6366f1' },
        markerEnd: { type: MarkerType.ArrowClosed, color: '#6366f1' }
      }
    ]
  }
};