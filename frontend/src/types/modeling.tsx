import { type Node } from '@xyflow/react';

export interface Field {
  id: string;
  name: string;
  type: string;
}

export interface Method {
  id: string;
  definition: string;
}

export interface EntityData extends Record<string, unknown> {
  label: string;
  fields: Field[];
  methods: Method[];
}

export type EntityNode = Node<EntityData>;
export type RelationshipType = 'OneToOne' | 'OneToMany' | 'ManyToMany' | 'Inheritance';