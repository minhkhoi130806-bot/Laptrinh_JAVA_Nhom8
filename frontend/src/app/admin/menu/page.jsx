'use client';
import ResourceList from '@/components/admin/ResourceList';
import { menu } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceList config={menu} />;
}
