import assert from 'node:assert/strict';
import { test } from 'node:test';
import { canAccessAdminRoute, canTakePayment } from '../src/lib/admin/access.js';

test('manager can open every operational and administrative page', () => {
  for (const path of ['/admin', '/admin/orders/create', '/admin/kitchen', '/admin/employees/12', '/admin/invoices/1', '/admin/reports', '/admin/analytics', '/admin/marketing', '/admin/shifts', '/admin/settings']) {
    assert.equal(canAccessAdminRoute(path, 'MANAGER'), true, path);
  }
});

test('cashier can take payments but cannot open employee administration or kitchen', () => {
  assert.equal(canTakePayment('CASHIER'), true);
  for (const path of ['/admin/orders/1', '/admin/invoices/1', '/admin/reports', '/admin/analytics']) {
    assert.equal(canAccessAdminRoute(path, 'CASHIER'), true, path);
  }
  for (const path of ['/admin/employees', '/admin/kitchen', '/admin/marketing', '/admin/shifts', '/admin/settings']) {
    assert.equal(canAccessAdminRoute(path, 'CASHIER'), false, path);
  }
});

test('waiter and chef keep access to orders without finance access', () => {
  for (const role of ['WAITER', 'CHEF']) {
    assert.equal(canTakePayment(role), false);
    assert.equal(canAccessAdminRoute('/admin/orders/1', role), true);
    assert.equal(canAccessAdminRoute('/admin/invoices/1', role), false);
    assert.equal(canAccessAdminRoute('/admin/reports', role), false);
    assert.equal(canAccessAdminRoute('/admin/employees/1', role), false);
  }
  assert.equal(canAccessAdminRoute('/admin/kitchen', 'CHEF'), true);
  assert.equal(canAccessAdminRoute('/admin/kitchen', 'WAITER'), false);
});

test('missing and customer roles cannot enter staff routes or take payments', () => {
  for (const role of [null, undefined, 'CUSTOMER', 'ADMIN']) {
    assert.equal(canAccessAdminRoute('/admin/orders', role), false);
    assert.equal(canTakePayment(role), false);
  }
});
