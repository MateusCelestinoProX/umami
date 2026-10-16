'use client';

import { useEffect, useState } from 'react';

export const SHADER_PRESETS = [
  { id: 'strands', name: 'Strands Luminosos', badge: 'Energia' },
  { id: 'plasmawave', name: 'Plasma Wave', badge: 'Fluido' },
  { id: 'siderays', name: 'Side Rays', badge: 'Cinemático' },
  { id: 'softaurora', name: 'Soft Aurora', badge: 'Nébula' },
  { id: 'ferrofluid', name: 'Ferrofluid', badge: 'Interativo' },
  { id: 'ghostfibers', name: 'Ghost Fibers', badge: 'Filamentos' },
  { id: 'webthreads', name: 'Web Threads', badge: 'Rede' },
  { id: 'dither', name: 'Dither Waves', badge: 'Retro' },
  { id: 'darkveil', name: 'Dark Veil', badge: 'Dark Cyber' },
  { id: 'balatro', name: 'Balatro Hypno', badge: 'Hipnótico' },
  { id: 'moltenmetal', name: 'Molten Metal', badge: 'Fusão' },
  { id: 'topography', name: 'Topography', badge: 'Cartografia' },
  { id: 'lighttunnel', name: 'Light Tunnel', badge: 'Hipervelocidade' },
];

declare global {
  interface Window {
    renderWebGLBackground?: (type: string) => void;
    stopWebGLBackground?: () => void;
  }
}

export function McpOsBackground() {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);

    const initialShader = localStorage.getItem('mcp_os_shader') || 'strands';

    const startEngine = () => {
      if (typeof window !== 'undefined' && typeof window.renderWebGLBackground === 'function') {
        window.renderWebGLBackground(initialShader);
      }
    };

    // Verifica se o script do motor WebGL já está carregado
    const existingScript = document.getElementById('mcp-os-webgl-script');
    if (!existingScript) {
      const script = document.createElement('script');
      script.id = 'mcp-os-webgl-script';
      script.type = 'module';
      script.src = '/mcp-os/mcp-os-webgl.js?v=20261016';
      script.onload = () => {
        // Breve intervalo para garantir registro de funções globais
        setTimeout(startEngine, 50);
      };
      document.head.appendChild(script);
    } else {
      setTimeout(startEngine, 50);
    }

    const handleSwitchShader = (e: CustomEvent<{ shader: string }>) => {
      const shader = e.detail?.shader;
      if (shader && typeof window.renderWebGLBackground === 'function') {
        localStorage.setItem('mcp_os_shader', shader);
        window.renderWebGLBackground(shader);
      }
    };

    window.addEventListener('mcp-switch-shader' as any, handleSwitchShader as any);

    return () => {
      window.removeEventListener('mcp-switch-shader' as any, handleSwitchShader as any);
      if (typeof window !== 'undefined' && typeof window.stopWebGLBackground === 'function') {
        window.stopWebGLBackground();
      }
    };
  }, []);

  if (!mounted) {
    return (
      <div
        id="bg-container"
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100vw',
          height: '100vh',
          zIndex: 0,
          pointerEvents: 'none',
          overflow: 'hidden',
          backgroundColor: '#040407',
        }}
      />
    );
  }

  return (
    <div
      id="bg-container"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        zIndex: 0,
        pointerEvents: 'none',
        overflow: 'hidden',
        backgroundColor: '#040407',
      }}
    >
      <div
        id="bg-webgl-container"
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          width: '100%',
          height: '100%',
          zIndex: 1,
          pointerEvents: 'none',
          display: 'block',
        }}
      />
      <div
        id="bg-overlay"
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          width: '100%',
          height: '100%',
          zIndex: 2,
          pointerEvents: 'none',
          background:
            'radial-gradient(ellipse at 50% 50%, rgba(4, 4, 7, 0.15) 0%, rgba(4, 4, 7, 0.65) 75%, rgba(4, 4, 7, 0.88) 100%)',
        }}
      />
    </div>
  );
}
