'use client';
import ResourceList from '@/components/admin/ResourceList';
import { categories } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceList config={categories} />;
}
