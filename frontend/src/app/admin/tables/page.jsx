'use client';
import ResourceList from '@/components/admin/ResourceList';
import { tables } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceList config={tables} />;
}
