import React from 'react';

interface AppLogoProps {
  size?: number;
  className?: string;
}

export const AppLogo: React.FC<AppLogoProps> = ({ size = 44, className = '' }) => {
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
          <stop offset="0%" stopColor="#8B1E28" />
          <stop offset="100%" stopColor="#521017" />
        </radialGradient>
        <linearGradient id="feyzGoldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#F5DC93" />
          <stop offset="50%" stopColor="#C89D42" />
          <stop offset="100%" stopColor="#9A711D" />
        </linearGradient>
      </defs>

      {/* Outer Squircle Emblem */}
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

      {/* Subtle Inner Tezhip Ring */}
      <circle
        cx="256"
        cy="256"
        r="204"
        stroke="url(#feyzGoldGrad)"
        strokeWidth="3"
        strokeOpacity="0.45"
        strokeDasharray="8 8"
      />

      {/* Rub el Hizb (8-Pointed Seljuk Star) */}
      <g transform="translate(256 256)">
        <rect
          x="-130"
          y="-130"
          width="260"
          height="260"
          rx="16"
          fill="#4A0D13"
          stroke="url(#feyzGoldGrad)"
          strokeWidth="12"
        />
        <rect
          x="-130"
          y="-130"
          width="260"
          height="260"
          rx="16"
          fill="#4A0D13"
          stroke="url(#feyzGoldGrad)"
          strokeWidth="12"
          transform="rotate(45)"
        />
        <circle
          cx="0"
          cy="0"
          r="108"
          fill="#68141D"
          stroke="url(#feyzGoldGrad)"
          strokeWidth="3.5"
          strokeOpacity="0.55"
        />
      </g>

      {/* Top Hilal (Crescent) */}
      <path
        d="M256 94C240 94 228 106 228 122C228 138 240 150 256 150C272 150 284 138 284 122C278 131 268 136 256 136C242 136 231 125 231 111C231 104 234 98 238 94C244 94 250 94 256 94Z"
        fill="url(#feyzGoldGrad)"
      />

      {/* Open Mushaf Pages */}
      <g>
        <path
          d="M148 194C148 186 154 180 162 180H242C248 180 252 184 252 190V314C252 320 247 324 241 324H162C154 324 148 318 148 310V194Z"
          fill="#FAF6EE"
          stroke="url(#feyzGoldGrad)"
          strokeWidth="5"
        />
        <path
          d="M260 190C260 184 264 180 270 180H350C358 180 364 186 364 194V310C364 318 358 324 350 324H271C265 324 260 320 260 314V190Z"
          fill="#FAF6EE"
          stroke="url(#feyzGoldGrad)"
          strokeWidth="5"
        />
        {/* Mushaf Page Lines */}
        <rect x="168" y="212" width="64" height="9" rx="4.5" fill="#C59B27" />
        <rect x="168" y="242" width="64" height="9" rx="4.5" fill="#7A1C24" />
        <rect x="168" y="272" width="46" height="9" rx="4.5" fill="#C59B27" />
        <rect x="280" y="212" width="64" height="9" rx="4.5" fill="#C59B27" />
        <rect x="280" y="242" width="64" height="9" rx="4.5" fill="#7A1C24" />
        <rect x="298" y="272" width="46" height="9" rx="4.5" fill="#C59B27" />
        {/* Rehal Cradle Base */}
        <path
          d="M174 344L256 322L338 344"
          stroke="url(#feyzGoldGrad)"
          strokeWidth="10"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </g>
    </svg>
  );
};
