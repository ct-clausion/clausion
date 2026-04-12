import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { reviewsApi } from '../../api/reviews';
import ReviewTimeline from '../../components/student/ReviewTimeline';
import type { ReviewTask } from '../../types';

const MOCK_TASKS: ReviewTask[] = [
  {
    id: '1',
    studentId: 's1',
    courseId: 'c1',
    skillId: 'sk1',
    title: 'Python 리스트 컴프리헨션 복습',
    reasonSummary: '망각위험 - 3일 전 학습 후 복습 없음',
    scheduledFor: '2026-04-08',
    status: 'PENDING',
    completedAt: null,
  },
  {
    id: '2',
    studentId: 's1',
    courseId: 'c1',
    skillId: 'sk2',
    title: '재귀 함수 기초 연습',
    reasonSummary: '약점보강 - 지난 과제에서 오류 빈발',
    scheduledFor: '2026-04-08',
    status: 'IN_PROGRESS',
    completedAt: null,
  },
  {
    id: '3',
    studentId: 's1',
    courseId: 'c1',
    skillId: 'sk3',
    title: '정렬 알고리즘 비교 정리',
    reasonSummary: '상담후속 - 강사 상담에서 권장',
    scheduledFor: '2026-04-08',
    status: 'COMPLETED',
    completedAt: '2026-04-08T10:00:00Z',
  },
  {
    id: '4',
    studentId: 's1',
    courseId: 'c1',
    skillId: 'sk4',
    title: '딕셔너리 활용 패턴',
    reasonSummary: '망각위험 - 주기 복습 예정',
    scheduledFor: '2026-04-07',
    status: 'COMPLETED',
    completedAt: '2026-04-07T15:00:00Z',
  },
  {
    id: '5',
    studentId: 's1',
    courseId: 'c1',
    skillId: 'sk5',
    title: '클로저 개념 이해',
    reasonSummary: '약점보강 - 이해도 점수 낮음',
    scheduledFor: '2026-04-06',
    status: 'SKIPPED',
    completedAt: null,
  },
];

type FilterStatus = 'all' | 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'SKIPPED';

const STATUS_BADGE: Record<string, { label: string; color: string }> = {
  PENDING: { label: '대기', color: 'bg-slate-100 text-slate-600' },
  IN_PROGRESS: { label: '진행 중', color: 'bg-indigo-100 text-indigo-700' },
  COMPLETED: { label: '완료', color: 'bg-emerald-100 text-emerald-700' },
  SKIPPED: { label: '건너뜀', color: 'bg-rose-100 text-rose-600' },
};

