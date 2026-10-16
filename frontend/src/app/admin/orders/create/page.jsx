'use client';
import { Suspense } from 'react';
import OrderCreate from '@/components/admin/OrderCreate';

export default function Page() {
  return <Suspense fallback={null}><OrderCreate /></Suspense>;
}
