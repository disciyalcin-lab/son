import React, { useState } from 'react';

interface AppLogoProps {
  size?: number;
  className?: string;
}

export const AppLogo: React.FC<AppLogoProps> = ({ size = 44, className = '' }) => {
  const [imgError, setImgError] = useState(false);

  if (!imgError) {
    return (
      <div
        style={{ width: size, height: size }}
        className={`relative shrink-0 overflow-hidden rounded-xl border border-[#C89D42]/45 shadow-xs select-none bg-[#0D4A42] ${className}`}
      >
        <img
          src="/logo.png"
          alt="Kuran Kelime 300 - Hızlı Öğrenme"
          width={size}
          height={size}
          loading="eager"
          onError={() => setImgError(true)}
          className="h-full w-full object-cover"
        />
      </div>
    );
  }

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 512 512"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={`shrink-0 select-none ${className}`}
      aria-label="Kur'an 300 Kelime Logosu"
    >
      <defs>
        <radialGradient id="feyzBgGrad" cx="50%" cy="45%" r="65%">
          <stop offset="0%" stopColor="#14655B" />
          <stop offset="100%" stopColor="#093832" />
        </radialGradient>
        <linearGradient id="feyzGoldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#F5DC93" />
          <stop offset="50%" stopColor="#C89D42" />
          <stop offset="100%" stopColor="#9A711D" />
        </linearGradient>
      </defs>
      <rect
        x="16"
        y="16"
        width="480"
        height="480"
        rx="116"
        fill="url(#feyzBgGrad)"
        stroke="url(#feyzGoldGrad)"
        strokeWidth="12"
      />
      <text
        x="256"
        y="290"
        textAnchor="middle"
        fill="url(#feyzGoldGrad)"
        fontSize="144"
        fontWeight="800"
        fontFamily="sans-serif"
      >
        300
      </text>
    </svg>
  );
};
