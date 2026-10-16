'use client';
import ResourceForm from '@/components/admin/ResourceForm';
import { tables } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceForm config={tables} />;
}
