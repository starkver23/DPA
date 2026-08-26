import {
  BaseEdge,
  EdgeLabelRenderer,
  getBezierPath,
  type EdgeProps,
} from '@xyflow/react';

type RelationshipEdgeData = {
  label?: string;
  parallelIndex?: number;
  parallelTotal?: number;
  cardinality?: string;
};

export function RelationshipEdgeComponent({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  markerEnd,
  style,
  selected,
  data,
}: EdgeProps) {
  const edgeData = (data || {}) as RelationshipEdgeData;
  const shouldShowCardinality = edgeData.cardinality && edgeData.cardinality !== edgeData.label;
  const total = edgeData.parallelTotal || 1;
  const index = edgeData.parallelIndex || 0;
  const centerOffset = index - (total - 1) / 2;
  const curvature = Math.min(0.75, Math.max(0.18, 0.28 + Math.abs(centerOffset) * 0.16));
  const signedOffset = centerOffset * 28;

  const dx = targetX - sourceX;
  const dy = targetY - sourceY;
  const length = Math.hypot(dx, dy) || 1;
  const normalX = (-dy / length) * signedOffset;
  const normalY = (dx / length) * signedOffset;

  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX: sourceX + normalX,
    sourceY: sourceY + normalY,
    sourcePosition,
    targetX: targetX + normalX,
    targetY: targetY + normalY,
    targetPosition,
    curvature,
  });

  return (
    <>
      <BaseEdge
        id={id}
        path={edgePath}
        markerEnd={markerEnd}
        style={{
          ...style,
          strokeWidth: selected ? 3 : style?.strokeWidth,
        }}
      />
      <EdgeLabelRenderer>
        <div
          className="relationship-edge-label"
          style={{
            transform: `translate(-50%, -50%) translate(${labelX + normalX * 0.35}px, ${labelY + normalY * 0.35}px)`,
          }}
        >
          <span>{edgeData.label}</span>
          {shouldShowCardinality && <small>{edgeData.cardinality}</small>}
        </div>
      </EdgeLabelRenderer>
    </>
  );
}
