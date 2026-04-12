import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { operatorApi } from '../../api/operator';
import GlassCard from '../../components/common/GlassCard';

export default function AtRiskStudents() {
  const queryClient = useQueryClient();

  const { data: atRisk, isLoading } = useQuery({
    queryKey: ['operator', 'at-risk'],
    queryFn: operatorApi.getAtRiskStudents,
  });

  const interventionMutation = useMutation({
    mutationFn: operatorApi.createIntervention,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['operator'] }),
  });

  const handleIntervene = (student: { id: string; courseId: string; aiSuggestion: string }) => {
    interventionMutation.mutate({
      studentId: student.id,
      courseId: student.courseId,
      interventionType: 'CONSULTATION',
      description: student.aiSuggestion,
      aiSuggested: true,
    });
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">이탈 위험군 관리</h1>
        <p className="text-sm text-slate-500 mt-1">AI가 감지한 이탈 위험 수강생 및 자동 개입 제안</p>
      </div>

      {isLoading ? (
        <p className="text-sm text-slate-400">로딩 중...</p>
      ) : atRisk && atRisk.length > 0 ? (
        <div className="space-y-4">
          {atRisk.map((student) => (
            <GlassCard key={student.id} className="p-5">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3">
                    <h3 className="text-sm font-bold text-slate-900">{student.name}</h3>
                    <span className={`px-2 py-0.5 rounded-full text-xs font-bold ${
                      student.overallRisk >= 0.8 ? 'bg-rose-100 text-rose-700' : 'bg-amber-100 text-amber-700'
                    }`}>
                      위험도 {(student.overallRisk * 100).toFixed(0)}%
                    </span>
                    {student.trend === 'DECLINING' && (
                      <span className="text-rose-500 text-xs font-bold">&#9660; 하락 중</span>
                    )}
                  </div>
                  <p className="text-xs text-slate-500 mt-1">{student.courseTitle}</p>
                  {student.consecutiveAbsences > 0 && (
                    <p className="text-xs text-rose-500 mt-1">
                      연속 결석 {student.consecutiveAbsences}일
                    </p>
                  )}

                  {/* AI suggestion */}
                  <div className="mt-3 p-3 rounded-lg bg-indigo-50 border border-indigo-100">
                    <p className="text-xs font-bold text-indigo-700 mb-1">AI 개입 제안</p>
                    <p className="text-xs text-indigo-600">{student.aiSuggestion}</p>
                  </div>
                </div>

                <div className="ml-4 flex flex-col gap-2">
                  <button
                    onClick={() => handleIntervene(student)}
                    disabled={interventionMutation.isPending}
                    className="px-4 py-2 rounded-lg bg-indigo-600 text-white text-xs font-bold hover:bg-indigo-700 disabled:opacity-50 transition-colors"
                  >
                    개입 승인
                  </button>
                  <button className="px-4 py-2 rounded-lg bg-slate-100 text-slate-600 text-xs font-medium hover:bg-slate-200 transition-colors">
                    상세 보기
                  </button>
                </div>
              </div>
            </GlassCard>
          ))}
        </div>
      ) : (
        <GlassCard className="p-8 text-center">
          <p className="text-slate-400">현재 이탈 위험 수강생이 없습니다.</p>
        </GlassCard>
      )}
    </div>
  );
}
