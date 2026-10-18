'use client';

import { useEffect, useRef, useState } from 'react';
import { SHADER_PRESETS } from './McpOsBackground';

export function ShaderSwitcher() {
  const [activeShader, setActiveShader] = useState<string>('strands');
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const saved = localStorage.getItem('mcp_os_shader') || 'strands';
    setActiveShader(saved);

    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (id: string) => {
    setActiveShader(id);
    localStorage.setItem('mcp_os_shader', id);
    setIsOpen(false);

    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('mcp-switch-shader', { detail: { shader: id } }));
    }
  };

  const currentPreset = SHADER_PRESETS.find(p => p.id === activeShader) || SHADER_PRESETS[0];

  return (
    <div ref={containerRef} style={{ position: 'relative', display: 'inline-block' }}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          padding: '5px 10px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.06)',
          backdropFilter: 'blur(16px)',
          WebkitBackdropFilter: 'blur(16px)',
          border: '1px solid rgba(255, 255, 255, 0.15)',
          color: '#ffffff',
          fontSize: '12px',
          fontWeight: 500,
          cursor: 'pointer',
          boxShadow: '0 4px 16px rgba(0, 0, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.2)',
          transition: 'all 0.2s ease',
          maxWidth: '180px',
        }}
        onMouseEnter={e => {
          e.currentTarget.style.background = 'rgba(255, 255, 255, 0.12)';
          e.currentTarget.style.borderColor = 'rgba(167, 139, 250, 0.45)';
          e.currentTarget.style.boxShadow = '0 6px 20px rgba(167, 139, 250, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.3)';
        }}
        onMouseLeave={e => {
          e.currentTarget.style.background = 'rgba(255, 255, 255, 0.06)';
          e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.15)';
          e.currentTarget.style.boxShadow = '0 4px 16px rgba(0, 0, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.2)';
        }}
      >
        <span
          style={{
            width: '8px',
            height: '8px',
            borderRadius: '50%',
            backgroundColor: '#a78bfa',
            boxShadow: '0 0 10px #a78bfa',
            display: 'inline-block',
            flexShrink: 0,
          }}
        />
        <span
          style={{
            opacity: 0.9,
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            maxWidth: '100px',
          }}
        >
          {currentPreset.name}
        </span>
        <span
          style={{
            fontSize: '9px',
            padding: '2px 5px',
            borderRadius: '6px',
            background: 'rgba(167, 139, 250, 0.25)',
            border: '1px solid rgba(167, 139, 250, 0.4)',
            color: '#e9d5ff',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
            flexShrink: 0,
          }}
        >
          Shader
        </span>
      </button>

      {isOpen && (
        <div
          style={{
            position: 'absolute',
            top: 'calc(100% + 8px)',
            right: 0,
            width: '260px',
            maxHeight: '360px',
            overflowY: 'auto',
            background: 'rgba(12, 12, 22, 0.85)',
            backdropFilter: 'blur(32px) saturate(190%)',
            WebkitBackdropFilter: 'blur(32px) saturate(190%)',
            border: '1px solid rgba(255, 255, 255, 0.16)',
            borderRadius: '16px',
            boxShadow: '0 20px 50px rgba(0, 0, 0, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.15)',
            zIndex: 1000,
            padding: '8px',
          }}
        >
          <div
            style={{
              padding: '6px 10px 8px',
              fontSize: '11px',
              fontWeight: 600,
              textTransform: 'uppercase',
              letterSpacing: '0.8px',
              color: 'rgba(255, 255, 255, 0.5)',
              borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
              marginBottom: '6px',
            }}
          >
            Shaders WebGL MCP OS
          </div>
          {SHADER_PRESETS.map(preset => {
            const isSelected = preset.id === activeShader;
            return (
              <button
                key={preset.id}
                type="button"
                onClick={() => handleSelect(preset.id)}
                style={{
                  width: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '8px 10px',
                  borderRadius: '10px',
                  border: isSelected
                    ? '1px solid rgba(167, 139, 250, 0.45)'
                    : '1px solid transparent',
                  background: isSelected
                    ? 'linear-gradient(135deg, rgba(167, 139, 250, 0.22) 0%, rgba(99, 102, 241, 0.15) 100%)'
                    : 'transparent',
                  color: isSelected ? '#ffffff' : 'rgba(255, 255, 255, 0.8)',
                  fontSize: '12px',
                  cursor: 'pointer',
                  textAlign: 'left',
                  transition: 'all 0.15s ease',
                  marginBottom: '2px',
                }}
                onMouseEnter={e => {
                  if (!isSelected) {
                    e.currentTarget.style.background = 'rgba(255, 255, 255, 0.08)';
                    e.currentTarget.style.color = '#ffffff';
                  }
                }}
                onMouseLeave={e => {
                  if (!isSelected) {
                    e.currentTarget.style.background = 'transparent';
                    e.currentTarget.style.color = 'rgba(255, 255, 255, 0.8)';
                  }
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span
                    style={{
                      width: '6px',
                      height: '6px',
                      borderRadius: '50%',
                      backgroundColor: isSelected ? '#a78bfa' : 'rgba(255, 255, 255, 0.3)',
                      boxShadow: isSelected ? '0 0 8px #a78bfa' : 'none',
                    }}
                  />
                  <span>{preset.name}</span>
                </div>
                <span
                  style={{
                    fontSize: '9px',
                    padding: '2px 5px',
                    borderRadius: '4px',
                    background: isSelected
                      ? 'rgba(167, 139, 250, 0.3)'
                      : 'rgba(255, 255, 255, 0.06)',
                    color: isSelected ? '#e9d5ff' : 'rgba(255, 255, 255, 0.45)',
                  }}
                >
                  {preset.badge}
                </span>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
