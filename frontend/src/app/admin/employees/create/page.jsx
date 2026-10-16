'use client';
import ResourceForm from '@/components/admin/ResourceForm';
import { employees } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceForm config={employees} />;
}
