'use client';

import { useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';

function TraCuuDatBanRedirect() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const phone = searchParams.get('phone');
    if (phone) {
      router.replace(`/don-hang?tab=reservations&phone=${encodeURIComponent(phone)}`);
    } else {
      router.replace('/don-hang?tab=reservations');
    }
  }, [router, searchParams]);

  return (
    <div style={{ textAlign: 'center', padding: '100px 20px', color: '#718096', fontSize: '15px' }}>
      <span className="shop-spinner" style={{ marginRight: '10px' }} />
      Đang mở lịch sử đặt bàn của bạn...
    </div>
  );
}

export default function ReservationLookupPage() {
  return (
    <Suspense fallback={null}>
      <TraCuuDatBanRedirect />
    </Suspense>
  );
}
