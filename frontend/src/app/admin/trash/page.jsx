'use client';
import { Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import TrashManager from '@/components/admin/TrashManager';

function Inner() {
  const type = useSearchParams().get('type');
  return <TrashManager initialType={type} />;
}

// /admin/trash?type=menu|customers|employees|tables
export default function Page() {
  return <Suspense fallback={null}><Inner /></Suspense>;
}
