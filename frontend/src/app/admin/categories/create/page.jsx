'use client';
import ResourceForm from '@/components/admin/ResourceForm';
import { categories } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceForm config={categories} />;
}
