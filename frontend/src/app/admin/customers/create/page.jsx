'use client';
import ResourceForm from '@/components/admin/ResourceForm';
import { customers } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceForm config={customers} />;
}
