'use client';
import ResourceForm from '@/components/admin/ResourceForm';
import { menu } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceForm config={menu} />;
}
