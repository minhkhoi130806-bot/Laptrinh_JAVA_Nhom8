'use client';
import ResourceForm from '@/components/admin/ResourceForm';
import { suppliers } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceForm config={suppliers} />;
}
