import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { instructorApi, type HeatmapEntry } from '../../api/instructor';
import { useCourseId } from '../../hooks/useCourseId';

interface HeatmapStudent {
  id: number;
  name: string;
  initial: string;
  risk: 'safe' | 'caution' | 'danger';
}

function toRiskLevel(overallRiskScore: number): 'safe' | 'caution' | 'danger' {
  if (overallRiskScore >= 70) return 'danger';
  if (overallRiskScore >= 40) return 'caution';
  return 'safe';
}

function mapEntry(e: HeatmapEntry): HeatmapStudent {
  return {
    id: e.studentId,
    name: e.studentName,
    initial: e.studentName.charAt(0),
    risk: toRiskLevel(e.overallRiskScore),
  };
}

const riskLabel: Record<string, string> = { safe: '안전', caution: '주의', danger: '위험' };

const riskCellClass: Record<string, string> = {
  safe: 'bg-emerald-100 text-emerald-700 hover:bg-emerald-200',
  caution: 'bg-amber-100 text-amber-700 hover:bg-amber-200',
  danger: 'bg-rose-100 text-rose-700 ring-2 ring-rose-300 hover:bg-rose-200',
};

export default function RiskHeatmap() {
  const courseId = useCourseId();
  const navigate = useNavigate();
  const [hoveredId, setHoveredId] = useState<number | null>(null);

  const { data: students = [] } = useQuery({
    queryKey: ['instructor', 'heatmap', courseId],
    queryFn: async () => {
      const entries = await instructorApi.getCourseHeatmap(courseId!);
      return entries.map(mapEntry);
    },
    enabled: !!courseId,
    staleTime: 30_000,
  });

  const counts = {
    safe: students.filter((s) => s.risk === 'safe').length,
    caution: students.filter((s) => s.risk === 'caution').length,
    danger: students.filter((s) => s.risk === 'danger').length,
  };

  return (
    <div className="bg-white/85 backdrop-blur-[12px] border border-white/60 rounded-2xl shadow-lg p-5">
      <h3 className="text-sm font-semibold text-slate-800 mb-4">학생 위험도 히트맵</h3>

      {students.length === 0 ? (
        <p className="text-sm text-slate-400 text-center py-8">
          수강 중인 학생이 없습니다
        </p>
      ) : (
        <>
          <div className="grid grid-cols-7 gap-2">
            {students.map((student) => (
              <div key={student.id} className="relative">
                <div
                  className={`w-[42px] h-[42px] rounded-lg flex items-center justify-center text-xs font-semibold transition-all duration-150 cursor-pointer select-none ${riskCellClass[student.risk]}`}
                  style={{
                    transform: hoveredId === student.id ? 'scale(1.1)' : 'scale(1)',
                  }}
                  onMouseEnter={() => setHoveredId(student.id)}
                  onMouseLeave={() => setHoveredId(null)}
                  onClick={() => navigate(`/instructor/students/${student.id}`, {
                    state: { studentName: student.name },
                  })}
                >
                  {student.initial}
                </div>

                {hoveredId === student.id && (
                  <div className="absolute -top-9 left-1/2 -translate-x-1/2 bg-slate-800 text-white text-[11px] px-2 py-1 rounded-md whitespace-nowrap z-10 shadow-lg pointer-events-none">
                    {student.name} · {riskLabel[student.risk]}
                    <div className="absolute top-full left-1/2 -translate-x-1/2 w-0 h-0 border-l-4 border-r-4 border-t-4 border-l-transparent border-r-transparent border-t-slate-800" />
                  </div>
                )}
              </div>
            ))}
          </div>

          <div className="flex items-center gap-4 mt-4 pt-3 border-t border-slate-100">
            <span className="flex items-center gap-1.5 text-xs text-slate-600">
              <span className="w-3 h-3 rounded-sm bg-emerald-100 border border-emerald-200" />
              안전 {counts.safe}명
            </span>
            <span className="flex items-center gap-1.5 text-xs text-slate-600">
              <span className="w-3 h-3 rounded-sm bg-amber-100 border border-amber-200" />
              주의 {counts.caution}명
            </span>
            <span className="flex items-center gap-1.5 text-xs text-slate-600">
              <span className="w-3 h-3 rounded-sm bg-rose-100 border border-rose-300" />
              위험 {counts.danger}명
            </span>
          </div>
        </>
      )}
    </div>
  );
}
