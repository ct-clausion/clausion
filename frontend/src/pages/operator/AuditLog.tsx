import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { operatorApi } from '../../api/operator';
import GlassCard from '../../components/common/GlassCard';

const actionLabels: Record<string, string> = {
  COURSE_APPROVE: '과정 승인',
  COURSE_REJECT: '과정 반려',
  ATTENDANCE_EDIT: '출결 수정',
  INTERVENTION_CREATE: '개입 생성',
  ANNOUNCEMENT_CREATE: '공지 작성',
  ANNOUNCEMENT_DELETE: '공지 삭제',
  CAPACITY_CHANGE: '정원 변경',
};

const targetLabels: Record<string, string> = {
  COURSE: '과정',
  STUDENT: '수강생',
  INSTRUCTOR: '강사',
  ANNOUNCEMENT: '공지사항',
  ATTENDANCE: '출결',
};

export default function AuditLog() {
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['operator', 'audit-logs', page],
    queryFn: () => operatorApi.getAuditLogs(page),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">감사 로그</h1>
        <p className="text-sm text-slate-500 mt-1">운영자 활동 이력 추적</p>
      </div>

      {isLoading ? (
        <p className="text-sm text-slate-400">로딩 중...</p>
      ) : (
        <>
          <GlassCard className="overflow-hidden">
            <div className="overflow-x-auto">
            <table className="w-full text-sm min-w-[640px]">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50">
                  <th className="text-left p-4 font-semibold text-slate-600">시간</th>
                  <th className="text-left p-4 font-semibold text-slate-600">행위</th>
                  <th className="text-left p-4 font-semibold text-slate-600">대상</th>
                  <th className="text-left p-4 font-semibold text-slate-600">대상 ID</th>
                  <th className="text-left p-4 font-semibold text-slate-600">상세</th>
                </tr>
              </thead>
              <tbody>
                {data?.content?.map((log) => (
                  <tr key={log.id} className="border-b border-slate-100 hover:bg-slate-50">
                    <td className="p-4 text-slate-500 whitespace-nowrap">{log.createdAt?.slice(0, 16).replace('T', ' ')}</td>
                    <td className="p-4">
                      <span className="px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-700 text-xs font-bold">
                        {actionLabels[log.actionType] ?? log.actionType}
                      </span>
                    </td>
                    <td className="p-4 text-slate-700">{targetLabels[log.targetType] ?? log.targetType}</td>
                    <td className="p-4 text-slate-500">#{log.targetId}</td>
                    <td className="p-4 text-xs max-w-[250px]">
                      {log.details && typeof log.details === 'object' ? (
                        <div className="space-y-0.5">
                          {Object.entries(log.details).map(([k, v]) => (
                            <div key={k} className="flex gap-1">
                              <span className="text-slate-400 shrink-0">{k}:</span>
                              <span className="text-slate-600 truncate">{String(v)}</span>
                            </div>
                          ))}
                        </div>
                      ) : log.details ? (
                        <span className="text-slate-500">{String(log.details)}</span>
                      ) : (
                        <span className="text-slate-300">-</span>
                      )}
                    </td>
                  </tr>
                ))}
                {(!data?.content || data.content.length === 0) && (
                  <tr><td colSpan={5} className="p-8 text-center text-slate-400">감사 로그가 없습니다.</td></tr>
                )}
              </tbody>
            </table>
            </div>
          </GlassCard>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2">
              <button
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 rounded-lg border border-slate-300 text-sm disabled:opacity-30"
              >
                이전
              </button>
              <span className="text-sm text-slate-500">
                {page + 1} / {data.totalPages}
              </span>
              <button
                onClick={() => setPage(Math.min(data.totalPages - 1, page + 1))}
                disabled={page >= data.totalPages - 1}
                className="px-3 py-1.5 rounded-lg border border-slate-300 text-sm disabled:opacity-30"
              >
                다음
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
