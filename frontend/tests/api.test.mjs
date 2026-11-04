import assert from 'node:assert/strict';
import { afterEach, beforeEach, test } from 'node:test';
import { api, uploadImage } from '../src/lib/api.js';

const browserGlobals = ['window', 'localStorage'];
const originalGlobals = browserGlobals.map((name) => Object.getOwnPropertyDescriptor(globalThis, name));
const staffToken = 'staff-session-token';
const member = JSON.stringify({ id: 12, fullName: 'Hội viên' });

beforeEach((t) => {
  const storage = new Map([['token', staffToken], ['currentUser', member]]);
  Object.defineProperties(globalThis, {
    window: { configurable: true, value: new EventTarget() },
    localStorage: {
      configurable: true,
      value: {
        getItem: (key) => storage.get(key) ?? null,
        removeItem: (key) => storage.delete(key),
      },
    },
  });
  t.mock.method(globalThis, 'fetch', async () => new Response('{}'));
});

afterEach(() => {
  browserGlobals.forEach((name, index) => {
    if (originalGlobals[index]) Object.defineProperty(globalThis, name, originalGlobals[index]);
    else delete globalThis[name];
  });
});

const catalogWrites = [
  ['POST', '/menu-items'],
  ['PUT', '/menu-items/42'],
  ['PATCH', '/menu-items/42/available?value=false'],
  ['PUT', '/menu-items/42/recipe'],
  ['DELETE', '/menu-items/42'],
  ['POST', '/menu-items/42/restore'],
  ['DELETE', '/menu-items/42/permanent'],
  ['POST', '/menu-items/sync-out-of-stock'],
  ['POST', '/categories'],
  ['PUT', '/categories/3'],
  ['DELETE', '/categories/3'],
];

for (const [method, path] of catalogWrites) {
  test(`${method} ${path} sends the staff token`, async () => {
    await api(path, { method, body: { description: 'Đá bào sữa topping xoài tươi' } });
    const [url, options] = fetch.mock.calls[0].arguments;
    assert.equal(url, `/api${path}`);
    assert.equal(options.headers?.Authorization, `Bearer ${staffToken}`);
    assert.equal(localStorage.getItem('token'), staffToken);
  });
}

test('catalog reads stay public for shop visitors, including with an expired staff session', async () => {
  for (const path of ['/menu-items', '/menu-items?categoryId=3', '/menu-items/42', '/categories', '/categories/3']) {
    await api(path);
  }
  for (const call of fetch.mock.calls) {
    assert.equal(call.arguments[1].headers?.Authorization, undefined);
  }
});

test('public customer actions do not send the staff token', async () => {
  for (const path of ['/customers/public-login', '/customers/send-registration-otp', '/customers/verify-registration-otp', '/reservations/public', '/public-orders', '/payments/momo/create', '/contact', '/event-inquiries']) {
    await api(path, { method: 'POST', body: {} });
  }
  for (const call of fetch.mock.calls) {
    assert.equal(call.arguments[1].headers?.Authorization, undefined);
  }
});

test('a catalog write rejected with 401 expires the staff session and preserves the member account', async (t) => {
  let expired = 0;
  window.addEventListener('auth:expired', () => { expired += 1; });
  t.mock.method(globalThis, 'fetch', async () => Response.json({ message: 'Phiên đăng nhập đã hết hạn' }, { status: 401 }));

  await assert.rejects(api('/menu-items/42', { method: 'PUT', body: {} }), /Phiên đăng nhập đã hết hạn/);
  assert.equal(localStorage.getItem('token'), null);
  assert.equal(localStorage.getItem('currentUser'), member);
  assert.equal(expired, 1);
});

test('a public request rejected with 401 keeps the staff session', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => new Response('{}', { status: 401 }));
  await assert.rejects(api('/customers/public-login', { method: 'POST', body: {} }));
  assert.equal(localStorage.getItem('token'), staffToken);
});

test('a 403 permission error keeps the authenticated staff session', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => Response.json({ message: 'Bạn không có quyền' }, { status: 403 }));
  await assert.rejects(api('/employees'), /Bạn không có quyền/);
  assert.equal(localStorage.getItem('token'), staffToken);
});

test('image uploads send the staff token and let the browser set the multipart boundary', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => Response.json({ url: '/uploads/menu/mango.png' }));
  assert.equal(await uploadImage(new Blob(['image']), 'menu'), '/uploads/menu/mango.png');
  const options = fetch.mock.calls[0].arguments[1];
  assert.equal(options.headers?.Authorization, `Bearer ${staffToken}`);
  assert.equal(options.headers?.['Content-Type'], undefined);
  assert.ok(options.body instanceof FormData);
});
