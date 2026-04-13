import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { questionsApi } from '../../api/questions';
import { useCourseId } from '../../hooks/useCourseId';
import type { Question } from '../../types';
import TagChip from '../common/TagChip';

const MOCK_QUESTIONS: Question[] = [];

const SKILL_NAMES: Record<string, string> = {
  s1: 'React Hooks',
  s2: 'TypeScript 제네릭',
  s3: '배열 알고리즘',
};

const difficultyLabel = (d: number) => {
  if (d <= 2) return { text: '기초', color: 'emerald' as const };
  if (d <= 3) return { text: '중급', color: 'amber' as const };
  return { text: '고급', color: 'rose' as const };
};

export default function QuestionReviewPanel() {
  const queryClient = useQueryClient();
  const courseId = useCourseId();
  const [actionedIds, setActionedIds] = useState<Set<string>>(new Set());

  const { data: questions = MOCK_QUESTIONS } = useQuery({
    queryKey: ['instructor', 'questions', 'pending', courseId],
    queryFn: () => questionsApi.getQuestions(courseId!, { approvalStatus: 'PENDING' }),
    enabled: !!courseId,
    placeholderData: MOCK_QUESTIONS,
    staleTime: 30_000,
  });

  const [errorId, setErrorId] = useState<string | null>(null);

  const approveMutation = useMutation({
    mutationFn: async ({ id, status }: { id: string; status: 'APPROVED' | 'REJECTED' }) => {
      if (status === 'APPROVED') return questionsApi.approveQuestion(id);
      return questionsApi.rejectQuestion(id);
    },
    onSuccess: (_, vars) => {
      setErrorId(null);
      setActionedIds((prev) => new Set(prev).add(vars.id));
      queryClient.invalidateQueries({ queryKey: ['instructor', 'questions'] });
    },
    onError: (_, vars) => {
      setErrorId(vars.id);
    },
  });

  const pendingQuestions = questions.filter((q) => !actionedIds.has(String(q.id)));

  return (
    <div className="bg-white/85 backdrop-blur-[12px] border border-white/60 rounded-2xl shadow-lg p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-slate-800">AI 생성 문제 검토</h3>
        <span className="text-xs text-slate-500">{pendingQuestions.length}개 대기</span>
      </div>

      <div className="space-y-3 max-h-[400px] overflow-y-auto pr-1">
        {pendingQuestions.map((q) => {
          const diff = difficultyLabel(Number(q.difficulty));
          return (
            <div
              key={q.id}
              className="p-3 rounded-xl bg-slate-50 border border-slate-100 space-y-2"
            >
              <p className="text-sm text-slate-700 leading-relaxed">{q.content}</p>

              <div className="flex items-center gap-2 flex-wrap">
                <TagChip label={SKILL_NAMES[q.skillId] ?? q.skillId} color="indigo" size="sm" />
                <TagChip label={diff.text} color={diff.color} size="sm" />
                <TagChip label={q.questionType} color="slate" size="sm" />
              </div>

              <p className="text-[11px] text-slate-400">{q.generationReason}</p>

              <div className="flex items-center gap-2 pt-1">
                <button
                  onClick={() => approveMutation.mutate({ id: String(q.id), status: 'APPROVED' })}
                  disabled={approveMutation.isPending}
                  className="px-3 py-1.5 text-xs font-medium rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-colors disabled:opacity-50"
                >
                  승인
                </button>
                <button
                  onClick={() => approveMutation.mutate({ id: String(q.id), status: 'REJECTED' })}
                  disabled={approveMutation.isPending}
                  className="px-3 py-1.5 text-xs font-medium rounded-lg bg-white border border-slate-300 text-slate-600 hover:bg-slate-50 transition-colors disabled:opacity-50"
                >
                  반려
                </button>
                {errorId === String(q.id) && (
                  <span className="text-xs text-rose-500">처리 실패</span>
                )}
              </div>
            </div>
          );
        })}

        {pendingQuestions.length === 0 && (
          <p className="text-sm text-slate-400 text-center py-8">
            검토 대기 중인 문제가 없습니다
          </p>
        )}
      </div>
    </div>
  );
}
