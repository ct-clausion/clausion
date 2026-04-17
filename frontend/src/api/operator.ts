import { api } from './client';
import type {
  OperatorDashboardSummary,
  CourseTwinSnapshot,
  Course,
  AttendanceRecord,
  CourseSessionInfo,
  Announcement,
  InterventionLog,
  OperatorAuditLog,
} from '../types';

// ── Dashboard ──────────────────────────────────────────────
export const operatorApi = {
  getDashboardSummary: () =>
    api.get<OperatorDashboardSummary>('/api/operator/dashboard/summary'),

  getCourseTwins: () =>
    api.get<CourseTwinSnapshot[]>('/api/operator/dashboard/course-twins'),

  getRiskAlerts: () =>
    api.get<Array<{ studentId: string; studentName: string; courseTitle: string; overallRisk: number; trend: string }>>('/api/operator/dashboard/risk-alerts'),

  getAttendanceToday: () =>
    api.get<{ totalSessions: number; avgAttendanceRate: number; absentCount: number }>('/api/operator/dashboard/attendance-today'),

  getPendingActions: () =>
    api.get<{ pendingCourses: number; pendingInterventions: number; unreadAlerts: number }>('/api/operator/dashboard/pending-actions'),

  // ── Course Management ──────────────────────────────────────
  getCourses: () =>
    api.get<Course[]>('/api/operator/courses'),

  getCourse: (id: string) =>
    api.get<Course & { courseTwin?: CourseTwinSnapshot }>(`/api/operator/courses/${id}`),

  approveCourse: (id: string, note?: string) =>
    api.put(`/api/operator/courses/${id}/approve`, { note }),

  rejectCourse: (id: string, note: string) =>
    api.put(`/api/operator/courses/${id}/reject`, { note }),

  updateCapacity: (id: string, capacity: number) =>
    api.put(`/api/operator/courses/${id}/capacity`, { maxCapacity: capacity }),

  // ── Personnel ──────────────────────────────────────────────
  getInstructors: () =>
    api.get<Array<{ id: string; name: string; email: string; courseCount: number; studentCount: number; consultationCount: number }>>('/api/operator/instructors'),

  getStudents: () =>
    api.get<Array<{ id: string; name: string; email: string; courseId: string | null; courseTitle: string; overallRisk: number; trend: string; attendanceRate: number }>>('/api/operator/students'),

  createIntervention: (data: { studentId: string; courseId: string; interventionType: string; description: string; aiSuggested?: boolean }) =>
    api.post<InterventionLog>('/api/operator/interventions', data),

  getInterventions: () =>
    api.get<InterventionLog[]>('/api/operator/interventions'),

  // ── Instructor Effectiveness (횡단 비교) ──────────────────
  getInstructorEffectiveness: () =>
    api.get<Array<{ id: string; name: string; courseCount: number; studentCount: number; avgMastery: number; avgMotivation: number; avgOverallRisk: number; consultationCount: number; atRiskStudentCount: number }>>('/api/operator/instructors/effectiveness'),

  getInstructorWorkload: () =>
    api.get<Array<{ id: string; name: string; studentCount: number; totalCapacity: number; consultationCount: number; courseCount: number; workloadScore: number; isOverloaded: boolean }>>('/api/operator/instructors/workload'),

  // ── Intervention Center (강사별 그룹핑) ──────────────────
  getInterventionCenter: () =>
    api.get<Array<{ instructorId: string; instructorName: string; courseName: string; courseId: string; atRiskStudents: Array<{ studentId: string; studentName: string; overallRisk: number; trend: string }>; aiSuggestion: string; studentCount: number }>>('/api/operator/intervention-center'),

  sendInterventionDirective: (data: { instructorId: string; studentIds: string[]; directiveType: string; message: string }) =>
    api.post('/api/operator/intervention-directive', data),

  getInterventionDirectives: () =>
    api.get<InterventionLog[]>('/api/operator/intervention-directives'),

  // ── Attendance ─────────────────────────────────────────────
  getSessions: (courseId: string) =>
    api.get<CourseSessionInfo[]>(`/api/operator/attendance/sessions/${courseId}`),

  createSession: (data: { courseId: string; sessionDate: string; title?: string }) =>
    api.post<CourseSessionInfo>('/api/operator/attendance/sessions', data),

  getAttendanceRecords: (sessionId: string) =>
    api.get<AttendanceRecord[]>(`/api/operator/attendance/records?sessionId=${sessionId}`),

  bulkUpdateAttendance: (records: Array<{ sessionId: string; studentId: string; status: string }>) =>
    api.post('/api/operator/attendance/records', records),

  getAttendanceStats: (courseId: string) =>
    api.get<{ courseId: string; totalSessions: number; avgAttendanceRate: number; studentStats: Array<{ studentId: string; studentName: string; presentCount: number; totalCount: number; rate: number }> }>(`/api/operator/attendance/stats/${courseId}`),

  // ── Announcements ──────────────────────────────────────────
  getAnnouncements: () =>
    api.get<Announcement[]>('/api/operator/announcements'),

  createAnnouncement: (data: { title: string; content: string; targetType: string; targetCourseId?: string; isUrgent?: boolean }) =>
    api.post<Announcement>('/api/operator/announcements', data),

  updateAnnouncement: (id: string, data: { title: string; content: string }) =>
    api.put<Announcement>(`/api/operator/announcements/${id}`, data),

  deleteAnnouncement: (id: string) =>
    api.delete(`/api/operator/announcements/${id}`),

  getAnnouncementStats: (id: string) =>
    api.get<{ totalRecipients: number; readCount: number; readRate: number }>(`/api/operator/announcements/${id}/stats`),

  // ── AI Operations ──────────────────────────────────────────
  simulate: (data: { scenarioType: string; targetStudentId?: string; targetCourseId?: string; parameters?: Record<string, unknown> }) =>
    api.post<{ currentScores: Record<string, number>; projectedScores: Record<string, number>; confidence: number; aiInterpretation: string; recommendation: string }>('/api/operator/ai/simulate', data),

  getWeeklyReport: () =>
    api.get<{ periodStart: string; periodEnd: string; summary: Record<string, unknown>; anomalies: string[]; recommendations: string[] }>('/api/operator/reports/weekly'),

  getInterventionSuggestions: () =>
    api.get<Array<{ studentId: string; studentName: string; courseTitle: string; suggestedAction: string; expectedImpact: string; urgency: string }>>('/api/operator/ai/intervention-suggestions'),

  // ── Audit ──────────────────────────────────────────────────
  getAuditLogs: (
    page = 0,
    size = 20,
    filters?: { actionType?: string; targetType?: string },
  ) => {
    const qs = new URLSearchParams({ page: String(page), size: String(size) });
    if (filters?.actionType) qs.set('actionType', filters.actionType);
    if (filters?.targetType) qs.set('targetType', filters.targetType);
    return api.get<{ content: OperatorAuditLog[]; totalPages: number; totalElements: number }>(
      `/api/operator/audit-logs?${qs.toString()}`,
    );
  },
};
