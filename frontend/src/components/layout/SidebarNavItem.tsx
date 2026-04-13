import { useNavigate, useLocation } from 'react-router-dom';

interface SidebarNavItemProps {
  id: string;
  label: string;
  icon: string;
  path: string;
  badge?: string | number;
}

export default function SidebarNavItem({ label, icon, path, badge }: SidebarNavItemProps) {
  const navigate = useNavigate();
  const location = useLocation();

  // 정확한 경로 매칭 — /operator/students 와 /operator/students/at-risk 중복 방지
  const isActive = location.pathname === path || location.pathname === path + '/';

  const handleClick = () => {
    navigate(path);
  };

  return (
    <button
      onClick={handleClick}
      className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-150 group ${
        isActive
          ? 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-500/20'
          : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
      }`}
    >
      <span className={`text-base flex-shrink-0 ${isActive ? 'opacity-100' : 'opacity-70 group-hover:opacity-100'}`}>
        {icon}
      </span>
      <span className="flex-1 text-left truncate">{label}</span>
      {badge !== undefined && (
        <span
          className={`px-1.5 py-0.5 text-[10px] font-bold rounded-full ${
            isActive ? 'bg-white/20 text-white' : 'bg-indigo-100 text-indigo-600'
          }`}
        >
          {badge}
        </span>
      )}
    </button>
  );
}
