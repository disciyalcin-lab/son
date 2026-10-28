import { QuranWord } from '../data/wordsData';

/**
 * Strips all Arabic diacritics (tashkeel/harakat: fathah, dammah, kasrah,
 * sukun, shaddah, tanwin, superscript alef, maddah, Quranic annotation marks),
 * tatweel/kashida, and normalizes Alef variants (أ, إ, آ, ٱ -> ا),
 * Ta Marbuta (ة -> ه), Alef Maqsura (ى -> ي), and Hamza carriers (ؤ -> و, ئ -> ي).
 */
export function normalizeArabic(text: string): string {
  if (!text) return '';
  return text
    .normalize('NFKC')
    // Remove Arabic harakat, tashkeel, tanwin, superscript alef, and Quranic symbols
    .replace(/[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640]/g, '')
    // Normalize all Alef forms (أ, إ, آ, ٱ, ٲ, ٳ, ٵ) to bare Alef (ا)
    .replace(/[أإآٱٲٳٵ]/g, 'ا')
    // Normalize Ta Marbuta (ة) to Ha (ه)
    .replace(/ة/g, 'ه')
    // Normalize Alef Maqsura (ى) and Farsi Yeh (ی) to Arabic Yeh (ي)
    .replace(/[ىی]/g, 'ي')
    // Normalize Waw with Hamza (ؤ) to Waw (و) and Yeh with Hamza (ئ) to Yeh (ي)
    .replace(/ؤ/g, 'و')
    .replace(/ئ/g, 'ي')
    // Normalize Kaf variants (ک) to Arabic Kaf (ك)
    .replace(/ک/g, 'ك')
    // Remove standalone hamza (ء) when comparing stems or keep clean spacing
    .replace(/[^\u0621-\u064A\s]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

/**
 * Normalizes Turkish strings preserving i-İ and ı-I rules via 'tr-TR' locale.
 */
export function normalizeTurkish(text: string): string {
  if (!text) return '';
  return text.toLocaleLowerCase('tr-TR').trim();
}

/**
 * Multilingual search matcher across Arabic (vowelized or unvowelized),
 * Turkish meaning, transliteration,wordType, and word ID.
 */
export function matchesMultilingualSearch(word: QuranWord, rawQuery: string): boolean {
  const trimmed = rawQuery.trim();
  if (!trimmed) return true;

  // 1. Direct ID match
  if (String(word.id) === trimmed || `#${word.id}` === trimmed) {
    return true;
  }

  // 2. Turkish & Transliteration match using tr-TR locale lowercase
  const trQuery = normalizeTurkish(trimmed);
  if (
    normalizeTurkish(word.turkishMeaning).includes(trQuery) ||
    normalizeTurkish(word.transliteration).includes(trQuery) ||
    normalizeTurkish(word.wordType).includes(trQuery)
  ) {
    return true;
  }

  // 3. Normalized Arabic match (handles both vowelized and unvowelized input)
  const arQuery = normalizeArabic(trimmed);
  if (arQuery.length > 0) {
    const normalizedWordArabic = normalizeArabic(word.arabic);
    const normalizedAyahArabic = normalizeArabic(word.exampleAyahArabic);
    if (
      normalizedWordArabic.includes(arQuery) ||
      arQuery.includes(normalizedWordArabic) ||
      normalizedAyahArabic.includes(arQuery)
    ) {
      return true;
    }
  }

  return false;
}

/**
 * Strips common Arabic proclitics (prefixes like ال, و, ف, ب, ل, ك)
 * and enclitics (pronoun/plural suffixes) to compare inflected Quranic forms.
 */
function stripArabicAffixes(normalizedWord: string): string {
  let stem = normalizedWord.trim();
  // Strip common prefixes: وال, فال, بال, كال, لل, ال, و, ف, ب, ل, ك, س
  stem = stem.replace(/^(?:[وفبلك]?ال|لل|[وفبلكس])(?=.{2,})/, '');
  // Strip common suffixes: كم, هم, هن, نا, ها, ه, ك, ي, ون, ين, ات, ان, وا, تم, ت
  stem = stem.replace(/(?:كم|هم|هن|هما|نا|ها|ون|ين|ات|ان|وا|تم|ت|ه|ك|ي)(?=.{0}$)/, '');
  return stem;
}

/**
 * Extracts weak-letter-free consonantal skeleton for Arabic root matching
 * (e.g., قال -> قل, يقولون -> قل, كان -> كن, آمنوا -> امن).
 */
function extractConsonantSkeleton(normalizedWord: string): string {
  const stripped = stripArabicAffixes(normalizedWord);
  // Remove weak/long vowels (ا, و, ي) from interior to match hollow/defective verb conjugations
  const consonants = stripped.replace(/[اويء]/g, '');
  return consonants.length >= 2 ? consonants : stripped;
}

export interface AyahSegment {
  text: string;
  isMatch: boolean;
}

/**
 * Splits an Arabic ayah into segments and highlights any token that matches
 * the target Quranic word (including inflected, prefixed, or suffixed forms).
 */
export function splitAyahWithHighlight(
  ayahArabic: string,
  targetWordArabic: string
): AyahSegment[] {
  if (!ayahArabic) return [];
  const normTarget = normalizeArabic(targetWordArabic);
  if (!normTarget) return [{ text: ayahArabic, isMatch: false }];

  const targetStem = stripArabicAffixes(normTarget);
  const targetNoAl = normTarget.replace(/^ال/, '');
  const targetSkeleton = extractConsonantSkeleton(normTarget);

  // Split by whitespace while keeping the tokens clean
  const rawTokens = ayahArabic.split(/(\s+)/);

  // First pass: check direct / stem / affix match
  const matchedIndices = new Set<number>();

  rawTokens.forEach((token, idx) => {
    if (/^\s+$/.test(token)) return;
    const normToken = normalizeArabic(token);
    if (!normToken) return;

    const tokenNoAl = normToken.replace(/^ال/, '');
    const tokenStem = stripArabicAffixes(normToken);

    const isDirectOrAffixMatch =
      normToken === normTarget ||
      tokenNoAl === targetNoAl ||
      (targetStem.length >= 2 && tokenStem === targetStem) ||
      (targetNoAl.length >= 2 && normToken.includes(targetNoAl)) ||
      (targetStem.length >= 2 && normToken.includes(targetStem));

    if (isDirectOrAffixMatch) {
      matchedIndices.add(idx);
    }
  });

  // Fallback pass: if no token matched directly (e.g., broken plural or conjugated hollow verb),
  // match by consonantal root skeleton
  if (matchedIndices.size === 0 && targetSkeleton.length >= 2) {
    rawTokens.forEach((token, idx) => {
      if (/^\s+$/.test(token)) return;
      const normToken = normalizeArabic(token);
      if (!normToken) return;
      const tokenSkeleton = extractConsonantSkeleton(normToken);
      if (
        tokenSkeleton.length >= 2 &&
        (tokenSkeleton === targetSkeleton ||
          (targetSkeleton.length >= 2 && tokenSkeleton.includes(targetSkeleton)))
      ) {
        matchedIndices.add(idx);
      }
    });
  }

  return rawTokens.map((token, idx) => ({
    text: token,
    isMatch: matchedIndices.has(idx)
  }));
}
