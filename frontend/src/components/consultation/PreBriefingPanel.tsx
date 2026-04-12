import { useQuery } from '@tanstack/react-query';
import { consultationsApi } from '../../api/consultations';

interface BriefingData {
  studentName: string;
  scores: {
    understanding: number;
    confidence: number;
    execution: number;
    forgettingRisk: number;
  };
  weakSkills: string[];
  suggestedQuestions: string[];
}

const EMPTY_BRIEFING: BriefingData = {
  studentName: '',
  scores: {
    understanding: 0,
    confidence: 0,
    execution: 0,
    forgettingRisk: 0,
  },
  weakSkills: [],
  suggestedQuestions: [],
};

function ScoreCell({ label, value }: { label: string; value: number }) {
  const color =
    value >= 70 ? 'text-emerald-400' :
    value >= 40 ? 'text-amber-400' :
    'text-rose-400';

  return (
    <div className="flex flex-col items-center justify-center p-3 rounded-xl bg-slate-700/50 border border-slate-600/50">
      <span className="text-[11px] text-slate-400 mb-1">{label}</span>
      <span className={`text-xl font-bold ${color}`}>{value}</span>
    </div>
  );
}

export default function PreBriefingPanel({ consultationId }: { consultationId?: string }) {
  const { data: briefing = EMPTY_BRIEFING } = useQuery({
    queryKey: ['consultation', 'briefing', consultationId],
    queryFn: async () => {
      if (!consultationId) return EMPTY_BRIEFING;
      try {
        const apiData = await consultationsApi.getConsultationBriefing(consultationId);
        return {
          studentName: (apiData as any).studentName ?? '',
          scores: {
            understanding: (apiData as any).scores?.understanding ?? 0,
            confidence: (apiData as any).scores?.confidence ?? 0,
            execution: (apiData as any).scores?.execution ?? 0,
            forgettingRisk: (apiData as any).scores?.forgettingRisk ?? 0,
          },
          weakSkills: apiData.riskAreas ?? [],
          suggestedQuestions: apiData.suggestedTopics ?? [],
        };
      } catch {
        return EMPTY_BRIEFING;
      }
    },
    enabled: !!consultationId,
    staleTime: 60_000,
  });

  return (
    <div className="h-full bg-gradient-to-b from-slate-800 to-slate-900 rounded-2xl p-5 text-white">
      <div className="flex items-center gap-3 mb-5">
        <div className="w-10 h-10 rounded-full bg-indigo-500/30 flex items-center justify-center text-sm font-bold text-indigo-300">
          {briefing.studentName ? briefing.studentName.charAt(0) : '?'}
        </div>
        <div>
          <h3 className="text-sm font-semibold">{briefing.studentName || '학생'}</h3>
          <span className="text-[11px] text-slate-400">AI 상담 브리핑</span>
        </div>
      </div>

      {/* 2x2 Score Grid */}
      <div className="grid grid-cols-2 gap-2 mb-5">
        <ScoreCell label="이해도" value={briefing.scores.understanding} />
        <ScoreCell label="자신감" value={briefing.scores.confidence} />
        <ScoreCell label="수행력" value={briefing.scores.execution} />
        <ScoreCell label="망각위험" value={briefing.scores.forgettingRisk} />
      </div>

      {/* Weak Skills */}
      <div className="mb-5">
        <h4 className="text-xs font-semibold text-slate-300 mb-2 uppercase tracking-wider">
          취약 스킬
        </h4>
        <ul className="space-y-1.5">
          {briefing.weakSkills.map((skill) => (
            <li key={skill} className="flex items-start gap-2 text-xs text-slate-300">
              <span className="w-1.5 h-1.5 rounded-full bg-rose-400 mt-1 flex-shrink-0" />
              {skill}
            </li>
          ))}
        </ul>
      </div>

      {/* Suggested Questions */}
      <div>
        <h4 className="text-xs font-semibold text-slate-300 mb-2 uppercase tracking-wider">
          추천 질문
        </h4>
        <div className="space-y-2">
          {briefing.suggestedQuestions.map((q, i) => (
            <div
              key={i}
              className="p-2.5 rounded-lg bg-slate-700/50 border border-slate-600/40 text-xs text-slate-300 leading-relaxed cursor-pointer hover:bg-slate-700 transition-colors"
            >
              <span className="text-indigo-400 font-semibold mr-1">Q{i + 1}.</span>
              {q}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
