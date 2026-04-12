import React from 'react';

interface SVGRadarChartProps {
  data: number[]; // 6 values, 0-100
  size?: number;
  showLabels?: boolean;
  labels?: string[];
}

const DEFAULT_LABELS = ['이해도', '수행력', '완료율', '망각위험', '집중도', '자신감'];

function polarToCartesian(
  cx: number,
  cy: number,
  r: number,
  angleIndex: number,
  totalAxes: number,
): [number, number] {
  const angle = (Math.PI * 2 * angleIndex) / totalAxes - Math.PI / 2;
  return [cx + r * Math.cos(angle), cy + r * Math.sin(angle)];
}

function buildPolygonPoints(
  cx: number,
  cy: number,
  radius: number,
  axes: number,
): string {
  return Array.from({ length: axes })
    .map((_, i) => polarToCartesian(cx, cy, radius, i, axes).join(','))
    .join(' ');
}

const SVGRadarChart: React.FC<SVGRadarChartProps> = ({
  data,
  size = 200,
  showLabels = true,
  labels = DEFAULT_LABELS,
}) => {
  const cx = 100;
  const cy = 90;
  const maxR = 70;
  const axes = 6;
  const gridLevels = [0.25, 0.5, 0.75, 1.0];

  const dataPoints = data.map((val, i) => {
    const safeVal = Number.isFinite(val) ? val : 0;
    const r = (Math.min(Math.max(safeVal, 0), 100) / 100) * maxR;
    return polarToCartesian(cx, cy, r, i, axes);
  });

  const dataPolygon = dataPoints.map((p) => p.join(',')).join(' ');

  const viewW = 200;
  const viewH = 180;
  const scale = size / viewW;

  return (
    <svg
      width={size}
      height={size * (viewH / viewW)}
      viewBox={`0 0 ${viewW} ${viewH}`}
      className="select-none overflow-visible"
      style={{ overflow: 'visible' }}
    >
      {/* Grid hexagons */}
      {gridLevels.map((level) => (
        <polygon
          key={level}
          points={buildPolygonPoints(cx, cy, maxR * level, axes)}
          fill="none"
          stroke="#e2e8f0"
          strokeWidth="0.5"
        />
      ))}

      {/* Axis lines */}
      {Array.from({ length: axes }).map((_, i) => {
        const [ex, ey] = polarToCartesian(cx, cy, maxR, i, axes);
        return (
          <line
            key={`axis-${i}`}
            x1={cx}
            y1={cy}
            x2={ex}
            y2={ey}
            stroke="#cbd5e1"
            strokeWidth="0.5"
          />
        );
      })}

      {/* Data area */}
      <polygon
        points={dataPolygon}
        fill="rgba(99,102,241,0.15)"
        stroke="#6366f1"
        strokeWidth="1.5"
      />

      {/* Data dots */}
      {dataPoints.map(([x, y], i) => (
        <circle
          key={`dot-${i}`}
          cx={x}
          cy={y}
          r="2.5"
          fill="#6366f1"
        />
      ))}

      {/* Labels */}
      {showLabels &&
        labels.map((label, i) => {
          const [lx, ly] = polarToCartesian(cx, cy, maxR + 16, i, axes);
          return (
            <text
              key={`label-${i}`}
              x={lx}
              y={ly}
              textAnchor="middle"
              dominantBaseline="central"
              className="fill-slate-600"
              fontSize={scale > 1 ? 9 : 10}
              fontWeight="500"
            >
              {label}
            </text>
          );
        })}
    </svg>
  );
};

export default SVGRadarChart;
