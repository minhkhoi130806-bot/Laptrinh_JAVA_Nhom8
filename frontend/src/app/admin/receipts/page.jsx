'use client';
import ResourceList from '@/components/admin/ResourceList';
import { receipts } from '@/lib/admin/resources';

export default function Page() {
  return <ResourceList config={receipts} />;
}
