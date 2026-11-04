'use client';

import { useEffect, useState } from 'react';

export const canTakePayment = (role) => role === 'MANAGER' || role === 'CASHIER';

const PAGE_ROLES = {
  '/admin/kitchen': ['MANAGER', 'CHEF'],
  '/admin/employees': ['MANAGER'],
  '/admin/invoices': ['MANAGER', 'CASHIER'],
  '/admin/analytics': ['MANAGER', 'CASHIER'],
  '/admin/reports': ['MANAGER', 'CASHIER'],
  '/admin/marketing': ['MANAGER'],
  '/admin/shifts': ['MANAGER'],
  '/admin/settings': ['MANAGER'],
};

export function canAccessAdminRoute(path, role) {
  if (!['MANAGER', 'CASHIER', 'WAITER', 'CHEF'].includes(role)) return false;
  const route = Object.keys(PAGE_ROLES).find((entry) => path === entry || path.startsWith(`${entry}/`));
  return !route || PAGE_ROLES[route].includes(role);
}

export function useStaffRole() {
  const [role, setRole] = useState(null);
  useEffect(() => {
    const restore = () => {
      try {
        setRole(JSON.parse(localStorage.getItem('currentUser') || 'null')?.role || null);
      } catch {
        setRole(null);
      }
    };
    restore();
    window.addEventListener('storage', restore);
    return () => window.removeEventListener('storage', restore);
  }, []);
  return role;
}
