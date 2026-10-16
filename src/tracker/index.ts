/** Public types for the browser tracker. */
export type TrackedProperties = {
  /**
   * Hostname of server
   *
   * @description extracted from `window.location.hostname`
   * @example 'analytics.umami.is'
   */
  hostname?: string;

  /** Distinct ID associated with the current visitor. */
  id?: string;

  /**
   * Browser language
   *
   * @description extracted from `window.navigator.language`
   * @example 'en-US', 'fr-FR'
   */
  language?: string;

  /**
   * Page referrer
   *
   * @description extracted from `document.referrer`
   * @example 'https://analytics.umami.is/docs/getting-started'
   */
  referrer?: string;

  /**
   * Screen dimensions
   *
   * @description extracted from `window.screen.width` and `window.screen.height`
   * @example '1920x1080', '2560x1440'
   */
  screen?: string;

  /** Tag configured on the tracker script. */
  tag?: string;

  /**
   * Page title
   *
   * @description extracted from `document.querySelector('head > title')`
   * @example 'umami'
   */
  title?: string;

  /**
   * Page url
   *
   * @description normalized from `window.location.href`
   * @example 'https://analytics.umami.is/docs/getting-started'
   */
  url?: string;

  /**
   * Website ID (required)
   *
   * @example 'b59e9c65-ae32-47f1-8400-119fcf4861c4'
   */
  website: string;
};

export type WithRequired<T, K extends keyof T> = T & { [P in K]-?: T[P] };

export type EventDataValue = boolean | number | string | null | EventData | EventDataValue[];

/**
 *
 * Event Data can work with any JSON data. There are a few rules in place to maintain performance.
 * - Numbers have a max precision of 4.
 * - Strings have a max length of 500.
 * - Arrays are converted to a String, with the same max length of 500.
 * - Objects have a max of 50 properties. Arrays are considered 1 property.
 */
export interface EventData {
  [key: string]: EventDataValue;
}

export type EventProperties = {
  /**
   * NOTE: event names will be truncated past 50 characters
   */
  name: string;
  data?: EventData;
} & WithRequired<TrackedProperties, 'website'>;
export type PageViewProperties = WithRequired<TrackedProperties, 'website'>;
export type CustomEventFunction = (
  props: PageViewProperties,
) => EventProperties | PageViewProperties;

export type UmamiTracker = {
  track: {
    /**
     * Track a page view
     *
     * @example ```
     * umami.track();
     * ```
     */
    (): Promise<void>;

    /**
     * Track an event with a given name
     *
     * NOTE: event names will be truncated past 50 characters
     *
     * @example ```
     * umami.track('signup-button');
     * ```
     */
    (eventName: string): Promise<void>;

    /**
     * Tracks an event with dynamic data.
     *
     * NOTE: event names will be truncated past 50 characters
     *
     * When tracking events, the default properties are included in the payload. This is equivalent to running:
     *
     * ```js
     * umami.track(props => ({
     *   ...props,
     *   name: 'signup-button',
     *   data: {
     *     name: 'newsletter',
     *     id: 123
     *   }
     * }));
     * ```
     *
     * @example ```
     * umami.track('signup-button', { name: 'newsletter', id: 123 });
     * ```
     */
    (eventName: string, obj: EventData): Promise<void>;

    /**
     * Tracks a page view with custom properties
     *
     * @example ```
     * umami.track({ website: 'e676c9b4-11e4-4ef1-a4d7-87001773e9f2', url: '/home', title: 'Home page' });
     * ```
     */
    (properties: PageViewProperties): Promise<void>;

    /**
     * Tracks an event with fully customizable dynamic data
     * If you don't specify any `name` and/or `data`, it will be treated as a page view
     *
     * @example ```
     * umami.track((props) => ({ ...props, url: path }));
     * ```
     */
    (eventFunction: CustomEventFunction): Promise<void>;
  };
  identify: {
    /**
     * Identify a visitor with optional associated data.
     *
     * @example ```
     * umami.identify('user-123', { plan: 'pro' });
     * ```
     */
    (id: string, data?: EventData): Promise<void>;

    /**
     * Associate data with the current visitor. An `id` string sets the Distinct ID.
     *
     * @example ```
     * umami.identify({ id: 'user-123', plan: 'pro' });
     * ```
     */
    (data: EventData & { id?: string }): Promise<void>;
  };
  getSession: () => {
    cache: string | undefined;
    website: string | null;
  };
  destroy?: () => void;
};

