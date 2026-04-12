import { useState, useEffect, useCallback, useRef } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import IncomingCallModal from '../consultation/IncomingCallModal';
import ChatbotFloatingButton from '../chatbot/ChatbotFloatingButton';
import ChatbotModal from '../chatbot/ChatbotModal';
import { useNotifications } from '../../hooks/useNotifications';

interface AppShellProps {
  role: 'student' | 'instructor' | 'operator';
}

interface CallInfo {
  consultationId: number;
  roomName: string;
  callerName: string;
  courseName?: string;
}

export default function AppShell({ role }: AppShellProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const isStudentDashboard = role === 'student' && (location.pathname === '/student' || location.pathname === '/student/');
  const { notifications } = useNotifications();
  const [incomingCall, setIncomingCall] = useState<CallInfo | null>(null);
  const handledNotifKey = useRef<string | null>(null);

  // Listen for INCOMING_CALL notifications (student side)
  useEffect(() => {
    if (role !== 'student') return;

    const latest = notifications[0];
    if (
      latest &&
      !latest.isRead &&
      latest.type === 'INCOMING_CALL' &&
      latest.data
    ) {
      // Dedup key: use id if available (REST), otherwise timestamp+type
      const key = latest.id ?? `${latest.createdAt ?? ''}_${latest.type}`;
      if (key === handledNotifKey.current) return;

      try {
        const raw = latest.data;
        const data: Record<string, unknown> =
          typeof raw === 'string' ? JSON.parse(raw) : (raw as Record<string, unknown>);
        handledNotifKey.current = key;
        setIncomingCall({
          consultationId: Number(data.consultationId),
          roomName: String(data.roomName),
          callerName: String(data.callerName),
          courseName: latest.message,
        });
      } catch {
        // skip malformed notification — don't mark as handled
      }
    }
  }, [notifications, role]);

  const handleAccept = useCallback(() => {
    if (!incomingCall) return;
    setIncomingCall(null);
    navigate(
      `/student/consultation/${incomingCall.consultationId}/video?room=${incomingCall.roomName}`,
    );
  }, [incomingCall, navigate]);

  const handleReject = useCallback(() => {
    setIncomingCall(null);
  }, []);

  return (
    <div className="flex h-screen bg-slate-50 overflow-hidden">
      <Sidebar role={role} />
      <main className="flex-1 overflow-y-auto">
        <div className="p-6 max-w-7xl mx-auto">
          <Outlet />
        </div>
      </main>

      {role === 'student' && (
        <>
          <IncomingCallModal
            visible={!!incomingCall}
            callerName={incomingCall?.callerName ?? ''}
            courseName={incomingCall?.courseName}
            onAccept={handleAccept}
            onReject={handleReject}
          />
          {!isStudentDashboard && <ChatbotFloatingButton />}
          {!isStudentDashboard && <ChatbotModal />}
        </>
      )}
    </div>
  );
}
