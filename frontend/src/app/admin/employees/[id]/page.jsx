'use client';
import { useParams } from 'next/navigation';
import ResourceForm from '@/components/admin/PhucResourceForm';
import { employees } from '@/lib/admin/resources';

export default function Page() {
  const { id } = useParams();
  return <ResourceForm config={employees} id={id} />;
}
