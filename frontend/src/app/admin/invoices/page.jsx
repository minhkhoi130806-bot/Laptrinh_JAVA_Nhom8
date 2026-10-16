'use client';
import ResourceList from '@/components/admin/ResourceList';
import { invoices } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceList config={invoices} />;
}
