'use client';
import ResourceList from '@/components/admin/PhucResourceList';
import EmployeeAccountManager from '@/components/admin/EmployeeAccountManager';
import { employees } from '@/lib/admin/resources';

export default function Page() {
  return (
    <>
      <EmployeeAccountManager />
      <ResourceList config={employees} />
    </>
  );
}
