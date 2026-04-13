import { api } from './client';

export interface HeatmapEntry {
  studentId: number;
  studentName: string;
  overallRiskScore: number;
  masteryScore: number;
  motivationScore: number;
  trendDirection: 'IMPROVING' | 'STABLE' | 'DECLINING' | null;
}

export interface StudentTwinEntry {
  studentId: number;
  studentName: string;
  masteryScore: number;
  executionScore: number;
  retentionRiskScore: number;
  motivationScore: number;
  consultationNeedScore: number;
  overallRiskScore: number;
  aiInsight: string | null;
  trendDirection: 'IMPROVING' | 'STABLE' | 'DECLINING' | null;
  updatedAt: string | null;
}

export interface EnrollmentEntry {
  enrollmentId: number;
  studentId: number;
  studentName: string;
  studentEmail: string;
  status: string;
  enrolledAt: string | null;
}

export const instructorApi = {
  // ── 출결 관리 ──────────────────────────────────────────────
  getAttendanceCourses: () =>
    api.get<Array<{ id: string; title: string; status: string }>>('/api/instructor/attendance/courses'),

  getAttendanceSessions: (courseId: string) =>
    api.get<Array<{ id: string; courseId: string; sessionDate: string; sessionNumber: number; title: string | null; status: string }>>(`/api/instructor/attendance/sessions/${courseId}`),

  createAttendanceSession: (data: { courseId: string; sessionDate: string; title?: string }) =>
    api.post<{ id: string; courseId: string; sessionDate: string; sessionNumber: number; title: string | null; status: string }>('/api/instructor/attendance/sessions', data),

  getAttendanceRecords: (sessionId: string) =>
    api.get<Array<{ id: string; sessionId: string; studentId: string; studentName: string; status: string; checkInTime: string | null; note: string | null }>>(`/api/instructor/attendance/records?sessionId=${sessionId}`),

  bulkUpdateAttendance: (records: Array<{ sessionId: string; studentId: string; status: string }>) =>
    api.post<void>('/api/instructor/attendance/records', records),

  getAttendanceStats: (courseId: string) =>
    api.get<{ courseId: string; totalSessions: number; avgAttendanceRate: number; studentStats: Array<{ studentId: string; studentName: string; presentCount: number; totalCount: number; rate: number }> }>(`/api/instructor/attendance/stats/${courseId}`),

  // ── 기존 API ──────────────────────────────────────────────
  getCourseHeatmap(courseId: string): Promise<HeatmapEntry[]> {
    return api.get<HeatmapEntry[]>(`/api/instructor/course/${courseId}/heatmap`);
  },

  getCourseStudents(courseId: string): Promise<StudentTwinEntry[]> {
    return api.get<StudentTwinEntry[]>(`/api/instructor/course/${courseId}/students`);
  },

  getEnrollments(courseId: string, status?: string): Promise<EnrollmentEntry[]> {
    const params = status ? `?status=${status}` : '';
    return api.get<EnrollmentEntry[]>(`/api/instructor/course/${courseId}/enrollments${params}`);
  },

  approveEnrollment(courseId: string, enrollmentId: number): Promise<EnrollmentEntry> {
    return api.put<EnrollmentEntry>(`/api/instructor/course/${courseId}/enrollments/${enrollmentId}/approve`);
  },

  rejectEnrollment(courseId: string, enrollmentId: number): Promise<EnrollmentEntry> {
    return api.put<EnrollmentEntry>(`/api/instructor/course/${courseId}/enrollments/${enrollmentId}/reject`);
  },
};
