// maintenance-check.js — فحص وضع الصيانة عبر Firestore REST API
// يعمل بدون الحاجة لتسجيل دخول (قراءة عامة)

(function () {
  const PROJECT_ID = 'takaful-f662e';
  const API_KEY = 'AIzaSyDuWfztq7byGdV72cLPdNr3hd7PE25SIFc';
  const SETTINGS_URL = `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents/settings/general?key=${API_KEY}`;
  const POLL_INTERVAL = 30000; // 30 ثانية

  // ── إنشاء شاشة الصيانة ──
  function showMaintenanceScreen() {
    if (document.getElementById('maintenance-overlay')) return;
    const overlay = document.createElement('div');
    overlay.id = 'maintenance-overlay';
    overlay.style.cssText = `
      position: fixed; top: 0; left: 0; width: 100%; height: 100%;
      background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%);
      z-index: 999999;
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      color: white; text-align: center; padding: 20px;
      font-family: 'Tajawal', 'Segoe UI', Tahoma, sans-serif;
      direction: rtl;
    `;
    overlay.innerHTML = `
      <div style="background: rgba(239,68,68,0.12); border: 1px solid rgba(239,68,68,0.3); padding: 28px; border-radius: 50%; margin-bottom: 28px; box-shadow: 0 0 40px rgba(239,68,68,0.2);">
        <svg width="60" height="60" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
        </svg>
      </div>
      <h1 style="font-size: 2rem; font-weight: 900; margin-bottom: 16px; color: #f1f5f9; letter-spacing: -0.5px;">
        الموقع تحت الصيانة
      </h1>
      <p style="font-size: 1.05rem; color: #94a3b8; max-width: 480px; line-height: 1.75; margin-bottom: 32px;">
        نأسف للإزعاج. نقوم حالياً ببعض التحديثات الضرورية لتحسين تجربتك.<br>
        يرجى المحاولة مرة أخرى قريباً.
      </p>
      <div style="display: flex; align-items: center; gap: 10px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); padding: 12px 24px; border-radius: 50px; font-size: 0.85rem; color: #64748b;">
        <span style="width: 8px; height: 8px; background: #ef4444; border-radius: 50%; animation: blink 1.2s ease-in-out infinite;"></span>
        صيانة مجدولة — نعود قريباً
      </div>
      <style>
        @keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
      </style>
    `;
    document.body.appendChild(overlay);
    document.body.style.overflow = 'hidden';
  }

  // ── إزالة شاشة الصيانة ──
  function hideMaintenanceScreen() {
    const overlay = document.getElementById('maintenance-overlay');
    if (overlay) {
      overlay.remove();
      document.body.style.overflow = '';
    }
  }

  // ── فحص حالة الصيانة عبر REST API ──
  async function checkMaintenance() {
    try {
      const res = await fetch(SETTINGS_URL);
      if (!res.ok) return; // تجاهل أخطاء الشبكة
      const data = await res.json();
      const fields = data.fields || {};
      const webMaintenance = fields.web_maintenance_mode?.booleanValue === true;

      if (webMaintenance) {
        showMaintenanceScreen();
      } else {
        hideMaintenanceScreen();
      }
    } catch (err) {
      // لا نفعل شيئاً عند خطأ الشبكة — لا نريد حجب الموقع بسبب مشكلة اتصال
      console.warn('[Maintenance Check] Network error, skipping:', err.message);
    }
  }

  // ── تشغيل الفحص الأول فور تحميل الصفحة ──
  checkMaintenance();

  // ── فحص دوري كل 30 ثانية ──
  setInterval(checkMaintenance, POLL_INTERVAL);
})();
