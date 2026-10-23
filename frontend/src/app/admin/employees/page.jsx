'use client';
import ResourceList from '@/components/admin/PhucResourceList';
import { employees } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceList config={employees} />;
}