const Review: React.FC = () => {
  const [filter, setFilter] = useState<FilterStatus>('all');
  const queryClient = useQueryClient();

  const { data: tasks } = useQuery<ReviewTask[]>({
    queryKey: ['reviewsToday'],
    queryFn: () => reviewsApi.getTodayReviews(),
    placeholderData: MOCK_TASKS,
  });

  const completeMutation = useMutation({
    mutationFn: (reviewId: string) => reviewsApi.completeReview(reviewId),
    onMutate: async (reviewId) => {
      await queryClient.cancelQueries({ queryKey: ['reviewsToday'] });
      const previous = queryClient.getQueryData<ReviewTask[]>(['reviewsToday']);
      queryClient.setQueryData<ReviewTask[]>(['reviewsToday'], (old) =>
        old?.map((t) =>
          String(t.id) === String(reviewId)
            ? { ...t, status: 'COMPLETED' as const, completedAt: new Date().toISOString() }
            : t,
        ),
      );
      return { previous };
    },
    onError: (_err, _id, context) => {
      if (context?.previous) {
        queryClient.setQueryData(['reviewsToday'], context.previous);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['reviewsToday'] });
    },
  });

  const list = tasks ?? MOCK_TASKS;
  const filtered =
    filter === 'all' ? list : list.filter((t) => t.status === filter);

  const stats = {
    total: list.length,
    completed: list.filter((t) => t.status === 'COMPLETED').length,
    pending: list.filter(
      (t) => t.status === 'PENDING' || t.status === 'IN_PROGRESS',
    ).length,
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="sticky top-0 z-30 bg-white/80 backdrop-blur border-b border-slate-100">
        <div className="max-w-5xl mx-auto px-6 py-3">
          <h1 className="text-xl font-bold text-slate-900">복습 관리</h1>
          <p className="text-xs text-slate-500">
            AI가 분석한 맞춤 복습 과제를 확인하세요
          </p>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-6 space-y-6">
        {/* Stats */}
        <div className="grid grid-cols-3 gap-4">
          {[
            {
              label: '전체 과제',
              value: stats.total,
              color: 'text-slate-800',
              bg: 'bg-white',
            },
            {
              label: '완료',
              value: stats.completed,
              color: 'text-emerald-700',
              bg: 'bg-emerald-50',
            },
            {
              label: '진행 대기',
              value: stats.pending,
              color: 'text-indigo-700',
              bg: 'bg-indigo-50',
            },
          ].map((s) => (
            <div
              key={s.label}
              className={`rounded-xl ${s.bg} border border-slate-100 p-4 text-center`}
            >
              <p className="text-xs text-slate-500">{s.label}</p>
              <p className={`text-2xl font-bold ${s.color} mt-1`}>{s.value}</p>
            </div>
          ))}
        </div>

        {/* Weekly timeline */}
        <ReviewTimeline />

        {/* Filter tabs */}
        <div className="flex gap-2">
          {(
            [
              { key: 'all', label: '전체' },
              { key: 'PENDING', label: '대기' },
              { key: 'IN_PROGRESS', label: '진행 중' },
              { key: 'COMPLETED', label: '완료' },
              { key: 'SKIPPED', label: '건너뜀' },
            ] as { key: FilterStatus; label: string }[]
          ).map((f) => (
            <button
              key={f.key}
              onClick={() => setFilter(f.key)}
              className={`rounded-full px-3.5 py-1.5 text-xs font-medium transition-colors ${
                filter === f.key
                  ? 'bg-indigo-600 text-white'
                  : 'bg-white text-slate-600 border border-slate-300 hover:bg-slate-50'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>

        {/* Task list */}
        <div className="space-y-3">
          {filtered.map((task, i) => {
            const badge = STATUS_BADGE[task.status] ?? STATUS_BADGE.PENDING;
            return (
              <motion.div
                key={task.id}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.04 }}
                className="rounded-xl bg-white border border-slate-100 p-4 flex items-center gap-4 hover:shadow-sm transition-shadow"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span
                      className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold ${badge.color}`}
                    >
                      {badge.label}
                    </span>
                    <span className="text-[10px] text-slate-400">
                      {new Date(task.scheduledFor).toLocaleDateString('ko-KR', {
                        month: 'short',
                        day: 'numeric',
                      })}
                    </span>
                  </div>
                  <h3 className="text-sm font-semibold text-slate-800">
                    {task.title}
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    {task.reasonSummary}
                  </p>
                </div>
                {(task.status === 'PENDING' ||
                  task.status === 'IN_PROGRESS') && (
                  <button
                    onClick={() => completeMutation.mutate(task.id)}
                    disabled={completeMutation.isPending}
                    className="shrink-0 rounded-lg bg-indigo-50 px-4 py-2 text-xs font-medium text-indigo-600 hover:bg-indigo-100 transition-colors disabled:opacity-50"
                  >
                    완료
                  </button>
                )}
                {task.status === 'COMPLETED' && task.completedAt && (
                  <span className="text-xs text-emerald-500 shrink-0">
                    {new Date(task.completedAt).toLocaleTimeString('ko-KR', {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}{' '}
                    완료
                  </span>
                )}
              </motion.div>
            );
          })}
          {filtered.length === 0 && (
            <div className="text-center py-12">
              <p className="text-sm text-slate-400">
                해당 상태의 복습 과제가 없습니다
              </p>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

export default Review;
