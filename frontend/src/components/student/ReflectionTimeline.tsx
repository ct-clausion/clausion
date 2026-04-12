import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { reflectionsApi } from '../../api/reflections';
import { useAuthStore } from '../../store/authStore';
import type { Reflection } from '../../types';

const MOCK_REFLECTIONS: Reflection[] = [
  {
    id: 'ref1',
    studentId: 's1',
    courseId: 'c1',
    content:
      '오늘 리스트 컴프리헨션을 배웠다. 처음엔 어려웠지만 예제를 몇 개 풀어보니 감이 잡혔다.',
    selfConfidenceScore: 4,
    emotionSummary: '긍정적, 성취감',
    createdAt: '2026-04-07T20:00:00Z',
  },
  {
    id: 'ref2',
    studentId: 's1',
    courseId: 'c1',
    content:
      '재귀 함수가 너무 헷갈린다. 기저 조건을 잡는 게 어렵다. 좀 더 연습이 필요할 것 같다.',
    selfConfidenceScore: 2,
    emotionSummary: '불안, 혼란',
    createdAt: '2026-04-05T21:00:00Z',
  },
  {
    id: 'ref3',
    studentId: 's1',
    courseId: 'c1',
    content:
      '정렬 알고리즘 비교를 정리했다. 시간복잡도 개념이 재미있다.',
    selfConfidenceScore: 3,
    emotionSummary: '호기심',
    createdAt: '2026-04-03T19:00:00Z',
  },
  {
    id: 'ref4',
    studentId: 's1',
    courseId: 'c1',
    content:
      '변수와 자료형 기초를 복습했다. 확실히 알고 있다고 생각했는데 미묘한 차이가 있었다.',
    selfConfidenceScore: 4,
    emotionSummary: '차분함, 발견',
    createdAt: '2026-04-01T18:00:00Z',
  },
];

function renderStars(score: number) {
  return Array.from({ length: 5 }).map((_, i) => (
    <span
      key={i}
      className={`text-xs ${i < score ? 'text-amber-400' : 'text-slate-300'}`}
    >
      ★
    </span>
  ));
}

const EMOTION_COLORS: Record<string, string> = {
  긍정적: 'bg-emerald-100 text-emerald-700',
  성취감: 'bg-emerald-100 text-emerald-700',
  불안: 'bg-rose-100 text-rose-700',
  혼란: 'bg-amber-100 text-amber-700',
  호기심: 'bg-sky-100 text-sky-700',
  차분함: 'bg-indigo-100 text-indigo-700',
  발견: 'bg-violet-100 text-violet-700',
};

function getEmotionColor(emotion: string): string {
  for (const [key, val] of Object.entries(EMOTION_COLORS)) {
    if (emotion.includes(key)) return val;
  }
  return 'bg-slate-100 text-slate-600';
}

interface ReflectionTimelineProps {
  studentId?: string;
}

const ReflectionTimeline: React.FC<ReflectionTimelineProps> = ({
  studentId: studentIdProp,
}) => {
  const { user } = useAuthStore();
  const studentId = studentIdProp ?? user?.id?.toString() ?? '';

  const { data: reflections } = useQuery<Reflection[]>({
    queryKey: ['reflections', studentId],
    queryFn: () => reflectionsApi.getReflections(studentId),
    enabled: !!studentId,
    placeholderData: MOCK_REFLECTIONS,
  });

  const list = reflections ?? MOCK_REFLECTIONS;

  return (
    <div className="rounded-2xl bg-white p-6 shadow-sm border border-slate-100">
      <h2 className="text-lg font-bold text-slate-900 mb-5">성찰 타임라인</h2>

      <div className="relative">
        {/* Vertical line */}
        <div className="absolute left-3 top-2 bottom-2 w-px bg-slate-200" />

        <div className="space-y-5">
          {list.map((ref, i) => {
            const date = new Date(ref.createdAt);
            const dateStr = date.toLocaleDateString('ko-KR', {
              month: 'short',
              day: 'numeric',
            });
            const emotions = ref.emotionSummary ? ref.emotionSummary.split(',').map((e) => e.trim()) : [];

            return (
              <motion.div
                key={ref.id}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.06 }}
                className="relative pl-8"
              >
                {/* Dot */}
                <div className="absolute left-1.5 top-1.5 w-3 h-3 rounded-full bg-indigo-500 border-2 border-white" />

                <div className="rounded-xl border border-slate-100 bg-slate-50/50 p-3.5">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs text-slate-400">{dateStr}</span>
                    <div className="flex items-center gap-0.5">
                      {renderStars(ref.selfConfidenceScore)}
                    </div>
                  </div>
                  <p className="text-sm text-slate-700 leading-relaxed mb-2">
                    {ref.content}
                  </p>
                  <div className="flex items-center gap-1.5 flex-wrap">
                    {emotions.map((e, j) => (
                      <span
                        key={j}
                        className={`inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-medium ${getEmotionColor(e)}`}
                      >
                        {e}
                      </span>
                    ))}
                  </div>
                </div>
              </motion.div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default ReflectionTimeline;
