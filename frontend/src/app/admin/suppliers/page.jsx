'use client';
import ResourceList from '@/components/admin/ResourceList';
import { suppliers } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceList config={suppliers} />;
}
