'use client';
import ResourceList from '@/components/admin/ResourceList';
import { orders } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceList config={orders} />;
}
