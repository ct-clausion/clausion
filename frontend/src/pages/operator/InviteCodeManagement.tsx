import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../api/client';
import GlassCard from '../../components/common/GlassCard';

interface InviteCode {
  id: number;
  code: string;
  createdByName: string | null;
  isUsed: boolean;
  usedByName: string | null;
  expiresAt: string;
  createdAt: string;
  usedAt: string | null;
}

export default function InviteCodeManagement() {
  const queryClient = useQueryClient();
  const [expiryDays, setExpiryDays] = useState(7);
  const [copiedId, setCopiedId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const { data: codes = [], isLoading: loading } = useQuery({
    queryKey: ['operator', 'invite-codes'],
    queryFn: () => api.get<InviteCode[]>('/api/operator/invite-codes'),
  });

  const createMutation = useMutation({
    mutationFn: () => api.post<InviteCode>('/api/operator/invite-codes', { expiryDays }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['operator', 'invite-codes'] });
      setError(null);
    },
    onError: (err: Error) => {
      setError(err.message || '초대 코드 생성에 실패했습니다.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/api/operator/invite-codes/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['operator', 'invite-codes'] });
    },
    onError: (err: Error) => {
      setError(err.message || '삭제에 실패했습니다.');
    },
  });

  const isExpired = (expiresAt: string) => new Date(expiresAt) < new Date();

  const getStatus = (code: InviteCode): 'active' | 'used' | 'expired' => {
    if (code.isUsed) return 'used';
    if (isExpired(code.expiresAt)) return 'expired';
    return 'active';
  };

  const statusBadge = (status: 'active' | 'used' | 'expired') => {
    if (status === 'used') {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-600">
          사용됨
        </span>
      );
    }
    if (status === 'expired') {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-50 text-red-600 border border-red-100">
          만료
        </span>
      );
    }
    return (
      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-100">
        활성
      </span>
    );
  };

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });

  const handleCopyCode = (code: InviteCode) => {
    navigator.clipboard.writeText(code.code).then(() => {
      setCopiedId(code.id);
      setTimeout(() => setCopiedId(null), 2000);
    });
  };

  const handleCreate = () => {
    setCreateError(null);
    createMutation.mutate();
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">초대 코드 관리</h1>
        <p className="text-sm text-slate-500 mt-1">
          운영자 계정 가입을 위한 초대 코드를 생성하고 관리합니다.
        </p>
      </div>

      {/* 생성 섹션 */}
      <GlassCard className="p-5">
        <h2 className="text-sm font-bold text-slate-800 mb-4">새 초대 코드 생성</h2>
        <div className="flex items-end gap-3 flex-wrap">
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">유효 기간</label>
            <select
              value={expiryDays}
              onChange={(e) => setExpiryDays(Number(e.target.value))}
              className="px-3 py-2 rounded-lg border border-slate-300 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-slate-400"
            >
              <option value={1}>1일</option>
              <option value={3}>3일</option>
              <option value={7}>7일</option>
              <option value={14}>14일</option>
              <option value={30}>30일</option>
            </select>
          </div>
          <button
            onClick={() => createMutation.mutate()}
            disabled={createMutation.isPending}
            className="px-4 py-2 rounded-lg bg-slate-800 text-white text-sm font-medium hover:bg-slate-900 disabled:opacity-50 transition-colors"
          >
            {createMutation.isPending ? '생성 중...' : '코드 생성'}
          </button>
        </div>

      {/* Error message */}
      {error && (
        <div className="p-3 rounded-lg bg-rose-50 border border-rose-200">
          <p className="text-sm text-rose-700">{error}</p>
        </div>
      )}

      {/* Code list */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <div className="px-5 py-3 border-b border-slate-100">
          <h2 className="text-sm font-bold text-slate-800">초대 코드 목록</h2>
        </div>

        {isLoading ? (
          <div className="p-10 text-center text-sm text-slate-400">불러오는 중...</div>
        ) : codes.length === 0 ? (
          <div className="p-10 text-center text-sm text-slate-400">
            생성된 초대 코드가 없습니다.
          </div>
        ) : (
          <div className="overflow-x-auto">
          <table className="w-full min-w-[700px]">
            <thead>
              <tr className="text-xs text-slate-500 border-b border-slate-100">
                <th className="text-left px-5 py-2 font-medium">코드</th>
                <th className="text-left px-5 py-2 font-medium">상태</th>
                <th className="text-left px-5 py-2 font-medium">생성자</th>
                <th className="text-left px-5 py-2 font-medium">사용자</th>
                <th className="text-left px-5 py-2 font-medium">만료일</th>
                <th className="text-left px-5 py-2 font-medium">생성일</th>
                <th className="text-right px-5 py-2 font-medium"></th>
              </tr>
            </thead>
            <tbody>
              {codes.map((code) => (
                <tr key={code.id} className="border-b border-slate-50 hover:bg-slate-50/50">
                  <td className="px-5 py-3">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-sm font-bold text-slate-900 tracking-wider">{code.code}</span>
                      {!code.isUsed && !isExpired(code.expiresAt) && (
                        <button
                          onClick={() => {
                            const url = `${window.location.origin}/operator/login`;
                            navigator.clipboard.writeText(`운영자 초대 코드: ${code.code}\n가입 링크: ${url}`);
                            setCopiedId(code.id);
                            setTimeout(() => setCopiedId(null), 2000);
                          }}
                          className="px-2 py-0.5 text-[10px] font-medium rounded bg-indigo-50 text-indigo-600 hover:bg-indigo-100 transition-colors"
                        >
                          {copiedId === code.id ? '복사됨!' : '코드+링크 복사'}
                        </button>
                      )}
                    </div>
                  </td>
                  <td className="px-5 py-3">{getStatusBadge(code)}</td>
                  <td className="px-5 py-3 text-sm text-slate-600">{code.createdByName ?? '-'}</td>
                  <td className="px-5 py-3 text-sm text-slate-600">{code.usedByName ?? '-'}</td>
                  <td className="px-5 py-3 text-xs text-slate-500">{formatDate(code.expiresAt)}</td>
                  <td className="px-5 py-3 text-xs text-slate-500">{formatDate(code.createdAt)}</td>
                  <td className="px-5 py-3 text-right">
                    {!code.isUsed && (
                      <button
                        onClick={() => deleteMutation.mutate(code.id)}
                        className="text-xs text-red-500 hover:text-red-700 font-medium"
                      >
                        삭제
                      </button>
                    )}
                  </td>
                </tr>
              </thead>
              <tbody>
                {codes.map((code) => {
                  const status = getStatus(code);
                  return (
                    <tr
                      key={code.id}
                      className="border-b border-slate-50 hover:bg-slate-50/40 transition-colors"
                    >
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            title="클릭하여 복사"
                            onClick={() => handleCopyCode(code)}
                            className="group flex items-center gap-2 focus:outline-none"
                          >
                            <span className="font-mono text-sm font-bold tracking-widest text-slate-900 bg-slate-100 group-hover:bg-indigo-50 group-hover:text-indigo-700 px-2.5 py-1 rounded-md transition-colors select-all">
                              {code.code}
                            </span>
                            <span className="text-[10px] font-medium text-slate-400 group-hover:text-indigo-500 transition-colors whitespace-nowrap">
                              {copiedId === code.id ? '복사됨!' : '복사'}
                            </span>
                          </button>
                          {!code.isUsed && !isExpired(code.expiresAt) && (
                            <button
                              type="button"
                              onClick={() => {
                                const url = `${window.location.origin}/operator/login`;
                                navigator.clipboard.writeText(`운영자 초대 코드: ${code.code}\n가입 링크: ${url}`);
                                setCopiedId(code.id);
                                setTimeout(() => setCopiedId(null), 2000);
                              }}
                              className="px-2 py-0.5 text-[10px] font-medium rounded bg-indigo-50 text-indigo-600 hover:bg-indigo-100 transition-colors"
                            >
                              코드+링크
                            </button>
                          )}
                        </div>
                      </td>
                      <td className="px-5 py-3">{statusBadge(status)}</td>
                      <td className="px-5 py-3 text-sm text-slate-600">
                        {code.createdByName ?? '-'}
                      </td>
                      <td className="px-5 py-3 text-sm text-slate-600">
                        {code.usedByName ?? '-'}
                      </td>
                      <td className="px-5 py-3 text-xs text-slate-500">
                        {formatDate(code.expiresAt)}
                      </td>
                      <td className="px-5 py-3 text-xs text-slate-500">
                        {formatDate(code.createdAt)}
                      </td>
                      <td className="px-5 py-3 text-right">
                        {!code.isUsed && (
                          <button
                            type="button"
                            onClick={() => deleteMutation.mutate(code.id)}
                            disabled={deleteMutation.isPending}
                            className="text-xs text-red-500 hover:text-red-700 font-medium disabled:opacity-40 transition-colors"
                          >
                            삭제
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </GlassCard>
    </div>
  );
}
