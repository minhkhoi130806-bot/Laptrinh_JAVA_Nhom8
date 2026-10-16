'use client';
import ResourceForm from '@/components/admin/ResourceForm';
import { ingredients } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceForm config={ingredients} />;
}
