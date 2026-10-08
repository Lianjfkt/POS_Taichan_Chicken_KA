/**
 * KA POS v3.0 — Food & Culinary Icon System
 * Elegant, professional vector SVG food icons replacing raw emojis.
 * Matches the dark luxury culinary aesthetic (warm amber, flame orange, gold).
 */
(function(window) {
  'use strict';

  // SVG Definitions — Curated High-End Culinary Vector Set
  const ICONS = {
    // Sate Taichan — skewer with grilled tender chicken cuts and chili dots
    taichan: `<svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="food-svg">
      <defs>
        <linearGradient id="tc-skewer" x1="4" y1="28" x2="28" y2="4" gradientUnits="userSpaceOnUse">
          <stop stop-color="#b45309"/>
          <stop offset="1" stop-color="#fde68a"/>
        </linearGradient>
        <linearGradient id="tc-meat" x1="8" y1="24" x2="24" y2="8" gradientUnits="userSpaceOnUse">
          <stop stop-color="#ea580c"/>
          <stop offset="0.5" stop-color="#f97316"/>
          <stop offset="1" stop-color="#fdba74"/>
        </linearGradient>
      </defs>
      <!-- Skewer Stick -->
      <path d="M4 28L28 4" stroke="url(#tc-skewer)" stroke-width="2.2" stroke-linecap="round"/>
      <!-- Meat Piece 1 (Bottom) -->
      <rect x="7" y="19" width="7" height="6" rx="2.5" transform="rotate(-45 7 19)" fill="url(#tc-meat)" stroke="#c2410c" stroke-width="0.8"/>
      <circle cx="10.5" cy="19.5" r="0.75" fill="#ef4444"/>
      <!-- Meat Piece 2 (Middle) -->
      <rect x="12" y="14" width="7.5" height="6.5" rx="2.5" transform="rotate(-45 12 14)" fill="url(#tc-meat)" stroke="#c2410c" stroke-width="0.8"/>
      <circle cx="15.5" cy="14.5" r="0.85" fill="#ef4444"/>
      <circle cx="17.2" cy="13.2" r="0.6" fill="#facc15"/>
      <!-- Meat Piece 3 (Top) -->
      <rect x="17" y="9" width="7" height="6" rx="2.5" transform="rotate(-45 17 9)" fill="url(#tc-meat)" stroke="#c2410c" stroke-width="0.8"/>
      <circle cx="20.5" cy="9.5" r="0.75" fill="#ef4444"/>
      <!-- Subtle Grill Char Marks -->
      <line x1="10" y1="18" x2="12" y2="17" stroke="#7c2d12" stroke-width="0.8" stroke-linecap="round"/>
      <line x1="15" y1="13" x2="17.5" y2="12" stroke="#7c2d12" stroke-width="0.8" stroke-linecap="round"/>
    </svg>`,

    // Chicken / Ayam Crispy — Crispy golden fried drumstick
    chicken: `<svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="food-svg">
      <defs>
        <linearGradient id="chk-gold" x1="10" y1="6" x2="26" y2="22" gradientUnits="userSpaceOnUse">
          <stop stop-color="#f59e0b"/>
          <stop offset="0.6" stop-color="#ea580c"/>
          <stop offset="1" stop-color="#c2410c"/>
        </linearGradient>
        <linearGradient id="chk-bone" x1="4" y1="28" x2="12" y2="20" gradientUnits="userSpaceOnUse">
          <stop stop-color="#f8fafc"/>
          <stop offset="1" stop-color="#cbd5e1"/>
        </linearGradient>
      </defs>
      <!-- Bone -->
      <path d="M7 25L11 21" stroke="url(#chk-bone)" stroke-width="3" stroke-linecap="round"/>
      <circle cx="6" cy="26" r="2.2" fill="url(#chk-bone)"/>
      <circle cx="8" cy="27" r="2.2" fill="url(#chk-bone)"/>
      <!-- Drumstick Meat Bulb -->
      <path d="M11 21C11 21 10 17 13 13C16 9 20 6 24 7C27 8 28 12 26 16C23 21 17 23 13 22C11.5 21.6 11 21 11 21Z" fill="url(#chk-gold)" stroke="#b45309" stroke-width="1"/>
      <!-- Crispy Batter Flakes / Highlights -->
      <ellipse cx="21" cy="11" rx="4" ry="2" transform="rotate(-30 21 11)" fill="#fbbf24" opacity="0.6"/>
      <circle cx="23" cy="15" r="1" fill="#fde68a"/>
      <circle cx="17" cy="14" r="0.8" fill="#fde68a"/>
      <circle cx="18" cy="18" r="0.9" fill="#fde68a"/>
    </svg>`,

    // Drink / Es Minuman — Refreshing tumbler with ice and straw
    drink: `<svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="food-svg">
      <defs>
        <linearGradient id="drk-liquid" x1="16" y1="12" x2="16" y2="27" gradientUnits="userSpaceOnUse">
          <stop stop-color="#38bdf8"/>
          <stop offset="0.5" stop-color="#0284c7"/>
          <stop offset="1" stop-color="#0369a1"/>
        </linearGradient>
        <linearGradient id="drk-glass" x1="8" y1="8" x2="24" y2="27" gradientUnits="userSpaceOnUse">
          <stop stop-color="#ffffff" stop-opacity="0.3"/>
          <stop offset="1" stop-color="#ffffff" stop-opacity="0.05"/>
        </linearGradient>
      </defs>
      <!-- Straw -->
      <path d="M22 4L19 9L15 25" stroke="#f97316" stroke-width="2" stroke-linecap="round"/>
      <!-- Cup Body -->
      <path d="M8 9L11 26C11.2 27.1 12.1 28 13.2 28H18.8C19.9 28 20.8 27.1 21 26L24 9H8Z" fill="url(#drk-glass)" stroke="rgba(255,255,255,0.4)" stroke-width="1.2"/>
      <!-- Liquid Fill -->
      <path d="M9.5 14L11.5 25.5C11.6 26.2 12.2 26.8 13 26.8H19C19.8 26.8 20.4 26.2 20.5 25.5L22.5 14H9.5Z" fill="url(#drk-liquid)" opacity="0.85"/>
      <!-- Ice Cubes inside -->
      <rect x="13" y="15" width="4.5" height="4.5" rx="1" fill="#e0f2fe" opacity="0.8" transform="rotate(15 13 15)"/>
      <rect x="16.5" y="19" width="4" height="4" rx="1" fill="#e0f2fe" opacity="0.7" transform="rotate(-10 16.5 19)"/>
      <!-- Cup Rim -->
      <path d="M7 9H25" stroke="rgba(255,255,255,0.7)" stroke-width="1.8" stroke-linecap="round"/>
    </svg>`,

    // Es Jeruk / Citrus Drink
    citrus: `<svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="food-svg">
      <defs>
        <linearGradient id="ctr-orange" x1="8" y1="8" x2="24" y2="24" gradientUnits="userSpaceOnUse">
          <stop stop-color="#fb923c"/>
          <stop offset="1" stop-color="#ea580c"/>
        </linearGradient>
      </defs>
      <!-- Orange Slice -->
      <circle cx="16" cy="16" r="11" fill="url(#ctr-orange)" stroke="#f97316" stroke-width="1.2"/>
      <circle cx="16" cy="16" r="9" fill="#fed7aa" opacity="0.3"/>
      <!-- Segments -->
      <circle cx="16" cy="16" r="1.5" fill="#fff"/>
      <line x1="16" y1="8" x2="16" y2="24" stroke="#ffedd5" stroke-width="1.2"/>
      <line x1="8" y1="16" x2="24" y2="16" stroke="#ffedd5" stroke-width="1.2"/>
      <line x1="10.3" y1="10.3" x2="21.7" y2="21.7" stroke="#ffedd5" stroke-width="1.2"/>
      <line x1="10.3" y1="21.7" x2="21.7" y2="10.3" stroke="#ffedd5" stroke-width="1.2"/>
      <circle cx="16" cy="16" r="12" stroke="#fdba74" stroke-width="1.2" opacity="0.7"/>
    </svg>`,

    // Air Mineral / Water
    water: `<svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="food-svg">
      <defs>
        <linearGradient id="wtr-grad" x1="16" y1="6" x2="16" y2="26" gradientUnits="userSpaceOnUse">
          <stop stop-color="#38bdf8"/>
          <stop offset="1" stop-color="#0284c7"/>
        </linearGradient>
      </defs>
      <!-- Water Drop -->
      <path d="M16 5C16 5 8 15 8 20C8 24.4 11.6 28 16 28C20.4 28 24 24.4 24 20C24 15 16 5 16 5Z" fill="url(#wtr-grad)"/>
      <path d="M18.5 13C18.5 13 21.5 17 21.5 20C21.5 21.5 20.8 22.8 19.8 23.8" stroke="#e0f2fe" stroke-width="1.4" stroke-linecap="round" opacity="0.8"/>
      <circle cx="13" cy="22" r="1.5" fill="#e0f2fe" opacity="0.6"/>
    </svg>`,

    // Nasi Putih / Rice Bowl
    rice: `<svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="food-svg">
      <defs>
        <linearGradient id="rc-bowl" x1="6" y1="16" x2="26" y2="27" gradientUnits="userSpaceOnUse">
          <stop stop-color="#1e293b"/>
          <stop offset="1" stop-color="#0f172a"/>
        </linearGradient>
        <linearGradient id="rc-white" x1="8" y1="9" x2="24" y2="17" gradientUnits="userSpaceOnUse">
          <stop stop-color="#ffffff"/>
          <stop offset="1" stop-color="#e2e8f0"/>
        </linearGradient>
      </defs>
      <!-- Steaming Rice Mound -->
      <ellipse cx="16" cy="15" rx="9" ry="6" fill="url(#rc-white)"/>
      <path d="M9 15C9 10 13 8 16 8C19 8 23 10 23 15Z" fill="url(#rc-white)"/>
      <!-- Ceramic Bowl -->
      <path d="M6 15C6 22 10.5 26 16 26C21.5 26 26 22 26 15H6Z" fill="url(#rc-bowl)" stroke="#334155" stroke-width="1.2"/>
      <rect x="12" y="26" width="8" height="2" rx="1" fill="#334155"/>
      <!-- Bowl Accent Rim -->
      <line x1="6" y1="15" x2="26" y2="15" stroke="#f97316" stroke-width="1.4"/>
      <!-- Steam Wisps -->
      <path d="M13 6C12.5 5 13.5 4 13 3" stroke="#94a3b8" stroke-width="1" stroke-linecap="round" opacity="0.6"/>
      <path d="M16 5C15.5 4 16.5 3 16 2" stroke="#94a3b8" stroke-width="1" stroke-linecap="round" opacity="0.6"/>
      <path d="M19 6C18.5 5 19.5 4 19 3" stroke="#94a3b8" stroke-width="1" stroke-linecap="round" opacity="0.6"/>
    </svg>`,

    // Nasi Goreng / Fried Rice Wok
    fried_rice: `<svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="food-svg">
      <defs>
        <linearGradient id="fr-gold" x1="8" y1="11" x2="24" y2="20" gradientUnits="userSpaceOnUse">
          <stop stop-color="#f59e0b"/>
          <stop offset="0.6" stop-color="#d97706"/>
          <stop offset="1" stop-color="#b45309"/>
        </linearGradient>
      </defs>
      <!-- Fried Rice Mound -->
      <ellipse cx="16" cy="15" rx="8.5" ry="5.5" fill="url(#fr-gold)"/>
      <path d="M9.5 15C9.5 11 13 9 16 9C19 9 22.5 11 22.5 15Z" fill="url(#fr-gold)"/>
      <!-- Sunny Side Up Egg on top -->
      <ellipse cx="16" cy="13" rx="4" ry="2.5" fill="#ffffff" opacity="0.95"/>
      <circle cx="16" cy="13" r="1.6" fill="#f59e0b"/>
      <!-- Skillet / Plate Body -->
      <path d="M6 16C6 22 10.5 25.5 16 25.5C21.5 25.5 26 22 26 16H6Z" fill="#1e293b" stroke="#475569" stroke-width="1.2"/>
      <!-- Skillet Handle -->
      <path d="M25 18L29 19" stroke="#475569" stroke-width="2.5" stroke-linecap="round"/>
      <!-- Green Scallion flakes -->
      <rect x="12" y="14" width="1.2" height="1.2" fill="#22c55e" rx="0.3"/>
      <rect x="18" y="14" width="1.2" height="1.2" fill="#22c55e" rx="0.3"/>
      <rect x="15" y="16" width="1.2" height="1.2" fill="#ef4444" rx="0.3"/>
    </svg>`,

    // Sambal / Chili / Pedas
    chili: `<svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="food-svg">
      <defs>
        <linearGradient id="ch-red" x1="12" y1="6" x2="24" y2="26" gradientUnits="userSpaceOnUse">
          <stop stop-color="#ef4444"/>
          <stop offset="0.5" stop-color="#dc2626"/>
          <stop offset="1" stop-color="#991b1b"/>
        </linearGradient>
      </defs>
      <!-- Chili Stem -->
      <path d="M21 6C20 8 18 8 18 9" stroke="#16a34a" stroke-width="2.2" stroke-linecap="round"/>
      <path d="M16 9C17.5 7.5 19.5 7.5 21 8" stroke="#15803d" stroke-width="1.2"/>
      <!-- Chili Body Curved -->
      <path d="M18 9C19 12 18 16 16 20C14 24 10 26 8 26C8.5 24 9.5 22 10.5 19C12 15 13.5 11 16 9Z" fill="url(#ch-red)" stroke="#b91c1c" stroke-width="0.8"/>
      <!-- Gloss Highlight -->
      <path d="M16 11C16.8 13.5 16 16.5 14.5 19" stroke="#fca5a5" stroke-width="1" stroke-linecap="round" opacity="0.65"/>
    </svg>`,

    // Daging Mentah / Chicken Meat / Raw Material
    meat: `<svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="food-svg">
      <defs>
        <linearGradient id="mt-red" x1="8" y1="8" x2="24" y2="24" gradientUnits="userSpaceOnUse">
          <stop stop-color="#f87171"/>
          <stop offset="0.7" stop-color="#e11d48"/>
          <stop offset="1" stop-color="#be123c"/>
        </linearGradient>
      </defs>
      <path d="M7 16C7 10 11 7 16 7C22 7 26 11 25 17C24 23 18 25 12 24C8 23 7 19 7 16Z" fill="url(#mt-red)" stroke="#9f1239" stroke-width="1"/>
      <circle cx="18" cy="13" r="3" fill="#fff" opacity="0.8"/>
      <circle cx="18" cy="13" r="1.5" fill="#fecdd3"/>
      <path d="M11 14C12 16 14 17 17 18" stroke="#fecdd3" stroke-width="1.2" stroke-linecap="round" opacity="0.7"/>
    </svg>`,

    // Minyak Goreng / Bumbu Jar
    oil: `<svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="food-svg">
      <defs>
        <linearGradient id="ol-gold" x1="12" y1="12" x2="20" y2="26" gradientUnits="userSpaceOnUse">
          <stop stop-color="#facc15"/>
          <stop offset="1" stop-color="#eab308"/>
        </linearGradient>
      </defs>
      <rect x="13" y="5" width="6" height="3" rx="1" fill="#ea580c"/>
      <path d="M14 8H18L19 12H13L14 8Z" fill="#94a3b8"/>
      <rect x="10" y="12" width="12" height="15" rx="3" fill="url(#ol-gold)" stroke="#ca8a04" stroke-width="1"/>
      <ellipse cx="16" cy="18" rx="3.5" ry="4.5" fill="#fef08a" opacity="0.5"/>
    </svg>`,

    // Gelas Cup / Kemasan
    cup: `<svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="food-svg">
      <path d="M9 10L11.5 26C11.7 27 12.5 28 13.5 28H18.5C19.5 28 20.3 27 20.5 26L23 10H9Z" fill="#334155" stroke="#64748b" stroke-width="1.2"/>
      <rect x="7.5" y="8" width="17" height="3" rx="1.5" fill="#475569"/>
      <path d="M12 14H20L19.5 19H12.5L12 14Z" fill="#f97316" opacity="0.8"/>
    </svg>`,

    // Fork & Spoon / Lainnya
    utensils: `<svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="food-svg">
      <defs>
        <linearGradient id="ut-silver" x1="8" y1="8" x2="24" y2="24" gradientUnits="userSpaceOnUse">
          <stop stop-color="#e2e8f0"/>
          <stop offset="1" stop-color="#94a3b8"/>
        </linearGradient>
      </defs>
      <!-- Fork -->
      <path d="M11 6V13C11 14.5 12 15.5 13.5 15.5V26" stroke="url(#ut-silver)" stroke-width="1.6" stroke-linecap="round"/>
      <path d="M16 6V13C16 14.5 15 15.5 13.5 15.5" stroke="url(#ut-silver)" stroke-width="1.6" stroke-linecap="round"/>
      <line x1="13.5" y1="6" x2="13.5" y2="13" stroke="url(#ut-silver)" stroke-width="1.6"/>
      <!-- Spoon -->
      <path d="M21 6C19 6 18 8 18 11C18 13.5 19 15 20.5 15.5V26" stroke="url(#ut-silver)" stroke-width="1.6" stroke-linecap="round"/>
      <path d="M21 6C23 6 24 8 24 11C24 13.5 23 15 20.5 15.5" stroke="url(#ut-silver)" stroke-width="1.6" stroke-linecap="round"/>
    </svg>`,

    // General Food / Cloche
    food: `<svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="food-svg">
      <circle cx="16" cy="10" r="2" fill="#f97316"/>
      <path d="M7 21C7 14 11 11 16 11C21 11 25 14 25 21H7Z" fill="linear-gradient(135deg, #f97316, #ea580c)" stroke="#c2410c" stroke-width="1"/>
      <rect x="5" y="22" width="22" height="2.5" rx="1.2" fill="#cbd5e1"/>
    </svg>`
  };

  // Keyword / Emoji to Icon Mapping
  const ICON_MAP = {
    // Emojis
    '🍢': 'taichan',
    '🍗': 'chicken',
    '🧊': 'drink',
    '🥤': 'drink',
    '🍊': 'citrus',
    '💧': 'water',
    '🍚': 'rice',
    '🍳': 'fried_rice',
    '🌶': 'chili',
    '🌶️': 'chili',
    '🔥': 'chili',
    '🍴': 'utensils',
    '🍽️': 'food',
    '🍽': 'food',
    '🫙': 'oil',
    '🥩': 'meat',

    // Category / Product names
    'taichan': 'taichan',
    'sate': 'taichan',
    'chicken': 'chicken',
    'ayam': 'chicken',
    'minuman': 'drink',
    'es teh': 'drink',
    'es': 'drink',
    'jeruk': 'citrus',
    'orange': 'citrus',
    'air': 'water',
    'mineral': 'water',
    'nasi': 'rice',
    'nasgor': 'fried_rice',
    'goreng': 'fried_rice',
    'pedas': 'chili',
    'sambal': 'chili',
    'bumbu': 'chili',
    'daging': 'meat',
    'minyak': 'oil',
    'kemasan': 'cup',
    'cup': 'cup',
    'lainnya': 'utensils',
    'other': 'utensils',
    'food': 'food',
    'semua': 'food',
    'semua menu': 'food',
    'all': 'food'
  };

  /**
   * Resolve icon key from raw emoji, name, or key string
   */
  function resolveIconKey(input) {
    if (!input) return 'taichan';
    const str = String(input).trim().toLowerCase();

    // Direct ICONS key match
    if (ICONS[input]) return input;
    if (ICONS[str]) return str;

    // Direct emoji / key match
    if (ICON_MAP[input]) return ICON_MAP[input];
    if (ICON_MAP[str]) return ICON_MAP[str];

    // Substring searches
    if (str.includes('semua') || str.includes('all') || str === 'food') return 'food';
    if (str.includes('taichan') || str.includes('sate') || str.includes('tusuk')) return 'taichan';
    if (str.includes('chicken') || str.includes('ayam') || str.includes('crispy')) return 'chicken';
    if (str.includes('jeruk') || str.includes('citrus') || str.includes('orange')) return 'citrus';
    if (str.includes('air') || str.includes('mineral') || str.includes('aqua')) return 'water';
    if (str.includes('nasgor') || str.includes('nasi goreng')) return 'fried_rice';
    if (str.includes('nasi') || str.includes('rice')) return 'rice';
    if (str.includes('teh') || str.includes('es ') || str.includes('minum') || str.includes('drink')) return 'drink';
    if (str.includes('sambal') || str.includes('pedas') || str.includes('chili') || str.includes('bumbu')) return 'chili';
    if (str.includes('daging') || str.includes('meat')) return 'meat';
    if (str.includes('minyak') || str.includes('oil')) return 'oil';
    if (str.includes('gelas') || str.includes('cup')) return 'cup';

    return null;
  }

  /**
   * Main FoodIcons Object
   */
  const FoodIcons = {
    /**
     * Get raw SVG string by key
     */
    getSvg(key, size = 28) {
      const resolved = resolveIconKey(key) || 'food';
      const svg = ICONS[resolved] || ICONS.food;
      if (size && size !== 32) {
        return svg.replace('<svg ', `<svg width="${size}" height="${size}" `);
      }
      return svg;
    },

    /**
     * Render an elegant wrapped food icon badge
     * @param {string} input - emoji, category, or product name
     * @param {object} opts - { size: 36, class: '', badge: '', style: '' }
     * @returns {string} HTML markup
     */
    get(input, opts = {}) {
      const size = opts.size || 36;
      const extraClass = opts.class || '';
      const customStyle = opts.style || '';
      const resolvedKey = resolveIconKey(input);

      if (resolvedKey && ICONS[resolvedKey]) {
        const svgContent = ICONS[resolvedKey].replace(
          '<svg ',
          `<svg width="${Math.round(size * 0.72)}" height="${Math.round(size * 0.72)}" `
        );
        return `
          <div class="food-icon-wrap food-icon-${resolvedKey} ${extraClass}" style="width:${size}px;height:${size}px;${customStyle}">
            ${svgContent}
            ${opts.badge ? `<span class="food-icon-badge">${opts.badge}</span>` : ''}
          </div>
        `;
      }

      // Fallback for custom user emoji: wrap inside luxury badge
      const emojiChar = input || '🍢';
      const fontSize = Math.round(size * 0.52);
      return `
        <div class="food-icon-wrap food-icon-custom ${extraClass}" style="width:${size}px;height:${size}px;${customStyle}">
          <span class="food-icon-emoji" style="font-size:${fontSize}px;">${emojiChar}</span>
          ${opts.badge ? `<span class="food-icon-badge">${opts.badge}</span>` : ''}
        </div>
      `;
    },

    /**
     * Category Icon pill snippet
     */
    getCategoryIcon(cat, size = 20) {
      const key = resolveIconKey(cat.emj || cat.nm || cat.id);
      return this.get(key || cat.emj, { size, class: 'cat-pill-icon' });
    }
  };

  window.FoodIcons = FoodIcons;

})(window);
