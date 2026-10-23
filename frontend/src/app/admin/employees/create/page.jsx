'use client';
import ResourceForm from '@/components/admin/PhucResourceForm';
import { employees } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceForm config={employees} />;
}
