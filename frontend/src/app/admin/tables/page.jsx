'use client';
import ResourceList from '@/components/admin/PhucResourceList';
import { tables } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceList config={tables} />;
}
