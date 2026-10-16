import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  window.umami?.destroy?.();
  delete (window as Window & { umami?: unknown }).umami;
  delete (window as Window & { fbq?: unknown }).fbq;
  delete (window as Window & { gtag?: unknown }).gtag;
  delete (document as Document & { currentScript?: HTMLScriptElement }).currentScript;
  delete (document as Document & { readyState?: DocumentReadyState }).readyState;
  document.body.innerHTML = '';
  vi.unstubAllGlobals();
  vi.resetModules();
});

test('identifies data-distinct-id before the initial page view', async () => {
  const script = document.createElement('script');
  script.src = 'https://analytics.example.com/script.js';
  script.dataset.websiteId = 'website-id';
  script.dataset.distinctId = 'visitor-id';

  Object.defineProperties(document, {
    currentScript: { configurable: true, value: script },
    readyState: { configurable: true, value: 'complete' },
  });

  const fetchMock = vi.fn().mockResolvedValue({ json: vi.fn().mockResolvedValue({}) });
  vi.stubGlobal('fetch', fetchMock);

  await import('./index');

  await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
  const requests = fetchMock.mock.calls.map(([, init]) => JSON.parse(init.body));

  expect(requests[0]).toMatchObject({
    type: 'identify',
    payload: { id: 'visitor-id', website: 'website-id' },
  });
  expect(requests[1]).toMatchObject({
    type: 'event',
    payload: { id: 'visitor-id', website: 'website-id' },
  });
});

test('bridges fbq and gtag calls to umami.track and umami.identify', async () => {
  const script = document.createElement('script');
  script.src = 'https://analytics.example.com/script.js';
  script.dataset.websiteId = 'website-id';

  Object.defineProperties(document, {
    currentScript: { configurable: true, value: script },
    readyState: { configurable: true, value: 'complete' },
  });

  const fetchMock = vi.fn().mockResolvedValue({ json: vi.fn().mockResolvedValue({}) });
  vi.stubGlobal('fetch', fetchMock);

  await import('./index');

  // Initial auto pageview
  await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));

  // Trigger Meta Pixel fbq call
  (window as unknown as { fbq: (...args: unknown[]) => void }).fbq('track', 'Purchase', {
    value: 199,
    currency: 'BRL',
  });

  await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
  const fbqRequest = JSON.parse(fetchMock.mock.calls[1][1].body);
  expect(fbqRequest).toMatchObject({
    type: 'event',
    payload: {
      name: 'Purchase',
      data: { value: 199, currency: 'BRL' },
    },
  });

  // Trigger Google Analytics gtag call
  (window as unknown as { gtag: (...args: unknown[]) => void }).gtag('event', 'generate_lead', {
    source: 'landing_page',
  });

  await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3));
  const gtagRequest = JSON.parse(fetchMock.mock.calls[2][1].body);
  expect(gtagRequest).toMatchObject({
    type: 'event',
    payload: {
      name: 'generate_lead',
      data: { source: 'landing_page' },
    },
  });
});

test('auto-captures form submissions and performs Automatic Advanced Matching', async () => {
  const script = document.createElement('script');
  script.src = 'https://analytics.example.com/script.js';
  script.dataset.websiteId = 'website-id';

  Object.defineProperties(document, {
    currentScript: { configurable: true, value: script },
    readyState: { configurable: true, value: 'complete' },
  });

  const fetchMock = vi.fn().mockResolvedValue({ json: vi.fn().mockResolvedValue({}) });
  vi.stubGlobal('fetch', fetchMock);

  await import('./index');

  await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));

  const form = document.createElement('form');
  form.id = 'newsletter-form';
  const emailInput = document.createElement('input');
  emailInput.type = 'email';
  emailInput.name = 'email';
  emailInput.value = 'lead@example.com';
  const nameInput = document.createElement('input');
  nameInput.name = 'nome';
  nameInput.value = 'Carlos Silva';

  form.appendChild(emailInput);
  form.appendChild(nameInput);
  document.body.appendChild(form);

  form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));

  await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3));

  const identifyCall = JSON.parse(fetchMock.mock.calls[1][1].body);
  expect(identifyCall).toMatchObject({
    type: 'identify',
    payload: {
      data: { email: 'lead@example.com', name: 'Carlos Silva' },
    },
  });

  const leadEventCall = JSON.parse(fetchMock.mock.calls[2][1].body);
  expect(leadEventCall).toMatchObject({
    type: 'event',
    payload: {
      name: 'Lead',
      data: { form: 'newsletter-form', hasEmail: true, hasName: true },
    },
  });
});
