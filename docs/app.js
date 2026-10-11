// docs/app.js - Interactive Motion & Live Release Dynamic Updater

document.addEventListener('DOMContentLoaded', () => {
  initNavbarScroll();
  initPhone3DTilt();
  initPhoneScreenSwitcher();
  initCursorAmbientGlow();
  fetchLatestReleaseInfo();
});

// 1. Navbar Glassmorphic Scroll Effect
function initNavbarScroll() {
  const header = document.querySelector('header');
  window.addEventListener('scroll', () => {
    if (window.scrollY > 40) {
      header.classList.add('scrolled');
    } else {
      header.classList.remove('scrolled');
    }
  }, { passive: true });
}

// 2. 3D Mouse Parallax Tilt on Phone Mockup
function initPhone3DTilt() {
  const phone = document.getElementById('interactivePhone');
  if (!phone) return;

  const showcase = document.querySelector('.phone-showcase');
  let isHovered = false;

  showcase.addEventListener('mouseenter', () => {
    isHovered = true;
  });

  showcase.addEventListener('mousemove', (e) => {
    const rect = showcase.getBoundingClientRect();
    const x = e.clientX - rect.left - rect.width / 2;
    const y = e.clientY - rect.top - rect.height / 2;

    const rotX = -(y / (rect.height / 2)) * 14;
    const rotY = (x / (rect.width / 2)) * 14;

    phone.style.transform = `rotateX(${rotX.toFixed(2)}deg) rotateY(${rotY.toFixed(2)}deg) scale3d(1.02, 1.02, 1.02)`;
  });

  showcase.addEventListener('mouseleave', () => {
    isHovered = false;
    // Spring back to subtle default perspective
    phone.style.transform = 'rotateX(6deg) rotateY(-8deg) scale3d(1, 1, 1)';
  });

  // Set initial tilt
  phone.style.transform = 'rotateX(6deg) rotateY(-8deg) scale3d(1, 1, 1)';
}

// 3. Interactive Phone Screen Switcher (Attendance, Timetable, Exams, OCR)
function initPhoneScreenSwitcher() {
  const tabBtns = document.querySelectorAll('.tab-btn');
  const screens = {
    attendance: document.getElementById('screenAttendance'),
    timetable: document.getElementById('screenTimetable'),
    exams: document.getElementById('screenExams'),
    ocr: document.getElementById('screenOcr')
  };

  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const target = btn.getAttribute('data-target');

      // Update button active state
      tabBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');

      // Hide all screens & show target with smooth fade
      Object.entries(screens).forEach(([key, screenEl]) => {
        if (!screenEl) return;
        if (key === target) {
          screenEl.style.display = 'block';
          screenEl.style.opacity = '0';
          screenEl.style.transform = 'translateY(8px)';
          setTimeout(() => {
            screenEl.style.transition = 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)';
            screenEl.style.opacity = '1';
            screenEl.style.transform = 'translateY(0)';
          }, 10);
        } else {
          screenEl.style.display = 'none';
        }
      });
    });
  });
}

// 4. Subtle Cursor Follower Glow
function initCursorAmbientGlow() {
  const glow = document.querySelector('.ambient-glow-1');
  if (!glow) return;

  let mouseX = window.innerWidth / 2;
  let mouseY = 150;
  let currentX = mouseX;
  let currentY = mouseY;

  window.addEventListener('mousemove', (e) => {
    mouseX = e.clientX;
    mouseY = e.clientY;
  }, { passive: true });

  function animate() {
    currentX += (mouseX - currentX) * 0.05;
    currentY += (mouseY - currentY) * 0.05;
    glow.style.left = `${currentX}px`;
    glow.style.top = `${currentY - 200}px`;
    requestAnimationFrame(animate);
  }
  animate();
}

// 5. Dynamic Fetch from GitHub Releases
async function fetchLatestReleaseInfo() {
  try {
    const res = await fetch('https://api.github.com/repos/senkuthegreat/NSync/releases/latest');
    if (!res.ok) return;

    const data = await res.json();
    const tagName = data.tag_name || 'v1.0.10';

    // Update release tags on page
    const versionBadges = document.querySelectorAll('.dynamic-version');
    versionBadges.forEach(el => {
      el.textContent = tagName;
    });

    // Find latest APK download URL and size
    const apkAsset = data.assets?.find(a => a.name.endsWith('.apk'));
    if (apkAsset && apkAsset.browser_download_url) {
      const downloadLinks = document.querySelectorAll('.dynamic-download-link');
      downloadLinks.forEach(link => {
        link.href = apkAsset.browser_download_url;
      });

      const sizeMB = (apkAsset.size / (1024 * 1024)).toFixed(1);
      const metaEls = document.querySelectorAll('.dynamic-apk-size');
      metaEls.forEach(el => {
        el.textContent = `Android 7.0+ • ~${sizeMB} MB • Free & Open Source`;
      });
    }
  } catch (err) {
    console.warn('Using cached release info:', err);
  }
}
