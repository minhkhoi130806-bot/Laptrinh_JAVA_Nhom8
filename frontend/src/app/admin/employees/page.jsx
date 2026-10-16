'use client';
import ResourceList from '@/components/admin/ResourceList';
import { employees } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceList config={employees} />;
}
