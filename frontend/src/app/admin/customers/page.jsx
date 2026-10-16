'use client';
import ResourceList from '@/components/admin/ResourceList';
import { customers } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceList config={customers} />;
}
