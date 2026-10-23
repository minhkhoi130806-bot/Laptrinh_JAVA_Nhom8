'use client';
import ResourceList from '@/components/admin/PhucResourceList';
import { customers } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceList config={customers} />;
}
