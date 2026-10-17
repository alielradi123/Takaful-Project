// supabase-config.js — إعداد Supabase لمشروع تكافل
// ─────────────────────────────────────────────────────────
// 🔧 الإعداد المطلوب من المستخدم:
//    1. اذهب إلى لوحة Supabase → Settings → API
//    2. انسخ "Project URL" واستبدل SUPABASE_URL
//    3. انسخ "anon public" key واستبدل SUPABASE_ANON_KEY
// ─────────────────────────────────────────────────────────

export const SUPABASE_URL = 'https://akvcfzbhyjwmpvbchsrl.supabase.co';  // ← ضع URL مشروعك هنا
export const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFrdmNmemJoeWp3bXB2YmNoc3JsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI5MDY3NDgsImV4cCI6MjA5ODQ4Mjc0OH0.QOvfSzcvjSS_S7ROfHQi_XPDkPJ7LkCoEV7QmK8dN7k';             // ← ضع المفتاح هنا

// اسم الـ Bucket الذي أنشأته في Supabase Storage
export const STORAGE_BUCKET = 'takaful-media';

/**
 * رفع صورة إلى Supabase Storage
 * @param {File}   file       - ملف الصورة
 * @param {string} path       - المسار داخل الـ Bucket (مثال: profile_pictures/uid.jpg)
 * @returns {Promise<string>} - الرابط العام للصورة
 */
export async function uploadToSupabase(file, path) {
  const safePath = path.split('/').map(encodeURIComponent).join('/');
  const uploadUrl = `${SUPABASE_URL}/storage/v1/object/${STORAGE_BUCKET}/${safePath}`;
  const controller = new AbortController();
  const uploadTimeoutMs = Math.min(
    5 * 60 * 1000,
    Math.max(2 * 60 * 1000, 30 * 1000 + Math.ceil(file.size / (256 * 1024)) * 1000)
  );
  const timeoutId = setTimeout(() => controller.abort(), uploadTimeoutMs);
  let response;

  try {
    response = await fetch(uploadUrl, {
      method: 'POST',
      headers: {
        'apikey': SUPABASE_ANON_KEY,
        'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        'Content-Type': file.type || 'application/octet-stream',
        'x-upsert': 'true',
      },
      body: file,
      signal: controller.signal,
    });
  } catch (error) {
    if (error.name === 'AbortError') {
      throw new Error('انتهت مهلة رفع الملف إلى التخزين. جرّب ملفاً أصغر أو تحقق من الاتصال ثم أعد المحاولة.');
    }
    throw new Error(`تعذر الاتصال بتخزين Supabase: ${error.message}`);
  } finally {
    clearTimeout(timeoutId);
  }

  if (!response.ok) {
    const responseText = await response.text().catch(() => '');
    let message = responseText || response.statusText;
    try { message = JSON.parse(responseText).message || message; } catch (error) {}
    throw new Error(`Supabase upload failed (${response.status}): ${message}`);
  }

  return `${SUPABASE_URL}/storage/v1/object/public/${STORAGE_BUCKET}/${safePath}`;
}

/**
 * رفع ملفات متعددة إلى Supabase Storage
 * @param {Array<File>} files - قائمة ملفات
 * @param {string} folder     - المجلد الأساسي
 * @returns {Promise<Array<string>>}
 */
export async function uploadMultipleToSupabase(files, folder) {
  return Promise.all(
    Array.from(files).map((file, index) => {
      // Sanitize file name to avoid upload errors with Arabic or special characters
      const ext = file.name.match(/\.([a-z0-9]{1,8})$/i)?.[1] || file.type.split('/')[1]?.replace(/[^a-z0-9]/gi, '') || 'bin';
      const safeName = `file_${Math.random().toString(36).substring(2, 8)}.${ext}`;
      return uploadToSupabase(file, `${folder}/${Date.now()}_${index}_${safeName}`);
    })
  );
}

/**
 * استخراج المسار من رابط Supabase العام
 * @param {string} url - الرابط العام
 * @returns {string|null} - المسار داخل الـ Bucket
 */
export function getPathFromSupabaseUrl(url) {
  if (!url || typeof url !== 'string') return null;
  const prefix = `/storage/v1/object/public/${STORAGE_BUCKET}/`;
  const index = url.indexOf(prefix);
  return index !== -1 ? url.substring(index + prefix.length) : null;
}

/**
 * حذف صورة من Supabase Storage
 * @param {string} pathOrUrl - المسار أو الرابط الكامل للصورة
 */
export async function deleteFromSupabase(pathOrUrl) {
  const path = pathOrUrl.startsWith('http') ? getPathFromSupabaseUrl(pathOrUrl) : pathOrUrl;
  if (!path) return;

  const deleteUrl = `${SUPABASE_URL}/storage/v1/object/${STORAGE_BUCKET}/${path}`;
  await fetch(deleteUrl, {
    method: 'DELETE',
    headers: { 'Authorization': `Bearer ${SUPABASE_ANON_KEY}` },
  });
}