declare global {
  interface Window {
    umami: UmamiTracker;
  }
}

type Payload = Record<string, unknown>;
type BeforeSend = (
  type: string,
  payload: Payload,
) => Payload | null | undefined | Promise<Payload | null | undefined>;
type TrackerWindow = Window &
  typeof globalThis & {
    doNotTrack?: string | number | null;
    navigator: Navigator & {
      msDoNotTrack?: string | number | null;
    };
  };
type TrackerDocument = Document & {
  currentScript: HTMLScriptElement | null;
};
type MetricEntry = PerformanceEntry & {
  activationStart: number;
  duration: number;
  hadRecentInput: boolean;
  interactionId: number;
  responseStart: number;
  startTime: number;
  value: number;
};
(window => {
  const {
    screen: { width, height },
    navigator: { language, doNotTrack: ndnt, msDoNotTrack: msdnt },
    location,
    document,
    history,
    top,
    doNotTrack,
  } = window;
  const { currentScript, referrer } = document as TrackerDocument;
  if (!currentScript) return;

  const { hostname, href, origin } = location;

  let localStorage: Storage | undefined;
  try {
    localStorage = href.startsWith('data:') ? undefined : window.localStorage;
  } catch {
    /* (DOMException) SecurityError: Access is denied for this document. */
  }

  const _data = 'data-';
  const _false = 'false';
  const _true = 'true';
  const attr = currentScript.getAttribute.bind(currentScript);
  const config = (value: string) => attr(`${_data}${value}`);

  const website = config('website-id');
  const hostUrl = config('host-url');
  const beforeSend = config('before-send');
  const distinctId = config('distinct-id') || undefined;
  const tag = config('tag') || undefined;
  const autoTrack = config('auto-track') !== _false;
  const dnt = config('do-not-track') === _true;
  const excludeSearch = config('exclude-search') === _true;
  const excludeHash = config('exclude-hash') === _true;
  const domain = config('domains') || '';
  const credentials = (config('fetch-credentials') || 'omit') as RequestCredentials;
  const perf = config('performance') === _true;
  const autoPageview = config('auto-pageview') !== _false;
  const autoEvents = config('auto-events') !== _false;
  const autoMatch = config('auto-match') !== _false;
  const trackScroll = config('track-scroll') !== _false;
  const trackEngagement = config('track-engagement') !== _false;
  const trackSpecs = config('track-specs') !== _false;
  const pixelBridge = config('pixel-bridge') !== _false;
  const persistentId = config('persistent-id') !== _false;

  const domains = domain.split(',').map(n => n.trim());
  const host =
    hostUrl || '__COLLECT_API_HOST__' || currentScript.src.split('/').slice(0, -1).join('/');
  const endpoint = `${host.replace(/\/$/, '')}__COLLECT_API_ENDPOINT__`;
  const screen = `${width}x${height}`;
  const eventRegex = /data-umami-event-([\w-_]+)/;
  const eventNameAttribute = `${_data}umami-event`;
  const delayDuration = 300;

  const abortController =
    typeof AbortController !== 'undefined' ? new AbortController() : undefined;
  const listenerOpts = (opts: Record<string, unknown> = {}) =>
    abortController ? { ...opts, signal: abortController.signal } : opts;

  /* Helper functions */

  const getPersistentVisitorId = (): string => {
    try {
      let vid = localStorage?.getItem('umami.vid');
      if (!vid && typeof document !== 'undefined') {
        const match = document.cookie?.match?.(/(?:^|; )umami\.vid=([^;]*)/);
        if (match) vid = decodeURIComponent(match[1]);
      }
      if (!vid) {
        vid =
          typeof crypto !== 'undefined' && crypto.randomUUID
            ? crypto.randomUUID()
            : 'umi_' + Math.random().toString(36).substring(2, 12) + Date.now().toString(36);
        try {
          localStorage?.setItem('umami.vid', vid);
        } catch {
          /* ignore */
        }
        try {
          document.cookie = `umami.vid=${encodeURIComponent(vid)}; Path=/; Max-Age=315360000; SameSite=Lax`;
        } catch {
          /* ignore */
        }
      }
      return vid;
    } catch {
      return '';
    }
  };

  const getDeviceSpecs = (): EventData => {
    try {
      const nav = window.navigator as Navigator & {
        deviceMemory?: number;
        connection?: {
          effectiveType?: string;
          downlink?: number;
        };
      };
      const conn = nav.connection;
      const tz =
        typeof Intl !== 'undefined' && Intl.DateTimeFormat
          ? Intl.DateTimeFormat().resolvedOptions().timeZone
          : undefined;
      const specs: EventData = {};
      if (nav.deviceMemory) specs.ram = `${nav.deviceMemory}GB`;
      if (nav.hardwareConcurrency) specs.cores = nav.hardwareConcurrency;
      if (conn?.effectiveType) specs.connection = conn.effectiveType;
      if (conn?.downlink) specs.downlink = `${conn.downlink}Mbps`;
      if (window.screen?.colorDepth) specs.colorDepth = `${window.screen.colorDepth}-bit`;
      if (window.devicePixelRatio) specs.pixelRatio = window.devicePixelRatio;
      if (nav.maxTouchPoints !== undefined) specs.touchPoints = nav.maxTouchPoints;
      if (window.innerWidth && window.innerHeight) specs.viewport = `${window.innerWidth}x${window.innerHeight}`;
      if (tz) specs.timezone = tz;
      if (window.matchMedia) {
        specs.theme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
      }
      return specs;
    } catch {
      return {};
    }
  };

  const normalize = (raw: string | URL): string => {
    if (!raw) return raw as string;
    try {
      const u = new URL(raw, location.href);
      if (excludeSearch) u.search = '';
      if (excludeHash) u.hash = '';
      return u.toString();
    } catch {
      return raw as string;
    }
  };

  // Strip the origin from same-origin referrers so the referrer domain
  // is never saved when it matches the current hostname
  const stripOrigin = (url: string): string =>
    url === origin || url?.startsWith(origin + '/') ? url.slice(origin.length) : url;

  const getPayload = () => ({
    website,
    screen,
    language,
    title: document.title,
    hostname,
    url: currentUrl,
    referrer: stripOrigin(currentRef),
    tag,
    id: identity ? identity : undefined,
  });

  const hasDoNotTrack = () => {
    const dnt = doNotTrack || ndnt || msdnt;
    return dnt === 1 || dnt === '1' || dnt === 'yes';
  };

  /* Event handlers */

  const handlePush = (_state: unknown, _title: string, url?: string | URL | null) => {
    if (!url) return;

    if (typeof flushPerformance === 'function') {
      flushPerformance();
    }

    currentRef = currentUrl;
    currentUrl = normalize(url);

    if (currentUrl !== currentRef && autoPageview) {
      setTimeout(track, delayDuration);
    }
  };

  const handlePathChanges = () => {
    const hook = (
      _this: History,
      method: 'pushState' | 'replaceState',
      callback: typeof handlePush,
    ) => {
      const orig = _this[method];
      return (...args: Parameters<History['pushState']>) => {
        const result = orig.apply(_this, args);
        callback.apply(null, args);
        return result;
      };
    };

    history.pushState = hook(history, 'pushState', handlePush);
    history.replaceState = hook(history, 'replaceState', handlePush);
  };

  const handleClicks = () => {
    const trackElement = async (el: Element) => {
      const eventName = el.getAttribute(eventNameAttribute);
      if (eventName) {
        const eventData: EventData = {};

        el.getAttributeNames().forEach(name => {
          const match = name.match(eventRegex);
          if (match) eventData[match[1]] = el.getAttribute(name) as string;
        });

        return track(eventName, eventData);
      }
    };
    const onClick = (e: MouseEvent) => {
      const el = e.target as Element;
      if (!el) return;

      const eventEl = el.closest(`[${eventNameAttribute}]`);
      if (eventEl) {
        if (eventEl.tagName === 'A' && (eventEl as HTMLAnchorElement).href) {
          const { href, target } = eventEl as HTMLAnchorElement;
          const external =
            target === '_blank' ||
            e.ctrlKey ||
            e.shiftKey ||
            e.metaKey ||
            (e.button && e.button === 1);
          if (!external) e.preventDefault();
          return trackElement(eventEl).finally(() => {
            if (!external) {
              (target === '_top' ? (top as WindowProxy).location : location).href = href;
            }
          });
        }
        return trackElement(eventEl);
      }

      // Auto-event capture (Meta Pixel style)
      if (autoEvents) {
        const anchor = el.closest('a') as HTMLAnchorElement | null;
        if (anchor && anchor.href) {
          const href = anchor.href;
          const linkText = (anchor.innerText || anchor.getAttribute('aria-label') || '')
            .trim()
            .slice(0, 50);

          try {
            const targetUrl = new URL(href, location.href);
            const isExternal = targetUrl.hostname && targetUrl.hostname !== hostname;

            if (isExternal) {
              // WhatsApp
              if (
                href.includes('wa.me') ||
                href.includes('whatsapp.com') ||
                href.startsWith('whatsapp:')
              ) {
                const phone = href.match(/\d{8,15}/)?.[0] || '';
                return track('WhatsApp Click', { url: href, phone, text: linkText });
              }

              // Social Networks
              if (
                /instagram\.com|facebook\.com|tiktok\.com|linkedin\.com|youtube\.com|twitter\.com|x\.com/.test(
                  targetUrl.hostname,
                )
              ) {
                const network = targetUrl.hostname.replace(/^www\./, '').split('.')[0];
                return track('Social Click', { network, url: href, text: linkText });
              }

              // Checkout / Gateways
              if (
                /hotmart|kiwify|eduzz|stripe|monetizze|braip|pagseguro|mercadopago|checkout|cart/.test(
                  href,
                )
              ) {
                return track('Initiate Checkout', { url: href, text: linkText });
              }

              // File Downloads
              if (/\.(pdf|zip|rar|csv|xlsx|docx|pptx|mp4|mp3|exe|apk)(\?.*)?$/i.test(href)) {
                const file = href.split('/').pop()?.split('?')[0] || 'file';
                return track('File Download', { file, url: href });
              }

              // Generic Outbound Link
              return track('Outbound Link', {
                url: href,
                domain: targetUrl.hostname,
                text: linkText,
              });
            }
          } catch {
            /* ignore invalid URL */
          }
        }

        // Button / CTA Click Auto-detection
        const btn = el.closest(
          'button, [role="button"], input[type="submit"], input[type="button"], .btn, .button, .cta',
        ) as HTMLElement | null;
        if (btn) {
          const text = (
            btn.innerText ||
            (btn as HTMLInputElement).value ||
            btn.getAttribute('aria-label') ||
            btn.getAttribute('title') ||
            ''
          )
            .trim()
            .slice(0, 50);
          if (text) {
            const btnData: EventData = { text, path: location.pathname };
            if (btn.id) btnData.id = btn.id;
            if (btn.className) btnData.class = String(btn.className).slice(0, 50);
            return track('Button Click', btnData);
          }
        }
      }
    };
    document.addEventListener('click', onClick, listenerOpts({ capture: true }));
  };

  const handleFormSubmissions = () => {
    const onSubmit = (e: Event) => {
      const form = e.target as HTMLFormElement;
      if (!form || form.tagName !== 'FORM') return;

      const leadData: Record<string, string> = {};
      const inputs = Array.from(
        form.querySelectorAll('input, select, textarea'),
      ) as (HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement)[];

      inputs.forEach(input => {
        const name = (input.name || input.id || '').toLowerCase();
        const type = (input.getAttribute('type') || '').toLowerCase();
        const val = (input.value || '').trim();

        // Security check: never capture passwords, credit cards, or security tokens
        if (
          type === 'password' ||
          name.includes('pass') ||
          name.includes('card') ||
          name.includes('cvv') ||
          name.includes('token') ||
          name.includes('secret')
        ) {
          return;
        }

        if (val) {
          if (
            type === 'email' ||
            name.includes('email') ||
            name.includes('mail') ||
            /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)
          ) {
            leadData.email = val;
          } else if (
            type === 'tel' ||
            name.includes('phone') ||
            name.includes('tel') ||
            name.includes('celular') ||
            name.includes('whatsapp')
          ) {
            leadData.phone = val;
          } else if (name.includes('name') || name.includes('nome')) {
            leadData.name = val;
          }
        }
      });

      const formId = form.id || form.name || form.getAttribute('action') || 'form';

      if (leadData.email || leadData.phone || leadData.name) {
        void identify(leadData);
      }

      const leadEventData: EventData = {
        form: formId,
        hasEmail: !!leadData.email,
        hasPhone: !!leadData.phone,
        hasName: !!leadData.name,
      };
      if (form.action) leadEventData.action = form.action.slice(0, 100);
      void track('Lead', leadEventData);
    };

    document.addEventListener('submit', onSubmit, listenerOpts({ capture: true }));
  };

  const handleScrollDepth = () => {
    const milestones = [25, 50, 75, 90, 100];
    const reached = new Set<number>();

    const onScroll = () => {
      const docEl = document.documentElement;
      const docHeight = docEl.scrollHeight - docEl.clientHeight;
      if (docHeight <= 0) return;

      const scrollPercent = Math.min(100, Math.round((window.scrollY / docHeight) * 100));

      milestones.forEach(m => {
        if (scrollPercent >= m && !reached.has(m)) {
          reached.add(m);
          void track('Scroll Depth', { depth: m, path: location.pathname });
        }
      });
    };

    window.addEventListener('scroll', onScroll, listenerOpts({ passive: true }));
  };

  const handleEngagement = () => {
    let activeSeconds = 0;
    let isEngaged = true;
    let idleTimer: ReturnType<typeof setTimeout> | undefined;

    const resetIdle = () => {
      isEngaged = true;
      if (idleTimer) clearTimeout(idleTimer);
      idleTimer = setTimeout(() => {
        isEngaged = false;
      }, 15000);
    };

    ['mousemove', 'keydown', 'scroll', 'touchstart', 'click'].forEach(evt => {
      window.addEventListener(evt, resetIdle, listenerOpts({ passive: true }));
    });

    const intervals = [15, 30, 60, 120, 300];
    const sentIntervals = new Set<number>();

    const timer = setInterval(() => {
      if (document.visibilityState === 'visible' && isEngaged) {
        activeSeconds += 5;
        intervals.forEach(sec => {
          if (activeSeconds >= sec && !sentIntervals.has(sec)) {
            sentIntervals.add(sec);
            void track('Engagement Pulse', { seconds: sec, path: location.pathname });
          }
        });
      }
    }, 5000);

    abortController?.signal?.addEventListener('abort', () => clearInterval(timer));
  };

  const initPixelBridge = () => {
    const win = window as unknown as Record<string, unknown>;
    const existingFbq = win.fbq;

    win.fbq = function (...args: unknown[]) {
      if (typeof existingFbq === 'function' && existingFbq !== win.fbq) {
        try {
          (existingFbq as Function).apply(this, args);
        } catch {
          /* ignore */
        }
      }
      const [action, eventName, eventData] = args;
      if (action === 'track' || action === 'trackCustom') {
        if (eventName && eventName !== 'PageView') {
          void track(
            eventName as string,
            typeof eventData === 'object' ? (eventData as EventData) : undefined,
          );
        }
      } else if (action === 'init' && typeof eventData === 'object') {
        void identify(eventData as EventData);
      }
    };

    const existingGtag = win.gtag;
    win.gtag = function (...args: unknown[]) {
      if (typeof existingGtag === 'function' && existingGtag !== win.gtag) {
        try {
          (existingGtag as Function).apply(this, args);
        } catch {
          /* ignore */
        }
      }
      const [action, eventName, eventParams] = args;
      if (action === 'event' && eventName) {
        void track(
          eventName as string,
          typeof eventParams === 'object' ? (eventParams as EventData) : undefined,
        );
      } else if (
        action === 'set' &&
        eventName === 'user_properties' &&
        typeof eventParams === 'object'
      ) {
        void identify(eventParams as EventData);
      }
    };
  };

  /* Tracking functions */

  const trackingDisabled = () =>
    disabled ||
    !website ||
    localStorage?.getItem('umami.disabled') ||
    (domain && !domains.includes(hostname)) ||
    (dnt && hasDoNotTrack());

  const send = async (payload: Payload | null | undefined, type = 'event'): Promise<void> => {
    if (trackingDisabled()) return;

    const callback = (window as unknown as Record<string, unknown>)[beforeSend as string] as
      | BeforeSend
      | undefined;

    if (typeof callback === 'function') {
      payload = await Promise.resolve(callback(type, payload as Payload));
    }

    if (!payload) return;

    try {
      const res = await fetch(endpoint, {
        keepalive: true,
        method: 'POST',
        body: JSON.stringify({ type, payload }),
        headers: {
          'Content-Type': 'application/json',
          'x-umami-website-id': website as string,
          'x-umami-hostname': hostname,
          ...(typeof cache !== 'undefined' && { 'x-umami-cache': cache }),
        },
        credentials,
      });

      const data = (await res.json()) as { cache?: string; disabled?: boolean } | null;
      if (data) {
        disabled = !!data.disabled;
        cache = data.cache;
      }
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (_e) {
      /* no-op */
    }
  };

  const init = () => {
    if (!initialized) {
      initialized = true;
      if (autoPageview) {
        const specs = trackSpecs ? getDeviceSpecs() : undefined;
        if (specs && Object.keys(specs).length > 0) {
          void track((props: Payload) => ({
            ...props,
            data: specs,
          }));
        } else {
          void track();
        }
      }
      handlePathChanges();
      handleClicks();
      if (autoMatch) handleFormSubmissions();
      if (trackScroll) handleScrollDepth();
      if (trackEngagement) handleEngagement();
      if (pixelBridge) initPixelBridge();
      if (perf) initPerformance();
    }
  };

  const track = (
    name?: string | Payload | ((payload: Payload) => Payload),
    data?: EventData,
  ): Promise<void> => {
    if (typeof name === 'string') return send({ ...getPayload(), name, data });
    if (typeof name === 'object') return send({ ...name });
    if (typeof name === 'function') return send(name(getPayload()));
    return send(getPayload());
  };

  const identify = (
    id: string | (EventData & { id?: string }),
    data?: EventData,
  ): Promise<void> => {
    const nextIdentity = typeof id === 'string' ? id : id.id;

    if (nextIdentity !== undefined) {
      identity = nextIdentity;
    }

    cache = '';
    return send(
      {
        ...getPayload(),
        data: typeof id === 'object' ? id : data,
      },
      'identify',
    );
  };

  /* Performance */

  const initPerformance = () => {
    const metrics: Record<string, number> = {};
    let sent = false;
    let timeoutId: ReturnType<typeof setTimeout> | undefined;
    let isInitialLoad = true;
    let activationStart = 0;
    let pageStartTime = 0;

    const observe = (type: string, callback: (entry: MetricEntry) => void) => {
      try {
        const observer = new PerformanceObserver(list => {
          (list.getEntries() as MetricEntry[]).forEach(callback);
        });
        observer.observe({ type, buffered: true });
      } catch {
        /* not supported */
      }
    };

    // TTFB
    observe('navigation', entry => {
      activationStart = entry.activationStart || 0;
      metrics.ttfb = Math.max(entry.responseStart - activationStart, 0);
    });

    // FCP
    observe('paint', entry => {
      if (entry.name === 'first-contentful-paint') {
        metrics.fcp = Math.max(entry.startTime - activationStart, 0);
      }
    });

    // LCP
    observe('largest-contentful-paint', entry => {
      metrics.lcp = Math.max(entry.startTime - activationStart, 0);
    });

    // CLS - session windows algorithm (gap < 1s, max 5s duration; report worst window)
    let clsSessionValue = 0;
    let clsSessionEntries: MetricEntry[] = [];
    observe('layout-shift', entry => {
      if (!entry.hadRecentInput) {
        const lastEntry = clsSessionEntries[clsSessionEntries.length - 1];
        const firstEntry = clsSessionEntries[0];
        if (
          lastEntry &&
          entry.startTime - lastEntry.startTime - lastEntry.duration < 1000 &&
          entry.startTime - firstEntry.startTime < 5000
        ) {
          clsSessionValue += entry.value;
          clsSessionEntries.push(entry);
        } else {
          clsSessionValue = entry.value;
          clsSessionEntries = [entry];
        }
        if (clsSessionValue > (metrics.cls || 0)) {
          metrics.cls = clsSessionValue;
        }
      }
    });

    // INP - group by interactionId, 98th percentile, 40ms threshold
    let interactions: Record<number, number> = {};
    let inpObserver: PerformanceObserver | undefined;
    const recordInteractions = (entries: PerformanceEntryList) => {
      (entries as MetricEntry[]).forEach(entry => {
        if (entry.interactionId) {
          const existing = interactions[entry.interactionId];
          if (!existing || entry.duration > existing) {
            interactions[entry.interactionId] = entry.duration;
          }
        }
      });
    };
    try {
      inpObserver = new PerformanceObserver(list => recordInteractions(list.getEntries()));
      inpObserver.observe({
        type: 'event',
        buffered: true,
        durationThreshold: 40,
      } as PerformanceObserverInit);
    } catch {
      /* not supported */
    }

    const computeInp = () => {
      if (inpObserver) recordInteractions(inpObserver.takeRecords());
      const values = Object.values(interactions).sort((a, b) => b - a);
      if (values.length) {
        const p98Index = Math.floor(Math.max(values.length, 10) * 0.02);
        metrics.inp = values[Math.min(p98Index, values.length - 1)];
      }
    };

    const getEntriesByType = (type: string): MetricEntry[] => {
      try {
        return (window.performance?.getEntriesByType?.(type) as MetricEntry[]) || [];
      } catch {
        return [];
      }
    };

    const applyFallbackMetrics = () => {
      if (!isInitialLoad) return;

      if (metrics.ttfb === undefined) {
        const navigation = getEntriesByType('navigation')?.[0];
        if (navigation) {
          metrics.ttfb = Math.max(navigation.responseStart - (navigation.activationStart || 0), 0);
        }
      }

      if (metrics.fcp === undefined) {
        const fcpEntry = getEntriesByType('paint')?.find(
          entry => entry.name === 'first-contentful-paint',
        );
        if (fcpEntry) {
          metrics.fcp = Math.max(fcpEntry.startTime - activationStart, 0);
        }
      }

      if (metrics.lcp === undefined) {
        const lcpEntries = getEntriesByType('largest-contentful-paint');
        const lcpEntry = lcpEntries?.[lcpEntries.length - 1];
        if (lcpEntry) {
          metrics.lcp = Math.max(lcpEntry.startTime - activationStart, 0);
        }
      }
    };

    const sendPerformance = () => {
      if (sent) return;

      applyFallbackMetrics();
      computeInp();
      metrics.duration = Math.round(performance.now() - pageStartTime);

      sent = true;
      if (timeoutId) clearTimeout(timeoutId);
      send({ ...getPayload(), ...metrics }, 'performance');
    };

    flushPerformance = () => {
      sendPerformance();
      isInitialLoad = false;
      Object.keys(metrics).forEach(k => {
        delete metrics[k];
      });
      activationStart = 0;
      pageStartTime = performance.now();
      clsSessionValue = 0;
      clsSessionEntries = [];
      interactions = {};
      sent = false;
      if (timeoutId) clearTimeout(timeoutId);
      timeoutId = setTimeout(sendPerformance, 10000);
    };
    timeoutId = setTimeout(sendPerformance, 10000);

    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'hidden') sendPerformance();
    });
    window.addEventListener('pagehide', sendPerformance);
  };

  /* Start */

  if (!window.umami) {
    window.umami = {
      track,
      identify,
      getSession: () => ({ cache, website }),
      destroy: () => abortController?.abort(),
    } as UmamiTracker;
  }

  let currentUrl = normalize(href);
  let currentRef = normalize(referrer);

  let initialized = false;
  let disabled = false;
  let cache: string | undefined;
  let identity = distinctId || (persistentId ? getPersistentVisitorId() : undefined);
  let flushPerformance: (() => void) | undefined;

  if (distinctId) {
    void identify(distinctId);
  }

  if (autoTrack && !trackingDisabled()) {
    if (document.readyState === 'complete') {
      init();
    } else {
      document.addEventListener('readystatechange', init, true);
    }
  }
})(window as TrackerWindow);
