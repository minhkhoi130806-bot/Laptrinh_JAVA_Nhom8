'use client';
import ResourceForm from '@/components/admin/PhucResourceForm';
import { tables } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceForm config={tables} />;
}
