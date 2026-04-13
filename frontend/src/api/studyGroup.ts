import { api } from './client';
import type { StudyGroupMember, StudyGroup } from '../types';

export const studyGroupApi = {
  getMatches(studentId: string, courseId: string): Promise<StudyGroupMember[]> {
    return api.get<StudyGroupMember[]>(`/api/study-groups/matches/${studentId}?courseId=${courseId}`);
  },

  createStudyGroup(data: {
    courseId: string;
    name: string;
    description: string;
    maxMembers: number;
  }): Promise<StudyGroup> {
    return api.post<StudyGroup>('/api/study-groups', data);
  },

  getStudyGroup(groupId: string): Promise<StudyGroup> {
    return api.get<StudyGroup>(`/api/study-groups/${groupId}`);
  },

  joinGroup(groupId: string): Promise<StudyGroup> {
    return api.post<StudyGroup>(`/api/study-groups/${groupId}/join`);
  },

  leaveGroup(groupId: string): Promise<void> {
    return api.delete<void>(`/api/study-groups/${groupId}/leave`);
  },

  kickMember(groupId: string, studentId: string): Promise<void> {
    return api.delete<void>(`/api/study-groups/${groupId}/members/${studentId}`);
  },

  deleteGroup(groupId: string): Promise<void> {
    return api.delete<void>(`/api/study-groups/${groupId}`);
  },

  getMyGroups(): Promise<StudyGroup[]> {
    return api.get<StudyGroup[]>('/api/study-groups/my');
  },

  getByCourse(courseId: string): Promise<StudyGroup[]> {
    return api.get<StudyGroup[]>(`/api/study-groups/course/${courseId}`);
  },
};
